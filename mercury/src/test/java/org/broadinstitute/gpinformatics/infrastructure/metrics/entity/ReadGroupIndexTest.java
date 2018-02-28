/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2017 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.metrics.entity;

import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

@Test(groups = DATABASE_FREE)
public class ReadGroupIndexTest {
    private String[] analysisTypes = new String[]{"AssemblyWithReference", "cDNA", "Resequencing"};

    String[] libraryTypes =
        new String[]{"cDNAShotgunReadTwoSense", "cDNAShotgunStrandAgnostic", "WholeGenomeShotgun", "HybridSelection"};

    @DataProvider(name = "analysisTypeScenarios")
    public Iterator<Object[]> Name() {
        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{"AssemblyWithoutReference", "cDNAShotgunReadTwoSense", null});
        testCases.add(new Object[]{"AssemblyWithoutReference", "cDNAShotgunStrandAgnostic", null});
        testCases.add(new Object[]{"AssemblyWithoutReference", "WholeGenomeShotgun", SubmissionLibraryDescriptor.WHOLE_GENOME});
        testCases.add(new Object[]{"AssemblyWithoutReference", "HybridSelection", SubmissionLibraryDescriptor.WHOLE_EXOME});

        testCases.add(new Object[]{"cDNA", "cDNAShotgunReadTwoSense", SubmissionLibraryDescriptor.RNA_SEQ});
        testCases.add(new Object[]{"cDNA", "cDNAShotgunStrandAgnostic", SubmissionLibraryDescriptor.RNA_SEQ});
        testCases.add(new Object[]{"cDNA", "WholeGenomeShotgun", SubmissionLibraryDescriptor.WHOLE_GENOME});
        testCases.add(new Object[]{"cDNA", "HybridSelection", SubmissionLibraryDescriptor.WHOLE_EXOME});

        testCases.add(new Object[]{"Resequencing", "cDNAShotgunReadTwoSense", SubmissionLibraryDescriptor.RNA_SEQ});
        testCases.add(new Object[]{"Resequencing", "cDNAShotgunStrandAgnostic", SubmissionLibraryDescriptor.RNA_SEQ});
        testCases.add(new Object[]{"Resequencing", "WholeGenomeShotgun", SubmissionLibraryDescriptor.WHOLE_GENOME});
        testCases.add(new Object[]{"Resequencing", "HybridSelection", SubmissionLibraryDescriptor.WHOLE_EXOME});

        return testCases.iterator();
    }


    @Test(dataProvider = "analysisTypeScenarios")
    public void testGetAnalysisType(String analysisType, String libraryType, SubmissionLibraryDescriptor expected)
        throws Exception {
        ReadGroupIndex readGroupIndex = new ReadGroupIndex(null, "flowcell", 1l, "my library",
            libraryType, analysisType, "project", "my sample", "PDO-1234");

        assertThat(readGroupIndex.getLibraryType(), is(equalTo(expected)));
    }
}
