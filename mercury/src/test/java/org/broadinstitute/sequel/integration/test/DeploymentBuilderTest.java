package org.broadinstitute.sequel.integration.test;

import org.broadinstitute.sequel.integration.DeploymentBuilder;
import org.broadinstitute.sequel.integration.test.beans.SimpleService;
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
        return DeploymentBuilder.buildSequelWar().addPackage("org.broadinstitute.sequel.test.beans");
    }

    @Test
    public void testInjection() {
        System.out.println("in inject test---");
        Assert.assertEquals(service.getName(), "SimpleServiceImpl");
    }
}
