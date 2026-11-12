package com.jht.chimera.lib.common.config.frames

import com.jht.chimera.lib.common.config.ProductConfig
import java.util.Locale

private typealias FrameConfigConstructor = () -> FrameConfig

enum class FrameConfigEnum(private val frameConfigConstructor: FrameConfigConstructor) {
    /* DO NOT MODIFY NAMES!!!!  Changing a name will cause machines to need to be setup again.  If
       you need to change a name just make a comment instead of changing the name.
     */
    Ascent_Performance(::FrameConfigAscent_Performance),
    Ascent_Performance_2027(::FrameConfigAscent_Performance_2027),
    Ascent_ProjectOne(::FrameConfigAscent_ProjectOne),
    Climbmill_Endurance(::FrameConfigClimbmill_Endurance),
    Climbmill_Performance(::FrameConfigClimbmill_Performance),
    Climbmill_ProjectOne(::FrameConfigClimbmill_ProjectOne),
    Elliptical_Endurance(::FrameConfigElliptical_Endurance),
    Elliptical_Performance(::FrameConfigElliptical_Performance),
    RecumbentElliptical_Endurance(::FrameConfigRecumbentElliptical_Endurance),
    HybridBike_Performance(::FrameConfigHybridBike_Performance),
    LowerBodyAscent_Performance(::FrameConfigLowerBodyAscent_Performance),
    RecumbentBike_Endurance(::FrameConfigRecumbentBike_Endurance),
    RecumbentBike_EnduranceMd(FrameConfigRecumbentBike_Endurance::MD),
    RecumbentBike_Performance(::FrameConfigRecumbentBike_Performance),
    RecumbentBike_Performance_NexGen(::FrameConfigRecumbentBike_Performance_NexGen),
    RecumbentBike_ProjectOne(::FrameConfigRecumbentBike_ProjectOne),
    Stepper_Endurance(::FrameConfigStepper_Endurance),
    Treadmill_Endurance(::FrameConfigTreadmill_Endurance),
    Treadmill_Performance(::FrameConfigTreadmill_Performance),
    Treadmill_Performance_Plus(::FrameConfigTreadmill_Performance_Plus),
    Treadmill_Performance_Plus_02(::FrameConfigTreadmill_Performance_Plus_02),
    Treadmill_ProjectOne(::FrameConfigTreadmill_ProjectOne),
    UprightBike_Endurance(::FrameConfigUprightBike_Endurance),
    UprightBike_Endurance_02(::FrameConfigUprightBike_Endurance_02),
    UprightBike_Performance(::FrameConfigUprightBike_Performance),
    UprightBike_ProjectOne(::FrameConfigUprightBike_ProjectOne),
    UprightBike_Performance_02(::FrameConfigUprightBike_Performance_02),
    UprightBike_Performance_Nexgen(::FrameConfigUprightBike_Performance_NexGen),
    CardioSpinBike(::FrameConfigCardioSpinBike),
    Tcv_Cycle_Fr(FrameConfigTcvIndoorCycle::FR),
    FrameConfigTcvIndoorCycle_Fx(::FrameConfigTcvIndoorCycle_Fx),
    Treadmill_Lifestyle(::FrameConfigTreadmill_Lifestyle),
    Elliptical_Lifestyle(::FrameConfigElliptical_Lifestyle),
    Climbmill_Lifestyle(::FrameConfigClimbmill_Lifestyle),
    RecumbentBike_Lifestyle(::FrameConfigRecumbentBike_Lifestyle),
    UprightBike_Lifestyle(::FrameConfigUprightBike_Lifestyle),
    Climbmill_Performance_2027(::FrameConfigClimbmill_Performance_2027),
    Treadmill_Performance_2027(::FrameConfigTreadmill_Performance_2027),
    Treadmill_Performance_Plus_2027(::FrameConfigTreadmill_Performance_Plus_2027),
    Treadmill_Hybrid(::FrameConfigTreadmill_Hybrid),

    // Parker Ports
    A30_02(::FrameConfigA30_02),
    A30_03(::FrameConfigA30_03),
    A30_04(::FrameConfigA30_04),
    A30_04_JIV(FrameConfigA30_04::JIV),
    A50(::FrameConfigA50),
    A50_03(::FrameConfigA50_03),
    A50_04(::FrameConfigA50_04),
    A50_04_JIV(FrameConfigA50_04::JIV),
    C50(::FrameConfigC50),
    C50_02(::FrameConfigC50_02),
    E30(::FrameConfigE30),
    E30_02(::FrameConfigE30_02),
    E30_02_JIV(FrameConfigE30_02::JIV),
    E30_03(::FrameConfigE30_03),
    E50(::FrameConfigE50),
    E50_02(::FrameConfigE50_02),
    E50_02_JIV(FrameConfigE50_02::JIV),
    E50_03(::FrameConfigE50_03),
    ICR70(::FrameConfigICR70),
    R30_03(::FrameConfigR30_03),
    R30_04(::FrameConfigR30_04),
    R30_04_JIV(FrameConfigR30_04::JIV),
    R50(::FrameConfigR50),
    R50_02(::FrameConfigR50_02),
    R50_02_JIV(FrameConfigR50_02::JIV),
    T30(::FrameConfigT30),
    T30_JIV(FrameConfigT30::JIV),
    T50_02(::FrameConfigT50_02),
    T50_02_JIV(FrameConfigT50_02::JIV),
    T50_03(::FrameConfigT50_03),
    T50_03_JIV(FrameConfigT50_03::JIV),
    T70_02_LCB_C1(::FrameConfigT70_02_LCB_C1),
    T70_02(::FrameConfigT70_02),
    T70_03(::FrameConfigT70_03),
    T70_04(::FrameConfigT70_04),
    T70_05(::FrameConfigT70_05),
    T70_06(::FrameConfigT70_06),
    T75_LCB_C1(::FrameConfigT75_LCB_C1),
    T75(::FrameConfigT75),
    T75_02(::FrameConfigT75_02),
    T75_04(::FrameConfigT75_04),
    T75_04_JIV(FrameConfigT75_04::JIV),
    TF30(::FrameConfigTF30),
    TF30_02(::FrameConfigTF30_02),
    TF30_02_JIV(FrameConfigTF30_02::JIV),
    TF50(::FrameConfigTF50),
    TF50_02(::FrameConfigTF50_02),
    TF50_02_JIV(FrameConfigTF50_02::JIV),
    TF50_03(::FrameConfigTF50_03),
    TF50_03_JIV(FrameConfigTF50_03::JIV),
    ST90(::FrameConfigST90),
    U30_02(::FrameConfigU30_02),
    U30_03(::FrameConfigU30_03),
    U30_03_JIV(FrameConfigU30_03::JIV),
    U50(::FrameConfigU50),
    U50_02(::FrameConfigU50_02),
    U50_02_JIV(FrameConfigU50_02::JIV),

    // 600E Ports
    U600(::FrameConfigU600), R600(::FrameConfigR600), S600(::FrameConfigS600), S700(::FrameConfigS700),
    T600E110V(::FrameConfigT600E110V),
    T600E220V(::FrameConfigT600E220V);

    fun createFrameConfig() : FrameConfig = frameConfigConstructor()

    companion object {
        @JvmStatic fun getFrameTypeFromString(frameType: String?): FrameConfigEnum? {
            if (frameType == null) return null
            // do we really need to ignore case? do we need to go as far as a private look-up map?
            /*val frameTypeUpper = frameType.uppercase(Locale.ROOT)
            return values().find { it.name.uppercase(Locale.ROOT) == frameTypeUpper }*/
            return try {
                valueOf(frameType)
            } catch (_:Exception) {
                null
            }
        }

        /* Not used?
        val frameForEachClassificationType: HashMap<MachineClassificationType, FrameConfigEnum>
            get() {
                val types = HashMap<MachineClassificationType, FrameConfigEnum>()
                for (frame in allFrames) {
                    if (frame.frameConfig.supportProductFlavors.contains(ProductConfig.flavor)) {
                        types[frame.frameConfig.machineClassType] = frame
                    }
                }
                return types
            }// https://stackoverflow.com/a/9572820
        */

        /** Performs [isFound] on each supported frame. If [isFound] returns true, operation aborts and returns true */
        @JvmStatic inline fun findSupportedFrameConfig(isFound: (FrameConfigEnum, FrameConfig)->Boolean) : Boolean {
            entries.forEach { frame ->
                val frameConfig = frame.createFrameConfig()
                if (frameConfig.supportProductFlavors.contains(ProductConfig.flavor)) {
                    if (isFound(frame, frameConfig)) return true
                }
            }
            return false
        }

        @JvmStatic fun getSupportedFrames(): Array<FrameConfigEnum> {
            val list = ArrayList<FrameConfigEnum>()
            findSupportedFrameConfig { frame, _ ->
                list.add(frame)
                return@findSupportedFrameConfig false
            }
            return list.toTypedArray() // https://stackoverflow.com/a/9572820
        }

        @JvmStatic fun getFrameFromSerial(frameSerial: String?, lcb: Int): FrameConfigEnum? {
            if (frameSerial == null) return null
            var frameConfigEnum: FrameConfigEnum? = null
            var framePrefixLength = 0
            val frameSerialUpper = frameSerial.uppercase(Locale.getDefault()) // make sure string is in uppercase
            findSupportedFrameConfig { frame, frameConfig ->
                if (lcb == 0 || frameConfig.supportsLcbType(lcb)) {  // only for this LCB
                    val framePrefixes = frameConfig.getFramePrefixes()
                    for (framePrefix in framePrefixes) { // iterate through all prefixes for this frame
                        // found the frame with the prefix and keep updating if the prefix length is larger
                        // (ie) TM694 is TF50 and TM694B is TF50-02
                        if (frameSerialUpper.contains(framePrefix) && framePrefix.length > framePrefixLength) {
                            frameConfigEnum = frame
                            framePrefixLength = framePrefix.length
                        }
                    }
                }
                return@findSupportedFrameConfig false
            }
            return frameConfigEnum
        }

        @JvmStatic fun isKnownLcb(lcb: Int): Boolean {
            // PRKS-2930 Retail still needs to reject known commercial frames, so check all frames for known LCB (REVERTING THIS FOR NOW)
            // At present it may be dangerous to block LCBs for which we have no usable configs, so only check flavor supported frames
            return findSupportedFrameConfig { frame, frameConfig -> frameConfig.supportsLcbType(lcb) }
        }

        @JvmStatic fun framePrefixKnown(frameSerialNumber: String?): Boolean {
            if (frameSerialNumber == null) return false
            return findSupportedFrameConfig { frame, frameConfig -> frameConfig.framePrefixValid(frameSerialNumber) }
        }

    }
}