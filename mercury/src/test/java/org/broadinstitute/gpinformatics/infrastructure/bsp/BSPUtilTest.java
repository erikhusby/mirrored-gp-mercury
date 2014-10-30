package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.DATABASE_FREE)
public class BSPUtilTest {

    public void isInBspFormatOrBareId_with_SM_prefix_returns_true() {
        assertThat(BSPUtil.isInBspFormatOrBareId("SM-1234"), equalTo(true));
    }

    public void isInBspFormatOrBareId_with_SP_prefix_returns_true() {
        assertThat(BSPUtil.isInBspFormatOrBareId("SP-1234"), equalTo(true));
    }

    public void isInBspFormatOrBareId_with_no_prefix_returns_true() {
        assertThat(BSPUtil.isInBspFormatOrBareId("1234"), equalTo(true));
    }

    public void isInBspFormatOrBareId_with_short_ID_returns_false() {
        assertThat(BSPUtil.isInBspFormatOrBareId("123"), equalTo(false));
    }

    public void isInBspFormatOrBareId_with_long_ID_returns_false() {
        assertThat(BSPUtil.isInBspFormatOrBareId("1234567"), equalTo(false));
    }
}
