package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.io.IOException;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;

public class ProductOrderAbandonFixupTest extends Arquillian {

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Inject
    private Log log;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(PROD, "prod");
    }

    @Test(enabled = false)
    public void abandon() {
        String [] pdos = new String[] {
            "PDO-55",
            "PDO-214"
        };

        for (String pdo : pdos) {
            try {
                productOrderEjb.abandon(pdo, "Abandoned by ProductOrderAbandonFixupTest");
            } catch (ProductOrderEjb.NoTransitionException e) {
                log.error(e.getMessage(), e);
            } catch (ProductOrderEjb.NoSuchPDOException e) {
                log.error("No such PDO found in DB: " + pdo, e);
            } catch (ProductOrderEjb.SampleDeliveryStatusChangeException e) {
                log.error(e.getMessage(), e);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
