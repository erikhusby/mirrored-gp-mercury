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

    public void testDeSerialize() throws Exception {

        String testJson = "{\n"
                          + "  \"submissionStatuses\": [\n"
                          + "    {\n"
                          + "      \"status\": \"ReadyForSubmission\",\n"
                          + "      \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618745\"\n"
                          + "    },\n"
                          + "    {\n"
                          + "      \"status\": \"ReadyForSubmission\",\n"
                          + "      \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618746\"\n"
                          + "    },\n"
                          + "    {\n"
                          + "      \"errors\": [\n"
                          + "        \"No bioproject found matching submitted accession BlahBlahBlah\",\n"
                          + "        \"No biosample found matching submitted id BlahBlahBlah\"\n"
                          + "      ],\n"
                          + "      \"status\": \"Failure\",\n"
                          + "      \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618749\"\n"
                          + "    },\n"
                          + "    {\n"
                          + "      \"errors\": [\n"
                          + "        \"No biosample found matching submitted id BlahBlahBlah\"\n"
                          + "      ],\n"
                          + "      \"status\": \"Failure\",\n"
                          + "      \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618748\"\n"
                          + "    },\n"
                          + "    {\n"
                          + "      \"errors\": [\n"
                          + "        \"No bioproject found matching submitted accession BlahBlah\"\n"
                          + "      ],\n"
                          + "      \"status\": \"Failure\",\n"
                          + "      \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618747\"\n"
                          + "    }\n"
                          + "  ]\n"
                          + "}";

        JSONJAXBContext context = new JSONJAXBContext(JSONConfiguration.natural().humanReadableFormatting(true).build(),
                SubmissionStatusResults.class);
        JSONMarshaller marshaller = context.createJSONMarshaller();

        StringWriter writer = new StringWriter();

        SubmissionStatusResults results = new SubmissionStatusResults();
        SubmissionStatusDetails detail1 =
                new SubmissionStatusDetails("7d835cc7-cd63-4cc6-9621-868155618745", "ReadyForSubmission");
        SubmissionStatusDetails detail2 =
                new SubmissionStatusDetails("7d835cc7-cd63-4cc6-9621-868155618746", "ReadyForSubmission");
        SubmissionStatusDetails detail3 =
                new SubmissionStatusDetails("7d835cc7-cd63-4cc6-9621-868155618749", "Failure",
                        "No bioproject found matching submitted accession BlahBlahBlah",
                        "No biosample found matching submitted id BlahBlahBlah");
        SubmissionStatusDetails detail4 =
                new SubmissionStatusDetails("7d835cc7-cd63-4cc6-9621-868155618748", "Failure",
                        "No biosample found matching submitted id BlahBlahBlah");
        SubmissionStatusDetails detail5 =
                new SubmissionStatusDetails("7d835cc7-cd63-4cc6-9621-868155618747", "Failure",
                        "No bioproject found matching submitted accession BlahBlah");
        results.setSubmissionStatuses(detail1, detail2, detail3, detail4, detail5);

        marshaller.marshallToJSON(results, writer);
        Assert.assertEquals(writer.toString().replaceAll("\\s+",""), testJson.replaceAll("\\s+",""));
    }


}
