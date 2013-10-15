package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.gpinformatics.athena.boundary.kits.SampleKitRequestDto;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.testng.annotations.Test;

/**
 */
public class BSPWorkRequestFactoryTest {

    @Test
    public void testBuildBspKitWorkRequest() throws Exception {
        SampleKitRequestDto sampleKitRequestDto = new SampleKitRequestDto("breilly", null, "0.75mL", 0, 96, "42", null, null, null);

        SampleKitWorkRequest workRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(
                "BSPKitRequestServiceTest.testBuildBspKitWorkRequest", "breilly", "PDO-1", 14038, 14038L, 42L, 96L);

        MatcherAssert.assertThat(workRequest.getWorkRequestName(),
                CoreMatchers.equalTo("BSPKitRequestServiceTest.testBuildBspKitWorkRequest"));
        MatcherAssert.assertThat(workRequest.getProjectManagerId(), CoreMatchers.equalTo(14038L));
        MatcherAssert.assertThat(workRequest.getPrimaryInvestigatorId(), CoreMatchers.equalTo(14038L));
        MatcherAssert.assertThat(workRequest.getPdoId(), CoreMatchers.equalTo("PDO-1"));
        MatcherAssert.assertThat(workRequest.getRequestUser(), CoreMatchers.equalTo("breilly"));
        MatcherAssert.assertThat(workRequest.getNumberOfSamples(), CoreMatchers.equalTo(96L));
        MatcherAssert.assertThat(workRequest.getSiteId(), CoreMatchers.equalTo(42L));
    }
}
