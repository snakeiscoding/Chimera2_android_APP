package com.jht.chimera.lib.common.datamatrixlists;

import com.jht.datamatrixlib.DataMatrix;

public final class DataMatrixListFactory {
    public static class WorkoutSnapshotConsumer extends DataMatrixListConsumer<WorkoutTrackingSnapshot, WorkoutTrackingSnapshotList> {
        WorkoutSnapshotConsumer() {
            super(WorkoutTrackingSnapshot.KEYS);
        }
        public void getList(DataMatrix dm, IOnGetList<WorkoutTrackingSnapshot> onGetList, IOnGetElement<WorkoutTrackingSnapshot> onGetElement) {
            super.getList(dm, onGetList, onGetElement, WorkoutTrackingSnapshotList.class, WorkoutTrackingSnapshot.class);
        }
    }

    public static class WorkoutSnapshotProducer extends DataMatrixListProducer<WorkoutTrackingSnapshot> {
        WorkoutSnapshotProducer(DataMatrix dm) {
            super(WorkoutTrackingSnapshot.PARCEL_SIZE, new WorkoutTrackingSnapshotList(), dm, WorkoutTrackingSnapshot.KEYS);
        }
    }

    public static WorkoutSnapshotConsumer snapshotListConsumer() {
        return new WorkoutSnapshotConsumer();
    }
    public static WorkoutSnapshotProducer snapshotListProducer(DataMatrix dm) {
        return new WorkoutSnapshotProducer(dm);
    }
}

