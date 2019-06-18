/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.submission.FileType;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.mockito.Mockito;

public class SubmissionTrackerTestFactory {
    public static SubmissionTracker getTracker(String uuid, String sample, String location, int version,
                                               String library) {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject("RP-1234");
        SubmissionTracker tracker =
            new SubmissionTracker(testResearchProject.getJiraTicketKey(), sample, Integer.toString(version),
                FileType.BAM, location, library);
        SubmissionTracker submissionTracker = Mockito.spy(tracker);
        Mockito.when(submissionTracker.createSubmissionIdentifier()).thenReturn(uuid);
        testResearchProject.addSubmissionTracker(submissionTracker);
        return submissionTracker;
    }
}
