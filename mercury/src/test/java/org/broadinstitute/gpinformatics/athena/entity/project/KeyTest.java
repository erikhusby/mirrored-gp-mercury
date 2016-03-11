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
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.DATABASE_FREE)
public class KeyTest {
    public void testNullSample() {
        SubmissionTracker.Key tuple1 = null;
        SubmissionTracker.Key
                tuple2 = new SubmissionTracker.Key("b", "b", "c","NCBI_PROTECTED", "Whole Genome");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testSampleNotEquals() {
        SubmissionTracker.Key
                tuple1 = new SubmissionTracker.Key("a", "b", "c","NCBI_PROTECTED", "Whole Genome");
        SubmissionTracker.Key
                tuple2 = new SubmissionTracker.Key("b", "b", "c","NCBI_PROTECTED", "Whole Genome");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testRepositoryNameDiffersNotEquals() {
        SubmissionTracker.Key
                tuple1 = new SubmissionTracker.Key("b", "b", "c", "NCBI_PROTECTED", "Whole Genome");
        SubmissionTracker.Key
                repositoryNameDiffers = new SubmissionTracker.Key("b", "b", "c", "NCBI_PRIVATE", "Whole Genome");
        assertThat(tuple1, not(equalTo(repositoryNameDiffers)));
        assertThat(repositoryNameDiffers, not(equalTo(tuple1)));
    }

    public void testLibraryNameDiffersNotEquals() {
        SubmissionTracker.Key
                tuple1 = new SubmissionTracker.Key("b", "b", "c", "NCBI_PROTECTED", "Whole Genome");
        SubmissionTracker.Key
                libraryNameDiffers = new SubmissionTracker.Key("b", "b", "c", "NCBI_PROTECTED", "Partially Hydrogenated Genome");
        assertThat(tuple1, not(equalTo(libraryNameDiffers)));
        assertThat(libraryNameDiffers, not(equalTo(tuple1)));
    }

    public void testEquals() {
        SubmissionTracker.Key
                tuple1 = new SubmissionTracker.Key("b", "b", "c","NCBI_PROTECTED", "Whole Genome");
        SubmissionTracker.Key
                tuple2 = new SubmissionTracker.Key("b", "b", "c","NCBI_PROTECTED", "Whole Genome");
        assertThat(tuple1, equalTo(tuple2));
        assertThat(tuple2, equalTo(tuple1));
    }

    public void testNullEquals() {
        SubmissionTracker.Key tuple1 = null;
        SubmissionTracker.Key tuple2 = null;
        assertThat(tuple1, equalTo(tuple2));
        assertThat(tuple2, equalTo(tuple1));
    }

    public void testFileNotEquals() {
        SubmissionTracker.Key
                tuple1 = new SubmissionTracker.Key("b", "b", "c","NCBI_PROTECTED", "Whole Genome");
        SubmissionTracker.Key
                tuple2 = new SubmissionTracker.Key("a", "c", "c","NCBI_PROTECTED", "Whole Genome");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testVersionNotEquals() {
        SubmissionTracker.Key
                tuple1 = new SubmissionTracker.Key("b", "b", "c","NCBI_PROTECTED", "Whole Genome");
        SubmissionTracker.Key
                tuple2 = new SubmissionTracker.Key("a", "b", "d","NCBI_PROTECTED", "Whole Genome");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullSampleNotEquals() {
        SubmissionTracker.Key
                tuple1 = new SubmissionTracker.Key(null, "b", "c","NCBI_PROTECTED", "Whole Genome");
        SubmissionTracker.Key
                tuple2 = new SubmissionTracker.Key("a", "b", "d","NCBI_PROTECTED", "Whole Genome");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullFileNotEquals() {
        SubmissionTracker.Key
                tuple1 = new SubmissionTracker.Key("a", null, "c","NCBI_PROTECTED", "Whole Genome");
        SubmissionTracker.Key
                tuple2 = new SubmissionTracker.Key("a", "b", "d","NCBI_PROTECTED", "Whole Genome");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullVersionNotEquals() {
        SubmissionTracker.Key
                tuple1 = new SubmissionTracker.Key("a", "b", null,"NCBI_PROTECTED", "Whole Genome");
        SubmissionTracker.Key
                tuple2 = new SubmissionTracker.Key("a", "b", "d","NCBI_PROTECTED", "Whole Genome");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

   public void testSiteNotEquals(){
       SubmissionTracker.Key
               tuple1 = new SubmissionTracker.Key("a", "b", "d","NCBI_PROTECTED", "Whole Genome");
       SubmissionTracker.Key
               tuple2 = new SubmissionTracker.Key("a", "b", "d","NCBI_RESTRICTED", "Whole Genome");
       assertThat(tuple1, not(equalTo(tuple2)));
       assertThat(tuple2, not(equalTo(tuple1)));

   }
    public void testNullSiteNotEqual(){
        SubmissionTracker.Key
                tuple1 = new SubmissionTracker.Key("a", "b", "d","NCBI_PROTECTED", "Whole Genome");
        SubmissionTracker.Key tuple2 = new SubmissionTracker.Key("a", "b", "d",null, "Whole Genome");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));

    }
   public void testLibraryNotEquals(){
       SubmissionTracker.Key
               tuple1 = new SubmissionTracker.Key("a", "b", "d","NCBI_PROTECTED", "Whole Genome");
       SubmissionTracker.Key
               tuple2 = new SubmissionTracker.Key("a", "b", "d","NCBI_RESTRICTED", "Partially Hydrogenated Genome");
       assertThat(tuple1, not(equalTo(tuple2)));
       assertThat(tuple2, not(equalTo(tuple1)));

   }
    public void testLibraryNullNotEqual(){
        SubmissionTracker.Key
                tuple1 = new SubmissionTracker.Key("a", "b", "d","NCBI_PROTECTED", "Whole Genome");
        SubmissionTracker.Key
                tuple2 = new SubmissionTracker.Key("a", "b", "d","NCBI_PROTECTED", null);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }
}
