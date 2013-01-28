package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.hibernate.cfg.CollectionSecondPass;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.QA;

/**
 *
 * Applies workflow names to known Products in Production.  This helps support the Workflow efforts of Mercury
 *
 * @author Scott Matthews
 *         Date: 12/20/12
 *         Time: 4:29 PM
 */
public class ProductFixupTest extends Arquillian {

    @Inject
    ProductDao productDao;

    /*
     * When applying this to Production, change the input to PROD, "prod"
     */
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void addExomeExpressWorkflowName() {

        Product exExProduct = productDao.findByPartNumber("P-EX-0002");
            exExProduct.setWorkflowName("Exome Express");

        productDao.persist(exExProduct);
    }

    @Test(enabled = false)
    public void addHybridSelectionWorkflowName() {

        Product hybSelProject = productDao.findByPartNumber("P-EX-0001");
            hybSelProject.setWorkflowName("Hybrid Selection");

        productDao.persist(hybSelProject);
    }

    @Test(enabled = false)
    public void addWholeGenomeWorkflowName() {

        List<Product> wgProducts = new ArrayList<Product>(3);

        Product wholeGenomeProduct1 = productDao.findByPartNumber("P-WG-0001");
            wholeGenomeProduct1.setWorkflowName("Whole Genome");
        Product wholeGenomeProduct2 = productDao.findByPartNumber("P-WG-0002");
            wholeGenomeProduct2.setWorkflowName("Whole Genome");
        Product wholeGenomeProduct3 = productDao.findByPartNumber("P-WG-0003");
            wholeGenomeProduct3.setWorkflowName("Whole Genome");

        Collections.addAll(wgProducts, wholeGenomeProduct1, wholeGenomeProduct2, wholeGenomeProduct3);

        productDao.persistAll(wgProducts);
    }


}
