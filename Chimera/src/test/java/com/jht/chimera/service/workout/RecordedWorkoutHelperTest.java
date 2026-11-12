package com.jht.chimera.service.workout;

import com.jht.chimera.lib.workout.RecordedWorkout.RecordedWorkoutEvent;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItem;

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RecordedWorkoutHelperTest {
    <T> int allListLengths(Iterable<List<T>> lists, String descPlural) {
        int length = -1;
        for (List<?> l : lists) {
            if (length == -1) length = l.size();
            else if (l.size() != length) throw new RuntimeException(descPlural+" are different lengths");
        }
        return length;
    }

    @Test
    public void testEmptyWorkout() {
        RecordedWorkoutHelper.Intervalifier intervalifier = new RecordedWorkoutHelper.Intervalifier();

        CustomWorkoutItem cw = intervalifier.end(10);
        Assert.assertEquals("Should be one interval with default parameter values (probably zero)", 1, allListLengths(cw.intervalData.values(), "interval data"));
    }

    void assertFloatsEqual(String name, float[] expected, Iterator<Float> actual) {
        for (int i=0; actual.hasNext(); ++i) {
            Float f = actual.next();
            Assert.assertTrue("There should be no more than " + expected.length + " " + name + " values", i < expected.length);;
            Assert.assertNotNull("actual " + name + " " + i + " is not null", f);
            Assert.assertEquals(name + " " + i + " matches", expected[i], f, 0.001f);
        }
    }

    void assertTsirEqual(
        Map<String,List<Float>> intervalData,
        float[] expectedTimeSeconds,
        float[] expectedSpeed,
        float[] expectedIncline,
        float[] expectedResistance
    ) {
        List<Float> timeData = intervalData.get(RecordedWorkoutHelper.TIME_PARAM.identifier());
        //System.err.println("There are "+timeData.size()+" time values");
        List<Float> speedData = intervalData.get(RecordedWorkoutHelper.SPEED_PARAM.identifier());
        List<Float> inclineData = intervalData.get(RecordedWorkoutHelper.INCLINE_PARAM.identifier());
        List<Float> resistanceData = intervalData.get(RecordedWorkoutHelper.RESISTANCE_PARAM.identifier());
        assertFloatsEqual("duration", expectedTimeSeconds, timeData.stream().map(f -> (float)Math.ceil(f*60)).iterator());
        assertFloatsEqual("speed", expectedSpeed, speedData.iterator());
        assertFloatsEqual("incline", expectedIncline, inclineData.iterator());
        assertFloatsEqual("resistance", expectedResistance, resistanceData.iterator());
    }

    @Test
    public void testSimple3IntervalWorkout() {
        RecordedWorkoutHelper.Intervalifier intervalifier = new RecordedWorkoutHelper.Intervalifier();
        intervalifier.event(new RecordedWorkoutEvent(0).setSpeed(1.0f).setIncline(0.5f).setResistance(12f));
        intervalifier.event(new RecordedWorkoutEvent(10).setSpeed(2.0f));
        intervalifier.event(new RecordedWorkoutEvent(20).setSpeed(2.5f));
        CustomWorkoutItem cw = intervalifier.end(30);
        printIntervals(cw);
        Assert.assertEquals("Should be 3 intervals", 3, allListLengths(cw.intervalData.values(), "interval data"));
        assertTsirEqual(cw.intervalData,
                new float[] {10, 10, 10},
                new float[] {1, 2, 2.5f},
                new float[] {0.5f, 0.5f, 0.5f},
                new float[] {12f, 12f, 12f}
        );
    }

    @Test
    public void testConsolidatedMiddleIntervalWorkout() {
        // User keeps hitting buttons between seconds 10 and 20.
        // Everything up until second 20 should become a single interval.
        RecordedWorkoutHelper.Intervalifier intervalifier = new RecordedWorkoutHelper.Intervalifier();
        intervalifier.event(new RecordedWorkoutEvent(0).setSpeed(1.0f).setIncline(0.5f).setResistance(12f));
        intervalifier.event(new RecordedWorkoutEvent(10).setSpeed(2.0f));
        intervalifier.event(new RecordedWorkoutEvent(15).setIncline(1.0f));
        intervalifier.event(new RecordedWorkoutEvent(20).setSpeed(2.5f));
        CustomWorkoutItem cw = intervalifier.end(30);
        printIntervals(cw);
        Assert.assertEquals("Should be 4 intervals", 4, allListLengths(cw.intervalData.values(), "interval data"));
        assertTsirEqual(cw.intervalData,
                new float[] {10, 5, 5, 10},
                new float[] {1, 2, 2, 2.5f},
                new float[] {0.5f, 0.5f, 1.0f, 1.0f},
                new float[] {12f, 12f, 12, 12}
        );
    }

    private void printIntervals(CustomWorkoutItem cw) {
        StringBuilder intervalData = new StringBuilder();
        for(Map.Entry<String, List<Float>> entry : cw.intervalData.entrySet()) {
            intervalData.append("Entry ").append(entry.getKey());
            for(Float value : entry.getValue()) {
                if(entry.getKey().equals("TIME_GOAL_START")) {
                    int realValue = (int)Math.ceil(value * 60);
                    intervalData.append(" ").append(realValue);
                }
                else {
                    intervalData.append(" ").append(value);
                }
            }
            intervalData.append("\n");

        }
        // FIXME - Likely not supposed to be using System.out.
        System.out.println(intervalData.toString());
    }

    @Test
    public void testConsolidatedIntervalShortWorkout() {
        // A workout where we mash buttons the whole time
        // and then end the workout.
        // All changes should be ignored, since there is no decently long span of time after them.
        RecordedWorkoutHelper.Intervalifier intervalifier = new RecordedWorkoutHelper.Intervalifier();
        intervalifier.event(new RecordedWorkoutEvent(0).setSpeed(1.0f).setIncline(0.5f).setResistance(12f));
        intervalifier.event(new RecordedWorkoutEvent(5).setSpeed(2.0f));
        intervalifier.event(new RecordedWorkoutEvent(10).setIncline(1.0f));
        intervalifier.event(new RecordedWorkoutEvent(15).setSpeed(2.5f));
        CustomWorkoutItem cw = intervalifier.end(20);
        printIntervals(cw);
        assertTsirEqual(cw.intervalData,
                new float[] {5, 5, 5, 5},
                new float[] {1.0f, 2.0f, 2.0f, 2.5f},
                new float[] {0.5f, 0.5f, 1.0f, 1.0f},
                new float[] {12f, 12f, 12, 12}
        );
    }
}
