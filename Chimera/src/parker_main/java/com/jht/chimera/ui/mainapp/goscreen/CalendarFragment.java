package com.jht.chimera.ui.mainapp.goscreen;

import static com.jht.chimera.lib.workout.WorkoutIds.LANDMARKS;
import static com.jht.chimera.lib.workout.WorkoutIds.SPRINT8;
import static com.jht.chimera.lib.workout.WorkoutIds.VIRTUAL_ACTIVE;
import static com.jht.chimera.ui.mainapp.ParkerGoScreenActivity.WorkoutLabelProvider;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.ObjectUtil;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.landmark.LandmarkDataKt;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workout.RecordedWorkout;
import com.jht.chimera.lib.workout.RecordedWorkoutDatabase;
import com.jht.chimera.lib.workout.SavedWorkoutData;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.lib.workout.WorkoutInput;
import com.jht.chimera.lib.workout.WorkoutManager;
import com.jht.chimera.lib.workout.platform.ChimeraWorkoutManager;
import com.jht.chimera.lib.workout.workoutlist.workoutmap.ParkerWorkoutMap;
import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.lib.workoutapps.AppIconManager;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.lib.workoutapps.AppInfo;
import com.jht.chimera.service.ui.panels.DigitalSafetyPinDialog;
import com.jht.chimera.service.workout.RecordedWorkoutHelper;
import com.jht.chimera.ui.mainapp.ParkerGoScreenActivity;
import com.jht.data.LandmarkWorkoutItem;
import com.jht.data.WorkoutItem;
import com.jht.datamatrixlib.DataMatrix;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@TargetApi(26)
public class CalendarFragment
    extends Fragment
    implements CalendarWorkoutRecyclerAdapter.WorkoutListEventListener, CalendarItemRecylerAdapter.DateSelectionListener
{

    static void assertTimeZero(Calendar day, String context) {
        if (day.get(Calendar.HOUR) != 0 || day.get(Calendar.HOUR_OF_DAY) != 0 || day.get(Calendar.MINUTE) != 0 || day.get(Calendar.SECOND) != 0 || day.get(Calendar.MILLISECOND) != 0) {
            throw new RuntimeException("Calendar passed to "+context+" should have 0 for hours/minutes/seconds, but they are nonzero: "+
                String.format("(%02d|%02d):%02d:%02d.%04d", day.get(Calendar.HOUR_OF_DAY), day.get(Calendar.HOUR),
                    day.get(Calendar.MINUTE), day.get(Calendar.SECOND), day.get(Calendar.MILLISECOND)));
        }
    }

    class SavedWorkoutsDataSource implements CalendarWorkoutDataSource {
        @Override
        public List<CalendarWorkoutData> getWorkoutsInDay(Calendar day) {
            assertTimeZero(day, "getWorkoutsInDay");
            Calendar nextDay = (Calendar)day.clone();
            nextDay.add(Calendar.DAY_OF_MONTH, 1);
            List<SavedWorkoutData> savedWorkouts = RecordedWorkoutDatabase.getInstance().getWorkoutsBetween(day.getTimeInMillis(), nextDay.getTimeInMillis());
            List<CalendarWorkoutData> calendarWorkouts = new ArrayList<>();
            for (SavedWorkoutData swd : savedWorkouts) {
                Calendar date = Calendar.getInstance();
                date.setTime(swd.endTime);

                CalendarWorkoutData cwd = new CalendarWorkoutData(date);
                cwd.recordedWorkout = swd.recordedWorkout;
                cwd.startupParams = swd.startupParams;
                cwd.durationSeconds = (int)swd.totalSummary.statsAccumulated.secondsStateRun;
                cwd.distanceKm = swd.totalSummary.statsAccumulated.distanceKm;
                cwd.calories = swd.totalSummary.statsAccumulated.calories;
                if (swd.totalSummary.statsAccumulated.distanceKm == 0) {
                    cwd.paceSecondsPerKm = -1; // Not appl
                } else {
                    cwd.paceSecondsPerKm = (int) (swd.totalSummary.statsAccumulated.secondsStateRun / swd.totalSummary.statsAccumulated.distanceKm);
                }
                cwd.speedKph = swd.totalSummary.statsAverages.averageSpeedKph;
                cwd.heartRate = swd.totalSummary.statsAverages.averageHeartRate;
                calendarWorkouts.add(cwd);
            }
            return calendarWorkouts;
        }

        @Override
        public Calendar getEarliestWorkoutDate() {
            return new Calendar.Builder().setInstant(RecordedWorkoutDatabase.getInstance().getEarliestWorkoutTimestamp()).build();
        }
        @Override
        public Calendar getLatestWorkoutDate() {
            return new Calendar.Builder().setInstant(RecordedWorkoutDatabase.getInstance().getLatestWorkoutTimestamp()).build();
        }
    }

    class RandomWorkoutsDataSource implements CalendarWorkoutDataSource {
        HashMap<Calendar,List<CalendarWorkoutData>> workoutsByDay = new HashMap<>();

        Random r = new Random();

        @Override
        public List<CalendarWorkoutData> getWorkoutsInDay(Calendar day) {
            assert(day != null);
            assert(day.get(Calendar.HOUR) == 0);
            assert(day.get(Calendar.MINUTE) == 0);
            assert(day.get(Calendar.SECOND) == 0);

            List<CalendarWorkoutData> workouts = workoutsByDay.get(day);
            if (workouts != null) return workouts;

            Calendar nextDay = ((Calendar)day.clone());
            nextDay.add(Calendar.DAY_OF_MONTH, 1);
            long dayTimeBegin = day.getTimeInMillis(), dayTimeEnd = nextDay.getTimeInMillis();

            workouts = new ArrayList<>();
            while (r.nextBoolean() && workouts.size() < 8) {
                workouts.add(CalendarWorkoutData.getRandom(day.getTimeInMillis(), nextDay.getTimeInMillis()));
            }

            Collections.sort(workouts);
            workoutsByDay.put(day, workouts);
            return workouts;
        }

        @Override
        public Calendar getEarliestWorkoutDate() {
            return new Calendar.Builder().setInstant(0).build();
        }
        @Override
        public Calendar getLatestWorkoutDate() {
            return new Calendar.Builder().setInstant(0).build();
        }
    }

    private static final String TAG = "CalendarFragment";
    protected ViewGroup container;
    // You can set the workoutDataSource to a RandomWorkoutDataSource for testing:
    protected CalendarWorkoutDataSource workoutDataSource = new SavedWorkoutsDataSource();

    private RecyclerView calendarRecyclerView;
    private CalendarItemRecylerAdapter calendarRecyclerAdapter = new CalendarItemRecylerAdapter(workoutDataSource);
    //private ArrayList<CalendarWorkoutData> workouts;
    private RecyclerView workoutRecyclerView;
    private LinearLayoutManager workoutLayoutManager;
    private CalendarWorkoutRecyclerAdapter workoutRecyclerAdapter;

    public CalendarFragment() {
        super();
    }

    private WorkoutLabelProvider workoutLabelProvider;
    public void setWorkoutLabelProvider(WorkoutLabelProvider workoutLabelProvider) {
        this.workoutLabelProvider = workoutLabelProvider;
        if (getView()!=null) changeMonth(0); // do we need to refresh anything if this changes? probably not, if you create a new workout you have to run it before it gets in the list
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        this.container = container;
        return inflater.inflate(R.layout.calendar_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        calendarRecyclerAdapter.addDateSelectionListener(this);
        calendarRecyclerView = view.findViewById(R.id.calendar_recycler);
        calendarRecyclerView.setAdapter(calendarRecyclerAdapter);
        calendarRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 7));

        workoutRecyclerView = view.findViewById(R.id.workout_list);
        workoutLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        workoutRecyclerView.setLayoutManager(workoutLayoutManager);
        workoutRecyclerAdapter = new CalendarWorkoutRecyclerAdapter(this, workoutLabelProvider);
        workoutRecyclerView.setAdapter(workoutRecyclerAdapter);

        goToMonth(Calendar.getInstance());
    }

    @Override
    public void dateSelected(@Nullable Calendar date) {
        if (date != null) assertTimeZero(date, "dateSelected");
        workoutRecyclerAdapter.setWorkouts(date == null ? Collections.emptyList() : workoutDataSource.getWorkoutsInDay(date));
    }

    protected void startSprint8Workout(CalendarWorkoutData data) {
        // Ensure startup params are not null
        if (data.startupParams == null) {
            Log.e(TAG, "Can't start Sprint 8 workout from calendar item; startingParams missing!");
            return;
        }

        // Check if workout is custom Sprint 8, if so start it
        // Only custom Sprint8 workouts have a sprint8Config. Number of sprints must also be not null
        if (data.startupParams.sprint8Config != null && data.startupParams.numberOfSprints != null) {
            new ChimeraWorkoutManager(getContext()).startSprint8Custom(
                    data.startupParams.sprint8Config,
                    data.startupParams.numberOfSprints
            );
            return;
        }

        // Check if workout is non-custom Sprint 8, if so start it
        // Only non-custom Sprint 8 workouts have a workout level. It must be not null for workout to start
        if (data.startupParams.workoutLevel != null) {
            new ChimeraWorkoutManager(getContext()).startSprint8(data.startupParams.workoutLevel);
            return;
        }

        Log.e(TAG, "Can't start Sprint 8 workout from calendar item. Workout missing required parameters!");
    }

    private void startVAWorkout(CalendarWorkoutData data) {
        WorkoutManager workoutManager = new WorkoutManager(new ParkerWorkoutMap());
        List<WorkoutItem> workoutItems = workoutManager.getVaWorkouts(getContext());
        if (data.startupParams == null || data.recordedWorkout == null || workoutItems.isEmpty()) {
            Log.e(TAG, "Can't start VA workout from calendar item!");
            return;
        }
        try {
            int randomVaIndex = new Random().nextInt(workoutItems.size());
            String vaID = workoutItems.get(randomVaIndex).getOptionalId();
            DataMatrix.putString(VaKeys.DATA_SELECTED_WORKOUT_UNIQUEID_STRING,vaID);
            int id = data.startupParams == null ? 0 : data.startupParams.programTypeId;
            workoutManager.loadWorkoutWithId(id, workoutInputs -> {
                if (workoutInputs != null) {
                    List<WorkoutInput> initialConditions = getVAInitialConditions(data, workoutInputs);
                    workoutManager.startLoadedWorkout(initialConditions, false);
                } else {
                    Log.e(TAG, "Not starting workout, no matching ID!");
                }
            });
        } catch (Exception e){
            Log.e(TAG, "Error launching VA workout: "+e);
        }
    }

    private void startManualWorkout(CalendarWorkoutData data, AppIcon appIcon) {
        boolean isSwitchWorkout = false;
        RecordedWorkout rw = data.recordedWorkout;
        if (rw == null) {
            Log.e(TAG, "Oh no, recorded workout from "+data.date+" was empty!  We can't run it!");
            return;
        }
        int programTypeId = data.startupParams == null ? WorkoutIds.MANUAL : data.startupParams.programTypeId;
        String scriptString = new RecordedWorkoutHelper().recordedWorkoutToScript(rw);
        WorkoutManager workoutManager = new WorkoutManager(new ParkerWorkoutMap());
        Log.d(TAG, "LOAD A WORKOUT");
        Log.d(TAG, "workoutScript: " + scriptString);
        String appUuid = appIcon != null && !appIcon.appInfo.category.equals(AppInfo.Category.System) ? appIcon.getUuid() : null;
        workoutManager.loadCustomWorkout(programTypeId, appUuid, scriptString, workoutInputs -> {
            Log.d(TAG, "START A WORKOUT??");
            DataMatrix.putParcelable(WorkoutKeys.DATA_CURRENT_RECORDED_WORKOUT_PARCELABLE, rw);
            DataMatrix.putString(WorkoutKeys.DATA_CUSTOM_WORKOUT_UUID_STRING, ObjectUtil.nullableToString(data.startupParams.customWorkoutUuid)); // do we need this?
            workoutManager.startLoadedWorkout(null, isSwitchWorkout);
        });
    }

    @Override
    public void onRunWorkout(CalendarWorkoutData data) {
        checkIfPinNeeded(()->{
            int programTypeId = data.startupParams == null ? 0 : data.startupParams.programTypeId == null ? 0 : data.startupParams.programTypeId ;
            String appUuid = data.startupParams == null ? "" : data.startupParams.appUuid;
            AppIcon appIcon = AppIconManager.getInstance().getAppIcon(appUuid);
            Log.d(TAG, "Program type ID of workout to be launched is "+programTypeId);
            switch (programTypeId){
                case SPRINT8:
                    startSprint8Workout(data);
                    break;
                case VIRTUAL_ACTIVE:
                    startVAWorkout(data);
                    break;
                case LANDMARKS:
                    appIcon = loadLandmarksWorkout(data);
                    startManualWorkout(data, appIcon);
                    break;
                default:
                    if (appIcon != null && appIcon.appInfo.useThirdPartyInterface) {
                        LaunchAppUtil.initialLaunchAppByIcon(getContext(), appIcon.appInfo, this.getView());
                    } else {
                        startManualWorkout(data, appIcon);
                    }
            }
        });
    }

    Calendar currentMonth;

    void changeMonth(int delta) {
        Calendar newMonth = (Calendar) currentMonth.clone();
        newMonth.set(Calendar.MONTH, newMonth.get(Calendar.MONTH)+delta);
        goToMonth(newMonth);
    }

    void previousMonthButtonClicked(View v) {
        changeMonth(-1);
    }
    void nextMonthButtonClicked(View v) {
        changeMonth(+1);
    }

    void updateMonthNavigation(ImageView button, boolean enabled, View.OnClickListener listenerWhenEnabled) {
        button.setColorFilter(enabled ? 0xFFFFFFFF : 0xFF808080);
        button.setOnClickListener(enabled ? listenerWhenEnabled : null);
    }

    void goToMonth(Calendar cal) {
        Calendar month = (Calendar) cal.clone();
        month.set(Calendar.DAY_OF_MONTH, 1);
        month.set(Calendar.HOUR_OF_DAY, 0);
        month.set(Calendar.MINUTE, 0);
        month.set(Calendar.SECOND, 0);
        month.set(Calendar.MILLISECOND, 0);

        currentMonth = month;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        YearMonth yearMonth = YearMonth.of(month.get(Calendar.YEAR), month.get(Calendar.MONTH)+1);
        String monthTitle = yearMonth.format(formatter);
        ((TextView)getView().findViewById(R.id.month_name)).setText(monthTitle);
        calendarRecyclerAdapter.goToMonth(month);

        Calendar nextMonth = (Calendar) currentMonth.clone();
        nextMonth.add(Calendar.MONTH, 1);

        Calendar earliestWorkoutDate = workoutDataSource.getEarliestWorkoutDate();
        Calendar latestWorkoutDate = workoutDataSource.getLatestWorkoutDate();

        boolean moreWorkoutsInPast = (earliestWorkoutDate.compareTo(currentMonth) < 0);
        boolean moreWorkoutsInFuture = (latestWorkoutDate.compareTo(nextMonth) >= 0);

        updateMonthNavigation(this.getView().findViewById(R.id.previous_month_button), moreWorkoutsInPast, this::previousMonthButtonClicked);
        updateMonthNavigation(this.getView().findViewById(R.id.next_month_button), moreWorkoutsInFuture, this::nextMonthButtonClicked);
    }


    private AppIcon loadLandmarksWorkout(CalendarWorkoutData data){
        List<LandmarkWorkoutItem> mLandmarksWorkoutList = LandmarkDataKt.getLandmarkWorkoutItemList();
        LandmarkWorkoutItem previousLandmark = null;
        String landmarkTitleString = "";
        for (int i = 0; i < mLandmarksWorkoutList.size(); i++) {
            landmarkTitleString = getString(mLandmarksWorkoutList.get(i).getTitleId());
            if (landmarkTitleString.equals(data.startupParams.landmarkTitle)){
                previousLandmark = mLandmarksWorkoutList.get(i);
                break;
            }
        }
        if (previousLandmark != null){
            DataMatrix.putString(WorkoutKeys.DATA_SELECTED_VA_OR_LANDMARK_TITLE_STRING, landmarkTitleString);
            String appUuid = AppIds.LANDMARKS_UUID;
            return AppIconManager.getInstance().getAppIcon(appUuid);
        }
        return null;
    }

    private List<WorkoutInput> getVAInitialConditions(CalendarWorkoutData data, List<WorkoutInput> inputs) {
        List<WorkoutInput> initialConditions = new ArrayList<>();
        for (WorkoutInput input : inputs) {
            switch (input.identifier()) {
                case ConsoleWorkoutVariables.START_SPEED:
                    //TODO: Default 0.5 unless we want to store more startup parameters in data.startupParams
                    initialConditions.add(new WorkoutInput(input.identifier(), (float)0.5));
                    break;
                case ConsoleWorkoutVariables.START_RESISTANCE:
                    //TODO: Default 0 unless we want to store more startup parameters in data.startupParams
                    initialConditions.add(new WorkoutInput(input.identifier(), 0));
                    break;
                case ConsoleWorkoutVariables.TIME_GOAL_START:
                    if (data.durationSeconds >= 0) {
                        float duration = (float) Math.min(data.durationSeconds, DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_TIME_MIN_INT));
                        initialConditions.add(new WorkoutInput(input.identifier(), duration));
                    }
                    break;
                case ConsoleWorkoutVariables.LEVEL:
                    try{
                        if (data.startupParams.workoutLevel != null && data.startupParams.workoutLevel >= 0) {
                            initialConditions.add(new WorkoutInput(input.identifier(), data.startupParams.workoutLevel));
                        }
                    } catch (Exception e){
                        initialConditions.add(new WorkoutInput(input.identifier(), 1));
                    }
                    break;
            }
        }
        return initialConditions;
    }

    // little cheesy but gets the job done for now
    private void checkIfPinNeeded(DigitalSafetyPinDialog.Action action) {
        FragmentActivity act = getActivity();
        if (act instanceof ParkerGoScreenActivity) {
            ((ParkerGoScreenActivity) act).checkIfSafetyPinNeeded(action);
        } else {
            action.doAction();
        }
    }
}
