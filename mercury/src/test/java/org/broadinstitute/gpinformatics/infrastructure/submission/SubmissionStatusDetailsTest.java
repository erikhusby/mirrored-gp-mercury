package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.ArrayUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionStatusDetailsTest {


    private final String testError1 = "Invalid UUID";
    private final String testError2 = "Unable to contact NCBI";
    private String testUuid = "d835cc7-cd63-4cc6-9621-868155618745";

    public void testStatusConstruction() throws Exception {
        SubmissionStatusDetails testStatus = new SubmissionStatusDetails();

        Assert.assertNull(testStatus.getUuid());
        Assert.assertNull(testStatus.getStatus());
        Assert.assertNull(testStatus.getErrors());

        testStatus.setUuid(testUuid);

        Assert.assertEquals(testStatus.getUuid(), testUuid);

        testStatus.setStatus("InTransit");

        Assert.assertEquals(testStatus.getStatus(),
                SubmissionStatusDetails.Status.IN_TRANSIT.getDescription());

        testStatus.setErrors(testError1, testError2);

        Assert.assertNotNull(testStatus.getErrors());

        Assert.assertEquals(2, testStatus.getErrors().length);
        Assert.assertTrue(ArrayUtils.contains(testStatus.getErrors(),testError1));
        Assert.assertTrue(ArrayUtils.contains(testStatus.getErrors(),testError2));
    }
}
