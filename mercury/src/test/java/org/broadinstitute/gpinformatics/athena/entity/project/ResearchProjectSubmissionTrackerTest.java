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

import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectSubmissionTrackerTest {
    public void testGetSubmissionTracker() {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        SubmissionTrackerStub tracker =
                new SubmissionTrackerStub(testProjectId, testAccessionID, testVersion, testFileType);
        SubmissionTrackerStub tracker2 =
                new SubmissionTrackerStub(testProjectId, testAccessionID + 2, testVersion + 2, BassFileType.PICARD);
        testResearchProject.addSubmissionTracker(tracker, tracker2);
        SubmissionTracker resultTracker = testResearchProject.getSubmissionTracker(new SubmissionTuple(testProjectId,
                testAccessionID, testVersion, testFileType));
        assertThat(tracker, equalTo(resultTracker));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetSubmissionTrackerTwoResults() {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        SubmissionTrackerStub tracker =
                new SubmissionTrackerStub(testProjectId, testAccessionID, testVersion, testFileType);
        testResearchProject.addSubmissionTracker(tracker, tracker);
        SubmissionTracker resultTracker = testResearchProject.getSubmissionTracker(new SubmissionTuple(testProjectId,
                testAccessionID, testVersion, testFileType));
        assertThat(resultTracker, equalTo(resultTracker));
    }

    public void testGetSubmissionTrackerNoResults() {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        SubmissionTracker tracker = new SubmissionTracker(testProjectId, testAccessionID, testVersion, testFileType);
        testResearchProject.addSubmissionTracker(tracker);
        SubmissionTracker resultTracker = testResearchProject
                .getSubmissionTracker(new SubmissionTuple("other", "using", "arguments", BassFileType.PICARD));
        assertThat(resultTracker, nullValue());
    }


}
