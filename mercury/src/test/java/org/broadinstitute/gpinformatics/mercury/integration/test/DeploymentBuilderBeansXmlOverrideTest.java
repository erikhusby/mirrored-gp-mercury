package org.broadinstitute.gpinformatics.mercury.integration.test;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.test.beans.AlternativeSimpleServiceImpl;
import org.broadinstitute.gpinformatics.mercury.integration.test.beans.SimpleService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * @author breilly
 */
@Test(groups = TestGroups.ALTERNATIVES)
@Dependent
public class DeploymentBuilderBeansXmlOverrideTest extends Arquillian {

    public DeploymentBuilderBeansXmlOverrideTest(){}

    @Inject
    private SimpleService service;

    @Deployment
    public static WebArchive makeArchive() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(AlternativeSimpleServiceImpl.class);
    }

    @Test(enabled = false)
    public void testInjection() {
        Assert.assertEquals(service.getName(), "AlternativeSimpleServiceImpl");
    }
}
