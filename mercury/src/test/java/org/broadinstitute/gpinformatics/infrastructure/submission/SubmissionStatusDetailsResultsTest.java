package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionStatusDetailsResultsTest {

    private SubmissionStatusDetails detail1;
    private SubmissionStatusDetails detail2;

    @BeforeMethod
    public void setUp() throws Exception {

        detail1 = new SubmissionStatusDetails("d835cc7-cd63-4cc6-9621-868155618745","Submitted");
        detail2 = new SubmissionStatusDetails("d835cc7-cd63-4cc6-9621-868155618745","Failure", "And error was returned from NCBI");

    }

    public void testResults() throws Exception {

        SubmissionStatusResults results = new SubmissionStatusResults();


        Assert.assertNull(results.getSubmissionStatuses());

        results.setSubmissionStatuses(detail1, detail2);

        Assert.assertNotNull(results.getSubmissionStatuses());
        Assert.assertEquals(2, results.getSubmissionStatuses().length);
    }

    public void testResultsFromArray() throws Exception {

        SubmissionStatusResults results = new SubmissionStatusResults();


        Assert.assertNull(results.getSubmissionStatuses());

        results.setSubmissionStatuses(new SubmissionStatusDetails[]{detail1, detail2});

        Assert.assertNotNull(results.getSubmissionStatuses());
        Assert.assertEquals(2, results.getSubmissionStatuses().length);
    }

}
