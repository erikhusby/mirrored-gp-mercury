package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionStatusDetailBeanTest {


    private final String testError1 = "Invalid UUID";
    private final String testError2 = "Unable to contact NCBI";
    private String testUuid = "d835cc7-cd63-4cc6-9621-868155618745";

    public void testStatusConstruction() throws Exception {
        SubmissionStatusDetailBean testStatus = new SubmissionStatusDetailBean();

        Assert.assertNull(testStatus.getUuid());
        Assert.assertNull(testStatus.getStatusString());
        Assert.assertTrue(testStatus.getErrors().isEmpty());

        testStatus.setUuid(testUuid);

        Assert.assertEquals(testStatus.getUuid(), testUuid);

        testStatus.setStatus(SubmissionStatusDetailBean.Status.IN_TRANSIT);

        Assert.assertEquals(testStatus.getStatusString(),
                SubmissionStatusDetailBean.Status.IN_TRANSIT.getLabel());

        testStatus.setErrors(Arrays.asList(testError1, testError2));

        Assert.assertNotNull(testStatus.getErrors());

        Assert.assertEquals(2, testStatus.getErrors().size());
        Assert.assertTrue(testStatus.getErrors().contains(testError1));
        Assert.assertTrue(testStatus.getErrors().contains(testError2));
    }
}
