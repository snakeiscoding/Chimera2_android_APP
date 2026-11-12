# Modules and shared artifacts

`settings.gradle.kts` and the directory tree list the modules. This covers what they do not say.

## UpdateService

The console update service, running in its own process (`com.jhtp.updateservice`) so it survives the
main app restarting. `UpdateOS` applies OTA payloads through Android's `UpdateEngine`;
`AutomatedUpdate` drives unattended updates through `UpdateManagerClient` (check, download, apply,
report) against a release from the OTA console update system or a manual manifest. It consumes exactly what the `buildSrc` Deploy
tasks produce, so deploy packaging changes and changes here are two halves of one job.

## Shared artifacts

Prebuilt, from CodeArtifact, pinned in `gradle/libs.versions.toml`:

- `com.jht.resources:ConsoleResources` — `com.jht.theme` (`DynamicTheme`, `ThemedActivity`,
  `ThemedViewModel`) and `com.jht.ui` (`computeSize`, device preview annotations).
- `com.jht.translations:translations` — user-facing strings.
- `com.jht.datamatrixlib:DataMatrix`, `com.jht.oauthlib:oauthlib`, `com.jht.engage360:PtpNative`,
  `com.jht.dapi:DAPIServiceLib`, and others.

Because `android.nonTransitiveRClass=false`, strings from the translations artifact resolve through
the consuming module's own `R` (e.g. `com.jht.landmarks.R.string.level_tc`) with no local XML
defining them. A missing `R.string` usually means the artifact needs a version bump, not a new local
string.
