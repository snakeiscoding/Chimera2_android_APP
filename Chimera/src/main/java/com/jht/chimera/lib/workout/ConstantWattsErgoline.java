package com.jht.chimera.lib.workout;

public class ConstantWattsErgoline {
    public static String getWorkoutScript() {
        return "settings\n" +
                "    workout_id = 90\n" +
                "    allow_skip_warmup = 0\n" +
                "    allow_skip_workout = 1\n" +
                "    graph_future = 0\n" +
                "    allow_user_goal_changes = 1\n" +
                "\n" +
                "input WATTS\n" +
                "input TIME_GOAL_START\n" +
                "input TARGET_WATTS_START\n" +
                "input TARGET_WATTS\n" +
                "input COOLDOWN_TIME_START\n" +
                "input RESISTANCE_IN\n" +
                // 0 = No, 1 = Yes
                "input LCB_CONTROLS_WATTS\n" +
                "\n" +
                "export ALLOW_PAUSE\n" +
                "export RESISTANCE\n" +
                "export INCLINE\n" +
                "export GOAL\n" +
                "export GOAL_TYPE\n" +
                "export TARGET_WATTS\n" +
                "export PREVENT_USER_GOAL_CHANGES\n" +
                "export PREVENT_USER_CHANGES\n" +
                // 0 = Resistance, 1 = Watts
                "export CONTROL_MODE\n" +

                "\n" +
                "stage \"workout\"\n" +
                "    PREVENT_USER_CHANGES = 1\n" +
                "    ALLOW_PAUSE = 0\n" +
                "    PREVENT_USER_GOAL_CHANGES = 1\n" +
                "    interval\n" +
                "        GOAL_TYPE = GOAL_TYPE_TIME_STAGE_CONSTANT\n" +
                "        GOAL = TIME_GOAL_START\n" +
                "\n" +
                "        CONTROL_MODE = LCB_CONTROLS_WATTS\n" +
                "\n" +
                "        TARGET_WATTS = TARGET_WATTS_START\n" +
                "        when INTERVAL_TIME_MS >= GOAL\n" +
                "            interval = interval + 1\n" +
                "        \n" +
                "        when LCB_CONTROLS_WATTS == 0 && TARGET_WATTS - 50 >= WATTS\n" +
                "            RESISTANCE = RESISTANCE_IN + 3\n" +
                "            wait 2000\n" +
                "        else when LCB_CONTROLS_WATTS == 0 && TARGET_WATTS - 50 < WATTS && TARGET_WATTS - 30 >= WATTS\n" +
                "            RESISTANCE = RESISTANCE_IN + 2\n" +
                "            wait 2000\n" +
                "        else when LCB_CONTROLS_WATTS == 0 && TARGET_WATTS - 30 < WATTS && TARGET_WATTS - 10 >= WATTS\n" +
                "            RESISTANCE = RESISTANCE_IN + 1\n" +
                "            wait 2000\n" +
                "        else when LCB_CONTROLS_WATTS == 0 && TARGET_WATTS + 10 <= WATTS && TARGET_WATTS + 30 >= WATTS\n" +
                "            RESISTANCE = max(RESISTANCE_IN - 1, 1)\n" +
                "            wait 2000\n" +
                "        else when LCB_CONTROLS_WATTS == 0 && TARGET_WATTS + 30 < WATTS && TARGET_WATTS + 50 >= WATTS\n" +
                "            RESISTANCE = max(RESISTANCE_IN - 2, 1)\n" +
                "            wait 2000\n" +
                "        else when LCB_CONTROLS_WATTS == 0 && TARGET_WATTS + 50 <= WATTS\n" +
                "            RESISTANCE = max(RESISTANCE_IN - 3, 1)\n" +
                "            wait 2000\n";
    }
}
