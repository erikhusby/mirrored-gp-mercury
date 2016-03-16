package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import com.sun.jersey.api.json.JSONUnmarshaller;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionStatusResultBeanTest {

    private SubmissionStatusDetailBean detail1;
    private SubmissionStatusDetailBean detail2;
    private String testUUID1;
    private String testUUID2;
    private String testJson;
    private SubmissionStatusResultBean results;
    private SubmissionStatusDetailBean detail11;
    private GregorianCalendar statusUpdate;
    private SubmissionStatusDetailBean detail21;
    private SubmissionStatusDetailBean detail3;
    private SubmissionStatusDetailBean detail4;
    private SubmissionStatusDetailBean detail5;
    private Date submissionUpdateDate;
    @BeforeMethod
    public void setUp() throws Exception {
    submissionUpdateDate = DateUtils.parseISO8601Date("2014-07-30T11:35:58Z");
        testUUID1 = "d835cc7-cd63-4cc6-9621-868155618745";
        detail1 = new SubmissionStatusDetailBean(testUUID1, "Submitted",
                SubmissionRepository.DEFAULT_REPOSITORY_NAME,
                SubmissionLibraryDescriptor.WHOLE_GENOME_DESCRIPTION, submissionUpdateDate);
        testUUID2 = "d835cc7-cd63-4cc6-9621-868155618746";
        detail2 = new SubmissionStatusDetailBean(testUUID2, "Failure", SubmissionRepository.DEFAULT_REPOSITORY_NAME,
                SubmissionLibraryDescriptor.WHOLE_GENOME_NAME, submissionUpdateDate,
                "And error was returned from NCBI");

        testJson = "{\n"
                          + "  \"submissionStatuses\": [\n"
                          + "    {\n"
                          + "      \"lastStatusUpdate\": \"2014-07-30T11:35:58Z\", \n"
                   + "      \"libraryDescriptor\": \"HumanWholeGenome\", \n"
                   + "      \"site\": \"NCBI_PROTECTED\", \n"
                          + "      \"status\": \"ReadyForSubmission\",\n"
                          + "      \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618745\"\n"
                          + "    },\n"
                          + "    {\n"
                          + "      \"libraryDescriptor\": \"Human Whole Genome\", \n"
                          + "      \"lastStatusUpdate\": \"2014-07-30T11:35:58Z\", \n"
                          + "      \"libraryDescriptor\": \"HumanWholeGenome\", \n"
                          + "      \"site\": \"NCBI_PROTECTED\", \n"
                          + "      \"status\": \"ReadyForSubmission\",\n"
                          + "      \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618746\"\n"
                          + "    },\n"
                          + "    {\n"
                          + "      \"errors\": [\n"
                          + "        \"No bioproject found matching submitted accession BlahBlahBlah\",\n"
                          + "        \"No biosample found matching submitted id BlahBlahBlah\"\n"
                          + "      ],\n"
                          + "      \"lastStatusUpdate\": \"2014-07-30T11:35:58Z\", \n"
                   + "      \"libraryDescriptor\": \"HumanWholeGenome\", \n"
                   + "      \"site\": \"NCBI_PROTECTED\", \n"
                          + "      \"status\": \"Failure\",\n"
                          + "      \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618749\"\n"
                          + "    },\n"
                          + "    {\n"
                          + "      \"errors\": [\n"
                          + "        \"No biosample found matching submitted id BlahBlahBlah\"\n"
                          + "      ],\n"
                          + "      \"lastStatusUpdate\": \"2014-07-30T11:35:58Z\", \n"
                   + "      \"libraryDescriptor\": \"HumanWholeGenome\", \n"
                   + "      \"site\": \"NCBI_PROTECTED\", \n"
                          + "      \"status\": \"Failure\",\n"
                          + "      \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618748\"\n"
                          + "    },\n"
                          + "    {\n"
                          + "      \"errors\": [\n"
                          + "        \"No bioproject found matching submitted accession BlahBlah\"\n"
                          + "      ],\n"
                          + "      \"site\": \"NCBI_PROTECTED\", \n"
                          + "      \"libraryDescriptor\": \"Human Whole Genome\", \n"
                          + "      \"lastStatusUpdate\": \"2014-07-30T11:35:58Z\", \n"
                          + "      \"status\": \"Failure\",\n"
                          + "      \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618747\"\n"
                          + "    }\n"
                          + "  ]\n"
                          + "}";
        results = new SubmissionStatusResultBean();
        detail11 = new SubmissionStatusDetailBean("7d835cc7-cd63-4cc6-9621-868155618745",
                "ReadyForSubmission", SubmissionRepository.DEFAULT_REPOSITORY_NAME,
                SubmissionLibraryDescriptor.WHOLE_GENOME_DESCRIPTION, submissionUpdateDate);
        statusUpdate = new GregorianCalendar();
        statusUpdate.set(Calendar.MILLISECOND, 0);

        statusUpdate.set(2014, Calendar.JULY, 30, 11, 35, 58);
        detail11.setLastStatusUpdate(statusUpdate.getTime());

        detail21 = new SubmissionStatusDetailBean("7d835cc7-cd63-4cc6-9621-868155618746", "ReadyForSubmission",
                SubmissionRepository.DEFAULT_REPOSITORY_NAME, SubmissionLibraryDescriptor.WHOLE_GENOME_DESCRIPTION,
                submissionUpdateDate);
        detail3 = new SubmissionStatusDetailBean("7d835cc7-cd63-4cc6-9621-868155618749", "Failure",
                SubmissionRepository.DEFAULT_REPOSITORY_NAME, SubmissionLibraryDescriptor.WHOLE_GENOME_DESCRIPTION,
                submissionUpdateDate,
                "No bioproject found matching submitted accession BlahBlahBlah",
                "No biosample found matching submitted id BlahBlahBlah");
        detail4 = new SubmissionStatusDetailBean("7d835cc7-cd63-4cc6-9621-868155618748", "Failure",
                SubmissionRepository.DEFAULT_REPOSITORY_NAME, SubmissionLibraryDescriptor.WHOLE_GENOME_DESCRIPTION,
                submissionUpdateDate,
                "No biosample found matching submitted id BlahBlahBlah");
        detail5 = new SubmissionStatusDetailBean("7d835cc7-cd63-4cc6-9621-868155618747", "Failure",
                SubmissionRepository.DEFAULT_REPOSITORY_NAME, SubmissionLibraryDescriptor.WHOLE_GENOME_DESCRIPTION,
                submissionUpdateDate,
                "No bioproject found matching submitted accession BlahBlah");

        detail21.setLastStatusUpdate(statusUpdate.getTime());
        detail3.setLastStatusUpdate(statusUpdate.getTime());
        detail4.setLastStatusUpdate(statusUpdate.getTime());
        detail5.setLastStatusUpdate(statusUpdate.getTime());
        results.setSubmissionStatuses(Arrays.asList(detail11, detail21, detail3, detail4, detail5));

    }

    public void testResults() throws Exception {

        SubmissionStatusResultBean results = new SubmissionStatusResultBean();


        Assert.assertTrue(results.getSubmissionStatuses().isEmpty());

        results.setSubmissionStatuses(Arrays.asList(detail1, detail2));

        Assert.assertNotNull(results.getSubmissionStatuses());
        Assert.assertEquals(2, results.getSubmissionStatuses().size());
    }

    public void testSerialize() throws Exception {

        JSONJAXBContext context = new JSONJAXBContext(JSONConfiguration.natural().humanReadableFormatting(true).build(),
                SubmissionStatusResultBean.class);
        JSONMarshaller marshaller = context.createJSONMarshaller();

        StringWriter writer = new StringWriter();

        marshaller.marshallToJSON(results, writer);
        Assert.assertEquals(writer.toString().replaceAll("\\s+", ""), testJson.replaceAll("\\s+", ""));
    }

    public void testDeSerialize() throws Exception {
        JSONJAXBContext context =
                new JSONJAXBContext(JSONConfiguration.natural().humanReadableFormatting(true).build(),
                        SubmissionStatusResultBean.class);

        JSONUnmarshaller unmarshaller = context.createJSONUnmarshaller();

        InputStream input = new ByteArrayInputStream(testJson.getBytes());

        SubmissionStatusResultBean deserializedResultBean = unmarshaller.unmarshalFromJSON(input, SubmissionStatusResultBean.class);

        Assert.assertEquals(deserializedResultBean, results);
    }

}
