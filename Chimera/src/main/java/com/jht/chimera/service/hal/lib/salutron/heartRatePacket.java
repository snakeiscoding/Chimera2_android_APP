package com.jht.chimera.service.hal.lib.salutron;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class heartRatePacket {

    FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
    DataMatrix dataMatrix = new DataMatrix();

    private boolean isPage21 = false;
    private static ParseState parseState = ParseState.PARSE_CHECKSUM;

    private enum ParseState {
        PARSE_CHECKSUM,
        PARSE_CLASS_SUB,
        PARSE_DATA
    }

    private enum packetFrame {
        START,
        LENGTH,
        CHECKSUM_1,
        CHECKSUM_2,
        RESPONSE,
        CLASS,
        SUB,
        DATA
    }

    heartRatePacket() {
    }

    public byte[] buildConnectPacket(salutronCommands.DeviceType deviceType, byte... name) {
        return buildTxPacket((byte) 0x01, (byte) 0x1C, deviceType.getSub(), name);
    }

    public byte[] buildANTProfilePacket(salutronCommands.ProfilePage page, boolean isPage21){
        this.isPage21 = isPage21;
        return buildANTProfilePacket(page);
    }

    private byte updateEventCounter = 0;
    public byte[] buildANTProfilePacket(salutronCommands.ProfilePage page) {
        byte[] data = new byte[8];
        data[0] = page.getPageNum();
        switch (page) {
            case FE_PAGE_16:             //General FE Data
                //machine type
                switch (frameConfig.machineClassType) {
                    case Treadmill:
                        data[1] = (byte) 0x13;
                        break;
                    case Elliptical:
                    case RecumbentElliptical:
                    case Ascent:
                    case Stepper:
                        data[1] = (byte) 0x14;
                        break;
                    case Climbmill:
                        data[1] = (byte) 0x17;
                        break;
                    case Bike:
                        if(isPage21)
                            data[1] = (byte) 0x15;
                        else
                            data[1] = (byte) 0x19;
                        break;
                }
                //Elapsed Time
                byte[] timeArray = ByteBuffer.allocate(4).putInt((int) (dataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000 / 0.25)).array();
//                Log.d("Salutron","workout total time: " + workoutData.workoutTotals.goalStatus.seconds.getValue());
                data[2] = timeArray[3];
                //Distance
//                Log.d("Salutron","workout total distance: " + (int)(workoutData.workoutTotals.distance.getValue().metricValue * 1000));
                byte[] disatnceArray = ByteBuffer.allocate(4).putInt((int) (dataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) * 1000)).array();
                data[3] = disatnceArray[3];
                //Speed
                byte[] speedArray;
/*                if(machineInfo.machineClassificationType.getValue() == JHTTypes.MachineClassificationType.Treadmill) {
                    Log.d("Salutron", "workout total targetSpeed: " + (machineData.targetSpeed.getValue().getValue(JHTTypes.Unit.Metric) * 1000) / 3600 / 0.001);
                    speedArray = ByteBuffer.allocate(4).putInt((int) ((machineData.targetSpeed.getValue().getValue(JHTTypes.Unit.Metric) * 1000) / 3600 / 0.001)).array();
                }
                else
                    speedArray = ByteBuffer.allocate(4).putInt((int) ((machineData.speed.getValue().getValue(JHTTypes.Unit.Metric) * 1000) / 3600 / 0.001)).array();
*/
                if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
//                    Log.d("Salutron", "workout total targetSpeed: " + (machineData.targetSpeed.getValue().getValue(JHTCommonTypes.Unit.Metric) * 1000) / 3600 / 0.001);
                    speedArray = ByteBuffer.allocate(4).putInt((int) ((dataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT) * 1000) / 3600 / 0.001)).array();
                } else
                    speedArray = ByteBuffer.allocate(4).putInt((int) ((dataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT) * 1000) / 3600 / 0.001)).array();


                data[4] = speedArray[3];           //LSB
                data[5] = speedArray[2];           //MSB
                //Heart Rate
//                Log.d("Salutron","workout total heart rate: " + workoutData.workoutTotals.heartRate.value());
                byte[] heartRateArray = ByteBuffer.allocate(4).putInt(dataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT)).array();
                data[6] = heartRateArray[3];
                data[7] = (byte) 0x34;
                break;
            case FE_PAGE_17:             //General settings page
                data[1] = (byte) 0xFF;
                data[2] = (byte) 0xFF;
                //Cycle length
                data[3] = (byte) 0xFF;    //0xFF indicates invalid
                //Incline
                byte[] inclineArray = ByteBuffer.allocate(4).putInt((int) dataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT)).array();
//                Log.d("Salutron","workout total incline: " + machineData.incline.getValue().intValue());
                data[4] = inclineArray[3];
                data[5] = inclineArray[2];
                //Resistance level
                byte[] resistanceArray = ByteBuffer.allocate(4).putInt(dataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT)).array();
//                Log.d("Salutron","workout total resistance: " + workoutData.currentSnapshot.resistance.value());
                data[6] = resistanceArray[3];
                data[7] = (byte)0x30;

                break;
            case FE_PAGE_18:             //General FE Metabolic Data
                data[1] = (byte) 0xFF;  //Reserved
                //Mets
                byte[] metsArray = ByteBuffer.allocate(4).putInt((int) (dataMatrix.getFloat(HALKeys.DATA_METS_FLOAT) * 100)).array();
                data[2] = metsArray[3];           //LSB
                data[3] = metsArray[2];           //MSB
                //Caloric burn rate
                byte[] burnRateArray = ByteBuffer.allocate(4).putInt((int) (dataMatrix.getInt(WorkoutKeys.DATA_CALORIES_PER_HOUR_INT) / 0.1)).array();
//                Log.d("Salutron","workout total caloriesPerHour: " + (int)(workoutData.workoutTotals.caloriesPerHour.value() / 0.1));
                data[4] = burnRateArray[3];           //LSB
                data[5] = burnRateArray[2];           //MSB
                //Calories
                byte[] caloricArray = ByteBuffer.allocate(4).putInt((int) dataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT)).array();
//                Log.d("Salutron","workout total calories: " + workoutData.workoutTotals.calories.getValue());
                data[6] = caloricArray[3];
                data[7] = (byte) 0x31;
                break;
            case FE_PAGE_19:            //Specific Treadmill Data
                data[1] = (byte) 0xFF;
                data[2] = (byte) 0xFF;
                data[3] = (byte) 0xFF;
                //Cadence
                data[4] = (byte) 0x00;
                //Negative vertical distance
                data[5] = (byte) 0x00;
                byte[] positiveVerticalArray = ByteBuffer.allocate(4).putInt((int)(dataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT) / 0.1)).array();
                //Positive vertical distance
                data[6] = positiveVerticalArray[3];
                data[7] = positiveVerticalArray[2];
                break;
            case FE_PAGE_20:         //Specific Elliptical Data
                data[1] = (byte) 0xFF;
                //Positive vertical distance
                data[2] = (byte) 0x00;
                //Stride count - rpm * min
                byte[] strideArray = ByteBuffer.allocate(4).putInt((dataMatrix.getInt(HALKeys.DATA_RPM_INT) / 60) * dataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000).array();
                data[3] = strideArray[3];
                //Cadence
                byte[] cadenceArrayE = ByteBuffer.allocate(4).putInt(dataMatrix.getInt(HALKeys.DATA_RPM_INT)).array();
                data[4] = cadenceArrayE[3];
                //Instantaneous Power
                byte[] powerArrayE = ByteBuffer.allocate(4).putInt((int) dataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT)).array();
                data[5] = powerArrayE[3];
                data[6] = powerArrayE[2];
                data[7] = (byte) 0x31;
                break;
            case FE_PAGE_23:       //Specific Climber Data
                data[1] = (byte) 0xFF;
                data[2] = (byte) 0xFF;
                //Cycle(Strides)
                data[3] = (byte) 0x00;
                //Cadence
                byte[] cadenceArrayC = ByteBuffer.allocate(4).putInt(dataMatrix.getInt(HALKeys.DATA_RPM_INT)).array();
                data[4] = cadenceArrayC[3];
                //Instantaneous Power
                byte[] powerArrayC = ByteBuffer.allocate(4).putInt((int) dataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT)).array();
                data[5] = powerArrayC[3];
                data[6] = powerArrayC[2];
                data[7] = (byte) 0x30;
                break;
            case FE_PAGE_25:             //Specific Trainer/Stationary Bike Data
                //Update Event Count
                data[1] = updateEventCounter++;
                //Instantaneous Cadence
                byte[] cadenceArrayB = ByteBuffer.allocate(4).putInt(dataMatrix.getInt(HALKeys.DATA_RPM_INT)).array();
                data[2] = cadenceArrayB[3];
                //Accumulated Power
                byte[] powerArray1 = ByteBuffer.allocate(4).putInt((int) dataMatrix.getFloat(WorkoutKeys.DATA_JOULES_FLOAT)).array();
//                Log.d("Salutron","workout total joules: " + workoutData.workoutTotals.joules.getValue().intValue());
                data[3] = powerArray1[3];  //LSB
                data[4] = powerArray1[2];  //MSB
                //Instantaneous Power
                byte[] powerArray2 = ByteBuffer.allocate(4).putInt((int) dataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT)).array();
                data[5] = powerArray2[3];  //LSB
                data[6] = (byte) (powerArray2[2] & (byte) 0x70);  //MSN(0-3 bits) & trainer Status(4-7 bits)
                data[7] = (byte) 0x30;
                break;

            case FE_PAGE_21:
                data[1] = (byte) 0xFF;  //Reserved
                data[2] = (byte) 0xFF;  //Reserved
                data[3] = (byte) 0xFF;  //Reserved
                //Cadence
                byte[] cadenceArrayB1 = ByteBuffer.allocate(4).putInt(dataMatrix.getInt(HALKeys.DATA_RPM_INT)).array();
                data[4] = cadenceArrayB1[3];
                //Instantaneous Power
                byte[] powerArrayB1 = ByteBuffer.allocate(4).putInt((int) dataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT)).array();
                data[5] = powerArrayB1[3];  //LSB
                data[6] = powerArrayB1[2];
                data[7] = (byte) 0x30;
                break;
            case FE_PAGE_80:             //Manufacturer's information
                data[1] = (byte) 0xFF;  //Reserved
                data[2] = (byte) 0xFF;  //Reserved
                data[3] = 0;           //HW Revision
                //Manufacturer ID
                byte[] idArray = ByteBuffer.allocate(4).putInt(122).array();
                data[4] = idArray[3];           //LSB   johnson_health_tech:122
                data[5] = idArray[2];           //MSB
                //Model Number
                data[6] = 0;           //LSB
                data[7] = 0;           //MSB
                break;
            case FE_PAGE_81:             //Product Information
                data[1] = (byte) 0xFF;  //Reserved
                data[2] = 0;           //SW Revision(Supplemental)
                data[3] = 0;           //SW Revision(Main)
                //Serial Number(32 bits)
                data[4] = 0;
                data[5] = 0;
                data[6] = 0;
                data[7] = 0;
                break;
            case PWR_PAGE_16:          //Standard Power
                //Update event count
                data[1] = updateEventCounter;
                //Pedal power
                data[2] = (byte) 0x0FF;
                //Instantaneous Cadence
                byte[] cadenceArrayPw16 = ByteBuffer.allocate(4).putInt(dataMatrix.getInt(HALKeys.DATA_RPM_INT)).array();
                data[3] = cadenceArrayPw16[3];
                //Accumulated power
                byte[] powerArrayPwA16 = ByteBuffer.allocate(4).putInt((int) dataMatrix.getFloat(WorkoutKeys.DATA_JOULES_FLOAT)).array();
                data[4] = powerArrayPwA16[3];
                data[5] = powerArrayPwA16[2];
                //Instantaneous Power
                byte[] powerArrayPwI16 = ByteBuffer.allocate(4).putInt((int) dataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT)).array();
                data[6] = powerArrayPwI16[3];
                data[7] = powerArrayPwI16[2];
                break;
            case PWR_PAGE_17:
                //Update event count
                data[1] = (byte) 0x00;
                //Wheel ticks
                data[2] = (byte) 0x00;
                //Instantaneous Cadence
                byte[] cadenceArrayPw17 = ByteBuffer.allocate(4).putInt(dataMatrix.getInt(HALKeys.DATA_RPM_INT)).array();
                data[3] = cadenceArrayPw17[3];
                //Wheel period
                data[4] = (byte) 0x00;
                data[5] = (byte) 0x00;
                //Instantaneous Torque
                data[6] = (byte) 0x00;
                data[7] = (byte) 0x00;
                break;
            case PWR_PAGE_18:
                //Update event count
                data[1] = (byte) 0x00;
                //Crank ticks
                data[2] = (byte) 0x00;
                //Instantaneous Cadence
                byte[] cadenceArrayPw18 = ByteBuffer.allocate(4).putInt(dataMatrix.getInt(HALKeys.DATA_RPM_INT)).array();
                data[3] = cadenceArrayPw18[3];
                //Period
                data[4] = (byte) 0x00;
                data[5] = (byte) 0x00;
                //Instantaneous Torque
                data[6] = (byte) 0x00;
                data[7] = (byte) 0x00;
                break;
            case PWR_PAGE_32:
                //Update event count
                data[1] = (byte) 0x00;
                //Slope
                data[2] = (byte) 0x00;
                data[3] = (byte) 0x00;
                //Time Stamp
                data[4] = (byte) 0x00;
                data[5] = (byte) 0x00;
                //Torque Ticks Stamp
                data[6] = (byte) 0x00;
                data[7] = (byte) 0x00;
                break;
            case BCS_PAGE_0:
                data[1] = (byte) 0xFF;
                data[2] = (byte) 0xFF;
                data[3] = (byte) 0xFF;
                //Bike Speed event time
                data[4] = (byte) 0x00;
                data[5] = (byte) 0x00;
                //Cumulative Speed revolution count
                data[6] = (byte) 0x00;
                data[7] = (byte) 0x00;
                break;
        }

        String packet = "";
        for (byte b : data) {
            packet += String.format("%X", b) + " ";
        }
//        Log.d("Salutron","Page Data: " + packet);

        return buildTxPacket((byte) 0x00, page.getCommandClass(), page.getSubType(), data);

    }

    public byte[] buildFTMSPacket(salutronCommands.FTMS ftmsCommand) {
        byte[] data = new byte[0];
//        salutronCommands.SalutronCommand command = null;
        switch (ftmsCommand) {
            case SET_FTMS_CONFIG: {
                data = new byte[12];/* 2 bytes Fitness Machine Type + 4 bytes Fitness Machine Features + 4 bytes Target Settings Features + 2 bytes FTMS Configuration*/
                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                JHTCommonTypes.MachineClassificationType machineType = frameConfig.machineClassType;
                switch (machineType) {
                    case Treadmill:
			//Machine Types Treadmill
                        byteBuffer.put((byte) 0x01);
                        byteBuffer.put((byte) 0x00);
			//Machine Features All open
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
			//Target Setting Features All open
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
			//FTMS Configuration Flags reference PC simulator
                        byteBuffer.put((byte) 0x02);
                        byteBuffer.put((byte) 0x00);

                        break;
                    case Bike:
			//Machine Types Indoor Bike
                        byteBuffer.put((byte) 0x20);
                        byteBuffer.put((byte) 0x00);
			//Machine Features All open
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
			//Target Setting Features All open
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
			//FTMS Configuration Flags reference PC simulator
                        byteBuffer.put((byte) 0x02);
                        byteBuffer.put((byte) 0x00);

                        break;
                    case Elliptical:
                    case RecumbentElliptical:
			//Machine Types Cross Trainer
                        byteBuffer.put((byte) 0x02);
                        byteBuffer.put((byte) 0x00);
			//Machine Features All open
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
			//Target Setting Features All open
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
			//FTMS Configuration Flags reference PC simulator
                        byteBuffer.put((byte) 0x02);
                        byteBuffer.put((byte) 0x00);

                        break;
                    case Stepper:
			//Machine Types Step Climber
                        byteBuffer.put((byte) 0x04);
                        byteBuffer.put((byte) 0x00);
			//Machine Features All open
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
			//Target Setting Features All open
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
			//FTMS Configuration Flags reference PC simulator
                        byteBuffer.put((byte) 0x02);
                        byteBuffer.put((byte) 0x00);

                        break;
                    case Ascent:
			//Machine Types Cross Trainer
                        byteBuffer.put((byte) 0x02);
                        byteBuffer.put((byte) 0x00);
			//Machine Features All open
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
			//Target Setting Features All open
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
			//FTMS Configuration Flags reference PC simulator
                        byteBuffer.put((byte) 0x02);
                        byteBuffer.put((byte) 0x00);
                        break;
                    case Climbmill:
			//Machine Types Stair Climber
                        byteBuffer.put((byte) 0x08);
                        byteBuffer.put((byte) 0x00);
			//Machine Features All open
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
			//Target Setting Features All open
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
                        byteBuffer.put((byte) 0xFF);
			//FTMS Configuration Flags reference PC simulator
                        byteBuffer.put((byte) 0x02);
                        byteBuffer.put((byte) 0x00);
                        break;
                }
            }
            break;
            case SEND_FTMS_TREADMILL: {
                data = new byte[34];/* 2 bytes Data Flags + 32 bytes Data Structure*/

                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                //Data Flags (2 bytes)
                byteBuffer.put((byte) 0xFF);
                byteBuffer.put((byte) 0xFF);

                //Instantaneous Speed  (2 bytes)
                int intTargetSpeed = (int) (dataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT) * 100);
                byteBuffer.putShort((short) intTargetSpeed);

                //Average Speed (2 bytes)
                int intAverageSpeed = (int) (dataMatrix.getFloat(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT));
                byteBuffer.putShort((short) intAverageSpeed);

                //Total Distance (3 bytes)
                int intDistance = (int) (dataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) * 1000);
                byte byte0Distance = byteGetNthOfInt(intDistance, 0);
                byteBuffer.put(byte0Distance);
                byte byte1Distance = byteGetNthOfInt(intDistance, 1);
                byteBuffer.put(byte1Distance);
                byte byte2Distance = byteGetNthOfInt(intDistance, 2);
                byteBuffer.put(byte2Distance);

                //Inclination (2 bytes)
                int intTargetIncline = (int) (dataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT) * 10);
                byteBuffer.putShort((short) intTargetIncline);

                //Ramp Angle (2 bytes)
                int intRampAngle = 0;
                byteBuffer.putShort((short) intRampAngle);

                //Positive Elevation Gain (2 bytes)
                int intPositiveElevation = (int) (dataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT) * 10);
                byteBuffer.putShort((short) intPositiveElevation);

                //Negative Elevation Gain (2 bytes)
                int intNegativeElevation = 0;
                byteBuffer.putShort((short) intNegativeElevation);

                //Instantaneous Pace (1 bytes) min/km
                int intPace = (int) ((dataMatrix.getInt(WorkoutKeys.DATA_PACE_SEC_PER_KM_INT) * 10) / 60); //unit second /60
	        /*Beacsue FTMS Instantaneous Pace define 1 byte and resolution is 0.1,
		   send to salutron 8700 value max is 255.*/
                if (intPace > 255)
                    intPace = 255;
                byte byte0Pace = byteGetNthOfInt(intPace, 0);
                byteBuffer.put(byte0Pace);

                //Average Pace (1 bytes)
                int intAveragePace = (int) ((dataMatrix.getInt(WorkoutKeys.DATA_AVERAGE_PACE_SEC_PER_KM_INT) * 10) / 60); //unit second /60
	        /*Beacsue FTMS Average Pace define 1 byte and resolution is 0.1,
		   send to salutron 8700 value max is 255.*/
                if (intAveragePace > 255)
                    intAveragePace = 255;
                byte byte0AveragePace = byteGetNthOfInt(intAveragePace, 0);
                byteBuffer.put(byte0AveragePace);

                //Total Energy (2 bytes)
                int intCalories = (int) dataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT);
                byteBuffer.putShort((short) intCalories);

                //Energy Per Hour (2 bytes)
                int intCaloriesPerHour = (int) dataMatrix.getInt(WorkoutKeys.DATA_CALORIES_PER_HOUR_INT);
                byteBuffer.putShort((short) intCaloriesPerHour);

                //Energy Per Minute (1 bytes)
                int intCaloriesPerMin = (int) (intCaloriesPerHour / 60);
	        /*Beacsue FTMS Energy Per Minute define 1 byte ,
		   send to salutron 8700 value max is 255.*/
                if (intCaloriesPerMin > 255)
                    intCaloriesPerMin = 255;
                byte byte0CaloriesPerMin = byteGetNthOfInt(intCaloriesPerMin, 0);
                byteBuffer.put(byte0CaloriesPerMin);

                //Heart Rate (1 bytes)
                int intHeartRate = dataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT);
	        /*Beacsue FTMS Heart Rate define 1 byte ,
		   send to salutron 8700 value max is 255.*/
                if (intHeartRate > 255)
                    intHeartRate = 255;
                byte byte0HeartRate = byteGetNthOfInt(intHeartRate, 0);
                byteBuffer.put(byte0HeartRate);

                //Metabolic Equivalent (1 bytes)
                int intMets = (int) (dataMatrix.getFloat(HALKeys.DATA_METS_FLOAT) * 10);
	        /*Beacsue FTMS Metabolic Equivalent define 1 byte and resolution is 0.1,
		   send to salutron 8700 value max is 255.*/
                if (intMets > 255)
                    intMets = 255;
                byte byte0Mets = byteGetNthOfInt(intMets, 0);
                byteBuffer.put(byte0Mets);

                //Elapsed Time (2 bytes)
                int intElapsedTime = dataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000;
                byteBuffer.putShort((short) intElapsedTime);

                //Remaining Time (2 bytes)
                int intRemainingTime = ((int) dataMatrix.getFloat(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT))/ 1000;
                byteBuffer.putShort((short) intRemainingTime);

                //Force On Belt (2 bytes)
                int intForceOnBelt = 0;
                byteBuffer.putShort((short) intForceOnBelt);

                //Output Power (2 bytes)
                int intOutputPower = (int) dataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT);
                byteBuffer.putShort((short) intOutputPower);

            }
            break;
            case SEND_FTMS_INDOORBIKE: {
                data = new byte[30];/* 2 bytes Data Flags + 28 bytes Data Structure*/

                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                //Data Flags (2 bytes)
                byteBuffer.put((byte) 0xFF);
                byteBuffer.put((byte) 0xFF);

                //Instantaneous Speed  (2 bytes)
                int intSpeed = (int) (dataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT) * 100);
                byteBuffer.putShort((short) intSpeed);

                //Average Speed (2 bytes)
                int intAverageSpeed = (int) (dataMatrix.getFloat(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT) * 100);
                byteBuffer.putShort((short) intAverageSpeed);

                //Instantaneous Cadence  (2 bytes unit: min/km)
                int intInstantaneousCadence = (int) (dataMatrix.getInt(WorkoutKeys.DATA_PACE_SEC_PER_KM_INT) / 30);
                byteBuffer.putShort((short) intInstantaneousCadence);

                //Average Cadence (2 bytes unit: min/km)
                int intAverageCadence = (int) (dataMatrix.getInt(WorkoutKeys.DATA_AVERAGE_PACE_SEC_PER_KM_INT) / 30);
                byteBuffer.putShort((short) intAverageCadence);

                //Total Distance (3 bytes)
                int intDistance = (int) (dataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) * 1000);
                byte byte0Distance = byteGetNthOfInt(intDistance, 0);
                byteBuffer.put(byte0Distance);
                byte byte1Distance = byteGetNthOfInt(intDistance, 1);
                byteBuffer.put(byte1Distance);
                byte byte2Distance = byteGetNthOfInt(intDistance, 2);
                byteBuffer.put(byte2Distance);

                //Resistance Level (2 bytes)
                int intResistanceLevel = dataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT);
                byteBuffer.putShort((short) intResistanceLevel);

                //Instantaneous Power (2 bytes)
                int intInstantaneousPower = (int) dataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT);
                byteBuffer.putShort((short) intInstantaneousPower);

                //Average Power (2 bytes)
                int intAveragePower = (int) dataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_WATTS_FLOAT);
                byteBuffer.putShort((short) intAveragePower);

                //Total Energy (2 bytes)
                int intCalories = (int) dataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT);
                byteBuffer.putShort((short) intCalories);

                //Energy Per Hour (2 bytes)

                int intCaloriesPerHour = (int) (dataMatrix.getInt(WorkoutKeys.DATA_CALORIES_PER_HOUR_INT));
                byteBuffer.putShort((short) intCaloriesPerHour);

                //Energy Per Minute (1 bytes)
                int intCaloriesPerMin = (int) (intCaloriesPerHour / 60);
	        /*Beacsue FTMS Energy Per Minute define 1 byte ,
		   send to salutron 8700 value max is 255.*/
                if (intCaloriesPerMin > 255)
                    intCaloriesPerMin = 255;
                byte byte0CaloriesPerMin = byteGetNthOfInt(intCaloriesPerMin, 0);
                byteBuffer.put(byte0CaloriesPerMin);

                //Heart Rate (1 bytes)
                int intHeartRate = dataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT);
	        /*Beacsue FTMS Heart Rate define 1 byte ,
		   send to salutron 8700 value max is 255.*/
                if (intHeartRate > 255)
                    intHeartRate = 255;
                byte byte0HeartRate = byteGetNthOfInt(intHeartRate, 0);
                byteBuffer.put(byte0HeartRate);

                //Metabolic Equivalent (1 bytes)
                int intMets = (int) (dataMatrix.getFloat(HALKeys.DATA_METS_FLOAT) * 10);
	        /*Beacsue FTMS Metabolic Equivalent define 1 byte and resolution is 0.1,
		   send to salutron 8700 value max is 255.*/
                if (intMets > 255)
                    intMets = 255;
                byte byte0Mets = byteGetNthOfInt(intMets, 0);
                byteBuffer.put(byte0Mets);

                //Elapsed Time (2 bytes)
                int intElapsedTime = dataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000;
                byteBuffer.putShort((short) intElapsedTime);

                //Remaining Time (2 bytes)
                int intRemainingTime = ((int) dataMatrix.getFloat(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT))/ 1000;
                byteBuffer.putShort((short) intRemainingTime);
            }
            break;
            case SEND_FTMS_CROSSTRAINER:{
                data = new byte[41];/* 3 bytes Data Flags + 38 bytes Data Structure*/

                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                //Data Flags (3 bytes)
                byteBuffer.put((byte) 0xFF);
                byteBuffer.put((byte) 0xFF);
                byteBuffer.put((byte) 0xFF);

                //Instantaneous Speed  (2 bytes)
                int intSpeed = (int) (dataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT) * 100);
                byteBuffer.putShort((short) intSpeed);

                //Average Speed (2 bytes)
                int intAverageSpeed = (int) (dataMatrix.getFloat(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT) * 100);
                byteBuffer.putShort((short) intAverageSpeed);

                //Total Distance (3 bytes)
                int intDistance = (int) (dataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) * 1000);
                byte byte0Distance = byteGetNthOfInt(intDistance, 0);
                byteBuffer.put(byte0Distance);
                byte byte1Distance = byteGetNthOfInt(intDistance, 1);
                byteBuffer.put(byte1Distance);
                byte byte2Distance = byteGetNthOfInt(intDistance, 2);
                byteBuffer.put(byte2Distance);

                //Step Per Minute (2 bytes)
                int intStepPerMinute = (int) (dataMatrix.getInt(HALKeys.DATA_RPM_INT) / 2);
                byteBuffer.putShort((short) intStepPerMinute);

                //Average Step Rate (2 bytes)
                int intAverageStepRate = (int) (dataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT) / 2);
                byteBuffer.putShort((short) intAverageStepRate);

                //Stride Count  (2 bytes)
                int intStrideCount = (int) 0;
                byteBuffer.putShort((short) intStrideCount);

                //Positive Elevation Gain (2 bytes)
                int intPositiveElevation = (int) (dataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT) * 10);
                byteBuffer.putShort((short) intPositiveElevation);

                //Negative Elevation Gain (2 bytes)
                int intNegativeElevation = 0;
                byteBuffer.putShort((short) intNegativeElevation);

                //Inclination (2 bytes)
                int intTargetIncline = (int) (dataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT) * 10);
                byteBuffer.putShort((short) intTargetIncline);

                //Ramp Angle (2 bytes)
                int intRampAngle = 0;
                byteBuffer.putShort((short) intRampAngle);

                //Resistance Level (2 bytes)
                int intResistanceLevel = (int) dataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT);
                byteBuffer.putShort((short) intResistanceLevel);

                //Instantaneous Power (2 bytes)
                int intInstantaneousPower = (int) dataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT);
                byteBuffer.putShort((short) intInstantaneousPower);

                //Average Power (2 bytes)
                int intAveragePower = (int) dataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_WATTS_FLOAT);
                byteBuffer.putShort((short) intAveragePower);

                //Total Energy (2 bytes)
                int intCalories = (int) dataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT);
                byteBuffer.putShort((short) intCalories);

                //Energy Per Hour (2 bytes)
                int intCaloriesPerHour = (int) (dataMatrix.getInt(WorkoutKeys.DATA_CALORIES_PER_HOUR_INT));
                byteBuffer.putShort((short) intCaloriesPerHour);

                //Energy Per Minute (1 bytes)
                int intCaloriesPerMin = (int) (intCaloriesPerHour / 60);
	        /*Beacsue FTMS Energy Per Minute define 1 byte ,
		   send to salutron 8700 value max is 255.*/
                if (intCaloriesPerMin > 255)
                    intCaloriesPerMin = 255;
                byte byte0CaloriesPerMin = byteGetNthOfInt(intCaloriesPerMin, 0);
                byteBuffer.put(byte0CaloriesPerMin);

                //Heart Rate (1 bytes)
                int intHeartRate = dataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT);
	        /*Beacsue FTMS Heart Rate define 1 byte ,
		   send to salutron 8700 value max is 255.*/
                if (intHeartRate > 255)
                    intHeartRate = 255;
                byte byte0HeartRate = byteGetNthOfInt(intHeartRate, 0);
                byteBuffer.put(byte0HeartRate);

                //Metabolic Equivalent (1 bytes)
                int intMets = (int) (dataMatrix.getFloat(HALKeys.DATA_METS_FLOAT) * 10);
	        /*Beacsue FTMS Metabolic Equivalent define 1 byte and resolution is 0.1,
		   send to salutron 8700 value max is 255.*/
                if (intMets > 255)
                    intMets = 255;
                byte byte0Mets = byteGetNthOfInt(intMets, 0);
                byteBuffer.put(byte0Mets);

                //Elapsed Time (2 bytes)
                int intElapsedTime = dataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000;
                byteBuffer.putShort((short) intElapsedTime);

                //Remaining Time (2 bytes)
                int intRemainingTime = ((int) dataMatrix.getFloat(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT))/ 1000;
                byteBuffer.putShort((short) intRemainingTime);
            	}
                break;
            case SEND_FTMS_STEP_CLIMBER:{
                data = new byte[23];/* 2 bytes Data Flags + 21 bytes Data Structure*/

                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                //Data Flags (2 bytes)
                byteBuffer.put((byte) 0xFF);
                byteBuffer.put((byte) 0xFF);

                //Floor Count (2 bytes)
                int intFloorCount = (int) dataMatrix.getFloat(WorkoutKeys.DATA_FLOORS_FLOAT);
                byteBuffer.putShort((short) intFloorCount);

                //Step Count (2 bytes)
                int intStepCount = (int) dataMatrix.getInt(WorkoutKeys.DATA_STEPS_INT);
                byteBuffer.putShort((short) intStepCount);

                //Step Per Minute (2 bytes)
                int intStepPerMinute = (int) (dataMatrix.getInt(HALKeys.DATA_RPM_INT) / 2);
                byteBuffer.putShort((short) intStepPerMinute);

                //Average Step Rate (2 bytes)
                int intAverageStepRate = (int) (dataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT) / 2);
                byteBuffer.putShort((short) intAverageStepRate);

                //Positive Elevation Gain (2 bytes)
                int intPositiveElevation = (int) (dataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT) * 10);
                byteBuffer.putShort((short) intPositiveElevation);

                //Total Energy (2 bytes)
                int intCalories = (int) dataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT);
                byteBuffer.putShort((short) intCalories);

                //Energy Per Hour (2 bytes)
                int intCaloriesPerHour = (int) (dataMatrix.getInt(WorkoutKeys.DATA_CALORIES_PER_HOUR_INT));
                byteBuffer.putShort((short) intCaloriesPerHour);

                //Energy Per Minute (1 bytes)
                int intCaloriesPerMin = (int) (intCaloriesPerHour / 60);
	        /*Beacsue FTMS Energy Per Minute define 1 byte ,
		   send to salutron 8700 value max is 255.*/
                if (intCaloriesPerMin > 255)
                    intCaloriesPerMin = 255;
                byte byte0CaloriesPerMin = byteGetNthOfInt(intCaloriesPerMin, 0);
                byteBuffer.put(byte0CaloriesPerMin);

                //Heart Rate (1 bytes)
                int intHeartRate = dataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT);
	        /*Beacsue FTMS Heart Rate define 1 byte ,
		   send to salutron 8700 value max is 255.*/
                if (intHeartRate > 255)
                    intHeartRate = 255;
                byte byte0HeartRate = byteGetNthOfInt(intHeartRate, 0);
                byteBuffer.put(byte0HeartRate);

                //Metabolic Equivalent (1 bytes)
                int intMets = (int) (dataMatrix.getFloat(HALKeys.DATA_METS_FLOAT) * 10);
	        /*Beacsue FTMS Metabolic Equivalent define 1 byte and resolution is 0.1,
		   send to salutron 8700 value max is 255.*/
                if (intMets > 255)
                    intMets = 255;
                byte byte0Mets = byteGetNthOfInt(intMets, 0);
                byteBuffer.put(byte0Mets);

                //Elapsed Time (2 bytes)
                int intElapsedTime = dataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000;
                byteBuffer.putShort((short) intElapsedTime);

                //Remaining Time (2 bytes)
                int intRemainingTime = ((int) dataMatrix.getFloat(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT))/ 1000;
                byteBuffer.putShort((short) intRemainingTime);
            	}
                break;
            case SEND_FTMS_STAIR_CLIMBER:{
                data = new byte[23];/* 2 bytes Data Flags + 21 bytes Data Structure*/

                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                //Data Flags (2 bytes)
                byteBuffer.put((byte) 0xFF);
                byteBuffer.put((byte) 0xFF);

                //Floor Count (2 bytes)
                int intFloorCount = (int) dataMatrix.getFloat(WorkoutKeys.DATA_FLOORS_FLOAT);
                byteBuffer.putShort((short) intFloorCount);

                //Step Per Minute (2 bytes)
                int intStepPerMinute = (int) (dataMatrix.getInt(HALKeys.DATA_SPM_INT) );
                byteBuffer.putShort((short) intStepPerMinute);

                //Average Step Rate (2 bytes)
                int intAverageStepRate = (int) (dataMatrix.getInt(WorkoutKeys.DATA_AVERAGE_SPM_INT));
                byteBuffer.putShort((short) intAverageStepRate);

                //Positive Elevation Gain (2 bytes)
                int intPositiveElevation = (int) (dataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT) * 10);
                byteBuffer.putShort((short) intPositiveElevation);

                //Stride Count  (2 bytes)
                int intStrideCount = (int) 0;
                byteBuffer.putShort((short) intStrideCount);

                //Total Energy (2 bytes)
                int intCalories = (int) dataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT);
                byteBuffer.putShort((short) intCalories);

                //Energy Per Hour (2 bytes)
                int intCaloriesPerHour = (int) (dataMatrix.getInt(WorkoutKeys.DATA_CALORIES_PER_HOUR_INT));
                byteBuffer.putShort((short) intCaloriesPerHour);

                //Energy Per Minute (1 bytes)
                int intCaloriesPerMin = (int) (intCaloriesPerHour / 60);
	        /*Beacsue FTMS Energy Per Minute define 1 byte ,
		   send to salutron 8700 value max is 255.*/
                if (intCaloriesPerMin > 255)
                    intCaloriesPerMin = 255;
                byte byte0CaloriesPerMin = byteGetNthOfInt(intCaloriesPerMin, 0);
                byteBuffer.put(byte0CaloriesPerMin);

                //Heart Rate (1 bytes)
                int intHeartRate = dataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT);
	        /*Beacsue FTMS Heart Rate define 1 byte ,
		   send to salutron 8700 value max is 255.*/
                if (intHeartRate > 255)
                    intHeartRate = 255;
                byte byte0HeartRate = byteGetNthOfInt(intHeartRate, 0);
                byteBuffer.put(byte0HeartRate);

                //Metabolic Equivalent (1 bytes)
                int intMets = (int) (dataMatrix.getFloat(HALKeys.DATA_METS_FLOAT) * 10);
	        /*Beacsue FTMS Metabolic Equivalent define 1 byte and resolution is 0.1,
		   send to salutron 8700 value max is 255.*/
                if (intMets > 255)
                    intMets = 255;
                byte byte0Mets = byteGetNthOfInt(intMets, 0);
                byteBuffer.put(byte0Mets);

                //Elapsed Time (2 bytes)
                int intElapsedTime = dataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000;
                byteBuffer.putShort((short) intElapsedTime);

                //Remaining Time (2 bytes)
                int intRemainingTime = ((int) dataMatrix.getFloat(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT))/ 1000;
                byteBuffer.putShort((short) intRemainingTime);
            	}
                break;
            case SEND_FTMS_STATUS:
                //not support
                break;
            case SEND_FTMS_CONTROL_POINT:
                //not support
                break;
            case SEND_FTMS_SUPPORTED_RANGE:
                break;
        }

        String packet = "";
        for (byte b : data) {
            packet += String.format("%X", b) + " ";
        }
//        Log.d("Salutron", "FTMS Data: " + packet);

        return buildTxPacket((byte) 0x00, ftmsCommand.getCommandClass(), ftmsCommand.getSubType(), data);

    }


    public byte[] buildTxPacket(salutronCommands.SalutronCommand command, byte... data) {
        return buildTxPacket((byte) 0x01, command.getCommandClass(), command.getSubType(), data);
    }

    public byte[] buildTxPacket(salutronCommands.SalutronCommand command) {
        return buildTxPacket((byte) 0x01, command.getCommandClass(), command.getSubType(), command.getSubData());
    }

    public byte[] buildTxPacket(byte response,salutronCommands.SalutronCommand command) {
        return buildTxPacket(response, command.getCommandClass(), command.getSubType(), command.getSubData());
    }

    public byte[] buildTxPacket(byte response, byte commandClass, byte subType, byte... data) {
        byte[] txPacket = new byte[data.length + 7];

        txPacket[packetFrame.START.ordinal()] = (byte) 0x0B;
        txPacket[packetFrame.LENGTH.ordinal()] = (byte) (data.length + 7);
        txPacket[packetFrame.RESPONSE.ordinal()] = response;
        txPacket[packetFrame.CLASS.ordinal()] = commandClass;
        txPacket[packetFrame.SUB.ordinal()] = subType;

        for (int i = 0; i < data.length; i++) {
            txPacket[i + packetFrame.DATA.ordinal()] = data[i];
        }

        byte[] checkSumData = new byte[data.length + 3];
        byte[] checkSum;

        for (int a = 0; a < data.length + 3; a++) {
            checkSumData[a] = txPacket[a + 4];
        }

        checkSum = FletcherChecksum.check_bytes(checkSumData);

        txPacket[packetFrame.CHECKSUM_1.ordinal()] = checkSum[0];
        txPacket[packetFrame.CHECKSUM_2.ordinal()] = checkSum[1];

        return txPacket;

    }

    public void parse(byte[] packet, int command_len) {
        byte[] checkSumData = new byte[packet.length - 2];
        byte[] checkSum;
        byte[] data = new byte[packet.length - 5];

        if (packet != null) {

            for (int index = 0; index < 3; index++) {
                switch (parseState) {
                    case PARSE_CHECKSUM:
                        for (int i = 2; i < packet.length; i++) {
                            checkSumData[i - 2] = packet[i];
                        }
                        checkSum = FletcherChecksum.check_bytes(checkSumData);
                        if (checkSum[0] == packet[0] && checkSum[1] == packet[1]) {
                            parseState = ParseState.PARSE_DATA;
                        }
                        break;
                    case PARSE_CLASS_SUB:
                        ByteBuffer classSub = ByteBuffer.allocate(2);
                        classSub.put(packet[3]);
                        classSub.put(packet[4]);
                        classSub.flip();
                        short classSubValue = classSub.order(ByteOrder.BIG_ENDIAN).getShort();
                        SalutronManager.Instance.receiveMessage(classSubValue, data);
                        parseState = ParseState.PARSE_CHECKSUM;
                        break;
                    case PARSE_DATA:
                        for (int i = 0; i < packet.length - 5; i++) {
                            data[i] = packet[i + 5];
                        }
                        parseState = ParseState.PARSE_CLASS_SUB;
                        break;
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : byteGetNthOfInt
     * Description    : process to get Nth byte of int
     * Input          : int , number of Nth byte(0-3)
     * Output         : None
     * Return         : byte
     *******************************************************************************/
    private byte byteGetNthOfInt(int intValue, int number) {
        int inputValue = intValue;
        byte rtnByte = (byte) 0x00;
        switch (number) {
            case 0:
                inputValue = intValue;
                break;
            case 1:
                inputValue = inputValue >> 8;
                break;
            case 2:
                inputValue = inputValue >> 16;
                break;
            case 3:
                inputValue = inputValue >> 24;
                break;
        }
        rtnByte = (byte) (inputValue & 0xFF);

        return rtnByte;
    }
}
