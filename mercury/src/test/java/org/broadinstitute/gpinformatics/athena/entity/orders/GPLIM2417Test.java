package org.broadinstitute.gpinformatics.athena.entity.orders;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;


public class GPLIM2417Test extends ContainerTest {

    /*
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD
    }
    */

    @Inject
    ProductOrderDao pdoDao;

    @Test
    public void testThatPDOWIthMoreThan1000SamplesWillLoad() {
        ProductOrder pdo = pdoDao.findByBusinessKey("PDO-1312");
        try {
            ProductOrder.loadLabEventSampleData(pdo.getSamples());
            // if this doesn't explode, the test passes
        }
        catch(Throwable t) {
            Assert.fail();
        }

    }
}
