package com.jht.chimera.ui.machinesetup;

/**
 * ProjectOne (Onyx) first time setup.
 *
 * The wizard is registered in main/AndroidManifest.xml as
 * com.jht.chimera.ui.machinesetup.MachineSetup, and a java source set cannot override a class in
 * main, so every product flavor has to supply a class by this name. All of the behavior lives in
 * {@link MachineSetupBase}. Anything ProjectOne specific belongs there behind a
 * ProductConfig.isProjectOne() or BuildConfig check, so the flavors cannot drift apart again.
 */
public class MachineSetup extends MachineSetupBase {
}
