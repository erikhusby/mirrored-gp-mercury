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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor.RNA_SEQ;
import static org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor.WHOLE_EXOME;
import static org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor.WHOLE_GENOME;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@Test(groups = DATABASE_FREE)
public class SubmissionLibraryDescriptorTest {

    private static List<String>
        ALL_LIBRARIES = Arrays.asList(RNA_SEQ.getName(), WHOLE_EXOME.getName(), WHOLE_GENOME.getName());

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
    public void testLibraryDescriptorNameMatches(String libraryName, final String expectedResult) {
        final String normalizedLibraryName = SubmissionLibraryDescriptor.getNormalizedLibraryName(libraryName);

        assertThat(normalizedLibraryName, is(equalTo(expectedResult)));

        // Test that the normalized name does not equal any of the other names.
        Collection<String> librariesShouldNotMatch = Collections2.filter(ALL_LIBRARIES, new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String input) {
                return !input.equals(normalizedLibraryName);
            }
        });

        // verify that the size of filtered list is at least as large as the original list -1. This
        assertThat(librariesShouldNotMatch.size(), greaterThanOrEqualTo(ALL_LIBRARIES.size() - 1));
        assertThat(librariesShouldNotMatch, not(hasItem(normalizedLibraryName)));
    }

}
