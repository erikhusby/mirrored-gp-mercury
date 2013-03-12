package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.testng.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedgerTest;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.FileInputStream;
import java.util.*;


@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class BillingTrackerManagerContainerTest extends ContainerTest {

    public static final String BILLING_TRACKER_TEST_FILENAME = "BillingTracker-ContainerTest.xlsx";

    private static final Log logger = LogFactory.getLog(BillingTrackerManagerContainerTest.class);

    @Inject
    BillingTrackerManager billingTrackerManager;

    @Inject
    BillingLedgerDao billingLedgerDao;

    @Inject
    private UserTransaction utx;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    // This test is too sensitive to actual data and broke because some new ledger entries were added on prod. Need
    // to create product order and upload as part of a more complete test
    @Test(enabled = false)
    public void testImport() throws Exception {

        FileInputStream fis = null;

        // Create a copy of the deployed test data file
        try {
            fis = (FileInputStream) Thread.currentThread().getContextClassLoader().getResourceAsStream(BILLING_TRACKER_TEST_FILENAME);

            Map<String, List<ProductOrder>>  billedProductOrdersMapByPartNumber = billingTrackerManager.parseFileForBilling(fis);
            Assert.assertNotNull(billedProductOrdersMapByPartNumber);
            // Should only be one sheet
            Assert.assertEquals(1, billedProductOrdersMapByPartNumber.size());

            // Check the RNA sheet
            String rnaSheetName = "P-RNA-0004";
            List<ProductOrder> rnaProductOrders = billedProductOrdersMapByPartNumber.get(rnaSheetName);
            Assert.assertNotNull(rnaProductOrders);

            // There should be one Order for the RNA product data
            Assert.assertEquals(1, rnaProductOrders.size());
            ProductOrder productOrder = rnaProductOrders.get(0);
            Set<BillingLedger> ledgerSet = billingLedgerDao.findByOrderList(productOrder);
            // There should be ledger entries
            Assert.assertFalse(ledgerSet.isEmpty());
            List<BillingLedger> ledgerList = new ArrayList<BillingLedger>(ledgerSet);
            Collections.sort(ledgerList, new Comparator<BillingLedger>() {
                @Override
                public int compare(BillingLedger o1, BillingLedger o2) {
                    return new CompareToBuilder().append(o1.getProductOrderSample().getSampleName(),
                            o2.getProductOrderSample().getSampleName()).append(
                            o1.getPriceItem().getName(), o2.getPriceItem().getName()).build();
                }
            });

            BillingLedger[] expectedBillingLedgerList = createExpectedBillingLedgerList();

            if (expectedBillingLedgerList.length != ledgerList.size()) {
                // Dumping the found ledger Items to log file before failing.
                StringBuilder stringBuilder = new StringBuilder();
                for (BillingLedger expected : expectedBillingLedgerList) {
                    for (BillingLedger actual : ledgerList) {
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
            for (BillingLedger actual : ledgerList) {
                BillingLedger expected = expectedBillingLedgerList[i];

                Assert.assertEquals(expected.getProductOrderSample().getSampleName(),
                        actual.getProductOrderSample().getSampleName());
                Assert.assertEquals(expected.getPriceItem().getName(), actual.getPriceItem().getName());
                Assert.assertEquals(expected.getQuantity(),
                        actual.getQuantity(),
                        "Quantity check for " + actual.getProductOrderSample().getSampleName() + " priceItem "  +
                                        actual.getPriceItem().getName() + " failed");
                i++;
            }
        } finally {
            IOUtils.closeQuietly(fis);
        }

    }


    private static BillingLedger[] createExpectedBillingLedgerList() {
        return new BillingLedger[] {
                // SM-3KBZD
                BillingLedgerTest.createOneBillingLedger("SM-3KBZD", "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva",
                        1),
                BillingLedgerTest.createOneBillingLedger("SM-3KBZD", "RNA Extract from FFPE", -1.5),
                // SM-3KBZE
                BillingLedgerTest.createOneBillingLedger("SM-3KBZE", "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva",
                        1),
                BillingLedgerTest.createOneBillingLedger("SM-3KBZE", "RNA Extract from FFPE", -1.5),
                BillingLedgerTest.createOneBillingLedger("SM-3KBZE", "Strand Specific RNA-Seq (high coverage-50M paired reads)",
                        1),
                // SM-3MPJX
                BillingLedgerTest.createOneBillingLedger("SM-3MPJX", "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva",
                        1),
                BillingLedgerTest.createOneBillingLedger("SM-3MPJX", "RNA Extract from FFPE", -1.5),
                BillingLedgerTest.createOneBillingLedger("SM-3MPJX", "Strand Specific RNA-Seq (high coverage-50M paired reads)",
                        1),
                // SM-3MPJY
                BillingLedgerTest.createOneBillingLedger("SM-3MPJY", "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva",
                        1),
                BillingLedgerTest.createOneBillingLedger("SM-3MPJY", "RNA Extract from FFPE", -1.5),
                BillingLedgerTest.createOneBillingLedger("SM-3MPJY", "Strand Specific RNA-Seq (high coverage-50M paired reads)",
                        1)
        };
    }
}
