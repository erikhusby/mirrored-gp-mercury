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

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor.RNA_SEQ;
import static org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor.WHOLE_EXOME;
import static org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor.WHOLE_GENOME;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@Test(groups = DATABASE_FREE)
public class SubmissionLibraryDescriptorTest {

    @DataProvider(name = "descriptorNamesProvider")
    public static Iterator<Object[]> descriptorNames() {
        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{"Exome", WHOLE_EXOME.getName()});
        testCases.add(new Object[]{"Whole Exome", WHOLE_EXOME.getName()});
        testCases.add(new Object[]{"WGS", WHOLE_GENOME.getName()});
        testCases.add(new Object[]{"Genome", WHOLE_GENOME.getName()});
        testCases.add(new Object[]{"Whole Genome", WHOLE_GENOME.getName()});
        testCases.add(new Object[]{"RNA", RNA_SEQ.getName()});
        testCases.add(new Object[]{"RNA Seq", RNA_SEQ.getName()});
        testCases.add(new Object[]{"N/A", "N/A"});
        testCases.add(new Object[]{"Gen", "Gen"});
        testCases.add(new Object[]{"", null});
        testCases.add(new Object[]{" ", null});
        testCases.add(new Object[]{null, null});
        return testCases.iterator();
    }

    @Test(dataProvider = "descriptorNamesProvider")
    public void testLibraryDescriptorNameMatches(String libraryName, String expectedResult) {
        assertThat(SubmissionLibraryDescriptor.getNormalizedLibraryName(libraryName), is(equalTo(expectedResult)));
    }

}
