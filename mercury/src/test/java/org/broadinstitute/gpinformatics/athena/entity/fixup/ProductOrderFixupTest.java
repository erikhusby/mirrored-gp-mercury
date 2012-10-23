package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;

/**
 * This "test" is an example of how to fixup some data.  Each fix method includes the JIRA ticket ID.
 * Set @Test(enabled=false) after running once.
 */
public class ProductOrderFixupTest extends Arquillian {

    @Inject
    private ProductOrderDao productOrderDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(PROD, "prod");
    }

    /**
     * Fixed up data per JIRA ticket.
     */
    @Test(enabled = false)
    public void fixupGplim123() {
        ProductOrder productOrder = productOrderDao.findById(1001L);
        productOrder.setComments(productOrder.getComments() + " fixup");
        // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
        // change gets flushed.  This is an artifact of the test environment.
        productOrderDao.persist(productOrder);
    }
}
