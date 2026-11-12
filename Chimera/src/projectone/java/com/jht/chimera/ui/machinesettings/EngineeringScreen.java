package com.jht.chimera.ui.machinesettings;

/**
 * ProjectOne (Onyx) machine settings / engineering screen.
 *
 * The screen is registered in main/AndroidManifest.xml as
 * com.jht.chimera.ui.machinesettings.EngineeringScreen, and a java source set cannot override a
 * class in main, so every product flavor has to supply a class by this name. All of the behavior
 * lives in {@link EngineeringScreenBase}. Anything ProjectOne specific belongs there behind a
 * ProductConfig.isProjectOne() or BuildConfig check, so the flavors cannot drift apart again.
 */
public class EngineeringScreen extends EngineeringScreenBase {
}
