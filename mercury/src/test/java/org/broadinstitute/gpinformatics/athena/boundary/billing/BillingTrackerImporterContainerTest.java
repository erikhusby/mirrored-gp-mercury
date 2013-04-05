package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.testng.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
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
import java.io.InputStream;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;


@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled=true)
public class BillingTrackerImporterContainerTest extends Arquillian {

    public static final String BILLING_TRACKER_TEST_FILENAME = "BillingTracker-ContainerTest.xlsx";

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private PriceListCache priceListCache;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private Log logger;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

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


    @Test
    public void testImport() throws Exception {

        InputStream inputStream = null;
        BillingTrackerImporter billingTrackerImporter = new BillingTrackerImporter(productOrderDao, priceListCache);

        try {
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(BILLING_TRACKER_TEST_FILENAME);

            Map<String, Map<String, Map<BillableRef, OrderBillSummaryStat>>> billingDataSummaryMapByPartNumber = billingTrackerImporter.parseFileForSummaryMap(inputStream);
            Assert.assertNotNull(billingDataSummaryMapByPartNumber);
            // Should only be one sheet
            Assert.assertEquals(1, billingDataSummaryMapByPartNumber.size());

            // Check the RNA Data
            String rnaSheetName = "P-RNA-0004";
            Map<String, Map<BillableRef, OrderBillSummaryStat>> rnaBillingDataByOrderId = billingDataSummaryMapByPartNumber.get(rnaSheetName);
            Assert.assertNotNull(rnaBillingDataByOrderId);

            // There should be one Order for the RNA product data
            Assert.assertEquals(1, rnaBillingDataByOrderId.size());
            String rnaOrderId = "PDO-23";
            Map<BillableRef, OrderBillSummaryStat> rnaBillingOrderDataByBillableRef = rnaBillingDataByOrderId.get(rnaOrderId);
            Assert.assertNotNull(rnaBillingOrderDataByBillableRef);
            // There should be three billable items for this order
            Assert.assertFalse(rnaBillingOrderDataByBillableRef.isEmpty());

            // Primary Product data
            String rnaPriceItemName = "Strand Specific RNA-Seq (high coverage-50M paired reads)";
            BillableRef rnaBillableRef = new BillableRef(rnaSheetName, rnaPriceItemName);
            OrderBillSummaryStat rnaPrimaryProductStatData = rnaBillingOrderDataByBillableRef.get(rnaBillableRef);
            Assert.assertEquals(3.0, rnaPrimaryProductStatData.getCharge());
            Assert.assertEquals(0.0, rnaPrimaryProductStatData.getCredit());

            // First AddOn data
            String rnaAddonName = "P-ESH-0004";
            String rnaAddonPriceItemName = "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva";
            BillableRef rnaAddonBillableRef = new BillableRef(rnaAddonName, rnaAddonPriceItemName);
            OrderBillSummaryStat rnaAddonStatData = rnaBillingOrderDataByBillableRef.get(rnaAddonBillableRef);
            Assert.assertEquals(4.0, rnaAddonStatData.getCharge());
            Assert.assertEquals(0.0, rnaAddonStatData.getCredit());

            // Second AddOn data
            String rnaSecondAddonName = "P-ESH-0008";
            String rnaSecondAddonPriceItemName = "RNA Extract from FFPE";
            BillableRef rnaSecondAddonBillableRef = new BillableRef(rnaSecondAddonName, rnaSecondAddonPriceItemName);
            OrderBillSummaryStat rnaSecondAddonStatData = rnaBillingOrderDataByBillableRef.get(rnaSecondAddonBillableRef);
            Assert.assertEquals(0.0, rnaSecondAddonStatData.getCharge());
            Assert.assertEquals(-6.0, rnaSecondAddonStatData.getCredit());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

}
