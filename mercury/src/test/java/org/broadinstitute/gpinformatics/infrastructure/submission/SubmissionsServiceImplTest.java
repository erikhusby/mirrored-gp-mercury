/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.hamcrest.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class SubmissionsServiceImplTest {

    private static int sequenceNumber = 1;

    public static final String BIO_PROJECT_ACCESSION_ID = "PRJNA75723";
    public static final String SAMPLE1_ID = "4304714212_K";
    public static final String SAMPLE2_ID = "4377315018_E";

    private SubmissionsService submissionsService;

    private BioProject bioProject;
    private SubmissionContactBean contactBean;

    @BeforeMethod
    public void setUp() throws Exception {
        bioProject = new BioProject(BIO_PROJECT_ACCESSION_ID);
        contactBean =
                new SubmissionContactBean("Jeff", "A", "Gentry", "jgentry@broadinstitute.org", "617-555-9292", "homer");
        submissionsService = new SubmissionsServiceImpl(SubmissionConfig.produce(Deployment.DEV));
    }

    public void testGetAllBioProjects() throws Exception {
        Collection<BioProject> allBioProjects = submissionsService.getAllBioProjects();
        assertThat(allBioProjects, is(not(Matchers.emptyCollectionOf(BioProject.class))));
    }

    public void testGetSubmissionSamples() throws Exception {
        String[] expectedSampleNames = {SAMPLE1_ID, SAMPLE2_ID, "4304714040_C"};

        Collection<String> submissionSamples = submissionsService.getSubmissionSamples(bioProject);
        int minimumExpectedSizeOfResult = 300;
        assertThat(submissionSamples.size(), greaterThan(minimumExpectedSizeOfResult));
        assertThat(submissionSamples, hasItems(expectedSampleNames));
    }

    public void testSubmit() {
        SubmissionBean submissionBean1 = new SubmissionBean(getTestUUID(), "jgentry",
                bioProject, new SubmissionBioSampleBean(SAMPLE1_ID, "/some/funky/file.bam", contactBean));
        SubmissionBean submissionBean2 = new SubmissionBean(getTestUUID(), "jgentry",
                bioProject, new SubmissionBioSampleBean(SAMPLE2_ID, "/some/funky/file2.bam", contactBean));
        SubmissionRequestBean submissionRequestBean =
                new SubmissionRequestBean(Arrays.asList(submissionBean1, submissionBean2));
        Collection<SubmissionStatusDetailBean>
                submissionResult = submissionsService.postSubmissions(submissionRequestBean);
        assertThat(submissionResult.size(), is(2));
        for (SubmissionStatusDetailBean submissionStatusDetailBean : submissionResult) {
            assertThat(submissionStatusDetailBean.getStatus(),
                    is(SubmissionStatusDetailBean.Status.READY_FOR_SUBMISSION.getLabel()));
            assertThat(submissionStatusDetailBean.getErrors(), emptyCollectionOf(String.class));
            assertThat(DateUtils.convertDateTimeToString(submissionStatusDetailBean.getLastStatusUpdate()),
                    notNullValue());
        }
    }

    public void testGetSubmissionStatusAlwaysReturnsSomething() {
        String[] testUUIDs = {"you wont find me", "yeah, and me neither"};
        Collection<SubmissionStatusDetailBean> submissionStatus = submissionsService.getSubmissionStatus(testUUIDs);
        assertThat(submissionStatus.size(), is(testUUIDs.length));
        for (SubmissionStatusDetailBean submissionStatusDetailBean : submissionStatus) {
            assertThat(testUUIDs, hasItemInArray(submissionStatusDetailBean.getUuid()));
            assertThat(submissionStatusDetailBean.getStatus(), nullValue());
            assertThat(submissionStatusDetailBean.getErrors(), emptyCollectionOf(String.class));
            assertThat(submissionStatusDetailBean.getLastStatusUpdate(), nullValue());
        }
    }

    public void testGetSubmissionStatusReadyForSubmission() {
        String testUUID = getTestUUID();

        SubmissionBean submissionBean = new SubmissionBean(testUUID, "jgentry", bioProject,
                new SubmissionBioSampleBean(SAMPLE1_ID, "/some/funky/file.bam", contactBean));
        SubmissionRequestBean submissionRequestBean = new SubmissionRequestBean(Arrays.asList(submissionBean));
        Collection<SubmissionStatusDetailBean> submissionResult =
                submissionsService.postSubmissions(submissionRequestBean);
        assertThat(submissionResult.size(), is(1));
        SubmissionStatusDetailBean submissionStatusDetailBean = submissionResult.iterator().next();
        assertThat(submissionStatusDetailBean.getStatus(),
                is(SubmissionStatusDetailBean.Status.READY_FOR_SUBMISSION.getLabel()));
        assertThat(submissionStatusDetailBean.getErrors(), emptyCollectionOf(String.class));
        assertThat(DateUtils.convertDateTimeToString(submissionStatusDetailBean.getLastStatusUpdate()),
                notNullValue());

        submissionResult = submissionsService.getSubmissionStatus(testUUID);
        assertThat(submissionResult.size(), is(1));
        submissionStatusDetailBean = submissionResult.iterator().next();
        assertThat(submissionStatusDetailBean.getStatus(),
                is(SubmissionStatusDetailBean.Status.READY_FOR_SUBMISSION.getLabel()));
        assertThat(submissionStatusDetailBean.getErrors(), emptyCollectionOf(String.class));
        assertThat(DateUtils.convertDateTimeToString(submissionStatusDetailBean.getLastStatusUpdate()),
                notNullValue());
    }

    public void testSubmissionStatusReportsEpsilon9Error() {
        String[] uuids = new String[]{"MERCURY_SUB_20160120951", "MERCURY_SUB_201602092951", "MERCURY_SUB_201602092952",
                "MERCURY_SUB_201602092953", "MERCURY_SUB_201602112984", "MERCURY_SUB_201602112985",
                "MERCURY_SUB_201602112986", "MERCURY_SUB_201602112987", "MERCURY_SUB_201602112988",
                "MERCURY_SUB_201602112989", "MERCURY_SUB_201602112990", "MERCURY_SUB_201602112991",
                "MERCURY_SUB_201602112992", "MERCURY_SUB_201602112993", "MERCURY_SUB_201602112994",
                "MERCURY_SUB_201602112995", "MERCURY_SUB_201602112996", "MERCURY_SUB_201602112997",
                "MERCURY_SUB_201602112998", "MERCURY_SUB_201602112999", "MERCURY_SUB_201602113000",
                "MERCURY_SUB_201602113951", "MERCURY_SUB_201602113952", "MERCURY_SUB_201602113953",
                "MERCURY_SUB_201602113954", "MERCURY_SUB_201602113955", "MERCURY_SUB_201602113956",
                "MERCURY_SUB_201602113957", "MERCURY_SUB_201602113958", "MERCURY_SUB_201602113959",
                "MERCURY_SUB_201602113960", "MERCURY_SUB_201602113961", "MERCURY_SUB_201602113962",
                "MERCURY_SUB_201602113963", "MERCURY_SUB_20160120952", "MERCURY_SUB_201512171",
                "MERCURY_SUB_201602102954", "MERCURY_SUB_201602102955", "MERCURY_SUB_201602102956",
                "MERCURY_SUB_201602102957", "MERCURY_SUB_201602102958", "MERCURY_SUB_201602102959",
                "MERCURY_SUB_201602102960", "MERCURY_SUB_201602102961", "MERCURY_SUB_201602102962",
                "MERCURY_SUB_201602102963", "MERCURY_SUB_201602102964", "MERCURY_SUB_201602102965",
                "MERCURY_SUB_201602102966", "MERCURY_SUB_201602102967", "MERCURY_SUB_201602102968",
                "MERCURY_SUB_201602102969", "MERCURY_SUB_201602102970", "MERCURY_SUB_201602102971",
                "MERCURY_SUB_201602102972", "MERCURY_SUB_201602102973", "MERCURY_SUB_201602102974",
                "MERCURY_SUB_201602102975", "MERCURY_SUB_201602102976", "MERCURY_SUB_201602102977",
                "MERCURY_SUB_201602102978", "MERCURY_SUB_201602102979", "MERCURY_SUB_201602102980",
                "MERCURY_SUB_201602102981", "MERCURY_SUB_201602102982", "MERCURY_SUB_201602102983",
                "MERCURY_SUB_201601221951", "MERCURY_SUB_201601221952"};
        Exception caught = null;
        try {
            submissionsService.getSubmissionStatus(uuids);
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught.getMessage(), containsString("URI length exceeds the configured limit of 2048 characters"));
    }

    private static synchronized String getTestUUID() {
        return String.format("MERCURY_TEST_SUB_%d_%04d", System.currentTimeMillis(), sequenceNumber++);
    }
}
