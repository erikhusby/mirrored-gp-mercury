package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.sap.entity.SAPMaterial;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
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
    private SAPProductPriceCache productPriceCache;

    @Inject
    private PriceListCache priceListCache;

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

        if (exExProduct.getWorkflowName() != Workflow.AGILENT_EXOME_EXPRESS) {
            exExProduct.setWorkflowName(Workflow.AGILENT_EXOME_EXPRESS);
            productDao.persist(exExProduct);
        }
    }

    @Test(enabled = false)
    public void addHybridSelectionWorkflowName() {

        Product hybSelProject = productDao.findByPartNumber("P-EX-0001");
            hybSelProject.setWorkflowName(Workflow.HYBRID_SELECTION);

        productDao.persist(hybSelProject);
    }

    @Test(enabled = false)
    public void addWholeGenomeWorkflowName() {

        List<Product> wgProducts = new ArrayList<>(3);

        Product wholeGenomeProduct1 = productDao.findByPartNumber("P-WG-0001");
            wholeGenomeProduct1.setWorkflowName(Workflow.WHOLE_GENOME);
        Product wholeGenomeProduct2 = productDao.findByPartNumber("P-WG-0002");
            wholeGenomeProduct2.setWorkflowName(Workflow.WHOLE_GENOME);
        Product wholeGenomeProduct3 = productDao.findByPartNumber("P-WG-0003");
            wholeGenomeProduct3.setWorkflowName(Workflow.WHOLE_GENOME);

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
        utx.begin();

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
        utx.commit();
    }

    @Test(enabled = false)
    public void support3237CloneProductsForHyperprep() throws Exception {

        userBean.loginOSUser();
        utx.begin();

        final Map<String, Pair<String, String>> partNumbersToClone = new HashMap<>();
        partNumbersToClone.put("P-WG-0048", Pair.of("P-WG-0079","PCR-Free Human WGS - 60x v1.1"));
        partNumbersToClone.put("P-WG-0049", Pair.of("P-WG-0080","PCR+ Human WGS - 30x v1.1"));
        partNumbersToClone.put("P-WG-0050", Pair.of("P-WG-0081","PCR+ Human WGS - 60x v1.1"));
        partNumbersToClone.put("P-WG-0074", Pair.of("P-WG-0082","PCR+ Human WGS - 15x v1.1"));
        partNumbersToClone.put("P-WG-0075", Pair.of("P-WG-0083","PCR-Free Human WGS - 15x v1.1"));
        partNumbersToClone.put("P-WG-0076", Pair.of("P-WG-0084","PCR-Free Human WGS - 80x v1.1"));
        partNumbersToClone.put("XTNL-WGS-010300", Pair.of("XTNL-WGS-010322","WGS-010300 Genome, PCR-Free, 30X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010301", Pair.of("XTNL-WGS-010323","WGS-010301 Genome, PCR-Free, 30X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010302", Pair.of("XTNL-WGS-010324","WGS-010302 Genome, PCR-Free, 30X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010303", Pair.of("XTNL-WGS-010325","WGS-010303 Genome, PCR-Free, 30X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010304", Pair.of("XTNL-WGS-010326","WGS-010304 Genome, PCR-Free, 60X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010306", Pair.of("XTNL-WGS-010327","WGS-010306 Genome, PCR Plus, 60X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010307", Pair.of("XTNL-WGS-010328","WGS-010307 Genome, PCR Plus, 30X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010308", Pair.of("XTNL-WGS-010329","WGS-010308 Genome, PCR Plus, 30X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010309", Pair.of("XTNL-WGS-010330","WGS-010309 Genome, PCR Plus, 30X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010310", Pair.of("XTNL-WGS-010331","WGS-010310 Genome, PCR Plus, 30X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010311", Pair.of("XTNL-WGS-010332","WGS-010311 Genome, PCR-Free, 60X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010312", Pair.of("XTNL-WGS-010333","WGS-010312 Genome, PCR-Free, 60X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010313", Pair.of("XTNL-WGS-010334","WGS-010313 Genome, PCR-Free, 60X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010314", Pair.of("XTNL-WGS-010335","WGS-010314 Genome, PCR Plus, 60X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010315", Pair.of("XTNL-WGS-010336","WGS-010315 Genome, PCR Plus, 60X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010316", Pair.of("XTNL-WGS-010337","WGS-010316 Genome, PCR Plus, 60X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010317", Pair.of("XTNL-WGS-010338","WGS-010317 Genome, Unaligned, PCR-Free v1.1"));
        partNumbersToClone.put("XTNL-WGS-010318", Pair.of("XTNL-WGS-010339","WGS-010318 Genome, PCR-Free, 30X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010319", Pair.of("XTNL-WGS-010340","WGS-010319 Genome, PCR-Free, 60X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010320", Pair.of("XTNL-WGS-010341","WGS-010320 Genome, PCR Plus,  30X v1.1"));
        partNumbersToClone.put("XTNL-WGS-010321", Pair.of("XTNL-WGS-010342","WGS-010321 Genome, PCR Plus,  60X v1.1"));

        final List<Product> productsToClone = productDao.findByPartNumbers(new ArrayList<String>(partNumbersToClone.keySet()));

        for (Product productToClone : productsToClone) {

            final String productName = partNumbersToClone.get(productToClone.getPartNumber()).getRight();
            final String partNumber = partNumbersToClone.get(productToClone.getPartNumber()).getLeft();

            Product clonedProduct = Product.cloneProduct(productToClone, productName, partNumber);

            productDao.persist(clonedProduct);
        }
        productDao.persist(new FixupCommentary("SUPPORT-3237 cloning exome products for HyperPrep Implementation"));
        utx.commit();

    }


    @Test(enabled = false)
    public void support3393CloneProductsForHyperprep() throws Exception {

        userBean.loginOSUser();
        utx.begin();

        final Map<String, Pair<String, String>> partNumbersToClone = new HashMap<>();
        partNumbersToClone.put("P-WG-0071", Pair.of("P-WG-0086","PCR-Free Human WGS - 20x v1.1 (High Volume, >10,000 samples)"));
        partNumbersToClone.put("P-WG-0069", Pair.of("P-WG-0087","PCR-Free Human WGS - 30x v1.1 (High Volume, >10,000 samples)"));
        partNumbersToClone.put("P-WG-0079", Pair.of("P-WG-0088","PCR-Free Human WGS - 60x v1.1 (High Volume, >10,000 samples)"));
        partNumbersToClone.put("P-WG-0084", Pair.of("P-WG-0089","PCR-Free Human WGS - 80x v1.1 (High Volume, >10,000 samples)"));
        partNumbersToClone.put("XTNL-WGS-010339", Pair.of("XTNL-WGS-010343","WGS-010322 Genome, PCR-Free, 20X v1.1"));


        final List<Product> productsToClone = productDao.findByPartNumbers(new ArrayList<String>(partNumbersToClone.keySet()));

        for (Product productToClone : productsToClone) {

            final String productName = partNumbersToClone.get(productToClone.getPartNumber()).getRight();
            final String partNumber = partNumbersToClone.get(productToClone.getPartNumber()).getLeft();

            Product clonedProduct = Product.cloneProduct(productToClone, productName, partNumber);

            productDao.persist(clonedProduct);
        }
        productDao.persist(new FixupCommentary("SUPPORT-3393 cloning products for new WGS products"));
        utx.commit();

    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/ProductCloningInfo.txt, so it
     * can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * SUPPORT-XXXX auto creating new products as clones of previous products
     * P-EX-1123[\t]P-EX-1134[\t]new cloned product for the old product
     * P-EX-1124[\t]P-EX-1135[\t]new cloned product for the other old product
     *
     */
    @Test(enabled = false)
    public void supportCloneProductsToNew() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("ProductCloningInfo.txt"));
        String fixupReason = lines.get(0);
        Assert.assertTrue(StringUtils.isNotBlank(fixupReason), "A fixup reason needs to be defined");
        final List<String> cloningProductsLines = lines.subList(1, lines.size());
        Assert.assertTrue(CollectionUtils.isNotEmpty(cloningProductsLines),
                "Lines representing the old and new products needd to be defined in order for this fixup to be run");
        final List<String> newProducts = new ArrayList<>();

        final Map<String, Pair<String, String>> partNumbersToClone = new HashMap<>();

        for (String cloningProduct : cloningProductsLines) {
            final String[] splitLine = cloningProduct.split("\t");
            Assert.assertTrue(splitLine.length == 3, "There appear to be more inputs than there should be on the row matching: " +cloningProduct);
            Assert.assertTrue(StringUtils.isNotBlank(splitLine[0]), "The value of the originating Product part number needs to be set for all lines");
            Assert.assertTrue(StringUtils.isNotBlank(splitLine[1]), "The value for the new product part number needs to be set on all lines");
            Assert.assertTrue(StringUtils.isNotBlank(splitLine[2]), "The value for the new product name needs to be set on all lines");
            partNumbersToClone.put(splitLine[0], Pair.of(splitLine[1], splitLine[2]));
            newProducts.add(splitLine[1]);
        }

        final List<Product> productsToClone = productDao.findByPartNumbers(new ArrayList<String>(partNumbersToClone.keySet()));

        for (Product productToClone : productsToClone) {

            final String productName = partNumbersToClone.get(productToClone.getPartNumber()).getRight();
            final String partNumber = partNumbersToClone.get(productToClone.getPartNumber()).getLeft();

            Product clonedProduct = Product.cloneProduct(productToClone, productName, partNumber);

            productDao.persist(clonedProduct);
        }

        final String loggedChanges = ".  Adding: " + newProducts + " from: " +
                                     StringUtils.join(partNumbersToClone.keySet(), ",");
        System.out.println("Proof of execution: " + loggedChanges);
        productDao.persist(new FixupCommentary(fixupReason));
        utx.commit();

    }

    @Test(enabled = false)
    public void testPriceDifferences() throws Exception {
        List<Product> allProducts =
                productDao.findProducts(ProductDao.Availability.CURRENT, ProductDao.TopLevelOnly.NO,
                        ProductDao.IncludePDMOnly.YES);

        List<String> errors = new ArrayList<>();
        for (Product currentProduct : allProducts) {
            SapIntegrationClientImpl.SAPCompanyConfiguration configuration = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
            if(currentProduct.isExternalOnlyProduct() || currentProduct.isClinicalProduct()) {
                configuration = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES;
            }
            final QuotePriceItem byKeyFields = priceListCache.findByKeyFields(currentProduct.getPrimaryPriceItem());
            if(byKeyFields != null) {
                BigDecimal qsPrice = new BigDecimal(byKeyFields.getPrice());
                final SAPMaterial material = productPriceCache.findByProduct(currentProduct, configuration);
                if (material != null) {
                    BigDecimal sapPrice = new BigDecimal(material.getBasePrice());
                    if (sapPrice.compareTo(qsPrice) != 0) {
                        errors.add("Price for " + currentProduct.getPartNumber() + " sold in " + configuration
                                .getCompanyCode() + " does not match SAP: QS price is " +
                                   qsPrice.toString() + " and SAP price is " + sapPrice.toString());
                    }
                }
            }
        }
        System.out.println(StringUtils.join(errors,"\n"));
    }
}
