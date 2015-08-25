/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.EnumSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class MaterialTypeMatcherTest {
    @DataProvider(name = "materialTypesMatcherData")
    public static Object[][] materialTypesMatcherData() {
        return new Object[][]{
                // Variations in this groups ought to match
                new Object[]{LabVessel.MaterialType.BUFFY_COAT, "Buffy Coat", true},
                new Object[]{LabVessel.MaterialType.BUFFY_COAT, "Buffy   Coat", true},
                new Object[]{LabVessel.MaterialType.BUFFY_COAT, "Buffy\tCoat", true},
                new Object[]{LabVessel.MaterialType.BUFFY_COAT, "Whole Blood:Buffy Coat", true},

                new Object[]{LabVessel.MaterialType.CELL_SUSPENSION, "Cell Suspension", true},

                new Object[]{LabVessel.MaterialType.DNA, "DNA", true},
                new Object[]{LabVessel.MaterialType.DNA, "DNA:DNA Genomic", true},
                new Object[]{LabVessel.MaterialType.DNA, "DNA: DNA Genomic", true},
                new Object[]{LabVessel.MaterialType.DNA, "DNA:DNA Somatic", true},

                new Object[]{LabVessel.MaterialType.FFPE, "FFPE", true},
                new Object[]{LabVessel.MaterialType.FFPE, "Tissue: FFPE", true},

                new Object[]{LabVessel.MaterialType.FRESH_BLOOD, "Whole Blood: Whole Blood Fresh", true},
                new Object[]{LabVessel.MaterialType.FRESH_BLOOD, "Fresh Blood", true},

                new Object[]{LabVessel.MaterialType.FRESH_FROZEN_BLOOD, "Fresh Frozen Blood", true},
                new Object[]{LabVessel.MaterialType.FRESH_FROZEN_BLOOD, "Whole Blood: Whole Blood Frozen", true},

                new Object[]{LabVessel.MaterialType.RNA, "RNA:Total RNA", true},
                new Object[]{LabVessel.MaterialType.RNA, "RNA:Messenger RNA", true},
                new Object[]{LabVessel.MaterialType.RNA, "RNA", true},

                // Variations in this groups should not match
                new Object[]{LabVessel.MaterialType.BUFFY_COAT, "Whole Blood", false},
                new Object[]{LabVessel.MaterialType.BUFFY_COAT, "Buffy Vampire Coat", false},

                new Object[]{LabVessel.MaterialType.FRESH_BLOOD, "Whole Blood:Buffy Coat", false},

                new Object[]{LabVessel.MaterialType.FRESH_FROZEN_BLOOD, "Whole Blood: Whole Blood Fresh", false},
                new Object[]{LabVessel.MaterialType.FRESH_FROZEN_BLOOD, "Whole Blood: Whole Blood", false},

                // todo: need more test cases!
        };
    }


    @Test(dataProvider = "materialTypesMatcherData")
    public void testStringMatchesMaterialType(LabVessel.MaterialType materialType, String stringUnderTest, boolean expectedToMatch) {
        assertThat(materialType.matches(stringUnderTest), is(expectedToMatch));
    }

    @Test(dataProvider = "materialTypesMatcherData")
    public void testStringMatchesOnlyMaterialType(LabVessel.MaterialType materialType, String stringUnderTest, boolean expectedToMatch) {
        if (expectedToMatch) {
            for (LabVessel.MaterialType otherType : EnumSet.complementOf(EnumSet.of(materialType))) {
                String errorMessage =
                        String.format("%s should not match %s", stringUnderTest, otherType.getDisplayName());
                assertThat(errorMessage, otherType.matches(stringUnderTest), is(false));
            }
        }
    }
}
