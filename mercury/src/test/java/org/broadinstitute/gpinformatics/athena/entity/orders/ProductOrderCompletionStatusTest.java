package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;

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

    // Display percentages

    /**
     * Test the percentage display strings when the calculations do not involve any rounding.
     */
    public void testDisplayPercentagesNoRounding() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(5, 10, 100);

        assertThat(status.getPercentAbandonedDisplay(), equalTo("5"));
        assertThat(status.getPercentCompletedDisplay(), equalTo("10"));
        assertThat(status.getPercentInProgressDisplay(), equalTo("85"));
    }

    /**
     * Test the percentage display strings when the calculations are rounded but where the sum of the rounded
     * percentages is 100.
     */
    public void testDisplayPercentagesWithMatchingRounding() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 1, 3);

        assertThat(status.getPercentAbandonedDisplay(), equalTo("0"));
        assertThat(status.getPercentCompletedDisplay(), equalTo(">33"));
        assertThat(status.getPercentInProgressDisplay(), equalTo("<67"));
    }

    /**
     * Test the percentage display strings when the calculations involve multiple round-ups such that the sum of the
     * rounded percentages is greater than 100. This occurs when two calculations each have .5% and are rounded up. The
     * desired output is to use "&lt;" and "&gt;" to indicate that there is some rounding error but that the base
     * numbers still sum to 100.
     */
    public void testDisplayPercentagesWithRoundUp() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(99, 101, 200);

        assertThat(status.getPercentAbandonedDisplay(), equalTo("<50"));
        assertThat(status.getPercentCompletedDisplay(), equalTo(">50"));
        assertThat(status.getPercentInProgressDisplay(), equalTo("0"));

        // Another case invoking the same logic but where the split is uneven.
        status = new ProductOrderCompletionStatus(31, 19, 200);

        assertThat(status.getPercentAbandonedDisplay(), equalTo(">15"));
        assertThat(status.getPercentInProgressDisplay(), equalTo("75"));
        assertThat(status.getPercentCompletedDisplay(), equalTo("<10"));
    }

    /**
     * Test the percentage display strings when the calculations involve multiple round-downs such that the sum of the
     * rounded percentages is less than 100. This occurs when more than two calculations share a percentage point making
     * each have < .5%. The desired output is to use "&gt;" to indicate that there is some rounding error. The sum of
     * the base numbers will only be 99, but the "&gt;" should be enough to communicate that the rounding error is where
     * the remaining 1% is.
     */
    public void testDisplayPercentagesWithRoundDown() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(1, 1, 3);

        assertThat(status.getPercentAbandonedDisplay(), equalTo(">33"));
        assertThat(status.getPercentCompletedDisplay(), equalTo(">33"));
        assertThat(status.getPercentInProgressDisplay(), equalTo(">33"));
    }

    /**
     * Test the percentage display strings when the calculations involve rounding that could make some numbers go to "0"
     * or "100". These are special cases where the desired output is "&lt;1" instead of "&gt;0" and "&gt;99" instead of
     * "&lt;100" to avoid giving the impression that no work or all work is complete.
     */
    public void testDisplayPercentagesAlmostComplete() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(0, 1, 500);

        assertThat(status.getPercentAbandonedDisplay(), equalTo("0"));
        assertThat(status.getPercentCompletedDisplay(), equalTo("<1"));
        assertThat(status.getPercentInProgressDisplay(), equalTo(">99"));
    }

    /**
     * Test the percentage display strings when the calculations involve some rounding to small numbers and some
     * non-rounding. This further exercises the "&lt;1" special case and makes sure that it doesn't degrade to the case
     * where two calculations each share .5%.
     */
    public void testDisplayPercentagesSomeFractionalSomeWhole() {
        ProductOrderCompletionStatus status = new ProductOrderCompletionStatus(1, 1, 200);

        assertThat(status.getPercentAbandonedDisplay(), equalTo("<1"));
        assertThat(status.getPercentCompletedDisplay(), equalTo("<1"));
        assertThat(status.getPercentInProgressDisplay(), equalTo("99"));
    }
}
