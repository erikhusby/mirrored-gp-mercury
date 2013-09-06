package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;


@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled=true)
public class BillingTrackerImporterContainerTest extends Arquillian {

    public static final String BILLING_TRACKER_TEST_FILENAME = "BillingTracker-ContaineFrTest.xlsx";

    @Inject
    private ProductDao productDao;

    @Inject
    private LedgerEntryDao ledgerEntryDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private PriceListCache priceListCache;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private Log logger;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    /**
     * Take the sheet names and create a new processor for each one.
     *
     * @param sheetNames The names of the sheets (should be all part numbers of products.
     *
     * @return The mapping of sheet names to processors.
     */
    private Map<String, BillingTrackerProcessor> getProcessors(List<String> sheetNames) {
        Map<String, BillingTrackerProcessor> processors = new HashMap<>();

        for (String sheetName : sheetNames) {
            BillingTrackerProcessor processor = new BillingTrackerProcessor(
                    sheetName, ledgerEntryDao, productDao, productOrderDao, priceItemDao, priceListCache, false);
            processors.put(sheetName, processor);
        }

        return processors;
    }

    @Test
    public void testImport() throws Exception {

        InputStream inputStream = null;

        try {
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(BILLING_TRACKER_TEST_FILENAME);

            // Should only be one sheet.
            List<String> sheetNames = PoiSpreadsheetParser.getWorksheetNames(inputStream);
            Assert.assertEquals(sheetNames.size(), 1, "Wrong number of worksheets");

            IOUtils.closeQuietly(inputStream);

            Map<String, BillingTrackerProcessor> processors = getProcessors(sheetNames);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(processors);

            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(BILLING_TRACKER_TEST_FILENAME);
            parser.processUploadFile(inputStream);

            List<String> validationErrors = new ArrayList<> ();
            for (BillingTrackerProcessor processor : processors.values()) {
                validationErrors.addAll(processor.getMessages());
            }

            if (!validationErrors.isEmpty()) {
                Assert.fail("Processing the billing tracker spreadsheet got errors: \n" +
                            StringUtils.join(validationErrors, "\n"));
            }

            // Check the RNA Data
            String rnaSheetName = "P-RNA-0004";
            BillingTrackerProcessor processor = processors.get(rnaSheetName);
            List<ProductOrder> productOrders = processor.getUpdatedProductOrders();
            Assert.assertTrue(CollectionUtils.isNotEmpty(productOrders), "Should have products");

            // There should be one Order for the RNA product data
            Assert.assertEquals(productOrders.size(), 1, "Should only be one product order");

            String rnaOrderId = "PDO-23";
            Assert.assertEquals(productOrders.get(0).getBusinessKey(), rnaOrderId, "Should have products");

            Set<Map.Entry<BillableRef, OrderBillSummaryStat>> entries =
                processor.getChargesMapByPdo().values().iterator().next().entrySet();

            // Primary Product data
            String rnaPriceItemName = "Strand Specific RNA-Seq (high coverage-50M paired reads)";
            OrderBillSummaryStat productStatData = getOrderBillSummaryStat(entries, rnaPriceItemName);
            Assert.assertEquals(productStatData.getCharge(), 0.0, "Charge mismatch");
            Assert.assertEquals(productStatData.getCredit(), 2.0, "Credit mismatch");

            // First AddOn data
            String rnaAddonPriceItemName = "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva";
            productStatData = getOrderBillSummaryStat(entries, rnaAddonPriceItemName);
            Assert.assertEquals(productStatData.getCharge(), 4.0, "Charge mismatch");
            Assert.assertEquals(productStatData.getCredit(), 0.0, "Credit mismatch");

            // Second AddOn data
            String rnaSecondAddonPriceItemName = "DNA and RNA Extract from Fresh Frozen Tissue or stool";
            productStatData = getOrderBillSummaryStat(entries, rnaSecondAddonPriceItemName);
            Assert.assertEquals(productStatData.getCharge(), 2.0, "Charge mismatch");
            Assert.assertEquals(productStatData.getCredit(), 0.0, "Credit mismatch");

            // Third AddOn data
            String rnaThirdAddonPriceItemName = "RNA Extract from FFPE";
            productStatData = getOrderBillSummaryStat(entries, rnaThirdAddonPriceItemName);
            Assert.assertEquals(productStatData.getCharge(), 2.0, "Charge mismatch");
            Assert.assertEquals(productStatData.getCredit(), 0.0, "Credit mismatch");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private OrderBillSummaryStat getOrderBillSummaryStat(Set<Map.Entry<BillableRef, OrderBillSummaryStat>> entries,
                                                         String rnaPriceItemName) {
        // Find the price item.
        Map.Entry<BillableRef, OrderBillSummaryStat> entry = null;
        Iterator<Map.Entry<BillableRef, OrderBillSummaryStat>> entryIterator = entries.iterator();
        while ((entry == null) && entryIterator.hasNext()) {
            entry = entryIterator.next();
            if (!entry.getKey().getPriceItemName().equals(rnaPriceItemName)) {
                entry = null;
            }
        }
        Assert.assertNotNull(entry, "Could not find the matching price item for: " + rnaPriceItemName);
        return entry.getValue();
    }

}
