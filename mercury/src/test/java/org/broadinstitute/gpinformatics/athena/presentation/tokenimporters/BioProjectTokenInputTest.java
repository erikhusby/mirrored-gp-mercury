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

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjectList;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjectTest;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionConfig;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.json.JSONException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BioProjectTokenInputTest {
    private final static SubmissionConfig submissionConfig = SubmissionConfig.produce(Deployment.DEV);
    public static final String NO_JSON_RESULT = "[]";
    private BioProjectList bioProjectList;
    private BioProjectTokenInput bioProjectTokenInput;
    private BioProject bioProject = new BioProject(BioProjectTest.TEST_ACCESSION_ID, BioProjectTest.TEST_ALIAS,
            BioProjectTest.TEST_PROJECT_NAME);

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

        assertThat(tokenName, containsString(bioProject.getProjectName()));
    }

    public void testFormatMessage() throws Exception {
        String messageFormat = "<div class=\"ac-dropdown-text\">{0}</div><div class=\"ac-dropdown-subtext\">{1}</div>";
        String message = bioProjectTokenInput.formatMessage(messageFormat, bioProject);
        String expected = String.format(
                "<div class=\"ac-dropdown-text\">%s</div><div class=\"ac-dropdown-subtext\">accession: %s alias: %s</div>",
                bioProject.getProjectName(), bioProject.getAccession(), bioProject.getAlias());
        assertThat(message, equalTo(expected));
    }

    public void testGetById() throws Exception {
        BioProject bioProjectById = bioProjectTokenInput.getById(BioProjectTest.TEST_ACCESSION_ID);
        assertThat(bioProjectById, equalTo(bioProject));
    }

    public void testJsonNoResult() throws JSONException {
        String searchString = StringUtils.reverse(BioProjectTest.TEST_ACCESSION_ID);
        String result = bioProjectTokenInput.getJsonString(searchString);
        assertThat(result, equalTo(NO_JSON_RESULT));
    }

    public void testJsonWithResult() throws JSONException {
        String searchString = BioProjectTest.TEST_ACCESSION_ID.substring(2, 7);
        String result = bioProjectTokenInput.getJsonString(searchString);
        assertThat(result, is(not(isEmptyOrNullString())));
    }

}
