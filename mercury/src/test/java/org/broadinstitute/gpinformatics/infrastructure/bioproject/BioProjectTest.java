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

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class BioProjectTest {
    public static final String TEST_ACCESSION_ID = "PRJNA74863";
    public static final String TEST_ALIAS = "phs000298";
    public static final String TEST_PROJECT_NAME = "ARRA Autism Sequencing Collaboration";

    private BioProject createBioProject() {
        return new BioProject(TEST_ACCESSION_ID, TEST_ALIAS, TEST_PROJECT_NAME);
    }

    public void testCreate() {
        BioProject bioProject = createBioProject();
        assertThat(bioProject.getAccession(), is(equalTo(TEST_ACCESSION_ID)));
        assertThat(bioProject.getAlias(), is(equalTo(TEST_ALIAS)));
        assertThat(bioProject.getProjectName(), is(equalTo(TEST_PROJECT_NAME)));
    }

    public void testEquals() throws Exception {
        BioProject bioProject1 = createBioProject();
        BioProject bioProject2 = createBioProject();

        assertThat(bioProject1, is(equalTo(bioProject2)));
        assertThat(bioProject2, is(equalTo(bioProject1)));
    }

    public void testEqualsJustAccessionConstructor() {
        BioProject bioProject1 = new BioProject(TEST_ACCESSION_ID);
        BioProject bioProject2 = new BioProject(TEST_ACCESSION_ID);
        assertThat(bioProject1, is(equalTo(bioProject2)));
        assertThat(bioProject2, is(equalTo(bioProject1)));
    }
}
