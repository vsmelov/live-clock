#!/usr/bin/env python3
"""Checks that the two locales cover the same keys and the same format arguments."""
import re, sys, pathlib

def load(path):
    text = pathlib.Path(path).read_text(encoding="utf-8")
    out = {}
    for m in re.finditer(r'<string name="([^"]+)"([^>]*)>(.*?)</string>', text, re.S):
        out[m.group(1)] = (m.group(3), "translatable=\"false\"" in m.group(2))
    return out

base = str(pathlib.Path(__file__).resolve().parent.parent / "app/src/main/res")
en = load(base + "/values/strings.xml")
ru = load(base + "/values-ru/strings.xml")

problems = []
translatable = {k: v for k, v in en.items() if not v[1]}
missing = sorted(set(translatable) - set(ru))
extra = sorted(set(ru) - set(en))
if missing:
    problems.append("missing in ru: " + ", ".join(missing))
if extra:
    problems.append("present in ru but not en: " + ", ".join(extra))

spec = re.compile(r"%\d+\$[a-z]")
for key in sorted(set(translatable) & set(ru)):
    a = sorted(spec.findall(en[key][0]))
    b = sorted(spec.findall(ru[key][0]))
    if a != b:
        problems.append("format args differ in %s: %s vs %s" % (key, a, b))

if problems:
    print("Locale problems:")
    for p in problems:
        print("  " + p)
    sys.exit(1)
print("Locales match: %d translatable keys, %d untranslated (search terms)"
      % (len(translatable), len(en) - len(translatable)))
