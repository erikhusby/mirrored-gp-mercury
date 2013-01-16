package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;

public class GPLIM_587_Test extends Arquillian {

    @Inject
    private ProductOrderEjb productOrderEjb;

    private static final Log log = LogFactory.getLog(GPLIM_587_Test.class);

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(TEST, "dev");
    }


    @Test(enabled = false)
    /**
     * This should work if there was actually a 'Close' or 'Complete' transition, which as of the time of this writing
     * does not exist
     */
    public void complete284() {
        try {
            productOrderEjb.complete("PDO-284", "This has been completed by my test!");
        } catch (ProductOrderEjb.SampleDeliveryStatusChangeException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        } catch (ProductOrderEjb.NoTransitionException e) {
            log.error(e);
        } catch (ProductOrderEjb.NoSuchPDOException e) {
            log.error(e);
        }
    }



    @Test(enabled = false)
    /**
     * works
     */
    public void abandon283() {
        try {
            productOrderEjb.abandon("PDO-283", "This has been abandoned by my test!");
        } catch (ProductOrderEjb.SampleDeliveryStatusChangeException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        } catch (ProductOrderEjb.NoTransitionException e) {
            log.error(e);
        } catch (ProductOrderEjb.NoSuchPDOException e) {
            log.error(e);
        }
    }


    @Test(enabled = false)
    /**
     * works
     */
    public void abandonSamplesIn282() {

        try {
            productOrderEjb.abandonSamples(
                    "PDO-282",
                    Arrays.asList(1, 3, 5),
                    Arrays.asList("Abandoning sample uno", "And sample tres", "And sample 5 too"));

        } catch (IOException e) {
            log.error(e);
        } catch (ProductOrderEjb.SampleDeliveryStatusChangeException e) {
            log.error(e);
        } catch (ProductOrderEjb.NoSuchPDOException e) {
            log.error(e);
        }
    }


    @Test(enabled = false)
    /**
     * works
     */
    public void completeSamplesIn282() {

        try {
            productOrderEjb.completeSamples(
                    "PDO-282",
                    Arrays.asList(12, 14, 16),
                    Arrays.asList("Completing sample 12", "And sample 14", "And sample 16 too"));

        } catch (IOException e) {
            log.error(e);
        } catch (ProductOrderEjb.SampleDeliveryStatusChangeException e) {
            log.error(e);
        } catch (ProductOrderEjb.NoSuchPDOException e) {
            log.error(e);
        }
    }

}
