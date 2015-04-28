package gr.com.ist.commun.core.domain;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;

public class TimePeriodTest {
    @Test
    public void testIncludeWithNullStartTimeAndEndTime() {
        TimePeriod tested = new TimePeriod();
        Assert.assertTrue(tested.includes(new Date()));
    }
    @Test
    public void testIncludeWithNullStartTimeAndNotNullEndTime() {
        TimePeriod tested = new TimePeriod();
        long oneSecondAgo = System.currentTimeMillis() - 1000;
        tested.setEndDateTime(new Date(oneSecondAgo));
        Assert.assertFalse(tested.includes(new Date()));
        Assert.assertTrue(tested.includes(new Date(oneSecondAgo)));
        Assert.assertTrue(tested.includes(new Date(oneSecondAgo - 1000)));
    }
    @Test
    public void testIncludeWithNotNullStartTimeAndNullEndTime() {
        TimePeriod tested = new TimePeriod();
        long oneSecondAgo = System.currentTimeMillis() - 1000;
        tested.setStartDateTime(new Date(oneSecondAgo));
        Assert.assertFalse(tested.includes(new Date(oneSecondAgo - 1000)));
        Assert.assertTrue(tested.includes(new Date()));
        Assert.assertTrue(tested.includes(new Date(oneSecondAgo)));
    }
    @Test
    public void testIncludeWithNotNullStartTimeAndNotNullEndTime() {
        TimePeriod tested = new TimePeriod();
        long oneSecondAgo = System.currentTimeMillis() - 1000;
        tested.setStartDateTime(new Date(oneSecondAgo - 1000));
        tested.setEndDateTime(new Date(oneSecondAgo));
        Assert.assertFalse(tested.includes(new Date(oneSecondAgo - 1001)));
        Assert.assertTrue(tested.includes(new Date(oneSecondAgo - 1000)));
        Assert.assertTrue(tested.includes(new Date(oneSecondAgo)));
        Assert.assertFalse(tested.includes(new Date(oneSecondAgo + 1)));
    }
    @Test
    public void testOverlapsBecauseItContainsOther() {
        long aMomentInTime = System.currentTimeMillis() - 1000;
        TimePeriod a = new TimePeriod();
        TimePeriod b = new TimePeriod();
        a.setStartDateTime(new Date(aMomentInTime - 1000));
        a.setEndDateTime(new Date(aMomentInTime + 1000));
        b.setStartDateTime(new Date(aMomentInTime - 500));
        b.setEndDateTime(new Date(aMomentInTime + 500));
        Assert.assertTrue(a.overlaps(b));
    }
    @Test
    public void testOverlapsBecauseItContainsStartOfOther() {
        long aMomentInTime = System.currentTimeMillis() - 1000;
        TimePeriod a = new TimePeriod();
        TimePeriod b = new TimePeriod();
        a.setStartDateTime(new Date(aMomentInTime - 1000));
        a.setEndDateTime(new Date(aMomentInTime + 1000));
        b.setStartDateTime(new Date(aMomentInTime - 500));
        b.setEndDateTime(new Date(aMomentInTime + 1500));
        Assert.assertTrue(a.overlaps(b));
    }
    @Test
    public void testOverlapsBecauseItContainsEndOfOther() {
        long aMomentInTime = System.currentTimeMillis() - 1000;
        TimePeriod a = new TimePeriod();
        TimePeriod b = new TimePeriod();
        a.setStartDateTime(new Date(aMomentInTime - 1000));
        a.setEndDateTime(new Date(aMomentInTime + 1000));
        b.setStartDateTime(new Date(aMomentInTime - 1500));
        b.setEndDateTime(new Date(aMomentInTime + 500));
        Assert.assertTrue(a.overlaps(b));
    }
    @Test
    public void testOverlapsBecauseItIsContainedByOther() {
        TimePeriod a = new TimePeriod();
        TimePeriod b = new TimePeriod();
        long aMomentInTime = System.currentTimeMillis() - 1000;
        a.setStartDateTime(new Date(aMomentInTime - 1000));
        a.setEndDateTime(new Date(aMomentInTime + 1000));
        b.setStartDateTime(new Date(aMomentInTime - 1500));
        b.setEndDateTime(new Date(aMomentInTime + 1500));
        Assert.assertTrue(a.overlaps(b));
    }
    @Test
    public void testOverlapsSupportsNullStartAndEndTime() {
        TimePeriod a = new TimePeriod();
        TimePeriod b = new TimePeriod();
        Assert.assertTrue(a.overlaps(b));
    }
    @Test
    public void testOverlapsInterpretsNullStartAsInfinitePast() {
        TimePeriod a = new TimePeriod();
        TimePeriod b = new TimePeriod();
        long aMomentInTime = System.currentTimeMillis() - 1000;
        a.setEndDateTime(new Date(aMomentInTime + 1000));
        a.setStartDateTime(new Date(aMomentInTime - 1500));
        b.setEndDateTime(new Date(aMomentInTime + 1500));
        Assert.assertTrue(a.overlaps(b));
    }
    @Test
    public void testOverlapsInterpretsNullEndAsInfiniteFuture() {
        TimePeriod a = new TimePeriod();
        TimePeriod b = new TimePeriod();
        long aMomentInTime = System.currentTimeMillis() - 1000;
        a.setStartDateTime(new Date(aMomentInTime - 1000));
        b.setStartDateTime(new Date(aMomentInTime - 1500));
        b.setEndDateTime(new Date(aMomentInTime + 1500));
        Assert.assertTrue(a.overlaps(b));
    }
    @Test
    public void testNonOverlapping() {
        TimePeriod a = new TimePeriod();
        TimePeriod b = new TimePeriod();
        long aMomentInTime = System.currentTimeMillis() - 1000;
        a.setEndDateTime(new Date(aMomentInTime));
        b.setStartDateTime(new Date(aMomentInTime + 1));
        Assert.assertFalse(a.overlaps(b));
    }
}
