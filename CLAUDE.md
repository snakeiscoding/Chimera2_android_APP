# CLAUDE.md

Android console software for Johnson Health Tech fitness equipment. One codebase, several product
lines (matrix commercial "chimera", matrix retail "parker", lite commercial "vision", premium
"Project One") built through Gradle product flavors. Legacy Java/XML in `Chimera/`, Kotlin/Compose in
`UI/`, `Core/`, `System/`, `Feature/`. `README.md` carries the product and deploy background.

## Commands

```bash
./gradlew :Chimera:assembleChimera_mainMatrixChimeraDebug        # usual dev build
./gradlew :UI:Landmarks:testDebugUnitTest                        # library module tests
./gradlew :Chimera:testChimera_mainMatrixChimeraReleaseUnitTest  # app tests (flavored only)
```

Variant names are `<product><ui><platform><buildType>`; `chimera_mainMatrixChimeraDebug` is the usual
one. Everything else — instrumentation tests, ADB tasks, device scripts — is in
`docs/build-and-flavors.md`.

## Do not touch

- **IMPORTANT: never run the `Deploy` or `Publish` Gradle tasks locally** without the local-properties
  overrides described in `README.md`. They can write to the real release locations.
- `Keystore/` and `Deploy/` hold signing material and release payloads, not ordinary source.
- Prebuilt artifacts (`ConsoleResources`, `translations`, `DataMatrix`, …) are read-only here. A fix
  means a version bump, not a local edit.

## Read when relevant

| Read | When |
|---|---|
| `docs/build-and-flavors.md` | building, tests, flavors, ADB and device scripts |
| `docs/datamatrix.md` | reading or writing any DataMatrix key |
| `docs/modules.md` | working across modules, or hitting a prebuilt artifact |
| `docs/ui-and-theming.md` | Compose: translations, colors, scaling, package layout |

## The architectural rule

`Core/Interfaces` is the app-facing API — abstract `*Model` classes plus the listener/emitter
vocabulary in `Listeners.kt` / `Emitters.kt`. `UI/*` modules depend **only** on those abstractions:
no `DataMatrix`, no `com.jht.chimera.*`, no hardware or service types. `Matrix*Model` implements them
over DataMatrix, Hilt binds them in `ModelInjection.kt`, and `Mock*Model` fakes them for both unit
tests and `@Preview`s.

New system data for a UI module therefore means: extend the abstract model, implement it in
`Matrix*`, extend the mock. `Core/Interfaces` is published through Dokka, so public members need KDoc.

## Gotchas that cost real time

- **`:Chimera` has only flavored unit-test tasks.** A bare `testDebugUnitTest` does not exist there,
  so a script or CI step using the unflavored name runs none of its tests and reports success.
- **The unit suite is not green on a clean tree.** As of September 2026 expect 7 standing failures
  (Friction, SettingsExport, NightlyBuilds). Match that baseline; do not bisect it.
- **One flavor building says nothing about the rest.** Resources and `ProductConfig` behaviour differ
  per product.
- **Compose activities must extend `RestringThemedActivity`** (`Core/Widgets`), not `ThemedActivity`.
  `stringResource()` reads `LocalContext.current.resources`, so skipping the shim silently shows
  bundled strings instead of downloaded translations, with no error.
- **`computeSize()` defaults to design width 1366, not 1920.** Use `computeSizeHd()` or
  `computeSize(designWidth)` to match the comp. The wrong one looks right on one console size only.
- **Never pipe `./gradlew` through `tail`/`head`** — it masks the exit code and turns a failed build
  into an apparent success.
- **A user-level `org.gradle.jvmargs`** in `~/.gradle/gradle.properties` silently overrides every
  project's heap settings. Check there first when builds OOM.
- **On Windows** (what nearly everyone here runs, though there is at least one Mac): Android Studio
  locks `debug` variant intermediates, so a CLI build can fail on `bundleLibCompileToJarDebug` while
  Studio is open. Close Studio or use the `release` unit-test variant.
- Versions live in `gradle/libs.versions.toml` and `gradle-wrapper.properties` — do not copy them
  anywhere that can go stale. Builds need a valid AWS session for CodeArtifact.

## Worth challenging before building

These fail silently rather than at build time, so they are worth raising up front: a change that only
works on the flavor you built; a widget, colour or string duplicating one that already exists in
`Core/Widgets`, the theme, or the translations catalogue; UI reaching past `Core/Interfaces` because
that is quicker than extending a model; a DataMatrix key that cannot do what is asked of it — an
`EVENT_` a late reader needs to poll, a `put` on a hot path, a listener with no matching `finish()`.

## Conventions

- **Kotlin for new files.** The repo is mid-migration, so Java is legacy by default even where it is
  the majority. Flag buggy Java as a rewrite candidate and say why; do not rewrite it unasked.
- **MVVM**, read loosely: `@HiltViewModel` taking `Core/Interfaces` models by constructor, stateless
  composables with state hoisted, `@Preview`s backed by `Mock*Model`, testable logic in the ViewModel.
  Never construct a ViewModel for a child screen — pass the instance down, or the second one silently
  gets its own state.
- **Reuse before adding.** Check `Core/Widgets`, the theme, and the translations catalogue first.
- **Comments** are short and about *why*; prefer KDoc on the declaration over a free-floating `//`.
- **Tests** come with new code, written first where practical, covering under/on/over each edge. When
  something cannot be tested without changing it, say what would make it testable. Run the affected
  module after a change, the full suite before a PR.
- **DAPI is being phased out** in favour of JUMP. Prefer `LoginModel` / `UserModel` over reading DAPI
  state directly.
- Branches and commits start with the ticket (`CGC-12345: Summary in imperative mood`, see
  `.gitmessage`). Work on a ticket branch; read `develop` and release branches but do not write to
  them. That is guidance, not enforcement — the real controls are a deny list in your own
  `~/.claude/settings.json` and Bitbucket branch permissions.
