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

package org.broadinstitute.gpinformatics.mercury.presentation.admin;

import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.PublicMessage;
import org.broadinstitute.gpinformatics.athena.presentation.MockStripesActionRunner;
import org.broadinstitute.gpinformatics.athena.presentation.ResolutionCallback;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.presentation.TestCoreActionBeanContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class PublicMessageActionBeanTest {
    private static String TEST_MESSAGE_TEXT = "This is a message.";
    private PublicMessageActionBean actionBean;

    @BeforeMethod
    private void setUp() {
        actionBean = new PublicMessageActionBean();
        actionBean.setContext(new TestCoreActionBeanContext());
        actionBean.setPublicMessage(new PublicMessage());
    }

    public void testText() throws Exception {
        actionBean.getPublicMessage().setMessage(TEST_MESSAGE_TEXT);
        ResolutionCallback resolutionCallback = new ResolutionCallback() {
            @Override
            public Resolution getResolution() throws Exception {
                return actionBean.text();
            }
        };

        MockHttpServletResponse response = MockStripesActionRunner.runStripesAction(resolutionCallback);
        Assert.assertEquals(response.getOutputString(), TEST_MESSAGE_TEXT);

    }
}
