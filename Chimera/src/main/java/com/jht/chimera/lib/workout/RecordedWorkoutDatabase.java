package com.jht.chimera.lib.workout;

import android.annotation.TargetApi;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.service.dapi.UserProfileManager;
import com.jht.enums.WorkoutId;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

@TargetApi(26)
public class RecordedWorkoutDatabase
{
    public static class WorkoutEncodingException extends Exception {
        public WorkoutEncodingException(String message) { super(message); }
        public WorkoutEncodingException(String message, Throwable cause) { super(message, cause); }
    }

    private static final String TAG = "RecordedWorkoutDatabase";

    private static final RecordedWorkoutDatabase instance = new RecordedWorkoutDatabase();

    public static RecordedWorkoutDatabase getInstance() {
        return instance;
    }

    static SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    static SimpleDateFormat WORKOUT_DIR_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/yyyy_MM_dd");
    static SimpleDateFormat WORKOUT_BASENAME_DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    static TimeZone TZ = TimeZone.getTimeZone("UTC"); //NON-NLS
    static {
        ISO8601.setTimeZone(TZ);
        WORKOUT_DIR_DATE_FORMAT.setTimeZone(TZ);
        WORKOUT_BASENAME_DATE_FORMAT.setTimeZone(TZ);
    }

    static final Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat(ISO8601.toPattern()).create();

    protected File getSaveFile(Date workoutEndTime) {
        return new File(getSaveDir(workoutEndTime), WORKOUT_BASENAME_DATE_FORMAT.format(workoutEndTime)+".json");
    }
    protected File getSaveDir() {
        return new File(UserProfileManager.getInstance().getUserFolder(), "workout-history");
    }
    protected File getSaveDir(Date workoutDay) {
        return new File(getSaveDir(), WORKOUT_DIR_DATE_FORMAT.format(workoutDay));
    }

    public void purgeAllUserHistory() {
        for (int i=0; i <= UserProfileManager.MAX_USER_LIMIT; ++i) {
            try {
                File workouts = new File(UserProfileManager.getInstance().getUserFolder(i), "workout-history");
                if (workouts.exists()) FileUtils.delete(workouts);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to purgeAllUserHistory user=" + i, ex);
            }
        }
    }

    protected static Calendar startOfGmtDay(long millis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")); //NON-NLS
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    protected static <X> X findFirstFile(File f, Comparator<File> comparator, Function<File,X> filter) {
        if (f.isDirectory()) {
            ArrayList<File> filesList = new ArrayList<File>();
            for (File subFile : f.listFiles()) filesList.add(subFile);
            Collections.sort(filesList, comparator);
            for (File subFile : filesList) {
                X result = findFirstFile(subFile, comparator, filter);
                if (result != null) return result;
            }
            return null;
        } else {
            return filter.apply(f);
        }
    }

    static Comparator<File> BY_FILENAME_ASC = (File a, File b) -> a.getName().compareTo(b.getName());
    static Comparator<File> BY_FILENAME_DESC = (File a, File b) -> b.getName().compareTo(a.getName());

    public static class LoadedWorkout {
        @Nullable SavedWorkoutData savedWorkoutData;
        ArrayList<String> errors;
    }

    protected static SavedWorkoutData validateSavedWorkoutData(SavedWorkoutData swd, String context) throws WorkoutEncodingException {
        if (swd == null) {
            throw new WorkoutEncodingException("Read null (instead of a SavedWorkoutData) from " + context);
        }
        // If any non-nullable fields are null, it's not valid and we shouldn't return it
        if (swd.endTime == null) {
            throw new WorkoutEncodingException("SavedWorkoutData from "+context+" lacks endTime so is invalid");
        }
        return swd;
    }

    public static SavedWorkoutData parseWorkout(String json) throws WorkoutEncodingException {
        try {
            SavedWorkoutData swd = gson.fromJson(json, SavedWorkoutData.class);
            return validateSavedWorkoutData(swd, "string: "+json); //NON-NLS
        } catch (JsonSyntaxException e) {
            throw new WorkoutEncodingException("JsonSyntaxException when loading workout from string: "+json, e);
        }
    }

    protected static SavedWorkoutData readWorkoutFile(File f) throws IOException, WorkoutEncodingException {
        try (FileReader fr = new FileReader(f)) {
            SavedWorkoutData swd = gson.fromJson(fr, SavedWorkoutData.class);
            return validateSavedWorkoutData(swd, f.getPath());
        } catch (JsonSyntaxException e) {
            throw new WorkoutEncodingException("JsonSyntaxException when loading workout from "+f, e);
        }
    }

    static @Nullable Long workoutTimestamp(File f) {
        try {
            SavedWorkoutData swd = readWorkoutFile(f);
            if (swd == null) return null;

            // TODO: Move this out of RecordedWorkoutDatabase (data layer) and filter in presentation layer instead
            if (RecordedWorkoutFilters.INSTANCE.isWorkoutHidden(swd)) {
                // Recorded workout is hidden, we should return null to treat it as if the workout is not in our database.
                return null;
            }

            return swd.endTime.getTime();
        } catch (WorkoutEncodingException e) {
            Log.w(TAG, "Failed to load workout from "+f, e);
            return null;
        } catch (IOException e) {
            return null;
        }
    }
    protected long unwrap(Long value, long defaultValue) {
        return value == null ? defaultValue : value.longValue();
    }
    public long getEarliestWorkoutTimestamp() {
        return unwrap(findFirstFile(getSaveDir(), BY_FILENAME_ASC, RecordedWorkoutDatabase::workoutTimestamp), Long.MAX_VALUE);
    }
    public long getLatestWorkoutTimestamp() {
        return unwrap(findFirstFile(getSaveDir(), BY_FILENAME_DESC, RecordedWorkoutDatabase::workoutTimestamp), Long.MIN_VALUE);
    }

    /** Return all workouts with timestamps between [millis0, millis1) */
    public List<SavedWorkoutData> getWorkoutsBetween(long millis0, long millis1) {
        ArrayList<SavedWorkoutData> workouts = new ArrayList<>();
        for (Calendar date = startOfGmtDay(millis0); date.getTimeInMillis() < millis1; date.add(Calendar.DAY_OF_MONTH, 1)) {
            // Need to convert back and forth between Calendars and Dates.  Yuck!
            // The reason is that we can pare dates,
            // and se can set explicit timezones on calendars.
            // It's all very silly.
            // Maybe we should try the newer date/time classes.
            File saveDir = getSaveDir(date.getTime());
            //Log.d(TAG, "Looking for workouts in "+saveDir+"...");
            File[] files = saveDir.listFiles();
            if (files == null || files.length == 0) continue;
            for (File f : files) {
                //Log.d(TAG, "Loading "+f);
                try {
                    SavedWorkoutData swd = readWorkoutFile(f);
                    if (swd == null) continue;
                    try {
                        long t = swd.endTime.getTime();
                        if (t < millis0 || t >= millis1) continue;

                        // Filter out hidden program types
                        // TODO: Move this out of RecordedWorkoutDatabase (data layer) and filter in presentation layer instead
                        if (RecordedWorkoutFilters.INSTANCE.isWorkoutHidden(swd)) {
                            // Recorded workout is hidden, skip adding it to the list of returned workouts
                            continue;
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse date from " + f + ": \"" + swd.endTime + "\"", e);
                        continue;
                    }
                    workouts.add(swd);
                } catch (WorkoutEncodingException|IOException e) {
                    Log.w(TAG, "Error reading " + f, e);
                }
            }
        }
        return workouts;
    }

    static final int MAX_RECENT_WORKOUT = 10;

    protected static PreviousWorkout savedWorkoutToPreviousWorkout(SavedWorkoutData swd) throws ParseException {
        return new PreviousWorkout(
                swd.startupParams,
                swd.endTime.getTime()
        );
    }

    public List<PreviousWorkout> getRecentWorkouts(Predicate<? super PreviousWorkout> filter) {
        RecordedWorkoutDatabase rwdb = RecordedWorkoutDatabase.getInstance();
        long chunkSize = TimeUnit.DAYS.toMillis(7);
        long currentTime = System.currentTimeMillis();
        long lastWorkout = rwdb.getLatestWorkoutTimestamp();
        if (lastWorkout > currentTime) currentTime = lastWorkout + 1000; // if clock is in the past, use last workout time plus one second
        long earliestBeginTime = DapiUser.isGuestUser() ?
                currentTime - TimeUnit.DAYS.toMillis(3) :
                currentTime - TimeUnit.DAYS.toMillis(90); // About 3 months ago
        List<PreviousWorkout> workouts = new ArrayList<>();
        for (
                long chunkEndTime = currentTime, chunkBeginTime = chunkEndTime - chunkSize;
                workouts.size() < MAX_RECENT_WORKOUT && chunkEndTime >= earliestBeginTime;
                chunkEndTime = chunkBeginTime, chunkBeginTime -= chunkSize
        ) {
            List<SavedWorkoutData> swdChunk = rwdb.getWorkoutsBetween(chunkBeginTime, chunkEndTime);
            Collections.sort(swdChunk, new Comparator<SavedWorkoutData>() {
                @Override
                public int compare(SavedWorkoutData a, SavedWorkoutData b) {
                    return b.endTime.compareTo(a.endTime); // sorting newest first
                }
            });
            for (SavedWorkoutData swd : swdChunk) {
                try {
                    PreviousWorkout pw = savedWorkoutToPreviousWorkout(swd);
                    if (filter.test(pw)) workouts.add(pw);
                } catch (ParseException e) {
                    Log.w(TAG, "Error parsing workout", e);
                }
            }
        }
        workouts = workouts.subList(0, Math.min(workouts.size(), MAX_RECENT_WORKOUT));
        Log.d(TAG, "Done loading recent workouts");
        return workouts;
    }

    public List<PreviousWorkout> getRecentWorkouts() {
        return getRecentWorkouts(t -> true);
    }

    public static String encodeSavedWorkoutData(SavedWorkoutData swd) {
        return gson.toJson(swd);
    }

    public void save(SavedWorkoutData savedWorkout) throws WorkoutEncodingException, IOException {
        if (savedWorkout.endTime == null) throw new WorkoutEncodingException("Workout endTime is null");

        File saveFile = getSaveFile(savedWorkout.endTime);
        saveFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
            fos.write(encodeSavedWorkoutData(savedWorkout).getBytes("UTF-8"));
            fos.write('\n'); // toJson doesn't include a trailing newline
            Log.d(TAG, "Wrote "+saveFile);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("getBytes don't know about UTF-8!", e);
        }
    }
}
