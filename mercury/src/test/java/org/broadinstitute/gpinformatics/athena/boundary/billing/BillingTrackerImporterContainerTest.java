package org.broadinstitute.gpinformatics.athena.boundary.billing;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
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


@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class BillingTrackerImporterContainerTest  extends Arquillian {

    public static final String BILLING_TRACKER_TEST_FILENAME = new String("BillingTracker-ContainerTest.xlsx");

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

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


    @Test
    public void testImport() throws Exception {

        InputStream inputStream = null;
        BillingTrackerImporter billingTrackerImporter = new BillingTrackerImporter(productOrderDao, productOrderSampleDao);

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
            // There should be three billable items for this order - Primary Product and and two Addons.
            Assert.assertEquals(3, rnaBillingOrderDataByBillableRef.size());

            // Primary Product data
            String rnaProductName = rnaSheetName;
            String rnaPriceItemName = "Strand Specific RNA-Seq (high coverage-50M paired reads)";
            BillableRef rnaBillableRef = new BillableRef(rnaProductName, rnaPriceItemName);
            OrderBillSummaryStat rnaPrimaryProductStatData = rnaBillingOrderDataByBillableRef.get(rnaBillableRef);
            Assert.assertEquals(4.0, rnaPrimaryProductStatData.getCharge());

            // First AddOn data
            String rnaAddonName = "P-ESH-0004";
            String rnaAddonPriceItemName = "DNA Extract from Blood, Fresh Frozen Tissue, cell pellet, stool, saliva";
            BillableRef rnaAddonBillableRef = new BillableRef(rnaAddonName, rnaAddonPriceItemName);
            OrderBillSummaryStat rnaAddonStatData = rnaBillingOrderDataByBillableRef.get(rnaAddonBillableRef);
            Assert.assertEquals(4.0, rnaAddonStatData.getCharge());

            // Second AddOn data
            String rnaSecondAddonName = "P-ESH-0008";
            String rnaSecondAddonPriceItemName = "RNA Extract from FFPE";
            BillableRef rnaSecondAddonBillableRef = new BillableRef(rnaSecondAddonName, rnaSecondAddonPriceItemName);
            OrderBillSummaryStat rnaSecondAddonStatData = rnaBillingOrderDataByBillableRef.get(rnaSecondAddonBillableRef);
            Assert.assertEquals(2.0, rnaSecondAddonStatData.getCharge());


//        // Check the ExomeExpress Data
//        String exSheetName = "P-EXEX-0001";
//        Map<String, Map<String, OrderBillSummaryStat>> exBillingDataByOrderId = billingDataSummaryMapByPartNumber.get(exSheetName);
//        Assert.assertNotNull(exBillingDataByOrderId);
//        Assert.assertEquals(1, exBillingDataByOrderId.size());
//
//        String exOrderId = "PDO-24";
//        Map<String, OrderBillSummaryStat>  exBillingOrderDataByProduct = exBillingDataByOrderId.get(exOrderId);
//        Assert.assertNotNull(exBillingOrderDataByProduct);
//        // There should be only one billable item.  Just the primary product no Addon.
//        Assert.assertEquals(1, exBillingOrderDataByProduct.size());
//        String exPrimaryProductName = exSheetName;
//        OrderBillSummaryStat exStatData = exBillingOrderDataByProduct.get(exPrimaryProductName);
//        // Eight units of charges appear per sample for this primary product
//        Assert.assertEquals(32.0, exStatData.getCharge());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

}
