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

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.SubmissionTrackerStub;
import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.testAccessionID;
import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.testFileName;
import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.testLibraryDescriptor;
import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.testRepository;
import static org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTrackerTest.testVersion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectSubmissionTrackerTest {
    public void testGetSubmissionTracker() {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        SubmissionTrackerStub tracker = new SubmissionTrackerStub(testAccessionID, testFileName, testVersion, testRepository, testLibraryDescriptor);
        SubmissionTrackerStub tracker2 = new SubmissionTrackerStub(testAccessionID + 2, testFileName + 2, testVersion + 2,testRepository, testLibraryDescriptor);

        testResearchProject.addSubmissionTracker(tracker, tracker2);

        SubmissionTracker resultTracker = testResearchProject.getSubmissionTracker(new SubmissionKey(testAccessionID, testFileName, testVersion, testRepository.getName(), testLibraryDescriptor.getName()));
        assertThat(tracker, equalTo(resultTracker));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetSubmissionTrackerTwoResults() {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        SubmissionTrackerStub tracker =
                new SubmissionTrackerStub(testAccessionID, testFileName, testVersion, testRepository, testLibraryDescriptor);
        testResearchProject.addSubmissionTracker(tracker, tracker);
        SubmissionTracker resultTracker = testResearchProject.getSubmissionTracker(new SubmissionKey(testAccessionID, testFileName, testVersion, testRepository.getName(), testLibraryDescriptor.getName()));
        assertThat(resultTracker, equalTo(resultTracker));
    }

    public void testGetSubmissionTrackerNoResults() {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        SubmissionTracker tracker =
                new SubmissionTracker(testAccessionID, testFileName, testVersion, testRepository, testLibraryDescriptor);
        testResearchProject.addSubmissionTracker(tracker);
        SubmissionTracker resultTracker = testResearchProject.getSubmissionTracker(new SubmissionKey("using", "phony", "arguments", "here is", "another"));
        assertThat(resultTracker, nullValue());
    }


}
