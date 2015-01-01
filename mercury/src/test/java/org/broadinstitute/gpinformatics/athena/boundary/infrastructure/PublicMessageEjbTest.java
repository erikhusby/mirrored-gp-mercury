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
import org.broadinstitute.gpinformatics.infrastructure.test.AbstractContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class PublicMessageEjbTest extends AbstractContainerTest {
    private static final String TEST_MESSAGE = "this is my message";
    @Inject
    private PublicMessageEjb publicMessageEjb;

    public void testPublicMessage() throws Exception {
        PublicMessage publicMessage = new PublicMessage(TEST_MESSAGE);
        publicMessageEjb.setPublicMessage(publicMessage);

        PublicMessage anotherPublicMessage = publicMessageEjb.getPublicMessage();
        Assert.assertNotNull(anotherPublicMessage);
        Assert.assertEquals(anotherPublicMessage.getMessage(), TEST_MESSAGE);
    }


    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @AfterMethod
    @BeforeMethod
    public void clearMessage() {
        if (!isRunningInContainer()) {
            return;
        }
        publicMessageEjb.clearPublicMessage();
    }
}
