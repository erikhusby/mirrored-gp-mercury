package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SampleProgressBarTest {

    // Tests for formatPercentageString().

    public void testFormatProgressZeroPercent() {
        assertThat(SampleProgressBar.formatPercentageString(0), equalTo("0"));
    }

    public void testFormatProgressLessThanOnePercent() {
        assertThat(SampleProgressBar.formatPercentageString(.005), equalTo("<1"));
    }

    public void testFormatProgressOnePercent() {
        assertThat(SampleProgressBar.formatPercentageString(.01), equalTo("1"));
    }

    public void testFormatProgressFiftyPercent() {
        assertThat(SampleProgressBar.formatPercentageString(.5), equalTo("50"));
    }

    public void testFormatProgressNinetyNinePercent() {
        assertThat(SampleProgressBar.formatPercentageString(.99), equalTo("99"));
    }

    public void testFormatProgressMoreThanNinetyNinePercent() {
        assertThat(SampleProgressBar.formatPercentageString(.995), equalTo(">99"));
    }

    public void testFormatProgressOneHundredPercent() {
        assertThat(SampleProgressBar.formatPercentageString(1), equalTo("100"));
    }

    // Tests for getPercentForProgressBarWidth().

    public void testGetPercentForProgressBarWidthZeroPercent() {
        assertThat(SampleProgressBar.getPercentForProgressBarWidth(0), equalTo(0L));
    }

    public void testGetPercentForProgressBarWidthLessThanOnePercent() {
        assertThat(SampleProgressBar.getPercentForProgressBarWidth(.005), equalTo(1L));
    }

    public void testGetPercentForProgressBarWidthOnePercent() {
        assertThat(SampleProgressBar.getPercentForProgressBarWidth(.01), equalTo(1L));
    }

    public void testGetPercentForProgressBarWidthFiftyPercent() {
        assertThat(SampleProgressBar.getPercentForProgressBarWidth(.5), equalTo(50L));
    }

    public void testGetPercentForProgressBarWidthNinetyNinePercent() {
        assertThat(SampleProgressBar.getPercentForProgressBarWidth(.99), equalTo(99L));
    }

    public void testGetPercentForProgressBarWidthMoreThanNinetyNinePercent() {
        assertThat(SampleProgressBar.getPercentForProgressBarWidth(.995), equalTo(99L));
    }

    public void testGetPercentForProgressBarWidthOneHundredPercent() {
        assertThat(SampleProgressBar.getPercentForProgressBarWidth(1), equalTo(100L));
    }
}
