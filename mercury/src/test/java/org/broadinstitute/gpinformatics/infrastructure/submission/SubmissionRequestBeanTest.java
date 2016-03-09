package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.common.MercuryStringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.StringWriter;
import java.util.Arrays;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionRequestBeanTest {


    private String testJson;
    private BioProject bioProject1;
    private BioProject bioProject2;
    private BioProject bioProject3;
    private SubmissionBioSampleBean bioSampleBean1;
    private SubmissionContactBean contact2;
    private SubmissionBioSampleBean bioSampleBean2;
    private SubmissionBioSampleBean bioSampleBean3;
    private String uuID1;
    private String uuID2;
    private String uuID3;
    private String uuID4;
    private String uuID5;
    private String studyContact1;
    private String studyContact2;
    private String studyContact3;
    private String studyContact4;
    private SubmissionRequestBean testRequest;

    @BeforeMethod
    public void setUp() throws Exception {


        testJson = "{\n"
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
                   + "            \"site\": \"NCBI_PROTECTED\",\n"
                   + "            \"datatype\": \"Whole Genome\",\n"
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
                   + "            \"site\": \"NCBI_PROTECTED\",\n"
                   + "            \"datatype\": \"Whole Genome\",\n"
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
                   + "            \"site\": \"NCBI_PROTECTED\",\n"
                   + "            \"datatype\": \"Whole Genome\",\n"
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
                   + "            \"site\": \"NCBI_PROTECTED\",\n"
                   + "            \"datatype\": \"Whole Genome\",\n"
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
                   + "            \"site\": \"NCBI_PROTECTED\",\n"
                   + "            \"datatype\": \"Whole Genome\",\n"
                   + "            \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618749\"\n"
                   + "        }\n"
                   + "    ]\n"
                   + "}";
        bioProject1 = new BioProject("PRJNA75333");
        bioProject2 = new BioProject("BlahBlah");
        bioProject3 = new BioProject("BlahBlahBlah");
        bioSampleBean1 = new SubmissionBioSampleBean("S_2507","/some/funky/file.bam",
        new SubmissionContactBean("Jeff", "A", "Gentry", "jgentry@broadinstitute.org","617-555-9292","homer"));
        contact2 = new SubmissionContactBean("Jeffrey", "A", "G", "jgentry2@broadinstitute.org", "617-555-5555",
                "homer");
        bioSampleBean2 = new SubmissionBioSampleBean("S_2651","/some/funky/file2.bam",
                contact2);
        bioSampleBean3 = new SubmissionBioSampleBean("BlahBlahBlah","/some/funky/file2.bam",
                contact2);
        uuID1 = "7d835cc7-cd63-4cc6-9621-868155618745";
        uuID2 = "7d835cc7-cd63-4cc6-9621-868155618746";
        uuID3 = "7d835cc7-cd63-4cc6-9621-868155618747";
        uuID4 = "7d835cc7-cd63-4cc6-9621-868155618748";
        uuID5 = "7d835cc7-cd63-4cc6-9621-868155618749";
        studyContact1 = "jgentry";
        studyContact2 = "noSuchProject";
        studyContact3 = "noSuchSample";
        studyContact4 = "noSuchProjectOrSample";
        testRequest = new SubmissionRequestBean();

        SubmissionRepository defaultRepository=new SubmissionRepository(SubmissionRepository.DEFAULT_REPOSITORY_NAME,
                "NCBI Controlled Access (dbGaP) submissions");
        SubmissionLibraryDescriptor defaultType = ProductFamily.defaultSubmissionType();

        testRequest.setSubmissions(Arrays.asList(new SubmissionBean(uuID1, studyContact1, bioProject1, bioSampleBean1,
                defaultRepository, defaultType),
                new SubmissionBean(uuID2, studyContact1, bioProject1, bioSampleBean2, defaultRepository, defaultType),
                new SubmissionBean(uuID3, studyContact2, bioProject2, bioSampleBean2, defaultRepository, defaultType),
                new SubmissionBean(uuID4, studyContact3, bioProject1, bioSampleBean3, defaultRepository, defaultType),
                new SubmissionBean(uuID5, studyContact4, bioProject3, bioSampleBean3, defaultRepository, defaultType)));
    }

    public void testSerialize() throws Exception {
        StringWriter writer = MercuryStringUtils.serializeJsonBean(testRequest);
        String unReplaced = writer.toString();
        String actual = unReplaced.replaceAll("\\s+", "");
        Assert.assertEquals(actual, testJson.replaceAll("\\s+", ""));
    }

    public void testDeSerialize() throws Exception {
        SubmissionRequestBean requestBean = MercuryStringUtils.deSerializeJsonBean(testJson,
                SubmissionRequestBean.class);

        Assert.assertEquals(requestBean.getSubmissions().size(), testRequest.getSubmissions().size());
        Assert.assertEquals(requestBean.getSubmissions(), testRequest.getSubmissions());

        Assert.assertEquals(requestBean, testRequest);
    }

}
