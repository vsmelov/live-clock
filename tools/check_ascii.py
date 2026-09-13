#!/usr/bin/env python3
"""Reports Cyrillic left in Kotlin sources. Strings in res/values-ru are exempt."""
import pathlib, sys, unicodedata

def has_cyrillic(text):
    return any("CYRILLIC" in unicodedata.name(ch, "") for ch in text)

root = pathlib.Path(__file__).resolve().parent.parent / "app/src"
bad = []
for path in sorted(root.rglob("*.kt")):
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if has_cyrillic(line):
            bad.append((path.relative_to(root), number, line.strip()[:90]))
if bad:
    print("Cyrillic left in %d places:" % len(bad))
    for path, number, line in bad[:40]:
        print("  %s:%d  %s" % (path, number, line))
    if len(bad) > 40:
        print("  ... and %d more" % (len(bad) - 40))
    sys.exit(1)
print("Kotlin sources are clean")
