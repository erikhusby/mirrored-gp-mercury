/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.EnumSet;

import static org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService.Option.NONE;
import static org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService.Option.Type;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.DATABASE_FREE)
public class SapOrderOptionsTest {
    public void testHasOption() throws Exception {
        SapIntegrationService.Option sapOrderOptions =
            SapIntegrationService.Option.create(Type.CREATING, Type.ORDER_VALUE_QUERY);

        assertThat(sapOrderOptions.hasOption(Type.ORDER_VALUE_QUERY), is(true));
        assertThat(sapOrderOptions.hasOption(Type.CREATING), is(true));
        assertThat(sapOrderOptions.hasOption(Type.CLOSING), not(true));
    }

    public void testNullOption() throws Exception {
        SapIntegrationService.Option none = SapIntegrationService.Option.NONE;

        assertThat(none.hasOption(Type.ORDER_VALUE_QUERY), not(true));
        assertThat(none.hasOption(Type.CREATING), not(true));
        assertThat(none.hasOption(Type.CLOSING), not(true));
    }

    public void testEquality() throws Exception {
        SapIntegrationService.Option creating = SapIntegrationService.Option.create(Type.CREATING);
        assertThat(creating, is(SapIntegrationService.Option.create(Type.CREATING)));
        assertThat(creating, not(NONE));
        EnumSet<Type> others = EnumSet.complementOf(EnumSet.of(Type.CREATING));

        others.forEach(type -> assertThat(type, not(Type.CREATING)));

    }
}
