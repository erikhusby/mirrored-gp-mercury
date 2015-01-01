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

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionConfig;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BioProjectListTest {

    private SubmissionsServiceImpl submissionsService =
            new SubmissionsServiceImpl(SubmissionConfig.produce(Deployment.DEV));
    private BioProjectList bioProjectList = new BioProjectList(submissionsService);

    public void testGetAllUsers(){
        assertThat(bioProjectList.getBioProjects(), not(Matchers.emptyCollectionOf(BioProject.class)));
    }

    public void testGetByAccession(){
        BioProject bioProject  = TestUtils.getFirst(bioProjectList.getBioProjects());
        assertThat(bioProject, Matchers.notNullValue());
        BioProject bioProjectById = bioProjectList.getBioProject(bioProject.getAccession());
        assertThat(bioProjectById, Matchers.equalTo(bioProject));
    }


    public void testSearchNoResult() {
        String searchString = StringUtils.reverse(BioProjectTest.TEST_ACCESSION_ID);
        Collection<BioProject> resultList = bioProjectList.search(searchString);
        assertThat(resultList, emptyCollectionOf(BioProject.class));
    }

    public void testSearchWithAccession() {
        String searchString =BioProjectTest.TEST_ACCESSION_ID.substring(2, 7);
        Collection<BioProject> resultList = bioProjectList.search(searchString);
        assertThat(resultList, not(emptyCollectionOf(BioProject.class)));
        assertThat(resultList.size(), is(greaterThan(30)));
    }
    public void testSearchWithAlias() {
        String searchString =BioProjectTest.TEST_ALIAS.substring(2, 7);
        Collection<BioProject> resultList = bioProjectList.search(searchString);
        assertThat(resultList, not(emptyCollectionOf(BioProject.class)));
        assertThat(resultList.size(), is(greaterThan(30)));
    }

    public void testSearchWithProjectName() {
        int subStringStart = 5;
        subStringStart = subStringStart < BioProjectTest.TEST_PROJECT_NAME.length() ? subStringStart :
                BioProjectTest.TEST_PROJECT_NAME.length();
        int subStringEnd = 10;
        subStringEnd = BioProjectTest.TEST_PROJECT_NAME.length() > subStringEnd ? subStringEnd :
                BioProjectTest.TEST_PROJECT_NAME.length();
        String searchString = BioProjectTest.TEST_PROJECT_NAME.substring(subStringStart, subStringEnd);
        Collection<BioProject> resultList = bioProjectList.search(searchString);
        assertThat(resultList, not(emptyCollectionOf(BioProject.class)));
        assertThat(resultList.size(), is(greaterThan(5)));
    }

}
