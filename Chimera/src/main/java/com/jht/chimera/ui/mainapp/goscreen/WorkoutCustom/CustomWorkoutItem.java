package com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom;

import com.jht.chimera.ui.mainapp.goscreen.GoScreenWorkoutDisplayItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by AlexN on 1/19/2019.
 */

public class CustomWorkoutItem extends GoScreenWorkoutDisplayItem
{
    /** ID for purposes of favoriting */
    public final UUID uuid;
    /** Map of WorkoutParameter identifier => list of values for that parameter, one per interval */
    public Map<String,List<Float>> intervalData;
    public boolean hasCooldown = true;

    public boolean allowGoalChanges = false;

    protected void ensureMembersNotNull() {
        if (title == null) throw new NullPointerException("CustomWorkoutItem title must not be null");
        if (uuid == null) throw new NullPointerException("CustomWorkoutItem uuid must not be null");
        if (intervalData == null) throw new NullPointerException("CustomWorkoutItem intervalData must not be null!");
    }

    public CustomWorkoutItem(String title, UUID uuid, Map<String,List<Float>> customWorkoutData, boolean hasCooldown) {
        super(title, GoScreenWorkoutDisplayItem.WORKOUT_TYPE);
        this.uuid = uuid;
        this.intervalData = customWorkoutData;
        this.hasCooldown = hasCooldown;
        ensureMembersNotNull();
    }

    public CustomWorkoutItem(String title, UUID uuid, int category, boolean hasCooldown) {
        super(title, category);
        this.uuid = uuid;
        intervalData = new HashMap<>();
        this.hasCooldown = hasCooldown;
        ensureMembersNotNull();
    }

    public CustomWorkoutItem(CustomWorkoutItem other) {
        super(other.title, other.categoryType);
        this.uuid = other.uuid;
        this.intervalData = new HashMap<>(other.intervalData);
        this.hasCooldown = other.hasCooldown;
        ensureMembersNotNull();
    }

    public boolean equals(Object other) {
        if (other == null) return false;
        if (!(other instanceof CustomWorkoutItem)) return false;

        CustomWorkoutItem otherCwi = (CustomWorkoutItem)other;

        return uuid.equals(otherCwi.uuid) && title.equals(otherCwi.title) && intervalData.equals(otherCwi.intervalData) && hasCooldown == otherCwi.hasCooldown;
    }
}
