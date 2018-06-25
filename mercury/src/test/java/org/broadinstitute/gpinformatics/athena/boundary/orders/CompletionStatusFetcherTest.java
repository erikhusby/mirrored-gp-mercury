package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

@Test(groups = TestGroups.DATABASE_FREE)
public class CompletionStatusFetcherTest {

    public static final double ERROR = .00001;

    public void testGetStatsForUnknownPdo() {
        String pdoKey = "PDO-1";
        CompletionStatusFetcher status =
                new CompletionStatusFetcher(Collections.<String, ProductOrderCompletionStatus>emptyMap());

        assertThat(status.getNumberOfSamples(pdoKey), equalTo(0));
        assertThat(status.getNumberCompleted(pdoKey), equalTo(0));
        assertThat(status.getNumberAbandoned(pdoKey), equalTo(0));
        assertThat(status.getNumberInProgress(pdoKey), equalTo(0));

        assertThat(status.getPercentCompleted(pdoKey), closeTo(0, ERROR));
        assertThat(status.getPercentAbandoned(pdoKey), closeTo(0, ERROR));
        assertThat(status.getPercentInProgress(pdoKey), closeTo(0, ERROR));
    }

    public void testGetCountsForKnownPdo() {
        String pdoKey = "PDO-1";
        int total = 6;
        int completed = 2;
        int abandoned = 1;
        int inProgress = 3;
        CompletionStatusFetcher status = new CompletionStatusFetcher(
                Collections.singletonMap(pdoKey, new ProductOrderCompletionStatus(abandoned, completed, total)));

        assertThat(status.getNumberOfSamples(pdoKey), equalTo(total));
        assertThat(status.getNumberCompleted(pdoKey), equalTo(completed));
        assertThat(status.getNumberAbandoned(pdoKey), equalTo(abandoned));
        assertThat(status.getNumberInProgress(pdoKey), equalTo(inProgress));

        assertThat(status.getPercentCompleted(pdoKey), closeTo(.33333, ERROR));
        assertThat(status.getPercentAbandoned(pdoKey), closeTo(.16666, ERROR));
        assertThat(status.getPercentInProgress(pdoKey), closeTo(.5, ERROR));
    }

    public void testGetStatusForKnownPdo() {
        String pdoKey = "PDO-1";
        ProductOrderCompletionStatus pdoStatus = new ProductOrderCompletionStatus(1, 2, 6);
        CompletionStatusFetcher status = new CompletionStatusFetcher(
                Collections.singletonMap(pdoKey, pdoStatus));

        ProductOrderCompletionStatus fetchedPdoStatus = status.getStatus(pdoKey);
        assertThat(fetchedPdoStatus, equalTo(pdoStatus));
    }
}
