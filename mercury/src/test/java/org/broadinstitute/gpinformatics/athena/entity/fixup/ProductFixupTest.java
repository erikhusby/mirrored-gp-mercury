package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.lang3.tuple.Pair;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            if (product.getAggregationDataType() != null) {
                product.setPairedEndRead(true);
                if (product.getAggregationDataType().startsWith("Exome")) {
                    product.setLoadingConcentration(BigDecimal.valueOf(225));
                } else if (product.getAggregationDataType().equals("WGS")) {
                    product.setLoadingConcentration(BigDecimal.valueOf(180));
                }
            }
        }
        productDao.persist(new FixupCommentary("GPLIM-4159 set initial values after adding new columns."));
        utx.commit();
    }

    @Test(enabled = false)
    public void GPLIM3614InitializeNewValues() {
        userBean.loginOSUser();
        List<Product> allProducts = productDao.findAll(Product.class);

        List<String> externalPartNumbers = Arrays.asList("P-CLA-0003", "P-CLA-0004", "P-EX-0011", "P-VAL-0010", "P-VAL-0016", "P-WG-0054", "P-VAL-0013", "P-VAL-0014", "P-VAL-0015");

        for(Product currentProduct:allProducts) {
            currentProduct.setExternalOnlyProduct(externalPartNumbers.contains(currentProduct.getPartNumber()) || currentProduct.isExternallyNamed());

            currentProduct.setSavedInSAP(false);
        }
        productDao.persist(new FixupCommentary("GPLIM-3614 initialized external indicator and saved in SAP indicator for all products"));

    }

    @Test(enabled = false)
    public void gplim4897CloneForQuicksilver() throws Exception {

        userBean.loginOSUser();

        final Map<String, Pair<String, String>> partNumbersToClone = new HashMap<>();
        partNumbersToClone.put("P-EX-0012", Pair.of("P-EX-0039","Express Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("P-EX-0013", Pair.of("P-EX-0040","Express Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("P-EX-0028", Pair.of("P-EX-0041","Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("P-EX-0029", Pair.of("P-EX-0042","Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("P-EX-0036", Pair.of("P-EX-0044","Human WES - Normal (150xMTC) v1.1"));
        partNumbersToClone.put("P-EX-0037", Pair.of("P-EX-0045","Human WES - Tumor (150xMTC) v1.1"));
        partNumbersToClone.put("XTNL-WES-010204", Pair.of("XTNL-WES-010252","WES-010204 Express Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010205", Pair.of("XTNL-WES-010253","WES-010205 Express Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010206", Pair.of("XTNL-WES-010254","WES-010206 Express Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010207", Pair.of("XTNL-WES-010255","WES-010207 Express Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010208", Pair.of("XTNL-WES-010256","WES-010208 Express Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010209", Pair.of("XTNL-WES-010257","WES-010209 Express Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010218", Pair.of("XTNL-WES-010258","WES-010218 Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010219", Pair.of("XTNL-WES-010259","WES-010219 Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010220", Pair.of("XTNL-WES-010260","WES-010220 Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010221", Pair.of("XTNL-WES-010261","WES-010221 Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010222", Pair.of("XTNL-WES-010262","WES-010222 Express FFPE Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010223", Pair.of("XTNL-WES-010263","WES-010223 Express FFPE Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010224", Pair.of("XTNL-WES-010264","WES-010224 Express FFPE Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010225", Pair.of("XTNL-WES-010265","WES-010225 FFPE Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010226", Pair.of("XTNL-WES-010266","WES-010226 FFPE Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010227", Pair.of("XTNL-WES-010267","WES-010227 FFPE Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010237", Pair.of("XTNL-WES-010268","WES-010237 Express Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010238", Pair.of("XTNL-WES-010269","WES-010238 Express Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010239", Pair.of("XTNL-WES-010270","WES-010239 Express Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010240", Pair.of("XTNL-WES-010271","WES-010240 Express Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010241", Pair.of("XTNL-WES-010272","WES-010241 Express Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010242", Pair.of("XTNL-WES-010273","WES-010242 Express Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010243", Pair.of("XTNL-WES-010274","WES-010243 Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010244", Pair.of("XTNL-WES-010275","WES-010244 Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010245", Pair.of("XTNL-WES-010276","WES-010245 Somatic Human WES (Standard Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010246", Pair.of("XTNL-WES-010277","WES-010246 Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010247", Pair.of("XTNL-WES-010278","WES-010247 Somatic Human WES (Deep Coverage) v1.1"));
        partNumbersToClone.put("XTNL-WES-010248", Pair.of("XTNL-WES-010279","WES-010248 Somatic Human WES (Deep Coverage) v1.1"));

        final List<Product> productsToClone = productDao.findByPartNumbers(new ArrayList<String>(partNumbersToClone.keySet()));

        for (Product productToClone : productsToClone) {

            final String productName = partNumbersToClone.get(productToClone.getPartNumber()).getRight();
            final String partNumber = partNumbersToClone.get(productToClone.getPartNumber()).getLeft();

            Product clonedProduct = Product.cloneProduct(productToClone, productName, partNumber);

            productDao.persist(clonedProduct);
        }
        productDao.persist(new FixupCommentary("GPLIM-4897 cloning exome products for Quicksilver"));

    }

}
