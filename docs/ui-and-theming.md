# UI, theming, and strings

## Translations are downloaded, not just bundled

Consoles download per-locale JSON files, and `RestringTranslationLoader`
(`Chimera/.../lib/language/`) feeds the active locale into Restring, which intercepts resource
lookups and returns the downloaded string when one exists. That is how a translation fix ships
without an app update, and why a device can display something other than its bundled resource.
Resolution is `language_REGION` → `language` → `en`; every failure falls back to bundled strings.

Inflation reads `android:text`/`android:hint` without a resource lookup, so `RestringActivity` and
`RestringThemedActivity` install a ViewPump delegate whose Reword interceptor re-applies them through
Restring. Views inflated with the application context (the UiService panels) are not covered.

Check for an existing translation key before adding a string — the artifact catalogue is large.

## Scaling

`com.jht.widgets.scaleSize()` in `Core/Widgets` is an older equivalent of `computeSize`, still used
in some modules. Both take a design width; neither defaults to 1920.

## Package layout

Inconsistent across `UI/*` — only about half the modules have a `ui/` package at all. Match the
module you are in rather than importing another module's shape. What holds everywhere: the entry
Activity and the module's main ViewModel sit at the module root, screen-level ViewModels sit beside
their screens, and reusable composables get their own subpackage (`widgets/` in `UI/Progression`,
`components/` in `UI/Seca`, `commoncomponents/` in `UI/FirstTimeSetup`). Where a module already has
a home for colors, dimens or navigation, add to it rather than starting a second one.

New modules aim at a `ui/` package holding screens, reusable composables, colors and navigation, as
`UI/Progression` does. Existing modules get aligned under CGC-25892, not opportunistically
mid-feature.

## Architecture direction

This is Android blended with embedded hardware and does not follow modern Android practice exactly.
MVI with Compose state management is under discussion, so proposing it is fine — but MVVM is what
the code does today and what new code matches.
