package org.broadinstitute.gpinformatics.mercury.boundary.rapsheet;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 *
 */
public class ReworkEjbTest extends Arquillian {

    @Inject
    ReworkEjb reworkEjb;

    /*
     * I know this is breaking the wall, but I need to create a few "Athena" items
     */
    @Inject
    ProductOrderDao productOrderDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, BSPSampleSearchServiceStub.class);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        if(reworkEjb == null) {
            return;
        }
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if(reworkEjb == null) {
            return;
        }
    }

}
