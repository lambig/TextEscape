"""Fail CI when generated documentation artifacts are empty or contain bytecode."""
from pathlib import Path
from zipfile import ZipFile

for classifier, suffix in (("javadoc", ".html"), ("sources", ".java")):
    archives = sorted(Path("build/libs").glob(f"*-{classifier}.jar"))
    if len(archives) != 1:
        raise SystemExit(f"Expected one {classifier} JAR, found {len(archives)}")
    with ZipFile(archives[0]) as archive:
        names = [entry.filename for entry in archive.infolist() if not entry.is_dir()]
    content = sum(name.endswith(suffix) for name in names)
    bytecode = sum(name.endswith(".class") for name in names)
    if content == 0 or bytecode != 0:
        raise SystemExit(
            f"{archives[0]}: expected {suffix} entries > 0 and .class entries = 0; "
            f"found {content} and {bytecode}"
        )
    print(f"{archives[0]}: {suffix}={content}, .class={bytecode}")
