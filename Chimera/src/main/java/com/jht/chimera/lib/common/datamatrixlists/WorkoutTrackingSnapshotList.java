package com.jht.chimera.lib.common.datamatrixlists;

import android.os.Parcel;

public class WorkoutTrackingSnapshotList extends DataMatrixList<WorkoutTrackingSnapshot> {

    public WorkoutTrackingSnapshotList() {
        super();
    }

    protected WorkoutTrackingSnapshotList(Parcel in) {
        super(in);
    }

    @Override
    protected ClassLoader getClassLoader() {
        return WorkoutTrackingSnapshot.class.getClassLoader();
    }

    public static final Creator<WorkoutTrackingSnapshotList> CREATOR = new Creator<WorkoutTrackingSnapshotList>() {
        @Override
        public WorkoutTrackingSnapshotList createFromParcel(Parcel in) {
            return new WorkoutTrackingSnapshotList(in);
        }

        @Override
        public WorkoutTrackingSnapshotList[] newArray(int size) {
            return new WorkoutTrackingSnapshotList[size];
        }
    };
}
