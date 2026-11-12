package com.jht.mastboard.lib;

public class TouchData {

    /** @noinspection unused*/
    public static final int EVENT_FLAG_DOWN = 0; // it is this value at bootup before any touches have been detected.

    public static final int EVENT_FLAG_LIFT_UP = 1;

    public static final int EVENT_FLAG_TOUCH = 2;

    /** @noinspection unused*/
    public static final int EVENT_FLAG_NOTHING = 3; // it is this sometimes this value when multiple touches are detected.

    public enum GestureID {
        MoveUp(0x10),
        MoveRight(0x14),
        MoveDown(0x18),
        MoveLeft(0x1C),
        ZoomIn(0x48),
        ZoomOut(0x49),
        NoGesture(0x00),
        TapRelease(0x42),
        Push(0x43);
        public int value;

        GestureID(int value) {  this.value = value;   }

        static GestureID[] GestureList = GestureID.values();

        static public GestureID getGesture(int value) {
            for (GestureID gestureID : GestureList) {
                if (value == gestureID.value) return gestureID;
            }

            return NoGesture;
        }
    }

    public GestureID gestureID = TouchData.GestureID.NoGesture;
    public int x;
    public int y;
    ///public int numTouchPoint;
    public int eventFlag;
    ///public int touchID;
    ///public int touchID2;
    ///public int touchWeight;
    ///public int touchArea;
    public int mEventAction;
    public long lastTouchDownTime;

    @Override
    public String toString() {
        return "TouchData{" +
                ", x=" + x +
                ", y=" + y +
                ", eventFlag=" + eventFlag +
                ", gestureID=" + gestureID +
                ", mEventAction=" + mEventAction +
                //touchID=" + touchID +
                //touchID2=" + touchID2 +
                '}';
    }
}
