/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderKitDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit_;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * This "test" is an example of how to fixup some data.  Each fix method includes the JIRA ticket ID.
 * Set @Test(enabled=false) after running once.
 */
@Test(groups = TestGroups.FIXUP)
public class ProductOrderSampleKitFixupTest extends Arquillian {

    @Inject
    private ProductOrderKitDao productOrderKitDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private Log log;

    // When you run this on prod, change to PROD and prod.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }
    // GPLIM-2427
    @Test(enabled=false)
    public void backPopulateKitRequestIsExomeExpress(){
        List<ProductOrderKit> nullExomeExpress = productOrderKitDao.findList(ProductOrderKit.class, ProductOrderKit_.exomeExpress, null);
        for (ProductOrderKit pdoKit : nullExomeExpress) {
            pdoKit.setExomeExpress(false);
        }
        productOrderKitDao.persistAll(nullExomeExpress);
        productOrderKitDao.flush();

    }
}
