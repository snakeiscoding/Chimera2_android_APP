package com.jht.mastboard.lib;

public class GestureDetector {
    final int MoveUp = 0x10;
    final int MoveRight = 0x14;
    final int MoveDown = 0x18;
    final int MoveLeft = 0x1C;
    final int ZoomIn = 0x48;
    final int ZoomOut = 0x49;
    final int NoGesture = 0x00;
    final int Push = 0x43;
    final int TapRelease = 0x42;

    final int NO_TOUCH = 0x00;
    final int TOUCH = 0x01;

    final int MIN_DISTANCE_GESTURE = 30;
    private int TouchState = NO_TOUCH;

    private int startTouchPoint_x;
    private int startTouchPoint_y;

    public int addDataPoint(int x, int y, boolean validtouch) {
        int retval = NoGesture;

        switch (TouchState) {
            default:
            case NO_TOUCH:
                if (validtouch) //starting to touch the screen
                {
                    TouchState = TOUCH;
                    startTouchPoint_x = x;
                    startTouchPoint_y = y;

                    retval = Push;
                } else {
                    // need to assume that we missed the pushdown time.  This shouldn't be possible
                    // with fast enough polling time on the int pin, but if for some reason the
                    // system can't read that fast (high CPU load, USB throughput clogs, or
                    // something like that), we should be able to handle catching a release with no
                    // push. if so, we also have to assume it's a tap, otherwise we should've caught it
                    retval = TapRelease;
                }
                break;

            case TOUCH:
                if (validtouch) //still touching the screen
                {
                    //wait for the touch to end
                    retval = Push;
                } else // == 1 (released) or == 0 (never been touched since bootup)
                {
                    //determine if a gesture has occurred.
                    int distance_x = Math.abs(x - startTouchPoint_x);
                    int distance_y = Math.abs(y - startTouchPoint_y);

                    if ((distance_x >= MIN_DISTANCE_GESTURE) ||
                            (distance_y >= MIN_DISTANCE_GESTURE)) {
                        //there was a gesture

                        //have we moved more in the x direction, or the y direction?
                        if (Math.abs(y - startTouchPoint_y) > Math.abs(x - startTouchPoint_x)) {
                            //moved more in the y direction
                            retval = (y > startTouchPoint_y) ? MoveDown : MoveUp;
                        } else {
                            //moved more in the x direction
                            retval = (x > startTouchPoint_x) ? MoveRight : MoveLeft;
                        }
                    } else {
                        retval = TapRelease;
                    }

                    TouchState = NO_TOUCH;
                }
                break;
        }

        return retval;
    }
}
