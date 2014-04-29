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

package org.broadinstitute.gpinformatics.athena.boundary.infrastructure;

import org.broadinstitute.gpinformatics.athena.entity.infrastructure.PublicMessage;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class PublicMessageEjbTest extends ContainerTest {
    private static final String TEST_MESSAGE = "this is my message";
    @Inject
    private PublicMessageEjb publicMessageEjb;

    public void testPublicMessage() throws Exception {
        publicMessageEjb.clearPublicMessage();

        PublicMessage publicMessage = new PublicMessage(TEST_MESSAGE);
        publicMessageEjb.setPublicMessage(publicMessage);

        PublicMessage anotherPublicMessage = publicMessageEjb.getPublicMessage();
        Assert.assertNotNull(anotherPublicMessage);
        Assert.assertEquals(anotherPublicMessage.getMessage(), TEST_MESSAGE);
        publicMessageEjb.clearPublicMessage();
    }

    public void testSetPublicMessage() {
        publicMessageEjb.clearPublicMessage();

        PublicMessage publicMessage = new PublicMessage();
        publicMessage.setMessage(TEST_MESSAGE);

        publicMessageEjb.setPublicMessage(publicMessage);
        publicMessageEjb.clearPublicMessage();
    }
}
