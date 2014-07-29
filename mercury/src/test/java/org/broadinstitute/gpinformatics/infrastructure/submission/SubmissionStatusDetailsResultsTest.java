package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.StringWriter;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionStatusDetailsResultsTest {

    private SubmissionStatusDetails detail1;
    private SubmissionStatusDetails detail2;
    private String testUUID1;
    private String testUUID2;

    @BeforeMethod
    public void setUp() throws Exception {

        testUUID1 = "d835cc7-cd63-4cc6-9621-868155618745";
        detail1 = new SubmissionStatusDetails(testUUID1,"Submitted");
        testUUID2 = "d835cc7-cd63-4cc6-9621-868155618746";
        detail2 = new SubmissionStatusDetails(testUUID2,"Failure", "And error was returned from NCBI");

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

    @Test(enabled = false)
    public void testDeSerialize() throws Exception {

        JSONJAXBContext context = new JSONJAXBContext(JSONConfiguration.natural().humanReadableFormatting(true).build(),
                SubmissionStatusResults.class);
        JSONMarshaller marshaller = context.createJSONMarshaller();

        StringWriter writer = new StringWriter();

        SubmissionStatusResults results = new SubmissionStatusResults();
        results.setSubmissionStatuses(detail1, detail2);

        marshaller.marshallToJSON(results, writer);
        System.out.println(writer.toString());
    }


}
