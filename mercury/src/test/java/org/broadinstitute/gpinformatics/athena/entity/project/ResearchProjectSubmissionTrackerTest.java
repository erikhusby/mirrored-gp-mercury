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

package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.infrastructure.submission.FileType;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionBioSampleBean;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.SubmissionTrackerStub;
import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.TEST_ACCESSION_ID;
import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.TEST_DATA_TYPE;
import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.TEST_FILE_TYPE;
import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.TEST_PROJECT_ID;
import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.TEST_PROCESSING_LOCATION;
import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.TEST_VERSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectSubmissionTrackerTest {
    private static final String EXOME = "Exome";

    public void testGetSubmissionTracker() {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        SubmissionTrackerStub tracker =
                new SubmissionTrackerStub(TEST_PROJECT_ID, TEST_ACCESSION_ID, TEST_VERSION, TEST_FILE_TYPE);
        SubmissionTrackerStub tracker2 =
                new SubmissionTrackerStub(TEST_PROJECT_ID, TEST_ACCESSION_ID + 2, TEST_VERSION + 2, FileType.PICARD);
        testResearchProject.addSubmissionTracker(tracker, tracker2);
        SubmissionTracker resultTracker = testResearchProject.getSubmissionTracker(new SubmissionTuple(TEST_PROJECT_ID,
            testResearchProject.getJiraTicketKey(), TEST_ACCESSION_ID, TEST_VERSION, SubmissionBioSampleBean.ON_PREM, EXOME));
        assertThat(tracker, equalTo(resultTracker));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetSubmissionTrackerTwoResults() {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        SubmissionTrackerStub tracker =
                new SubmissionTrackerStub(TEST_PROJECT_ID, TEST_ACCESSION_ID, TEST_VERSION, TEST_FILE_TYPE);
        testResearchProject.addSubmissionTracker(tracker, tracker);
        SubmissionTracker resultTracker = testResearchProject.getSubmissionTracker(new SubmissionTuple(TEST_PROJECT_ID,
            testResearchProject.getJiraTicketKey(), TEST_ACCESSION_ID, TEST_VERSION, SubmissionBioSampleBean.ON_PREM, EXOME));
        assertThat(resultTracker, equalTo(resultTracker));
    }

    public void testGetSubmissionTrackerNoResults() {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        SubmissionTracker tracker = new SubmissionTracker(TEST_PROJECT_ID, TEST_ACCESSION_ID, TEST_VERSION,
            TEST_FILE_TYPE, TEST_PROCESSING_LOCATION, TEST_DATA_TYPE);
        testResearchProject.addSubmissionTracker(tracker);
        SubmissionTracker resultTracker = testResearchProject
                .getSubmissionTracker(new SubmissionTuple("other", testResearchProject.getJiraTicketKey(),
                    "using", "arguments", SubmissionBioSampleBean.GCP, EXOME));
        assertThat(resultTracker, nullValue());
    }


}
