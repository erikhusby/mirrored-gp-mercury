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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class MaterialTypeTest {

    private static final boolean THROWS_EXCEPTION = true;
    private static final boolean IS_VALID = true;

    @SuppressWarnings("PointlessBooleanExpression")
    @DataProvider(name = "materialTypes")
    public static Object[][] materialTypes() {
        return new Object[][]{
                new Object[]{"goop", null, THROWS_EXCEPTION, !IS_VALID},
                new Object[]{null, MaterialType.NONE, !THROWS_EXCEPTION, !IS_VALID},
                new Object[]{"Cell Suspension", MaterialType.CELL_SUSPENSION, !THROWS_EXCEPTION, IS_VALID},
        };
    }

    @Test(dataProvider = "materialTypes")
    public void testFromDisplayName(String displayName, MaterialType materialType, boolean exceptionExpected,
                                    boolean isValid) throws Exception {

        try {
            assertThat(MaterialType.fromDisplayName(displayName), equalTo(materialType));
            assertThat(exceptionExpected, is(false));
            assertThat(MaterialType.isValid(displayName), equalTo(isValid));
        } catch (Exception ignored) {
        }
    }

}
