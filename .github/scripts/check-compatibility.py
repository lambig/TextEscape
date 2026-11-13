"""Compare the public API and run a client compiled against the published 0.1.0."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import urllib.request
from zipfile import ZipFile
import xml.etree.ElementTree as ET

candidate = Path(sys.argv[1]).resolve()
work = Path('build/compatibility').resolve()
work.mkdir(parents=True, exist_ok=True)
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
assert ET.parse(candidate.with_suffix('.pom')).findall('.//m:dependency', ns) == []
module = json.loads(candidate.with_suffix('.module').read_text())
for variant in module['variants']:
    assert not variant.get('dependencies'), variant['name']
    if variant['name'] in ('apiElements', 'runtimeElements'):
        assert variant['attributes']['org.gradle.jvm.version'] == 11
    for f in variant['files']:
        data = (candidate.parent / f['url']).read_bytes()
        assert f['size'] == len(data)
        for alg in ('sha512', 'sha256', 'sha1', 'md5'):
            if alg in f:
                assert hashlib.new(alg, data).hexdigest() == f[alg]

url = 'https://repo.maven.apache.org/maven2/io/github/lambig/TextEscape/0.1.0/TextEscape-0.1.0.jar'
baseline = work / 'TextEscape-0.1.0.jar'
baseline.write_bytes(urllib.request.urlopen(url, timeout=60).read())
assert hashlib.sha1(baseline.read_bytes()).hexdigest() == urllib.request.urlopen(url + '.sha1', timeout=60).read().decode().strip()
java_home = Path(os.environ['JAVA_HOME'])
runtime = Path(os.environ.get('TEST_JAVA_HOME', os.environ['JAVA_HOME'])) / 'bin/java'

def classes(jar):
    with ZipFile(jar) as archive:
        names = sorted(n for n in archive.namelist() if n.endswith('.class'))
        for name in names:
            assert int.from_bytes(archive.read(name)[6:8], 'big') == 55, name
    return [n[:-6].replace('/', '.') for n in names]

types = classes(baseline)
assert types == classes(candidate)
for name in types:
    apis = []
    for label, jar in [('baseline', baseline), ('candidate', candidate)]:
        api = subprocess.check_output([str(java_home / 'bin/javap'), '-public', '-s', '-classpath', str(jar), name], text=True)
        (work / (name + '-' + label + '-api.txt')).write_text(api, encoding='utf-8')
        apis.append(sorted(line.strip() for line in api.splitlines() if not line.startswith('Compiled from')))
    assert apis[0] == apis[1], name + ': public signature changed'

client = work / 'client'
client.mkdir(exist_ok=True)
source = Path('example/src/main/java/example/CompatibilityExample.java')
subprocess.run([str(java_home / 'bin/javac'), '--release', '11', '-cp', str(baseline), '-d', str(client), str(source)], check=True)
for jar in (baseline, candidate):
    result = subprocess.check_output([str(runtime), '-cp', os.pathsep.join(map(str, (client, jar))), 'example.CompatibilityExample'], text=True)
    assert result.strip() == 'existing API passed'
print(f'Public signatures preserved: {len(types)} classes; Java 11 bytecode=55')
print(f'Baseline SHA256: {hashlib.sha256(baseline.read_bytes()).hexdigest()}')
print('Old compiled client passed; runtime dependencies empty; module file hashes match')
