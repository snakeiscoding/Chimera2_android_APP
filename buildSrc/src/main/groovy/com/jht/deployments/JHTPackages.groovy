package com.jht.deployments

import org.jetbrains.annotations.Nullable

/**
 * List all packages.
 * TODO:  This file is obsolete. Deploys are setup through AWS in buildSrc/src/main/kotlin now. @Deprecated
 */
enum JHTPackages {
    UPDATER("Updater", "d9a76997-7a2d-4f47-b429-6273de4bcfeb", "chimera_updater", ["UpdateService"], "software_updater", true),
    // reboot overridden by doesConsoleAppNeedReboot()
    CONSOLE_APPS("Apps", "63a74e6c-8380-4416-963a-2ee9d2357cc6", "chimera_mainapp", "general", true, false),
    CONSOLE_APPS_DEBUG("AppsDebug", "63a74e6c-8380-4416-963a-2ee9d2357cc6", "chimera_mainapp", "general", true, false),
    ASSETS("Assets", "f5a6a28a-dacc-44a5-9949-697fc379873d", "chimera_assets", "general", false, false),
    CHIMERA_OS_K2_2("OS K2_2", "2ec6a147-375f-4a61-ad53-6050c17c27bb", "chimera_os", "general", true),
    CHIMERA2_OS("OS2", "517bcd4c-4049-4c3e-b20b-1a3eb97c3616", "chimera_os", "general", true),
    CHIMERA3_OS("OS4", "63f7fa02-a12c-11fc-edeb-29e2973f7716", "chimera_os", "general", true),
    CHIMERA_TPA("Third Party Apps", "fd9193ca-3ab3-43e9-829f-b41e83fc915f", "chimera_tpa", "general", false),
    CHIMERA_IO("IO", "fb50aaa6-64bc-4217-a2aa-fdeda79684b0", "chimera_firmware", "general", true),
    CHIMERA_IO2("IO2", "348ffaf2-804c-47ac-9250-8b011a7bf9fc", "chimera_firmware", "general", true),
    CHIMERA_IO3("IO3", "e4640af0-abf9-11ec-b909-0242ac120002", "chimera_firmware", "general", true),
    CHIMERA_IO5("IO5", "6b90c9a5-2b75-5a65-66de-12a0a06717f5", "chimera_firmware", "general", true),
    CHIMERA_LCB1("LCB1", "100ec386-af52-4ee5-ab62-326388ea36e1", "chimera_firmware", "general", true, true),
    CHIMERA_LCB2("LCB2", "94f764e0-b90e-4c4a-ba13-0f0be210ecbb", "chimera_firmware", "general", true, true),
    CHIMERA_LCB_Climbmill("LCB Climbmill", "41e6d75f-ec8a-4481-8e4a-91885b429fd0", "chimera_firmware", "general", true, true),
    PARKER_LCB_RETAIL_CLIMBMILL("LCB Climbmill Retail", "dd256a8f-99b2-4ff6-9f8b-adeb82cd66b3", "chimera_firmware", "general", true, true),
    CHIMERA_LCBA("LCBA", "c3136682-5a04-4795-88d2-61044f3a14ef", "chimera_firmware", "general", true, true),
    CHIMERA_LCB1xBike("LCB1xBike", "18bb5557-e9d2-4256-a99a-44bfa2e51168", "chimera_firmware", "general", true, true),
    CHIMERA_LCBAthena("LCBAthena", "eb7359d0-68e8-4293-ada3-2c42392b62c8", "chimera_firmware", "general", true, true),
    ICR70_LCB("ICR70-LCB", "004e7300-42d7-4e88-8c9b-d41fddd68a21", "chimera_firmware", "general", true, true),
    SALUTRON("Salutron", "acd5713e-3e57-4b15-98f9-37716a3440a6", "chimera_salutron", "general", false),
    CHIMERA_TOUCH_PANEL("Touch Panel Firmware", "41595365-8fce-438a-98ff-5e089c1a01eb", "chimera_touch_panel", "general", false),
    CHIMERA2_TOUCH_PANEL("Touch Panel Firmware2", "fe0ce7df-cef6-45e8-8189-3c922e7f03bb", "chimera_touch_panel", "general", false),
    CHIMERA_TOUCH_PANEL_ATHENA("Touch Panel Firmware Athena", "41595365-8fce-438a-98ff-5e089c1a01eb", "chimera_touch_panel_athena", "general", false),
    WLT_BLUETOOTH("Bluetooth", "f253ef1f-960a-4400-aca8-a4c84ff18df3", "chimera_firmware", "general", false),
    RFID2("RFID2", "56ecf39c-dee0-11ed-b5ea-0242ac120002", "chimera_firmware", "general", false),
    RFID3("RFID3", "caa013cd-b9b5-4df0-9269-46623b32a1a4", "chimera_firmware", "general", false),
    RFID4("RFID4", "19cbd702-2fb8-425c-8197-a7699ecf77dc", "chimera_firmware", "general", false),
    CAN_SINGLE("CAN_SINGLE", "4d5890bb-3cd8-480b-99de-41c568db17aa", "chimera_firmware", "general", false),
    CAN_RIGHT("CAN_RIGHT", "65664e6d-0128-4ba7-b6c0-c9dd01a38ed3", "chimera_firmware", "general", false),
    CAN_LEFT("CAN_LEFT", "86f5f0cd-dc6e-4f6c-b5ba-3d7c9df02f86", "chimera_firmware", "general", false),
    CHIMERA_U_BOOT("U-Boot", "d3ff5cbb-cdba-4c7d-a8c8-2f5bcf977ce7", "chimera_uboot", "general", true),
    PARKER_OS("OS", "2ec6a147-375f-4a61-ad53-6050c17c27bb", "chimera_os", "os", true), // same UUID as CHIMERA_OS_K2_2
    UT_OS("UT-OS", "4d95cc64-dbef-4173-9deb-bee7ae9cb3be", "chimera_os", "general", true), // type must be general to avoid parker non-A/B update logic
    PARKER_IO("IO", "fb50aaa6-64bc-4217-a2aa-fdeda79684b0", "chimera_firmware", "general", false), // same UUID as CHIMERA_IO
    PARKER3_IO("IO3", "6b3d43a2-46a4-48e9-9c0d-bdae7af28f2b", "chimera_firmware", "general", false),
    PARKER_IR("IR", "0cccd5fd-cc77-4467-9aff-d20de5f4cae0", "chimera_firmware", "general", false),
    VISION_AD_BOARD("ADboard", "251db1fe-cbbc-4c9f-b396-7505c1643111", "vision_adboard", "general", false),
    CHIMERA_US5("US5 Tuner", "6349cde6-09ae-4e67-9f42-bcc7cd94c7e7", "chimera_firmware", "general", false),
    CHIMERA_US6("US6 Tuner", "3de6ce69-b8b5-4be9-ad25-400c168a7ffe", "chimera_firmware", "general", false),
    CHIMERA_US7("US7 Tuner", "d6d20f13-5a88-4314-a3ec-183bdefe7cb9", "chimera_firmware", "general", false),
    CHIMERA_KM5("KM5 Tuner", "1d739cc3-0875-4ad8-9556-1d7fff102a33", "chimera_firmware", "general", false),
    P1_TOGGLE_TOUCH_FIRMWARE("Toggle Touch Firmware", "7916ce3e-3223-11ee-be56-0242ac120002","chimera_firmware","general",false),
    P1_BUMP_SENSOR("Bump Sensor", "0aa9a8ea-990a-4b07-91f5-4e7777dedf8b", "chimera_firmware", "general", false),

    /**
     * Type must be general to avoid Parker non-A/B update logic
     */
    PARKER3_OS("OS3", "3a57fa29-173c-4644-a33c-4aaf9867b23a", "chimera_os", "general", true),

    /** Return true if package has to be compiled */
    boolean hasToCompileApk() {
        return this == UPDATER || this == CONSOLE_APPS || this == CONSOLE_APPS_DEBUG
    }

    /**
     * The folder in the deploy project with the update script.
     */
    private String baseFolder

    /**
     * If there is a code folder that should be used for md5 hash, set this to that folder relative
     * to repo root
     */
    private List<String> codeFolder

    /**
     * The name of the package.
     */
    private String packageName

    /**
     * The package UUID.
     */
    private String id

    /**
     * The update type
     */
    private String updateType

    /**
     * True if we need to reboot after installing the package.
     */
    private boolean reboot

    /**
     * True if we need to force a manual reboot after installing the package
     */
    private boolean manualReboot

    /**
     * Creates the package enum with the properties.
     *
     * @param packageName The package name.
     * @param id          The package UUID.
     * @param baseFolder  The folder containing the update script for the package.
     * @param updateType  The update type.
     * @param reboot      true if we need to reboot after installing the package.
     */
    JHTPackages(String packageName, String id, String baseFolder, String updateType, boolean reboot) {
        this(packageName, id, baseFolder, null, updateType, reboot)
    }

    /**
     * Creates the package enum with the properties.
     *
     * @param packageName The package name
     * @param id          The package UUID
     * @param baseFolder  The folder containing the update script for the package
     * @param codeFolder  The files/folders containing the source code for the package (if using source
     * code for md5 hashing)
     * @param updateType  The update type
     * @param reboot      True if we need to reboot after installing the package.
     */
    JHTPackages(String packageName, String id, String baseFolder, List<String> codeFolder, String updateType, boolean reboot) {
        this.packageName = packageName
        this.id = id
        this.baseFolder = baseFolder
        this.codeFolder = codeFolder
        this.updateType = updateType
        this.reboot = reboot
        this.manualReboot = false
    }

    /**
     * Creates the package enum with the properties.
     *
     * @param packageName  The package name
     * @param id           The package UUID
     * @param baseFolder   The folder containing the update script for the package
     * @param updateType   The update type
     * @param reboot       True if we need to reboot after installing the package.
     * @param manualReboot True if we need to force a manual reboot after installing the package
     */
    JHTPackages(String packageName, String id, String baseFolder, String updateType, boolean reboot, boolean manualReboot) {
        this.packageName = packageName
        this.id = id
        this.baseFolder = baseFolder
        this.codeFolder = null
        this.updateType = updateType
        this.reboot = reboot
        this.manualReboot = manualReboot
    }

    /**
     * @return The folder that contains the update script.
     */
    String baseFolder() {
        return baseFolder
    }

    /**
     * @return the code folder that should be used for the MD5 hash, else null.
     */
    @Nullable
    List<String> codeFolder() {
        return codeFolder
    }

    /**
     * @return The package name
     */
    String packageName() {
        return packageName
    }

    /**
     * @return The package UUID
     */
    String id() {
        return id
    }

    /**
     * @return The package update type.
     */
    String updateType() {
        return updateType
    }

    /**
     * @return True if we need to reboot after installing the package.
     */
    boolean reboot() {
        return reboot
    }

    /**
     * @return True if we need to reboot after installing the package.
     */
    boolean manualReboot() {
        return manualReboot
    }
}