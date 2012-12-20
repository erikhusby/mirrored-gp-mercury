package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;

public class ProductOrderAbandon extends Arquillian {

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Inject
    private Log log;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(PROD, "prod");
    }

    @Test
    public void abandon() {
        String [] pdos = new String[] {
                "PDO-11",
                "PDO-14",
                "PDO-15",
                "PDO-19",
                "PDO-22",
                "PDO-34",
                "PDO-75",
                "PDO-171",
                "PDO-180",
                "PDO-182",
                "PDO-185"
        };

        for (String pdo : pdos) {
            try {
                productOrderEjb.abandon(pdo);
            } catch (ProductOrderEjb.NoCancelTransitionException e) {
                log.error("No Cancel transition found for " + pdo + ": " + e.getMessage(), e);
            } catch (ProductOrderEjb.NoSuchPDOException e) {
                log.error("No such PDO found in DB: " + pdo);
            }
        }

    }
}
