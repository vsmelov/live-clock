# tools

`actions.py` is the single source of truth for the action catalogue: values,
copy in both languages, evidence and allowances. Everything else is generated
from it, which is what keeps the two locales from drifting apart.

```bash
python3 tools/gen.py      # res/values/strings.xml and res/values-ru/strings.xml
python3 tools/gen_kt.py   # domain/EventType.kt
```

Checks, both of which are cheap to run before committing:

```bash
python3 tools/check_locales.py   # same keys and same format arguments in both locales
python3 tools/check_ascii.py     # no Cyrillic left in Kotlin sources
```

Adding an action means one entry in `actions.py`, one constant in
`Coefficients.kt`, and a regeneration. Do not hand-edit `EventType.kt` or the
generated string files — the next regeneration would discard the change.
