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

import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

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
        testResearchProject.addProductOrder(ProductOrderTestFactory.createDummyProductOrder());

        SubmissionTrackerStub tracker = new SubmissionTrackerStub(testAccessionID, testFileName, testVersion, testRepository, testLibraryDescriptor);
        SubmissionTrackerStub tracker2 = new SubmissionTrackerStub(testAccessionID + 2, testFileName + 2, testVersion + 2,testRepository, testLibraryDescriptor);

        testResearchProject.addSubmissionTracker(tracker, tracker2);
        BassDTO bassDTO = getBassDTO(testAccessionID, testFileName, testVersion);
        SubmissionTracker resultTracker = testResearchProject.getSubmissionTracker(bassDTO);
        assertThat(tracker, equalTo(resultTracker));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetSubmissionTrackerThrowsExceptionWhenMoreThenOneResult() {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        testResearchProject.addProductOrder(ProductOrderTestFactory.createDummyProductOrder());

        SubmissionTrackerStub tracker = new SubmissionTrackerStub(testAccessionID, testFileName, testVersion, testRepository, testLibraryDescriptor);

        testResearchProject.addSubmissionTracker(tracker, tracker);
        BassDTO bassDTO = getBassDTO(testAccessionID, testFileName, testVersion);
        testResearchProject.getSubmissionTracker(bassDTO);
    }

    public void testGetSubmissionTrackerReturnsNullWhenThereAreNone() {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        testResearchProject.addSubmissionTracker();
        BassDTO bassDTO = getBassDTO(testAccessionID, testFileName, testVersion);
        SubmissionTracker resultTracker = testResearchProject.getSubmissionTracker(bassDTO);
        assertThat(resultTracker, nullValue());
    }

    private BassDTO getBassDTO(String testAccessionID, String testFileName, String testVersion) {
        Map<BassDTO.BassResultColumn, String> bassMap = new HashMap<>();
        bassMap.put(BassDTO.BassResultColumn.sample, testAccessionID);
        bassMap.put(BassDTO.BassResultColumn.path, testFileName);
        bassMap.put(BassDTO.BassResultColumn.version, testVersion);
        return new BassDTO(bassMap);
    }

    public void testGetSubmissionTrackerNoResults() {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        SubmissionTracker tracker =
                new SubmissionTracker(testAccessionID, testFileName, testVersion, testRepository, testLibraryDescriptor);
        testResearchProject.addSubmissionTracker(tracker);
        Map<BassDTO.BassResultColumn, String> bassMap = new HashMap<>();
        bassMap.put(BassDTO.BassResultColumn.sample, "using" );
        bassMap.put(BassDTO.BassResultColumn.path, "phony");
        bassMap.put(BassDTO.BassResultColumn.version, "9");
        BassDTO bassDTO = new BassDTO(bassMap);
        SubmissionTracker resultTracker = testResearchProject.getSubmissionTracker(bassDTO);
        assertThat(resultTracker, nullValue());
    }


}
