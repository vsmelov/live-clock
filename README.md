# live-clock

A home screen widget showing how much life is left — and moving that number
according to what you do.

The model is microlives (David Spiegelhalter): one microlife is half an hour of
life expectancy. Two cigarettes cost roughly one microlife; twenty minutes of
exercise gives roughly one back.

```
          ┌──────────────────────────────┐
          │      17310d 1:56:26          │  ← days on a schedule, seconds on their own
          │  👶 ▓▓▓▓▓▓▓░░░░░ 💀 40.671%   │  ← the fraction already lived
          │  47.3918 years · today −45m  │
          │  ┌────────┐┌────────┐┌─────┐ │
          │  │ Smoked ││ Rested ││Coffee│ │  ← pinned actions
          │  └────────┘└────────┘└─────┘ │
          └──────────────────────────────┘
```

Works entirely offline. The database on the device is the source of truth;
uploading to a server is off by default and decides nothing.

Interface in English or Russian, switchable in the app.

## Building

- JDK 17 or newer
- Android SDK: platform `android-36` and `build-tools;36.0.0`
- Android Studio is not required

The SDK path comes from `ANDROID_HOME` or from `local.properties` in the project
root (gitignored — create your own):

```properties
sdk.dir=/path/to/android-sdk
```

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk, 31 MiB
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk, 3.6 MiB
```

Use the release build for a phone: R8 makes it nine times smaller. It is signed
with the same debug key as the debug build — for a personal app installed over
USB a separate keystore is one more thing to lose, and losing it means being
unable to update an installed build. Put your own key in `signingConfigs` in
`app/build.gradle.kts` if it ever matters.

R8 renames classes, and Glance and WorkManager look some of them up by name, so
`proguard-rules.pro` keeps `ActionCallback`, `GlanceAppWidget`,
`GlanceAppWidgetReceiver` and `ListenableWorker`. Without those the widget
buttons would silently stop working — and only in release, where it is hardest
to notice.

The build runs with `allWarningsAsErrors`: any Kotlin warning fails it.

## Installing over USB

1. On the phone: Settings → About phone → tap Build number seven times.
2. Settings → Developer options → enable USB debugging.
3. Connect the cable and allow debugging for this computer.

```bash
adb devices                                                  # must show "device"
adb install -r app/build/outputs/apk/release/app-release.apk
```

Or `./gradlew installDebug` to build and install in one step. If installation
fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, a build signed with a different
key is already installed: `adb uninstall com.vsmelov.liveclock`.

### Adding the widget

Long-press an empty spot on the home screen → Widgets → Live Clock. It lays out
from 2×2 upwards: on a narrow one the type shrinks and the emoji leaves the
buttons; on a tall one a second row of buttons and a summary line appear.

## Where the numbers come from

Every value lives in `Coefficients.kt` and carries a proof in `EventType.kt`,
shown in the app behind the "i" button next to each action. Sources come in five
grades, visible in the list without opening anything:

| Grade | Meaning |
|---|---|
| Solid | meta-analysis or a very large cohort, effect holds up |
| Moderate | one large study, or studies that disagree |
| Weak | small or old study, reverse causation likely |
| Estimate | direction known, size set by hand |
| No effect | checked, no influence on mortality found |
| Your value | chosen by you, not by a study |

Values absent from the ready-made microlives table are computed by the formula
from the same paper — `Microlives.minutesPerDay(hazardRatio)`:

```
microlives per day = −10.9 × ln(HR)     (men; −9.3 for women)
```

Checked against the published rows: red meat HR 1.13 gives −1.33 microlives
(table says −1), vegetables HR 0.66 gives +4.53 (+4), exercise HR 0.81 gives
+2.30 (+2). So a new action needs no invention: take the hazard ratio from the
study and put it in.

The authors only promise the approximation for HR between 0.75 and 1.3;
`Microlives.isTrusted` returns false outside that. Sauna, cycling and running sit
outside it, which is why they are the largest numbers in the list and almost
certainly inflated.

### Allowance per period

The epidemiology almost everywhere measures an EXTRA serving on top of habit,
not every serving. Meat twice a week is the ordinary background the baseline
forecast was computed from; charging for it is wrong on the merits and useless as
a signal.

So some actions carry an allowance: inside it one price, beyond it another.

| Action | Allowance | Within | Beyond |
|---|---|---|---|
| Red meat | 3 per week | 0 | −30 |
| Fast food | 1 per week | 0 | −30 |
| Sugary drink | 2 per week | 0 | −20 |
| Alcohol | 1 per day | **+30** | −30 |
| Sweets | 1 per day | 0 | −10 |
| Screen time | 2 per day | 0 | −15 |
| Coffee | 3 per day | +10 | 0 |
| Vegetables | 5 per day | +24 | 0 |
| Nuts | 1 per day | +73 | 0 |

Alcohol is not indulgence here but a return to the source: in the table the first
drink of the day goes up and only the following ones go down. Benefits hit a
ceiling symmetrically — a fourth cup of coffee gives nothing and takes nothing.

Cigarettes have no allowance on purpose: there is no safe dose.

The price is computed at write time against the current log and frozen into the
event, so the log stays a record of what happened.

### What these numbers do not mean

Almost the whole table measures "per day of a lifelong habit from age 35". A tap
coarsens that into a single event: convenient, but worth remembering.

And these are observational studies, not experiments. People who eat a lot of
meat also smoke more and move less; people who have more sex are healthier rather
than the other way round. Adjustments are made, residual confounding remains.
Every action carries a caveat in its proof — mandatory, and enforced by a test.

## Editing the catalogue

`tools/actions.py` is the single source of truth for actions: values, copy in
both languages, evidence and allowances. `EventType.kt` and both locales' string
files are generated from it, which is what keeps the translations in step.

```bash
python3 tools/gen.py          # regenerates both strings.xml files
python3 tools/gen_kt.py       # regenerates EventType.kt
python3 tools/check_locales.py
python3 tools/check_ascii.py
```

Adding an action: a constant in `Coefficients.kt`, an entry in
`tools/actions.py`, then regenerate. Search, pinning, the widget and sync all
build from `EventType.entries` and pick it up on their own.

Widget buttons are the one exception: there are only a few and they are chosen by
hand, by pinning them in the app.

Two rules about `id`: it is written into the database, so it must not change
after the first run; renaming an enum entry or reordering entries is safe,
because `id` is what gets serialised, not `name` or `ordinal`.

Search terms are deliberately not translated — they carry both languages at once,
so "сижка" finds smoking with an English interface and "smoke" finds it with a
Russian one.

## Why the number refreshes twice an hour while the seconds tick

This is a system limit, not an omission.

- `updatePeriodMillis` in `res/xml/life_clock_widget_info.xml` is `1800000`
  (30 minutes). Anything smaller is silently rounded up.
- The periodic `WorkManager` job runs every 15 minutes — its hard floor.
- The seconds come from `android.widget.Chronometer`, embedded through
  `AndroidRemoteViews` with `setCountDown(true)`. The launcher ticks it while our
  process sleeps.

Days are substituted straight into the Chronometer format, so the line stays
single: `setChronometer(id, base, "17310d %s", true)` — the days are ours, the
"H:MM:SS" it draws and ticks itself.

They are split that way because a Chronometer can only format "H:MM:SS" and
cannot do days: 17,310 days would come out as 415,440 hours.

Hence a known seam: at the moment the within-day remainder hits zero, the day
count is still the old one and the timer dips negative until the next widget
refresh. Once a day, for no longer than fifteen minutes.

A button press does exactly two things: write to the database and redraw the
widget. No network, no waiting — the callback runs in a narrow window the system
grants, and any delay there becomes a button that feels stuck.

## Backup

Sync is off and the DataStore is the only copy of anything, so a lost phone is a
lost history. Settings → Backup writes the whole log to a JSON file and reads it
back. The format is the same one the app stores, pretty-printed, so the file can
be read and edited by hand.

A restore replaces the state wholesale rather than merging: merging two logs by
timestamp would silently double every event that was re-imported.

## Sync (optional)

Off by default, and the app is entirely self-sufficient without it.

Turn it on in the app: Sync → the switch, a URL, a Bearer token. After that every
new event queues a background upload — a `POST` with this body:

```json
{
  "events": [
    {"type": "smoke", "at_epoch_second": 1757000000, "delta_minutes": -15}
  ],
  "sent_at_epoch_second": 1757000042
}
```

Header: `Authorization: Bearer <token>`.

Only the tail the server has not seen is sent. With no network the event sits in
the database and the worker retries later. The server returns nothing and cannot
overwrite local state.

**https only.** Over plain http the token would travel in the clear, and Android
with `targetSdk 36` blocks cleartext anyway, so the app rejects such a URL
outright and says why.

## Tests

```bash
./gradlew testDebugUnitTest
```

105 tests over the domain: the remaining-life arithmetic, applying and undoing
events, allowances, streaks, the weekly summary, `EventType` extensibility, the
hazard-ratio conversion, the storage format, backups and sync client selection.

The domain deliberately does not depend on `android.*` beyond `@StringRes`
constants, so the tests run on a bare JVM — no Robolectric and no emulator.

## How it is put together

```
domain/     LifeState, LifeEvent, EventType, Coefficients, Microlives, LifeMath,
            Dosing, Evidence, AppLanguage. All the arithmetic lives here.
data/       DataStore Preferences plus kotlinx.serialization, and backups.
i18n/       Resolving resources in the chosen language, widget included.
sync/       EventSyncClient: NoopSyncClient (default) and HttpSyncClient.
work/       SyncWorker (upload) and WidgetRefreshWorker (recompute).
widget/     The Glance widget, the Chronometer and the tap handler.
ui/         The Compose Activity: actions, streaks, the week, settings, backup.
tools/      The action catalogue and its generators.
```

The calculation, entirely in `LifeMath.expectedDeathInstant`:

```
expected moment = midnight on the date of birth
                + base life expectancy
                + the sum of every delta in the log
remaining       = expected moment − now
```

Whole years are added by the calendar (`LocalDate.plusYears`), so leap years are
exact. The mean year length (365.2425 days) is only needed for a fractional
expectancy such as `80.5`.

The remainder is signed: if the expected moment is already behind, a negative
number is shown rather than a zero.

The lived fraction is measured from midnight on the date of birth to the expected
moment, so it accounts for the whole log: every cigarette moves the denominator
as well as the remainder, and the percentage rises slightly faster than time
alone would carry it.

## Stack

Kotlin 2.1.21 · Gradle 8.14.3 (Kotlin DSL) · AGP 8.13.0 · minSdk 31 (Android 12)
· targetSdk 36 · Jetpack Glance 1.2.0 · Compose · DataStore Preferences ·
kotlinx.serialization · WorkManager

No Firebase, no analytics, no third-party SDKs. The HTTP client is the JDK's
`HttpURLConnection`: pulling in a dependency for one POST endpoint is not worth
it.
