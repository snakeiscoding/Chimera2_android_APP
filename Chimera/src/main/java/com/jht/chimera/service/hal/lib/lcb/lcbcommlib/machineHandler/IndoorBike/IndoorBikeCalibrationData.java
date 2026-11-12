package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike;

// This is meant to decode and encode command 53/54 athena/icr data payloads
// Values are stored as doubles which should be able to store 32bit integers without precision loss

import org.jetbrains.annotations.NonNls;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.Locale;

public class IndoorBikeCalibrationData {
    protected byte version;
    protected BikeParameter[] parameters; // does not include the header at parameter1 (this should reflect the parameter map in data payload)

    public static IndoorBikeCalibrationData fromBytes(byte[] data, int offset) {
        // decode parameter count (1 byte)
        if (data == null || (data.length - offset) < 1) return null; // make sure we actually got data
        int numOfParams = data[offset++] & 0xFF;
        if (numOfParams < 1) return null; // need at least the header
        if ((data.length - offset) < numOfParams*4) return null; // check if enough data is left for parameters
        // decode header (3 bytes + 1 byte revision)
        for (int j = 0; j < 3; ++j){
            if (data[offset++] != 0x55) return null;
        }
        IndoorBikeCalibrationData result;
        byte version = data[offset++];
        switch (version) {
            case 1:
                result = new AthenaCalibrationData();
                break;
            case 2:
                result = new IcrCalibrationData();
                break;
            default:
                result = createGeneric(version, numOfParams);
                break;
        }
        // decode other parameters
        for (BikeParameter p : result.parameters) {
            p.fromBytes(data, offset);
            offset+=4;
        }
        return result;
    }

    public int requiredByteSize() {
        return 1 + (1 + parameters.length) * 4; // 1 for count, 4 * N for other parameters (including header parameter)
    }
    public void toBytes(byte[] data, int offset) {
        data[offset++] = (byte) (1 + parameters.length); // get number of parameters (include header in the count)
        for (int i = 0; i < 3; ++i) data[offset++] = 0x55; // header parameter is not in the array so add one
        data[offset++] = version;
        for (BikeParameter p : parameters) {
            p.toBytes(data, offset);
            offset+=4;
        }
    }

    public BikeParameter[] getParameters() { return parameters; }
    public String getReport() {
        StringBuilder result = new StringBuilder();
        result.append("version: ").append(version).append(" count: ").append(1 + parameters.length).append("\n");
        for (BikeParameter p : parameters) {
            if (!p.isReserved()) result.append(p.getReport()).append("\n");
        }
        return result.toString();
    }

    protected double generateTimestampValue() { return (double) (System.currentTimeMillis() / 1000L); } // note this is a 64bit counter (double precision good to about 53bits?)
    public void setTimeStamp() {} // if there is one, derived class should override

    protected static IndoorBikeCalibrationData createGeneric(byte version, int numberOfParameters) {
        IndoorBikeCalibrationData result = new IndoorBikeCalibrationData();
        result.version = version;
        result.parameters = new BikeParameter[numberOfParameters-1];
        for (int i=0; i<result.parameters.length; ++i) {
            result.parameters[i] = new BikeParameterIntHex("Field " + (i + 2)); // fields number base one, field 1 is header
        }
        return result;
    }
    public BikeParameterIntHex getHeader() {
        BikeParameterIntHex header = new BikeParameterIntHex("Field 1");
        header.value = (0x55555500 | version);
        return header;
    }

    /*** BIKE PARAMETER TYPES ***/

    public static class BikeParameter {
        public String name;
        public double value;
        public BikeParameter(@NonNls String parameterName) { name = parameterName; }
        public void fromBytes(byte[] data, int offset) { }
        public void toBytes(byte[] data, int offset) {
            for (int i = offset; i < offset+4; ++i) data[i] = 0; // since we use the base for reserved, should it be set zero or skipped?
        }
        public boolean isReserved() { return name == null; }
        public static BikeParameter RESERVED() { return reservedParameter; }
        public String getValue() { return Double.toString(value); }
        public String getReport() { return name + " = " + getValue(); }
    }
    protected static final BikeParameter reservedParameter = new BikeParameter(null);
    public static class BikeParameterFloat extends BikeParameter {
        public BikeParameterFloat(@NonNls String parameterName) { super(parameterName); }
        @Override
        public void fromBytes(byte[] data, int offset) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, 4);
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            value = byteBuffer.getFloat();
        }
        @Override
        public void toBytes(byte[] data, int offset) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, 4);
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            float v = (float)value;
            byteBuffer.putFloat(v);
        }
        @Override
        public String getValue() {
            DecimalFormat df = new DecimalFormat("0.#");
            df.setMaximumFractionDigits(100);
            return df.format(value);
        }
    }
    public static class BikeParameterInt extends BikeParameter {
        public BikeParameterInt(@NonNls String parameterName) { super(parameterName); }
        @Override
        public void fromBytes(byte[] data, int offset) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, 4);
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            value = byteBuffer.getInt();
        }
        @Override
        public void toBytes(byte[] data, int offset) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, 4);
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            int v = (int)(Math.round(value));
            byteBuffer.putInt(v);
        }
        @Override
        public String getValue() {
            return String.format(Locale.ENGLISH, "%d", (long)value);
        }
    }
    public static class BikeParameterIntHex extends BikeParameterInt {
        public BikeParameterIntHex(@NonNls String parameterName) { super(parameterName); }
        @Override
        public String getValue() {
            return String.format(Locale.ENGLISH, "0x%04X", (long)value);
        }
    }

    /*** CalibrationData Classes ***/

    public static class AthenaCalibrationData extends IndoorBikeCalibrationData {
        public BikeParameter STF0 = new BikeParameterFloat("system_torque_rpm_FWD_0");
        public BikeParameter STF1 = new BikeParameterFloat("system_torque_rpm_FWD_1");
        public BikeParameter STF2 = new BikeParameterFloat("system_torque_rpm_FWD_2");
        public BikeParameter STR0 = new BikeParameterFloat("system_torque_rpm_REV_0");
        public BikeParameter STR1 = new BikeParameterFloat("system_torque_rpm_REV_1");
        public BikeParameter STR2 = new BikeParameterFloat("system_torque_rpm_REV_2");
        public BikeParameter BrakeRpm1 = new BikeParameterFloat("brake_rpm_1");
        public BikeParameter BrakeRpm2 = new BikeParameterFloat("brake_rpm_2");
        public BikeParameter BrakeRpm3 = new BikeParameterFloat("brake_rpm_3");
        public BikeParameter TorqueEcb0 = new BikeParameterFloat("torque_v_ecb_0");
        public BikeParameter TorqueEcb1 = new BikeParameterFloat("torque_v_ecb_1");
        public BikeParameter TorqueEcb2 = new BikeParameterFloat("torque_v_ecb_2");
        public BikeParameter TorqueEcb3 = new BikeParameterFloat("torque_v_ecb_3");
        public BikeParameter TorqueEcb4 = new BikeParameterFloat("torque_v_ecb_4");
        public BikeParameter beltRatio = new BikeParameterFloat("standard_belt_ratio");
        public BikeParameter EcbAtZero = new BikeParameterInt("ecb_position_at_0_mechanical");
        public BikeParameter timeStamp = new BikeParameterInt("time_stamp");
        public BikeParameter stScale = new BikeParameterFloat("system_torque_scale");
        public BikeParameter stScaleOld = new BikeParameterFloat("Old_system_torque_scale");
        public BikeParameter checkingValue = new BikeParameterInt("coupled_system_checking_value");

        public AthenaCalibrationData() {
            version = 1;
            parameters = new BikeParameter[] {
                    STF0, STF1, STF2,
                    STR0, STR1, STR2,
                    BrakeRpm1, BrakeRpm2, BrakeRpm3,
                    TorqueEcb0, TorqueEcb1, TorqueEcb2, TorqueEcb3, TorqueEcb4,
                    beltRatio, EcbAtZero, timeStamp,
                    stScale, stScaleOld, checkingValue
            };
        }

        @Override
        public void setTimeStamp() {
            timeStamp.value = generateTimestampValue();
        }
    } // END CLASS: AthenaCalibrationData


    public static class IcrCalibrationData extends IndoorBikeCalibrationData {
        public BikeParameter ST0 = new BikeParameterFloat("system_torque_coefficient_0");
        public BikeParameter ST1 = new BikeParameterFloat("system_torque_coefficient_1");
        public BikeParameter ST2 = new BikeParameterFloat("system_torque_coefficient_2");
        public BikeParameter MT0 = new BikeParameterFloat("magnet_torque_coefficient_0");
        public BikeParameter MT1 = new BikeParameterFloat("magnet_torque_coefficient_1");
        public BikeParameter MT2 = new BikeParameterFloat("magnet_torque_coefficient_2");
        public BikeParameter zeroADC = new BikeParameterInt("zero_adc");
        //public BikeParameter adcScale = new BikeParameterFloat("adc_scale");
        //public BikeParameter minADC = new BikeParameterInt("min_adc");
        //public BikeParameter maxADC = new BikeParameterInt("max_adc");
        public BikeParameter beltRatio = new BikeParameterFloat("standard_belt_ratio");
        public BikeParameter radiusFlywheelToResistance = new BikeParameterFloat("radius_flywheel_to_resistance");
        public BikeParameter timeStamp = new BikeParameterInt("time_stamp");
        public BikeParameter stScale = new BikeParameterFloat("system_torque_scale");

        public IcrCalibrationData() {
            version = 2;
            parameters = new BikeParameter[] {
                    ST0, ST1, ST2,
                    MT0, MT1, MT2,
                    BikeParameter.RESERVED(), BikeParameter.RESERVED(), BikeParameter.RESERVED(),
                    zeroADC, BikeParameter.RESERVED(), BikeParameter.RESERVED(),
                    BikeParameter.RESERVED(), BikeParameter.RESERVED(),
                    beltRatio, radiusFlywheelToResistance,
                    timeStamp, stScale,
                    BikeParameter.RESERVED(), BikeParameter.RESERVED(),
            };
        }

        @Override
        public void setTimeStamp() {
            timeStamp.value = generateTimestampValue();
        }
    } // END CLASS: IcrCalibrationData

} // END CLASS: IndoorBikeCalibrationData
