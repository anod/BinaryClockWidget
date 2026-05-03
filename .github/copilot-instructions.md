# Copilot Instructions — Binary Clock Widget

## Build & Run

```sh
# Build debug APK
./gradlew assembleDebug

# Run library module tests (binary digit logic)
./gradlew :binaryclock:check

# Run a single test class
./gradlew :binaryclock:testDebugUnitTest --tests "info.anodsplace.binaryclock.BinaryClockDigitsTest"

# Install and launch on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n info.anodsplace.binaryclockwidget/.MainActivity
```

## Architecture

Two Gradle modules:

- **`:binaryclock`** — Kotlin Multiplatform library (androidMain + commonMain). Contains all widget logic: Glance UI, refresh scheduler, per-widget config state, and shared binary digit math.
- **`:app`** — Android application. Hosts the widget via manifest merge. Provides `MainActivity` (Jetpack Compose) for widget preview, configuration screen, and widget instance listing.

### Widget (Jetpack Glance)

`BinaryClockGlanceWidget` renders time as quad dots (2×2 rounded boxes per bit). `BinaryClockWidgetReceiver` handles `APPWIDGET_UPDATE`, `TIME_CHANGED`, `TIMEZONE_CHANGED`, and a custom `REFRESH` action.

**Refresh mechanism**: AlarmManager schedules per-minute alarms. Each tick writes a `lastUpdated` timestamp to Glance preferences to force recomposition, then calls `updateAll`. When any widget has `showSeconds` enabled, the receiver runs a coroutine loop (via `goAsync()`) that updates widgets every second until the next minute boundary. Per-second alarms are avoided because `canScheduleExactAlarms()` is false by default and `setWindow` has a ~10s jitter window.

**Per-widget configuration** uses `PreferencesGlanceStateDefinition` (DataStore preferences scoped per GlanceId). Config is read with `currentState<Preferences>()` inside `provideContent` and written from outside with `updateAppWidgetState()`.

### Glance Constraints

- **Row/Column has a 10-child limit** (RemoteViews XML templates). Digits are grouped into `DigitPair` composables to stay under the limit.
- **`ColorProvider(Color)` triggers a false-positive lint error** with K2 UAST (issuetracker.google.com/324087645). The file-level `@SuppressLint("RestrictedApi")` suppression is intentional.
- **Glance does not auto-recompose for time changes**. `LocalTime.now()` is not reactive — state must be mutated (via `lastUpdated` key) before calling `updateAll` to trigger re-render.
- **Per-second alarms are unreliable on modern Android**. `canScheduleExactAlarms()` returns false without explicit user permission, and `setWindow` has ~10s jitter. Use a coroutine loop within `goAsync()` instead.

### MainActivity Modes

`MainActivity` operates in two modes based on whether an `EXTRA_APPWIDGET_ID` is present in the intent:
- **Config mode**: Shows `WidgetConfigScreen` with preview + toggles + Save button. Used when adding a widget or tapping an existing one.
- **Normal mode**: Shows `MainScreen` with a preview section (with toggles) and a widget instances list.

Both preview screens use a `LaunchedEffect` ticker to update the displayed time — every second when `showSeconds` is enabled, every minute otherwise.

## Conventions

- Widget UI composables use Glance imports (`androidx.glance.*`), not Jetpack Compose (`androidx.compose.*`). These are separate frameworks with similar APIs.
- Colors come from `GlanceTheme.colors` (system dynamic colors). No hardcoded widget colors.
- Widget background uses `appWidgetBackground()` + `cornerRadius(android.R.dimen.system_app_widget_background_radius)` for system-consistent appearance.
- The `binaryclock` module's `BinaryClockRefreshScheduler` is `private`. Use `BinaryClockGlanceWidget.rescheduleRefresh(context)` from the app module.
- `BinaryClockDigits` (commonMain) is pure logic with no Android dependencies — it can be unit tested without instrumentation.
