package org.broadinstitute.gpinformatics.athena.boundary.billing;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedgerTest;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.FileInputStream;
import java.util.*;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;


@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled=true)
public class BillingTrackerManagerContainerTest extends Arquillian {

    public static final String BILLING_TRACKER_TEST_FILENAME = new String("BillingTracker-ContainerTest.xlsx");

    private Log logger = LogFactory.getLog(BillingTrackerManagerContainerTest.class);

    @Inject
    BillingTrackerManager billingTrackerManager;

    @Inject
    BillingLedgerDao billingLedgerDao;

    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

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


    @Test( enabled = true )
    public void testImport() throws Exception {

        FileInputStream fis=null;

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
            Set<BillingLedger> ledgerSet = billingLedgerDao.findByOrderList(new ProductOrder[]{productOrder});
            // There should be ledger entries
            Assert.assertFalse(ledgerSet.isEmpty());
            BillingLedger[] ledgerArray = ledgerSet.toArray(new BillingLedger[0]);
            List<BillingLedger> ledgerList = Arrays.asList(ledgerArray);
            Collections.sort(ledgerList, new Comparator<BillingLedger>() {
                @Override
                public int compare(BillingLedger o1, BillingLedger o2) {
                    return new CompareToBuilder().append(o1.getProductOrderSample().getSampleName(),
                            o2.getProductOrderSample().getSampleName()).append(
                            o1.getPriceItem().getName(), o2.getPriceItem().getName()).build();
                }
            });

            List<BillingLedger> expectedBillingLedgerList = createExpectedBillingLedgerList1();

            if ( expectedBillingLedgerList.size() != ledgerList.size() ) {
                // Dumping the found ledger Items to log file before failing.
                StringBuilder stringBuilder = new StringBuilder();
                for ( BillingLedger expBillingLedger : expectedBillingLedgerList ) {
                    for ( BillingLedger actBillingLedger : ledgerList ) {
                        if ( actBillingLedger.getProductOrderSample().getSampleName().equals( expBillingLedger.getProductOrderSample().getSampleName()) &&
                                actBillingLedger.getPriceItem().getName().equals( expBillingLedger.getPriceItem().getName())  ) {
                            logger.debug( "Found ledger item for " + expBillingLedger.getProductOrderSample().getSampleName() +
                                    " and " + expBillingLedger.getPriceItem().getName() + " quantity " +  + expBillingLedger.getQuantity() );
                            break;
                        }
                    }
                }
                Assert.fail( "The number of expected ledger items is different than the number of actual ledger items");
            }

            int i = 0;
            for ( BillingLedger billingLedger : ledgerList ) {
                BillingLedger expBillingLedger = expectedBillingLedgerList.get(i);

                Assert.assertEquals(expBillingLedger.getProductOrderSample().getSampleName(), billingLedger.getProductOrderSample().getSampleName() );
                Assert.assertEquals(expBillingLedger.getPriceItem().getName(), billingLedger.getPriceItem().getName() );
                Assert.assertEquals("Quantity check for " + billingLedger.getProductOrderSample().getSampleName() + " priceItem "  +
                        billingLedger.getPriceItem().getName() + " failed",
                        expBillingLedger.getQuantity(), billingLedger.getQuantity() );
                i++;
            }
        } catch ( Exception e ) {
            logger.error(e);
            Assert.fail( e.getMessage() );
        } finally {
            IOUtils.closeQuietly(fis);
        }

    }


    private BillingLedger createOneBillingLedger(String sampleName, String priceItemName, double quantity ) {
        return BillingLedgerTest.createOneBillingLedger(sampleName, priceItemName, quantity);
    }

    private List<BillingLedger> createExpectedBillingLedgerList1 () {
        List<BillingLedger> expList = new ArrayList<BillingLedger>();

        //SM-3KBZD
        {
            BillingLedger billingLedgerExp = BillingLedgerTest.createOneBillingLedger("SM-3KBZD", "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva", 1);
            expList.add( billingLedgerExp );
        }
        {
            BillingLedger billingLedgerExp = BillingLedgerTest.createOneBillingLedger("SM-3KBZD", "RNA Extract from FFPE", -1.5);
            expList.add( billingLedgerExp );
        }
        //SM-3KBZE
        {
            BillingLedger billingLedgerExp = BillingLedgerTest.createOneBillingLedger("SM-3KBZE", "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva", 1);
            expList.add( billingLedgerExp );
        }
        {
            BillingLedger billingLedgerExp = BillingLedgerTest.createOneBillingLedger("SM-3KBZE", "RNA Extract from FFPE", -1.5);
            expList.add( billingLedgerExp );
        }
        {
            BillingLedger billingLedgerExp = BillingLedgerTest.createOneBillingLedger("SM-3KBZE", "Strand Specific RNA-Seq (high coverage-50M paired reads)", 1);
            expList.add( billingLedgerExp );
        }
        //SM-3MPJX
        {
            BillingLedger billingLedgerExp = BillingLedgerTest.createOneBillingLedger("SM-3MPJX", "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva", 1);
            expList.add( billingLedgerExp );
        }
        {
            BillingLedger billingLedgerExp = BillingLedgerTest.createOneBillingLedger("SM-3MPJX", "RNA Extract from FFPE", -1.5);
            expList.add( billingLedgerExp );
        }
        {
            BillingLedger billingLedgerExp = BillingLedgerTest.createOneBillingLedger("SM-3MPJX", "Strand Specific RNA-Seq (high coverage-50M paired reads)", 1);
            expList.add( billingLedgerExp );
        }
        //SM-3MPJY
        {
            BillingLedger billingLedgerExp = BillingLedgerTest.createOneBillingLedger("SM-3MPJY", "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva", 1);
            expList.add( billingLedgerExp );
        }
        {
            BillingLedger billingLedgerExp = BillingLedgerTest.createOneBillingLedger("SM-3MPJY", "RNA Extract from FFPE", -1.5);
            expList.add( billingLedgerExp );
        }
        {
            BillingLedger billingLedgerExp = BillingLedgerTest.createOneBillingLedger("SM-3MPJY", "Strand Specific RNA-Seq (high coverage-50M paired reads)", 1);
            expList.add( billingLedgerExp );
        }
        return expList;
    }


}
