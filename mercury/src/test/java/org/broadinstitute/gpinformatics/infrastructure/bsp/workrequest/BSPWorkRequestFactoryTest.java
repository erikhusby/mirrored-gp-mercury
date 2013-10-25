package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for factory class for creating {@link SampleKitWorkRequest}s for use by {@link BSPKitRequestService}.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class BSPWorkRequestFactoryTest {

    @Test
    public void testBuildBspKitWorkRequest() throws Exception {
        SampleKitWorkRequest workRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(
                "BSPKitRequestServiceTest.testBuildBspKitWorkRequest", "breilly", "PDO-1", 1, 2, 3, 4L);

        assertThat(workRequest.getPrimaryInvestigatorId(), equalTo(1L));
        assertThat(workRequest.getProjectManagerId(), equalTo(2L));
        assertThat(workRequest.getExternalCollaboratorId(), nullValue());
        assertThat(workRequest.getBarCode(), nullValue());
        assertThat(workRequest.getWorkRequestName(), equalTo("BSPKitRequestServiceTest.testBuildBspKitWorkRequest"));
        assertThat(workRequest.getRequestUser(), equalTo("breilly"));
        assertThat(workRequest.getPdoId(), equalTo("PDO-1"));
        assertThat(workRequest.getStatus(), nullValue());
        assertThat(workRequest.getNotificationList(), nullValue());
        assertThat(workRequest.getErrors(), nullValue());
        assertThat(workRequest.getWarnings(), nullValue());
        assertThat(workRequest.getInfo(), nullValue());
        assertThat(workRequest.getMoleculeType(), nullValue());
        assertThat(workRequest.getSiteId(), equalTo(3L));
        assertThat(workRequest.getNumberOfSamples(), equalTo(4L));
        assertThat(workRequest.getTransferMethod(), equalTo(SampleKitWorkRequest.TransferMethod.SHIP_OUT));
    }
}
