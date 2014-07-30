package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.StringWriter;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionRequestBeanTest {


    public void testDeSerialize() throws Exception {

        BioProject bioProject1 = new BioProject("PRJNA75333");
        BioProject bioProject2 = new BioProject("BlahBlah");
        BioProject bioProject3 = new BioProject("BlahBlahBlah");

        SubmissionBioSampleBean bioSampleBean1 =
                new SubmissionBioSampleBean("S_2507","/some/funky/file.bam",
                new SubmissionContactBean("jgentry@broadinstitute.org","617-555-9292","homer","Jeff","Gentry", "A"));
        SubmissionContactBean contact2 = new SubmissionContactBean("jgentry2@broadinstitute.org", "617-555-5555",
                "homer", "Jeffrey", "G", "A");
        SubmissionBioSampleBean bioSampleBean2 =
                new SubmissionBioSampleBean("S_2651","/some/funky/file2.bam",
                        contact2);
        SubmissionBioSampleBean bioSampleBean3 =
                new SubmissionBioSampleBean("BlahBlahBlah","/some/funky/file2.bam",
                        contact2);

        String uuID1 = "7d835cc7-cd63-4cc6-9621-868155618745";
        String uuID2 = "7d835cc7-cd63-4cc6-9621-868155618746";
        String uuID3 = "7d835cc7-cd63-4cc6-9621-868155618747";
        String uuID4 = "7d835cc7-cd63-4cc6-9621-868155618748";
        String uuID5 = "7d835cc7-cd63-4cc6-9621-868155618749";


        String studyContact1 = "jgentry";
        String studyContact2 = "noSuchProject";
        String studyContact3 = "noSuchSample";
        String studyContact4 = "noSuchProjectOrSample";


        String testJson = "{\n"
                          + "    \"submissions\": [\n"
                          + "        {\n"
                          + "            \"bioproject\": {\n"
                          + "                \"accession\": \"PRJNA75333\"\n"
                          + "            },\n"
                          + "            \"biosample\": {\n"
                          + "                \"contact\": {\n"
                          + "                    \"email\": \"jgentry@broadinstitute.org\",\n"
                          + "                    \"firstName\": \"Jeff\",\n"
                          + "                    \"lab\": \"homer\",\n"
                          + "                    \"lastName\": \"Gentry\",\n"
                          + "                    \"middleName\": \"A\",\n"
                          + "                    \"phone\": \"617-555-9292\"\n"
                          + "                },\n"
                          + "                \"filePath\": \"/some/funky/file.bam\",\n"
                          + "                \"sampleId\": \"S_2507\"\n"
                          + "            },\n"
                          + "            \"studyContact\": \"jgentry\",\n"
                          + "            \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618745\"\n"
                          + "        },\n"
                          + "        {\n"
                          + "            \"bioproject\": {\n"
                          + "                \"accession\": \"PRJNA75333\"\n"
                          + "            },\n"
                          + "            \"biosample\": {\n"
                          + "                \"contact\": {\n"
                          + "                    \"email\": \"jgentry2@broadinstitute.org\",\n"
                          + "                    \"firstName\": \"Jeffrey\",\n"
                          + "                    \"lab\": \"homer\",\n"
                          + "                    \"lastName\": \"G\",\n"
                          + "                    \"middleName\": \"A\",\n"
                          + "                    \"phone\": \"617-555-5555\"\n"
                          + "                },\n"
                          + "                \"filePath\": \"/some/funky/file2.bam\",\n"
                          + "                \"sampleId\": \"S_2651\"\n"
                          + "            },\n"
                          + "            \"studyContact\": \"jgentry\",\n"
                          + "            \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618746\"\n"
                          + "        },\n"
                          + "        {\n"
                          + "            \"bioproject\": {\n"
                          + "                \"accession\": \"BlahBlah\"\n"
                          + "            },\n"
                          + "            \"biosample\": {\n"
                          + "                \"contact\": {\n"
                          + "                    \"email\": \"jgentry2@broadinstitute.org\",\n"
                          + "                    \"firstName\": \"Jeffrey\",\n"
                          + "                    \"lab\": \"homer\",\n"
                          + "                    \"lastName\": \"G\",\n"
                          + "                    \"middleName\": \"A\",\n"
                          + "                    \"phone\": \"617-555-5555\"\n"
                          + "                },\n"
                          + "                \"filePath\": \"/some/funky/file2.bam\",\n"
                          + "                \"sampleId\": \"S_2651\"\n"
                          + "            },\n"
                          + "            \"studyContact\": \"noSuchProject\",\n"
                          + "            \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618747\"\n"
                          + "        },\n"
                          + "        {\n"
                          + "            \"bioproject\": {\n"
                          + "                \"accession\": \"PRJNA75333\"\n"
                          + "            },\n"
                          + "            \"biosample\": {\n"
                          + "                \"contact\": {\n"
                          + "                    \"email\": \"jgentry2@broadinstitute.org\",\n"
                          + "                    \"firstName\": \"Jeffrey\",\n"
                          + "                    \"lab\": \"homer\",\n"
                          + "                    \"lastName\": \"G\",\n"
                          + "                    \"middleName\": \"A\",\n"
                          + "                    \"phone\": \"617-555-5555\"\n"
                          + "                },\n"
                          + "                \"filePath\": \"/some/funky/file2.bam\",\n"
                          + "                \"sampleId\": \"BlahBlahBlah\"\n"
                          + "            },\n"
                          + "            \"studyContact\": \"noSuchSample\",\n"
                          + "            \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618748\"\n"
                          + "        },\n"
                          + "        {\n"
                          + "            \"bioproject\": {\n"
                          + "                \"accession\": \"BlahBlahBlah\"\n"
                          + "            },\n"
                          + "            \"biosample\": {\n"
                          + "                \"contact\": {\n"
                          + "                    \"email\": \"jgentry2@broadinstitute.org\",\n"
                          + "                    \"firstName\": \"Jeffrey\",\n"
                          + "                    \"lab\": \"homer\",\n"
                          + "                    \"lastName\": \"G\",\n"
                          + "                    \"middleName\": \"A\",\n"
                          + "                    \"phone\": \"617-555-5555\"\n"
                          + "                },\n"
                          + "                \"filePath\": \"/some/funky/file2.bam\",\n"
                          + "                \"sampleId\": \"BlahBlahBlah\"\n"
                          + "            },\n"
                          + "            \"studyContact\": \"noSuchProjectOrSample\",\n"
                          + "            \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618749\"\n"
                          + "        }\n"
                          + "   ]\n"
                          + "}\n";

        JSONJAXBContext context = new JSONJAXBContext(
                JSONConfiguration.natural().humanReadableFormatting(true).build(),
                SubmissionRequestBean.class);
        JSONMarshaller marshaller = context.createJSONMarshaller();

        StringWriter writer = new StringWriter();

        SubmissionRequestBean request = new SubmissionRequestBean();
        request.setSubmissions(new SubmissionBean(uuID1, studyContact1, bioProject1, bioSampleBean1),
                new SubmissionBean(uuID2, studyContact1, bioProject1, bioSampleBean2),
                new SubmissionBean(uuID3, studyContact2, bioProject2, bioSampleBean2),
                new SubmissionBean(uuID4, studyContact3, bioProject1, bioSampleBean3),
                new SubmissionBean(uuID5, studyContact4, bioProject3, bioSampleBean3));

        marshaller.marshallToJSON(request, writer);
        Assert.assertEquals(writer.toString().replaceAll("\\s+", ""), testJson.replaceAll("\\s+", ""));

    }
}
