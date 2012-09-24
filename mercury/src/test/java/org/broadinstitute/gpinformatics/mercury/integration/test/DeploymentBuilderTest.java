package org.broadinstitute.gpinformatics.mercury.integration.test;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.integration.test.beans.SimpleService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * @author breilly
 */
public class DeploymentBuilderTest extends Arquillian {

    @Inject
    private SimpleService service;

    @Deployment
    public static WebArchive makeArchive() {
        return DeploymentBuilder.buildMercuryWar().addPackage("org.broadinstitute.gpinformatics.mercury.test.beans");
    }

    @Test
    public void testInjection() {
        System.out.println("in inject test---");
        Assert.assertEquals(service.getName(), "SimpleServiceImpl");
    }
}
