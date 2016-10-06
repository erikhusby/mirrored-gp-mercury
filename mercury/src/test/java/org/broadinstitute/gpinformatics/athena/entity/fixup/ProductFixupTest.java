package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_.product;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 *
 * Applies workflow names to known Products in Production.  This helps support the Workflow efforts of Mercury
 *
 * @author Scott Matthews
 *         Date: 12/20/12
 *         Time: 4:29 PM
 */
@Test(groups = TestGroups.FIXUP)
public class ProductFixupTest extends Arquillian {

    @Inject
    ProductDao productDao;

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction utx;

    /*
     * When applying this to Production, change the input to PROD, "prod"
     */
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    // Required for Arquillian tests so it should remain enabled for sprint4.
    @Test(enabled = false)
    public void addExomeExpressWorkflowName() {

        Product exExProduct = productDao.findByPartNumber("P-EX-0002");

        if (exExProduct.getWorkflow() != Workflow.AGILENT_EXOME_EXPRESS) {
            exExProduct.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
            productDao.persist(exExProduct);
        }
    }

    @Test(enabled = false)
    public void addHybridSelectionWorkflowName() {

        Product hybSelProject = productDao.findByPartNumber("P-EX-0001");
            hybSelProject.setWorkflow(Workflow.HYBRID_SELECTION);

        productDao.persist(hybSelProject);
    }

    @Test(enabled = false)
    public void addWholeGenomeWorkflowName() {

        List<Product> wgProducts = new ArrayList<>(3);

        Product wholeGenomeProduct1 = productDao.findByPartNumber("P-WG-0001");
            wholeGenomeProduct1.setWorkflow(Workflow.WHOLE_GENOME);
        Product wholeGenomeProduct2 = productDao.findByPartNumber("P-WG-0002");
            wholeGenomeProduct2.setWorkflow(Workflow.WHOLE_GENOME);
        Product wholeGenomeProduct3 = productDao.findByPartNumber("P-WG-0003");
            wholeGenomeProduct3.setWorkflow(Workflow.WHOLE_GENOME);

        Collections.addAll(wgProducts, wholeGenomeProduct1, wholeGenomeProduct2, wholeGenomeProduct3);

        productDao.persistAll(wgProducts);
    }

    @Test(enabled = false)
    public void gplim4159() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        for (Product product : productDao.findAll(Product.class)) {
            if (product.getAggregationDataType() != null && product.getAggregationDataType().startsWith("Exome")) {
                product.setLoadingConcentration(BigDecimal.valueOf(225));
                product.setPairedEndRead(true);
            } else if (product.getAggregationDataType() != null && product.getAggregationDataType().equals("WGS")) {
                product.setLoadingConcentration(BigDecimal.valueOf(180));
                product.setPairedEndRead(true);
            }
        }
        productDao.persist(new FixupCommentary("GPLIM-4159 set initial values after adding new columns."));
        utx.commit();
    }
}
