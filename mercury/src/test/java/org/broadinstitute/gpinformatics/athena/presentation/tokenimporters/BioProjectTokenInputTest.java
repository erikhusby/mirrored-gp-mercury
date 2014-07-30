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

package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjectList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionConfig;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BioProjectTokenInputTest {
    public static final String TEST_ACCESSION_ID = "PRJNA74863";
    public static final String TEST_ALIAS = "phs000298";
    public static final String TEST_PROJECT_NAME = "ARRA Autism Sequencing Collaboration";


    private final static SubmissionConfig submissionConfig=SubmissionConfig.produce(Deployment.DEV);
    private BioProjectList bioProjectList;
    private BioProjectTokenInput bioProjectTokenInput;
    private BioProject bioProject=new BioProject(TEST_ACCESSION_ID, TEST_ALIAS, TEST_PROJECT_NAME);

    @BeforeMethod
    public void setUp() throws Exception {
        bioProjectList = new BioProjectList(new SubmissionsServiceImpl(submissionConfig));
        bioProjectTokenInput = new BioProjectTokenInput(bioProjectList);
    }

    public void testGetTokenId() throws Exception {
        String tokenId = bioProjectTokenInput.getTokenId(bioProject);
        assertThat(tokenId, equalTo(bioProject.getAccession()));
    }

    public void testGetTokenName() throws Exception {
        String tokenName = bioProjectTokenInput.getTokenName(bioProject);
        assertThat(tokenName, equalTo(bioProject.displayName()));
    }

    public void testFormatMessage() throws Exception {
        String message = bioProjectTokenInput.formatMessage("foo {0}", bioProject);
        assertThat(message, equalTo("foo " + bioProject.displayName()));
    }

    public void testGetById() throws Exception {
        BioProject bioProjectById = bioProjectTokenInput.getById(TEST_ACCESSION_ID);
        assertThat(bioProjectById, equalTo(bioProject));
    }
}
