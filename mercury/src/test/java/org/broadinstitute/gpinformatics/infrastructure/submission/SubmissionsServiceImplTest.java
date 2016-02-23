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

    private static synchronized String getTestUUID() {
        return String.format("MERCURY_TEST_SUB_%d_%04d", System.currentTimeMillis(), sequenceNumber++);
    }
}
