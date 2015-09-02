package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

/**
 * Tests for ProductOrderCompletionStatus, which is responsible for holding raw counts and calculating percentages.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderCompletionStatusTest {

    public static final double ERROR = .0001;

    public void testCounts() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(1, 2, 6);
        assertThat(status.getNumberAbandoned(), equalTo(1));
        assertThat(status.getNumberCompleted(), equalTo(2));
        assertThat(status.getTotal(), equalTo(6));
        assertThat(status.getNumberInProgress(), equalTo(3));
    }

    // Tests for getPercentComplete()

    public void testPercentCompleteNoSamples() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 0, 0);
        assertThat(status.getPercentCompleted(), closeTo(0, ERROR));
    }

    public void testPercentCompleteNone() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 0, 1);
        assertThat(status.getPercentCompleted(), closeTo(0, ERROR));
    }

    public void testPercentCompleteLow() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 1, 200);
        assertThat(status.getPercentCompleted(), closeTo(.005, ERROR));
    }

    public void testPercentCompleteMid() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 1, 2);
        assertThat(status.getPercentCompleted(), closeTo(.5, ERROR));
    }

    public void testPercentCompleteHigh() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(1, 199, 200);
        assertThat(status.getPercentCompleted(), closeTo(.995, ERROR));
    }

    public void testPercentCompleteAll() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 1, 1);
        assertThat(status.getPercentCompleted(), closeTo(1, ERROR));
    }

    // Tests for getPercentInProgress()

    public void testPercentInProgressNoSamples() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 0, 0);
        assertThat(status.getPercentInProgress(), closeTo(0, ERROR));
    }

    public void testPercentInProgressNone() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(1, 0, 1);
        assertThat(status.getPercentInProgress(), closeTo(0, ERROR));
    }

    public void testPercentInProgressLow() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 199, 200);
        assertThat(status.getPercentInProgress(), closeTo(.005, ERROR));
    }

    public void testPercentInProgressMid() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 1, 2);
        assertThat(status.getPercentInProgress(), closeTo(.5, ERROR));
    }

    public void testPercentInProgressHigh() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 1, 200);
        assertThat(status.getPercentInProgress(), closeTo(.995, ERROR));
    }

    public void testPercentInProgressAll() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 0, 1);
        assertThat(status.getPercentInProgress(), closeTo(1, ERROR));
    }

    // Tests for getPercentAbandoned()

    public void testPercentAbandonedNoSamples() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 0, 0);
        assertThat(status.getPercentAbandoned(), closeTo(0, ERROR));
    }

    public void testPercentAbandonedNone() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 0, 1);
        assertThat(status.getPercentAbandoned(), closeTo(0, ERROR));
    }

    public void testPercentAbandonedLow() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(1, 0, 200);
        assertThat(status.getPercentAbandoned(), closeTo(.005, ERROR));
    }

    public void testPercentAbandonedMid() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(1, 1, 2);
        assertThat(status.getPercentAbandoned(), closeTo(.5, ERROR));
    }

    public void testPercentAbandonedHigh() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(199, 0, 200);
        assertThat(status.getPercentAbandoned(), closeTo(.995, ERROR));
    }

    public void testPercentAbandonedAll() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(1, 0, 1);
        assertThat(status.getPercentAbandoned(), closeTo(1, ERROR));
    }
}
