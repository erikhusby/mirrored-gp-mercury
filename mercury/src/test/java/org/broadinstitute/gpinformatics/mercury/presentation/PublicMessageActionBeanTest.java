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

package org.broadinstitute.gpinformatics.mercury.presentation;

import net.sourceforge.stripes.mock.MockRoundtrip;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.PublicMessageEjb;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.PublicMessage;
import org.broadinstitute.gpinformatics.athena.presentation.StripesMockTestUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Test(groups = TestGroups.DATABASE_FREE)
public class PublicMessageActionBeanTest {
    private static String TEST_MESSAGE_TEXT = "This is a message.";
    private PublicMessageEjb publicMessageEjb;

    @BeforeMethod
    private void setUp() {
        publicMessageEjb = mock(PublicMessageEjb.class);
        PublicMessage publicMessage = new PublicMessage(TEST_MESSAGE_TEXT);
        when(publicMessageEjb.getPublicMessage()).thenReturn(publicMessage);
    }

    public void testText() throws Exception {
        MockRoundtrip roundtrip =
                StripesMockTestUtils.createMockRoundtrip(PublicMessageActionBean.class, publicMessageEjb);

        roundtrip.execute(PublicMessageActionBean.TEXT);
        Assert.assertEquals(roundtrip.getOutputString(), TEST_MESSAGE_TEXT);
        verify(publicMessageEjb).getPublicMessage();
    }
}
