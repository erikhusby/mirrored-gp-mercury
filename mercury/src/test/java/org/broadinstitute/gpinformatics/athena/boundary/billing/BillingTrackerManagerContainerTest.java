package org.broadinstitute.gpinformatics.athena.boundary.billing;

import net.sourceforge.stripes.validation.ValidationErrors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntryTest;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class BillingTrackerManagerContainerTest extends ContainerTest {

    public static final String BILLING_TRACKER_TEST_FILENAME = "BillingTracker-ContainerTest.xlsx";

    @Inject
    LedgerEntryDao ledgerEntryDao;

    @Inject
    ProductDao productDao;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    PriceItemDao priceItemDao;

    @Inject
    PriceListCache priceListCache;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    /**
     * Take the sheet names and create a new processor for each one.
     *
     * @param validationErrors Collect all errors.
     * @param sheetNames The names of the sheets (should be all part numbers of products.
     * @param doPersist Preview mode does not persist, but saving does.
     *
     * @return The mapping of sheet names to processors.
     */
    private Map<String, BillingTrackerProcessor> getProcessors(
            ValidationErrors validationErrors, List<String> sheetNames, boolean doPersist) {
        Map<String, BillingTrackerProcessor> processors = new HashMap<> ();

        for (String sheetName : sheetNames) {
            BillingTrackerProcessor processor = new BillingTrackerProcessor(
                    sheetName, ledgerEntryDao, productDao, productOrderDao, priceItemDao, priceListCache,
                    validationErrors, doPersist);
            processors.put(sheetName, processor);
        }

        return processors;
    }

    // This test is too sensitive to actual data and broke because some new ledger entries were added on prod. Need
    // to create product order and upload as part of a more complete test.
    @Test(enabled = false)
    public void testImport() throws Exception {

        FileInputStream fis = null;

        // Create a copy of the deployed test data file.
        try {
            fis = (FileInputStream) Thread.currentThread().getContextClassLoader().getResourceAsStream(BILLING_TRACKER_TEST_FILENAME);

            ValidationErrors validationErrors = new ValidationErrors();

            // Should only be one sheet.
            List<String> sheetNames = PoiSpreadsheetParser.getWorksheetNames(fis);
            Assert.assertEquals(sheetNames.size(), 1, "Wrong number of worksheets");

            String rnaSheetName = "P-RNA-0004";
            Assert.assertEquals(sheetNames.get(0), rnaSheetName, "Unexpected sheet name")
            ;
            Map<String, BillingTrackerProcessor> processors = getProcessors(validationErrors, sheetNames, false);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(processors);

            IOUtils.closeQuietly(fis);
            fis = (FileInputStream) Thread.currentThread().getContextClassLoader().getResourceAsStream(BILLING_TRACKER_TEST_FILENAME);

            parser.processUploadFile(fis);

            // There should be one Order for the RNA product data.
            BillingTrackerProcessor processor = processors.get(rnaSheetName);
            List<ProductOrder> productOrders = processor.getUpdatedProductOrders();
            Assert.assertEquals(productOrders.size(), 1, "Should only be one product order");

            ProductOrder productOrder = productOrders.get(0);
            Set<LedgerEntry> ledgerSet = ledgerEntryDao.findByOrderList(productOrder);

            // There should be ledger entries.
            Assert.assertFalse(ledgerSet.isEmpty(), "Must have ledger entries");
            List<LedgerEntry> ledgerList = new ArrayList<>(ledgerSet);
            Collections.sort(ledgerList, new Comparator<LedgerEntry>() {
                @Override
                public int compare(LedgerEntry o1, LedgerEntry o2) {
                    return new CompareToBuilder().append(o1.getProductOrderSample().getSampleName(),
                            o2.getProductOrderSample().getSampleName()).append(
                            o1.getPriceItem().getName(), o2.getPriceItem().getName()).build();
                }
            });

            LedgerEntry[] expectedLedgerEntryList = createExpectedLedgerEntryList();

            if (expectedLedgerEntryList.length != ledgerList.size()) {
                // Dumping the found ledger Items to log file before failing.
                StringBuilder stringBuilder = new StringBuilder();
                for (LedgerEntry expected : expectedLedgerEntryList) {
                    for (LedgerEntry actual : ledgerList) {
                        if (actual.getProductOrderSample().getSampleName().equals(expected.getProductOrderSample().getSampleName()) &&
                            actual.getPriceItem().getName().equals(expected.getPriceItem().getName())) {
                            stringBuilder.append("Found ledger item for ")
                                    .append(expected.getProductOrderSample().getSampleName())
                                    .append(" and ").append(expected.getPriceItem().getName())
                                    .append(" quantity ").append(expected.getQuantity()).append("\n");
                            break;
                        }
                    }
                }

                Assert.fail( "The number of expected ledger items is different than the number of actual ledger items\n\n" + stringBuilder.toString() );
            }

            int i = 0;
            for (LedgerEntry actual : ledgerList) {
                LedgerEntry expected = expectedLedgerEntryList[i];

                Assert.assertEquals(actual.getProductOrderSample().getSampleName(),
                        expected.getProductOrderSample().getSampleName(), "Unexpected sample name");
                Assert.assertEquals(actual.getPriceItem().getName(), expected.getPriceItem().getName(), "Unexpected price item");
                Assert.assertEquals(actual.getQuantity(), expected.getQuantity(),
                        "Quantity check for " + actual.getProductOrderSample().getSampleName() + " priceItem "  +
                                        actual.getPriceItem().getName() + " failed");
                i++;
            }
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    private static LedgerEntry[] createExpectedLedgerEntryList() {
        return new LedgerEntry[] {
            // SM-3KBZD
            LedgerEntryTest.createOneLedgerEntry("SM-3KBZD",
                    "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva", 1),
            LedgerEntryTest.createOneLedgerEntry("SM-3KBZD",
                    "RNA Extract from FFPE", -1.5),

            // SM-3KBZE
            LedgerEntryTest.createOneLedgerEntry("SM-3KBZE",
                    "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva", 1),
            LedgerEntryTest.createOneLedgerEntry("SM-3KBZE",
                    "RNA Extract from FFPE", -1.5),
            LedgerEntryTest.createOneLedgerEntry("SM-3KBZE",
                    "Strand Specific RNA-Seq (high coverage-50M paired reads)", 1),

            // SM-3MPJX
            LedgerEntryTest.createOneLedgerEntry("SM-3MPJX",
                    "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva", 1),
            LedgerEntryTest.createOneLedgerEntry("SM-3MPJX",
                    "RNA Extract from FFPE", -1.5),
            LedgerEntryTest.createOneLedgerEntry("SM-3MPJX",
                    "Strand Specific RNA-Seq (high coverage-50M paired reads)", 1),

            // SM-3MPJY
            LedgerEntryTest.createOneLedgerEntry("SM-3MPJY",
                    "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva", 1),
            LedgerEntryTest.createOneLedgerEntry("SM-3MPJY",
                    "RNA Extract from FFPE", -1.5),
            LedgerEntryTest.createOneLedgerEntry("SM-3MPJY",
                    "Strand Specific RNA-Seq (high coverage-50M paired reads)", 1)
        };
    }
}
