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

    public SubmissionsService submissionsService = new SubmissionsServiceImpl(SubmissionConfig.produce(Deployment.DEV));

    public void testGetAllBioProjects() throws Exception {
        Collection<BioProject> allBioProjects = submissionsService.getAllBioProjects();
        assertThat(allBioProjects, is(not(Matchers.emptyCollectionOf(BioProject.class))));
    }

    public void testGetSubmissionSamples() throws Exception {
        BioProject bioProject = new BioProject("PRJNA75723");
        String[] expectedSampleNames = {"4304714212_K", "4377315018_E", "4304714040_C"};

        Collection<String> submissionSamples = submissionsService.getSubmissionSamples(bioProject);
        int minimuimExpectedSizeOfResult = 300;
        assertThat(submissionSamples.size(), greaterThan(minimuimExpectedSizeOfResult));
        assertThat(submissionSamples, hasItems(expectedSampleNames));
    }

    @Test(enabled = false)
    public void testSubmit() {
        SubmissionContactBean jeff = new SubmissionContactBean("Jeff", "A", "Gentry", "jgentry@broadinstitute.org", "617-555-9292","homer");
        BioProject prjna75333 = new BioProject("PRJNA75333");

        SubmissionBean submissionBean1 = new SubmissionBean("7d835cc7-cd63-4cc6-9621-868155618745", "jgentry",
                prjna75333, new SubmissionBioSampleBean("S_2507", "/some/funky/file.bam", jeff));
        SubmissionBean submissionBean2 = new SubmissionBean("7d835cc7-cd63-4cc6-9621-868155618746", "jgentry",
                prjna75333, new SubmissionBioSampleBean("S_2651", "/some/funky/file2.bam", jeff));
        SubmissionRequestBean submissionRequestBean = new SubmissionRequestBean(Arrays.asList(submissionBean1, submissionBean2));
        Collection<SubmissionStatusDetailBean>
                submissionResult = submissionsService.postSubmissions(submissionRequestBean);
        assertThat(false, is(true));

    }

    public void testGetSubmissionStatusAlwaysReturnsSomething() {
//        String[] testUUIDs = {"7d835cc7-cd63-4cc6-9621-868155618746", "7d835cc7-cd63-4cc6-9621-868155618745"};
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
        String testUUID= "7d835cc7-cd63-4cc6-9621-868155618746";

        Collection<SubmissionStatusDetailBean> submissionStatus = submissionsService.getSubmissionStatus(testUUID);
        assertThat(submissionStatus.size(), is(1));
        for (SubmissionStatusDetailBean submissionStatusDetailBean : submissionStatus) {
            assertThat(submissionStatusDetailBean.getStatus(), is(SubmissionStatusDetailBean.Status.READY_FOR_SUBMISSION.getLabel()));
            assertThat(submissionStatusDetailBean.getErrors(), emptyCollectionOf(String.class));
            assertThat(DateUtils.convertDateTimeToString(submissionStatusDetailBean.getLastStatusUpdate()), notNullValue());
        }
    }

}
