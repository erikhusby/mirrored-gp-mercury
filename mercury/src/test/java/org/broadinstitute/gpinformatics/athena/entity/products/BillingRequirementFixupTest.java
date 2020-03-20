/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2020 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Arrays;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.FIXUP)
public class BillingRequirementFixupTest extends Arquillian {
    @Inject
    private UserBean userBean;
    @Inject
    private UserTransaction utx;
    @Inject
    private ProductDao productDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void testGPLIM_6698_AoUBillingRequirements() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<Product> allOfUsProducts =
            productDao.findByPartNumbers(Arrays.asList("P-CLA-0008", "P-CLA-0007", "P-WG-0113", "P-WG-0056"));

        allOfUsProducts.forEach(product -> {
            if (product.getRequirement() == null) {
                product.addRequirement(new BillingRequirement("CAN_BILL", Operator.EQUALS, 1.0));
            }
        });

        productDao.persist(new FixupCommentary("GPLIM_6698 set default billing requirements for All Of Us."));

        utx.commit();
    }
}
