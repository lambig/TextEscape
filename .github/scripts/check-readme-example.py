"""The README must use the same tested consumer source and output."""
from pathlib import Path
import re
import sys

readme = Path('README.md').read_text(encoding='utf-8')
programs = re.findall(r'```java\n(.*?)```', readme, re.S)
assert programs == [Path('example/src/main/java/example/TextEscapeExample.java').read_text(encoding='utf-8')]
expected = re.findall(r'```text\n(.*?)```', readme, re.S)[0].strip()
for build in ('gradle', 'maven'):
    log = Path('build/consumer-results') / sys.argv[1] / (build + '.log')
    assert expected in log.read_text(encoding='utf-8'), str(log)
print('README source and output match both independent consumers')
