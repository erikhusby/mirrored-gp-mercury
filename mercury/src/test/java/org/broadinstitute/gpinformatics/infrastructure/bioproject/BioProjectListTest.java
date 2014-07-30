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

package org.broadinstitute.gpinformatics.infrastructure.bioproject;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionConfig;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BioProjectListTest {
    public static final String TEST_ACCESSION_ID = "PRJNA75555";
    private SubmissionsServiceImpl submissionsService =
            new SubmissionsServiceImpl(SubmissionConfig.produce(Deployment.DEV));
    private BioProjectList bioProjectList = new BioProjectList(submissionsService);

    public void testGetAllUsers(){
        assertThat(bioProjectList.getBioProjects(), not(Matchers.emptyCollectionOf(BioProject.class)));
    }

    public void testGetByAccession(){
        BioProject bioProject  = bioProjectList.getBioProject(TEST_ACCESSION_ID);
        assertThat(bioProject.getAccession(), Matchers.equalTo(TEST_ACCESSION_ID));
    }
}
