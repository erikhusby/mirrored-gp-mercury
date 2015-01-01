package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.DATABASE_FREE)
public class BSPUtilTest {

    @DataProvider(name = "isInBspFormatData")
    public Object[][] createIsInBspFormatTestData() {
        // @formatter:off
        return new Object[][] {
                {"SM-2ACG",         Boolean.TRUE},
                {"SM-2ACG5",        Boolean.TRUE},
                {"SP-2ACG5",        Boolean.TRUE},
                {"Blahblahblah",    Boolean.FALSE},
                {"12345.0",         Boolean.FALSE}, // that's a GSSR id, not a BSP id
                {"4FHTK",           Boolean.FALSE}, // "bare ids" are not considered valid BSP barcodes
                {"SM-ABC0",         Boolean.FALSE}, // 0 is not allowed
                {"SM-ABCO",         Boolean.TRUE}   // O is allowed
        };
        // @formatter:on
    }

    @Test(dataProvider = "isInBspFormatData")
    public void isInBspFormatTest(String sampleId, Boolean expectedResult) {
        assertThat(BSPUtil.isInBspFormat(sampleId), equalTo(expectedResult));
    }

    @DataProvider(name = "isInBspFormatOrBareIdData")
    public Object[][] createIsInBspFormatOrBareIdData() {
        // @formatter:off
        return new Object[][] {
                {"SM-1234",     Boolean.TRUE},  // SM- prefix
                {"SP-1234",     Boolean.TRUE},  // SP- prefix
                {"1234",        Boolean.TRUE},  // "bare" id is accepted by isInBspFormatOrBareId()
                {"123",         Boolean.FALSE}, // too short
                {"1234567",     Boolean.FALSE}  // too long
        // @formatter:on
        };
    }

    @Test(dataProvider = "isInBspFormatOrBareIdData")
    public void isInBspFormatOrBareIdTest(String sampleId, Boolean expectedResult) {
        assertThat(BSPUtil.isInBspFormatOrBareId(sampleId), equalTo(expectedResult));
    }
}
