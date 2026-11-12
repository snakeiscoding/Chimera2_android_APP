package com.jht.chimera.lib.workout.generator;

import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.GOAL;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.GOAL_TYPE;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.GOAL_TYPE_TIME_STAGE;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.INCLINE;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.INCLINE_IN;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.INTERVAL_GOAL;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.INTERVAL_GOAL_TYPE;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.IS_RAMPING;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.IS_SPRINTING;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.NEXT_INCLINE;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.NEXT_RESISTANCE;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.NEXT_SPEED;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.PREVENT_USER_GOAL_CHANGES;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.RAMP_RATE_INCLINE;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.RAMP_RATE_RESISTANCE;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.RAMP_RATE_SPEED;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.RESISTANCE;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.RESISTANCE_IN;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.SPEED;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.SPEED_IN;
import static com.jht.workoutcontrollerlib.WorkoutController.INTERVAL;
import static com.jht.workoutcontrollerlib.WorkoutController.INTERVAL_TIME_MS;

import android.util.Log;

import com.jht.chimera.lib.common.ObjectUtil;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.workoutcontrollerlib.DefinitionInfo;
import com.jht.workoutcontrollerlib.evaluator.ExpressionEvaluator;
import com.jht.workoutcontrollerlib.model.DataModel;
import com.jht.workoutcontrollerlib.parser.ScriptException;
import com.jht.workoutcontrollerlib.workoutcardio.CardioDefinitionInfo;
import com.jht.workoutcontrollerlib.workoutcardio.WorkoutSettings;
import com.jht.workoutcontrollerlib.workoutdefinition.Interval;
import com.jht.workoutcontrollerlib.workoutdefinition.Level;
import com.jht.workoutcontrollerlib.workoutdefinition.SpecialCondition;
import com.jht.workoutcontrollerlib.workoutdefinition.Stage;
import com.jht.workoutcontrollerlib.workoutdefinition.WhenCondition;
import com.jht.workoutcontrollerlib.workoutdefinition.WorkoutDefinition;
import com.jht.workoutcontrollerlib.workoutdefinition.statements.EndStatement;
import com.jht.workoutcontrollerlib.workoutdefinition.statements.IExecutableStatement;
import com.jht.workoutcontrollerlib.workoutdefinition.statements.IfStatement;
import com.jht.workoutcontrollerlib.workoutdefinition.statements.SetStatement;
import com.jht.workoutcontrollerlib.workoutdefinition.statements.SettingValue;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates a workout definition based on a set of passed in intervals. This is specifically tailored
 * to create a Sprint8 workout. Sprint8 workouts are a set of sprints along with recovery periods.
 */
public class Sprint8Generator {
    public static final int TREADMILL_TYPE = 0;
    public static final int ASCENT_TYPE = 1;
    public static final int RESISTANCE_TYPE = 2;
    private static final float MINUTES_PER_SPRINT = 2.0f; // each sprint has duration of 2 minutes
    private static final float BASE_SPRINT_MINUTES = 0.5f; // last sprint has duration of 30 seconds
    private static final float MAX_SPRINT_MINUTES = 14.5f; // max number of sprint is 14.5 minutes as per current design

    public static CardioDefinitionInfo createDefinition(BaseSprint8Config sprint8Config) {
        List<SettingValue> settings = buildSettings();
        List<String> inputs = buildInputs(sprint8Config.type);
        List<String> exports = buildExports(sprint8Config.type);
        List<IExecutableStatement> statements = getGlobalStatements(sprint8Config.type);
        List<WhenCondition> whenConditions = getGlobalWhenConditions();
        List<Stage> stages = getStages(sprint8Config);

        WorkoutDefinition definition = new WorkoutDefinition(settings, inputs, exports, statements,
                whenConditions, stages, null, null, null, null);

        return new CardioDefinitionInfo(new DefinitionInfo(definition));
    }

    private static List<SettingValue> buildSettings() {
        List<SettingValue> settingValues = new ArrayList<>();
        settingValues.add(new SettingValue(WorkoutSettings.WORKOUT_ID, 18));
        settingValues.add(new SettingValue(WorkoutSettings.ALLOW_SKIP_WARMUP, 0));
        settingValues.add(new SettingValue(WorkoutSettings.ALLOW_SKIP_WORKOUT, 1));
        settingValues.add(new SettingValue(WorkoutSettings.GRAPH_FUTURE, 1));
        return settingValues;
    }

    private static List<String> buildInputs(int type) {
        List<String> inputs = new ArrayList<>();
        switch (type) {
            case TREADMILL_TYPE:
                inputs.add(SPEED_IN);
                inputs.add(INCLINE_IN);
                inputs.add(RAMP_RATE_SPEED);
                inputs.add(RAMP_RATE_INCLINE);
                break;
            case ASCENT_TYPE:
                inputs.add(INCLINE_IN);
                inputs.add(RESISTANCE_IN);
                inputs.add(RAMP_RATE_INCLINE);
                inputs.add(RAMP_RATE_RESISTANCE);
                break;
            case RESISTANCE_TYPE:
                inputs.add(RESISTANCE_IN);
                inputs.add(RAMP_RATE_RESISTANCE);
                break;
        }

        return inputs;
    }

    private static List<String> buildExports(int type) {
        List<String> exports = new ArrayList<>();
        switch (type) {
            case TREADMILL_TYPE:
                exports.add(SPEED);
                exports.add(INCLINE);
                exports.add(GOAL_TYPE);
                exports.add(GOAL);
                exports.add(PREVENT_USER_GOAL_CHANGES);
                exports.add(NEXT_SPEED);
                exports.add(NEXT_INCLINE);
                exports.add(IS_RAMPING);
                exports.add(IS_SPRINTING);
                exports.add(INTERVAL_GOAL);
                exports.add(INTERVAL_GOAL_TYPE);
                break;
            case ASCENT_TYPE:
                exports.add(INCLINE);
                exports.add(RESISTANCE);
                exports.add(GOAL_TYPE);
                exports.add(GOAL);
                exports.add(PREVENT_USER_GOAL_CHANGES);
                exports.add(NEXT_INCLINE);
                exports.add(NEXT_RESISTANCE);
                exports.add(IS_RAMPING);
                exports.add(IS_SPRINTING);
                exports.add(INTERVAL_GOAL);
                exports.add(INTERVAL_GOAL_TYPE);
                break;
            case RESISTANCE_TYPE:
                exports.add(RESISTANCE);
                exports.add(GOAL_TYPE);
                exports.add(GOAL);
                exports.add(PREVENT_USER_GOAL_CHANGES);
                exports.add(NEXT_RESISTANCE);
                exports.add(IS_RAMPING);
                exports.add(IS_SPRINTING);
                exports.add(INTERVAL_GOAL);
                exports.add(INTERVAL_GOAL_TYPE);
                break;
        }

        return exports;
    }

    private static List<IExecutableStatement> getGlobalStatements(int type) {
        List<IExecutableStatement> statements = new ArrayList<>();
        switch (type) {
            case TREADMILL_TYPE:
                statements.add(new SetStatement(NEXT_SPEED, 0));
                statements.add(new SetStatement(NEXT_INCLINE, 0));
                statements.add(new SetStatement(IS_RAMPING, 0));
                break;
            case ASCENT_TYPE:
                statements.add(new SetStatement(NEXT_RESISTANCE, 0));
                statements.add(new SetStatement(NEXT_INCLINE, 0));
                statements.add(new SetStatement(IS_RAMPING, 0));
                break;
            case RESISTANCE_TYPE:
                statements.add(new SetStatement(NEXT_RESISTANCE, 0));
                statements.add(new SetStatement(IS_RAMPING, 0));
                break;
        }

        return statements;
    }

    private static List<WhenCondition> getGlobalWhenConditions() {
        // There are no global when conditions for sprint8
        return new ArrayList<>();
    }

    private static List<Stage> getStages(BaseSprint8Config config) {
        List<Stage> stages = new ArrayList<>();
        stages.add(getWarmupStage(config));
        stages.add(getWorkoutStage(config));
        stages.add(getCooldownStage(config));
        return stages;
    }

    private static Stage getWarmupStage(BaseSprint8Config config) {
        String name = "warmup"; //NON-NLS
        List<IExecutableStatement> statements = getWarmupStatements(config);
        List<WhenCondition> whenConditions = getWarmupWhenConditions(config);
        List<Level> levels = getWarmupLevels(config);

        return new Stage(name, statements, whenConditions, levels);
    }

    private static List<IExecutableStatement> getWarmupStatements(BaseSprint8Config config) {
        List<IExecutableStatement> statements = new ArrayList<>();

        statements.add(new SetStatement(PREVENT_USER_GOAL_CHANGES, 1));
        statements.add(new SetStatement(GOAL_TYPE, ConsoleWorkoutVariables.GOAL_TYPE_TIME_STAGE));
        statements.add(new SetStatement(GOAL, 180000)); // warmup time is always 3 minutes
        statements.add(new SetStatement(INTERVAL_GOAL_TYPE, ConsoleWorkoutVariables.GOAL_TYPE_TIME));
        statements.add(new SetStatement(INTERVAL_GOAL, 180000));
        statements.add(new SetStatement(IS_SPRINTING, 0));

        // Set the initial values for the frame parameters
        for (DataModel setData : config.warmupValues) {
            SetStatement statement = new SetStatement(setData.getParameter(), setData.getValue());
            statements.add(statement);
        }

        return statements;
    }

    @NonNls private static final String TAG = "Sprint8Generator";

    private static List<WhenCondition> getWarmupWhenConditions(BaseSprint8Config config) {
        try {
            ExpressionEvaluator rampCondition = getRampCondition(config.type);
            List<IExecutableStatement> rampStatements = getRampStatements(config.type);
            WhenCondition rampWhen = new WhenCondition(rampCondition, 0, rampStatements);

            ExpressionEvaluator advanceIntervalCondition = new ExpressionEvaluator(INTERVAL_TIME_MS + ">=" + GOAL);
            List<IExecutableStatement> advanceIntervalStatements = new ArrayList<>();
            advanceIntervalStatements.add(new SetStatement(IS_RAMPING, 0));
            advanceIntervalStatements.add(new SetStatement(INTERVAL, new ExpressionEvaluator(INTERVAL + " + 1")));
            WhenCondition advanceIntervalWhen = new WhenCondition(advanceIntervalCondition, 0, advanceIntervalStatements);

            List<WhenCondition> whenConditions = new ArrayList<>();
            whenConditions.add(rampWhen);
            whenConditions.add(advanceIntervalWhen);
            return whenConditions;
        } catch (ScriptException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    private static ExpressionEvaluator getRampCondition(int type) {
        try {
            @NonNls String expressionString = "";
            switch (type) {
                case TREADMILL_TYPE:
                    expressionString = IS_RAMPING + " != 1 && " +
                            "((" + INTERVAL_TIME_MS + " >= " + GOAL + " - (abs(" + NEXT_SPEED + " - " + SPEED_IN + ") / " + RAMP_RATE_SPEED + ") * 1000) ||" + // NON-NLS
                            "(" + INTERVAL_TIME_MS + " >= " + GOAL + " - (abs(" + NEXT_INCLINE + " - " + INCLINE_IN + ") / " + RAMP_RATE_INCLINE + ") * 1000))"; // NON-NLS
                    break;
                case ASCENT_TYPE:
                    expressionString = IS_RAMPING + " != 1 && " +
                            "((" + INTERVAL_TIME_MS + " >= " + GOAL + " - (abs(" + NEXT_RESISTANCE + " - " + RESISTANCE_IN + ") / " + RAMP_RATE_RESISTANCE + ") * 1000) ||" + // NON-NLS
                            "(" + INTERVAL_TIME_MS + " >= " + GOAL + " - (abs(" + NEXT_INCLINE + " - " + INCLINE_IN + ") / " + RAMP_RATE_INCLINE + ") * 1000))"; // NON-NLS
                    break;
                case RESISTANCE_TYPE:
                    expressionString = IS_RAMPING + " != 1 && " + // NON-NLS
                            "(" + INTERVAL_TIME_MS + " >= " + GOAL + " - (abs(" + NEXT_RESISTANCE + " - " + RESISTANCE_IN + ") / " + RAMP_RATE_RESISTANCE + ") * 1000)"; // NON-NLS
                    break;
            }

            return new ExpressionEvaluator(expressionString);
        } catch (ScriptException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    private static List<IExecutableStatement> getRampStatements(int type) {
        try {
            List<IExecutableStatement> rampStatements = new ArrayList<>();
            switch (type) {
                case TREADMILL_TYPE:
                    rampStatements.add(new SetStatement(SPEED, new ExpressionEvaluator(NEXT_SPEED)));
                    rampStatements.add(new SetStatement(INCLINE, new ExpressionEvaluator(NEXT_INCLINE)));
                    break;
                case ASCENT_TYPE:
                    rampStatements.add(new SetStatement(RESISTANCE, new ExpressionEvaluator(NEXT_RESISTANCE)));
                    rampStatements.add(new SetStatement(INCLINE, new ExpressionEvaluator(NEXT_INCLINE)));
                    break;
                case RESISTANCE_TYPE:
                    rampStatements.add(new SetStatement(RESISTANCE, new ExpressionEvaluator(NEXT_RESISTANCE)));
                    break;
            }
            rampStatements.add(new SetStatement(IS_RAMPING, 1));

            return rampStatements;
        } catch (ScriptException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    private static List<Level> getWarmupLevels(BaseSprint8Config config) {
        List<Level> levels = new ArrayList<>();

        String name = "";
        List<IExecutableStatement> statements = new ArrayList<>();
        List<WhenCondition> whenConditions = new ArrayList<>();
        List<Interval> intervals = getWarmupIntervals(config);

        Level warmupLevel = new Level(name, statements, whenConditions, intervals,
                null, null, null, null);
        levels.add(warmupLevel);
        return levels;
    }

    private static List<Interval> getWarmupIntervals(BaseSprint8Config config) {
        List<Interval> intervals = new ArrayList<>();

        String name = "";
        List<WhenCondition> whenConditions = new ArrayList<>();
        List<IExecutableStatement> executableStatements = getWarmupSetNextValueStatements(config);
        intervals.add(new Interval(name, executableStatements, whenConditions));
        return intervals;
    }

    private static List<IExecutableStatement> getWarmupSetNextValueStatements(BaseSprint8Config config) {
        List<IExecutableStatement> statements = new ArrayList<>();
        switch (config.type) {
            case TREADMILL_TYPE:
                statements.add(new SetStatement(NEXT_SPEED, getNextValue(config, SPEED, 0, false)));
                statements.add(new SetStatement(NEXT_INCLINE, getNextValue(config, INCLINE, 0, false)));
                break;
            case ASCENT_TYPE:
                statements.add(new SetStatement(NEXT_RESISTANCE, getNextValue(config, RESISTANCE, 0, false)));
                statements.add(new SetStatement(NEXT_INCLINE, getNextValue(config, INCLINE, 0, false)));
                break;
            case RESISTANCE_TYPE:
                statements.add(new SetStatement(NEXT_RESISTANCE, getNextValue(config, RESISTANCE, 0, false)));
                break;
        }

        return statements;
    }

    private static float getNextValue(BaseSprint8Config config, String parameter, int sprint, boolean getRecovery) {
        if (sprint < config.intervals.size()) {
            float value = 0;
            List<DataModel> dataModels = getRecovery ? config.intervals.get(sprint).recoveryStatements : config.intervals.get(sprint).sprintStatements;
            for (DataModel sprintData : dataModels) {
                if (sprintData.getParameter().equals(parameter)) {
                    value = sprintData.getValue();
                }
            }

            return value;
        } else {
            return 0;
        }
    }

    private static Stage getWorkoutStage(BaseSprint8Config config) {
        String name = "workout"; // NON-NLS
        List<IExecutableStatement> statements = getWorkoutStatements(config);
        List<WhenCondition> whenConditions = getWorkoutWhenConditions(config);
        List<Level> levels = getWorkoutLevels(config);
        SpecialCondition resumeCondition = getWorkoutResumeCondition();

        return new Stage(name, statements, whenConditions, levels, resumeCondition, null, null, null);
    }

    private static List<IExecutableStatement> getWorkoutStatements(BaseSprint8Config config) {
        List<IExecutableStatement> statements = new ArrayList<>();
        statements.add(new SetStatement(GOAL_TYPE, GOAL_TYPE_TIME_STAGE));
        float totalWorkoutMinutes = MINUTES_PER_SPRINT * (config.intervals.size() - 1) + BASE_SPRINT_MINUTES;
        totalWorkoutMinutes = ObjectUtil.coerce(totalWorkoutMinutes, BASE_SPRINT_MINUTES, MAX_SPRINT_MINUTES);
        statements.add(new SetStatement(GOAL, totalWorkoutMinutes * 60 * 1000));
        statements.add(new SetStatement(PREVENT_USER_GOAL_CHANGES, 1));

        statements.add(new SetStatement("currentInterval", -1)); // NON-NLS
        statements.add(new SetStatement("nextSpeedInterval", -1)); // NON-NLS
        statements.add(new SetStatement("timeLeftInInterval", 0)); // NON-NLS
        return statements;
    }

    private static List<WhenCondition> getWorkoutWhenConditions(BaseSprint8Config config) {
        try {
            List<WhenCondition> whenConditions = new ArrayList<>();

            ExpressionEvaluator intervalNotEqualCondition = new ExpressionEvaluator("interval != currentInterval"); // NON-NLS
            List<IExecutableStatement> intervalNotEqualStatements = new ArrayList<>();
            intervalNotEqualStatements.add(new SetStatement("currentInterval", new ExpressionEvaluator(INTERVAL))); // NON-NLS
            intervalNotEqualStatements.add(new SetStatement(GOAL, new ExpressionEvaluator(GOAL + "- timeLeftInInterval"))); // NON-NLS
            WhenCondition intervalNotEqualWhen = new WhenCondition(intervalNotEqualCondition, 0, intervalNotEqualStatements);

            ExpressionEvaluator alwaysTrueCondition = new ExpressionEvaluator("1");
            List<IExecutableStatement> alwaysTrueStatements = new ArrayList<>();
            alwaysTrueStatements.add(new SetStatement("timeLeftInInterval", new ExpressionEvaluator(INTERVAL_GOAL + " - " + INTERVAL_TIME_MS))); // NON-NLS
            WhenCondition alwaysTrueWhen = new WhenCondition(alwaysTrueCondition, 0, alwaysTrueStatements);

            ExpressionEvaluator rampExpression = getWorkoutRampCondition(config.type);
            List<IExecutableStatement> rampStatements = new ArrayList<>();
            rampStatements.add(new SetStatement(IS_RAMPING, 1));
            switch (config.type) {
                case TREADMILL_TYPE:
                    rampStatements.add(new SetStatement(SPEED, new ExpressionEvaluator(NEXT_SPEED)));
                    rampStatements.add(new SetStatement(INCLINE, new ExpressionEvaluator(NEXT_INCLINE)));
                    break;
                case ASCENT_TYPE:
                    rampStatements.add(new SetStatement(INCLINE, new ExpressionEvaluator(NEXT_INCLINE)));
                    rampStatements.add(new SetStatement(RESISTANCE, new ExpressionEvaluator(NEXT_RESISTANCE)));
                    break;
                case RESISTANCE_TYPE:
                    rampStatements.add(new SetStatement(RESISTANCE, new ExpressionEvaluator(NEXT_RESISTANCE)));
                    break;
            }
            WhenCondition rampWhen = new WhenCondition(rampExpression, 0, rampStatements);

            ExpressionEvaluator updateIntervalCondition = new ExpressionEvaluator(INTERVAL_TIME_MS + ">=" + INTERVAL_GOAL);
            List<IExecutableStatement> updateIntervalStatements = new ArrayList<>();
            updateIntervalStatements.add(new SetStatement(IS_RAMPING, 0));
            updateIntervalStatements.add(new SetStatement(INTERVAL, new ExpressionEvaluator(INTERVAL + " + 1")));
            WhenCondition updateIntervalWhen = new WhenCondition(updateIntervalCondition, 0, updateIntervalStatements);

            whenConditions.add(intervalNotEqualWhen);
            whenConditions.add(alwaysTrueWhen);
            whenConditions.add(rampWhen);
            whenConditions.add(updateIntervalWhen);

            return whenConditions;
        } catch (ScriptException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    private static ExpressionEvaluator getWorkoutRampCondition(int type) {
        try {
            String expressionString = "";
            switch (type) {
                case TREADMILL_TYPE:
                    expressionString = IS_RAMPING + " != 1 && " + INTERVAL_GOAL + " == 90000 && " + // NON-NLS
                            "((" + INTERVAL_TIME_MS + " >= " + INTERVAL_GOAL + " - (abs(" + NEXT_SPEED + " - " + SPEED_IN + ") / " + RAMP_RATE_SPEED + ") * 1000) ||" + // NON-NLS
                            "(" + INTERVAL_TIME_MS + " >= " + INTERVAL_GOAL + " - (abs(" + NEXT_INCLINE + " - " + INCLINE_IN + ") / " + RAMP_RATE_INCLINE + ") * 1000))"; // NON-NLS
                    break;
                case ASCENT_TYPE:
                    expressionString = IS_RAMPING + " != 1 && " + INTERVAL_GOAL + " == 90000 && " +
                            "((" + INTERVAL_TIME_MS + " >= " + INTERVAL_GOAL + " - (abs(" + NEXT_RESISTANCE + " - " + RESISTANCE_IN + ") / " + RAMP_RATE_RESISTANCE + ") * 1000) ||" + // NON-NLS
                            "(" + INTERVAL_TIME_MS + " >= " + INTERVAL_GOAL + " - (abs(" + NEXT_INCLINE + " - " + INCLINE_IN + ") / " + RAMP_RATE_INCLINE + ") * 1000))"; // NON-NLS
                    break;
                case RESISTANCE_TYPE:
                    expressionString = IS_RAMPING + " != 1 && " + INTERVAL_GOAL + " == 90000 && " + // NON-NLS
                            "(" + INTERVAL_TIME_MS + " >= " + INTERVAL_GOAL + " - (abs(" + NEXT_RESISTANCE + " - " + RESISTANCE_IN + ") / " + RAMP_RATE_RESISTANCE + ") * 1000)"; // NON-NLS
                    break;
            }

            return new ExpressionEvaluator(expressionString);
        } catch (ScriptException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    private static List<Level> getWorkoutLevels(BaseSprint8Config config) {
        List<Level> levels = new ArrayList<>();
        String name = "";
        List<IExecutableStatement> statements = new ArrayList<>();
        List<WhenCondition> whenConditions = new ArrayList<>();
        List<Interval> intervals = getWorkoutIntervals(config);
        levels.add(new Level(name, statements, whenConditions, intervals,
                null, null, null, null));
        return levels;
    }

    private static List<Interval> getWorkoutIntervals(BaseSprint8Config config) {
        List<Interval> intervals = new ArrayList<>();
        List<Sprint8Interval> configIntervals = config.intervals;
        for (int i = 0; i < configIntervals.size(); i++) {
            intervals.add(createSprintInterval(config, i));
            if (i + 1 < configIntervals.size()) {
                intervals.add(createRecoveryInterval(config, i));
            }
        }

        return intervals;
    }

    private static Interval createSprintInterval(BaseSprint8Config config, int sprintIndex) {
        List<IExecutableStatement> statements = new ArrayList<>();
        Sprint8Interval currentInterval = config.intervals.get(sprintIndex);
        statements.add(new SetStatement(INTERVAL_GOAL, 30000));
        statements.add(new SetStatement(IS_SPRINTING, 1));

        switch (config.type) {
            case TREADMILL_TYPE:
                statements.add(new SetStatement(SPEED, getValue(SPEED, currentInterval.sprintStatements)));
                statements.add(new SetStatement(INCLINE, getValue(INCLINE, currentInterval.sprintStatements)));
                statements.add(new SetStatement(NEXT_SPEED, getValue(SPEED, currentInterval.recoveryStatements)));
                statements.add(new SetStatement(NEXT_INCLINE, getValue(INCLINE, currentInterval.recoveryStatements)));
                break;
            case ASCENT_TYPE:
                statements.add(new SetStatement(RESISTANCE, getValue(RESISTANCE, currentInterval.sprintStatements)));
                statements.add(new SetStatement(INCLINE, getValue(INCLINE, currentInterval.sprintStatements)));
                statements.add(new SetStatement(NEXT_RESISTANCE, getValue(RESISTANCE, currentInterval.recoveryStatements)));
                statements.add(new SetStatement(NEXT_INCLINE, getValue(INCLINE, currentInterval.recoveryStatements)));
                break;
            case RESISTANCE_TYPE:
                statements.add(new SetStatement(RESISTANCE, getValue(RESISTANCE, currentInterval.sprintStatements)));
                statements.add(new SetStatement(NEXT_RESISTANCE, getValue(RESISTANCE, currentInterval.recoveryStatements)));
                break;
        }

        return new Interval("", statements, new ArrayList<WhenCondition>());
    }

    private static Interval createRecoveryInterval(BaseSprint8Config config, int sprintIndex) {
        List<IExecutableStatement> statements = new ArrayList<>();
        Sprint8Interval currentInterval = config.intervals.get(sprintIndex);
        Sprint8Interval nextInterval = config.intervals.size() > (sprintIndex + 1) ? config.intervals.get(sprintIndex + 1) : null;
        statements.add(new SetStatement(INTERVAL_GOAL, 90000));
        statements.add(new SetStatement(IS_SPRINTING, 0));

        switch (config.type) {
            case TREADMILL_TYPE:
                statements.add(new SetStatement(SPEED, getValue(SPEED, currentInterval.recoveryStatements)));
                statements.add(new SetStatement(INCLINE, getValue(INCLINE, currentInterval.recoveryStatements)));
                if (nextInterval != null) {
                    statements.add(new SetStatement(NEXT_SPEED, getValue(SPEED, nextInterval.sprintStatements)));
                    statements.add(new SetStatement(NEXT_INCLINE, getValue(INCLINE, nextInterval.sprintStatements)));
                }
                break;
            case ASCENT_TYPE:
                statements.add(new SetStatement(RESISTANCE, getValue(RESISTANCE, currentInterval.recoveryStatements)));
                statements.add(new SetStatement(INCLINE, getValue(INCLINE, currentInterval.recoveryStatements)));
                if (nextInterval != null) {
                    statements.add(new SetStatement(NEXT_RESISTANCE, getValue(RESISTANCE, nextInterval.sprintStatements)));
                    statements.add(new SetStatement(NEXT_INCLINE, getValue(INCLINE, nextInterval.sprintStatements)));
                }
                break;
            case RESISTANCE_TYPE:
                statements.add(new SetStatement(RESISTANCE, getValue(RESISTANCE, currentInterval.recoveryStatements)));
                if (nextInterval != null) {
                    statements.add(new SetStatement(NEXT_RESISTANCE, getValue(RESISTANCE, nextInterval.sprintStatements)));
                }
                break;
        }

        return new Interval("", statements, new ArrayList<WhenCondition>());
    }

    private static float getValue(String parameter, List<DataModel> values) {
        if (values == null) return 0;

        for (DataModel value : values) {
            if (parameter.equals(value.getParameter())) {
                return value.getValue();
            }
        }

        return 0;
    }

    private static SpecialCondition getWorkoutResumeCondition() {
        try {
            List<IExecutableStatement> statements = new ArrayList<>();

            ExpressionEvaluator condition = new ExpressionEvaluator(INTERVAL_GOAL + " == 30000");
            List<IExecutableStatement> trueStatements = new ArrayList<>();
            trueStatements.add(new SetStatement(INTERVAL, new ExpressionEvaluator(INTERVAL + " + 1")));
            IfStatement ifStatement = new IfStatement(condition, trueStatements);
            statements.add(ifStatement);

            return new SpecialCondition(statements);
        } catch (ScriptException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    private static Stage getCooldownStage(BaseSprint8Config config) {
        String name = "cooldown"; // NON-NLS
        List<WhenCondition> whenConditions = getCooldownWhenConditions();
        List<Level> levels = getCooldownLevels();
        List<IExecutableStatement> statements = getCooldownStatements(config);
        return new Stage(name, statements, whenConditions, levels);
    }

    private static List<IExecutableStatement> getCooldownStatements(BaseSprint8Config config) {
        List<IExecutableStatement> statements = new ArrayList<>();
        statements.add(new SetStatement(PREVENT_USER_GOAL_CHANGES, 1));
        statements.add(new SetStatement(GOAL_TYPE, GOAL_TYPE_TIME_STAGE));
        statements.add(new SetStatement(GOAL, 2.5f * 60 * 1000));
        statements.add(new SetStatement(INTERVAL_GOAL, 150000f));
        statements.add(new SetStatement(IS_SPRINTING, 0));

        List<DataModel> setData = config.intervals.get(config.intervals.size() - 1).recoveryStatements;
        switch (config.type) {
            case TREADMILL_TYPE:
                statements.add(new SetStatement(SPEED, getValue(SPEED, setData)));
                statements.add(new SetStatement(INCLINE, getValue(INCLINE, setData)));
                break;
            case ASCENT_TYPE:
                statements.add(new SetStatement(RESISTANCE, getValue(RESISTANCE, setData)));
                statements.add(new SetStatement(INCLINE, getValue(INCLINE, setData)));
                break;
            case RESISTANCE_TYPE:
                statements.add(new SetStatement(RESISTANCE, getValue(RESISTANCE, setData)));
                break;
        }

        return statements;
    }

    private static List<WhenCondition> getCooldownWhenConditions() {
        try {
            List<WhenCondition> whenConditions = new ArrayList<>();
            ExpressionEvaluator endCondition = new ExpressionEvaluator(INTERVAL_TIME_MS + ">=" + GOAL);
            List<IExecutableStatement> endStatements = new ArrayList<>();
            endStatements.add(new EndStatement());
            whenConditions.add(new WhenCondition(endCondition, 0, endStatements));
            return whenConditions;
        } catch (ScriptException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    private static List<Level> getCooldownLevels() {
        List<Level> levels = new ArrayList<>();
        String name = "";
        List<WhenCondition> whenConditions = new ArrayList<>();
        List<Interval> intervals = getCooldownIntervals();
        List<IExecutableStatement> statements = new ArrayList<>();
        levels.add(new Level(name, statements, whenConditions, intervals,
                null, null, null, null));
        return levels;
    }

    private static List<Interval> getCooldownIntervals() {
        List<Interval> intervals = new ArrayList<>();
        String name = "";
        List<WhenCondition> whenConditions = new ArrayList<>();
        List<IExecutableStatement> statements = new ArrayList<>();
        intervals.add(new Interval(name, statements, whenConditions));
        return intervals;
    }
}
