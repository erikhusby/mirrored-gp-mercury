package org.broadinstitute.gpinformatics.mercury.integration.test;

import org.broadinstitute.gpinformatics.mercury.integration.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.integration.test.beans.AlternativeSimpleServiceImpl;
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
public class DeploymentBuilderBeansXmlOverrideTest extends Arquillian {

    @Inject
    private SimpleService service;

    @Deployment
    public static WebArchive makeArchive() {
        return DeploymentBuilder.buildSequelWarWithAlternatives(AlternativeSimpleServiceImpl.class);
    }

    @Test
    public void testInjection() {
        Assert.assertEquals(service.getName(), "AlternativeSimpleServiceImpl");
    }
}
