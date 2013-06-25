package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Scott Matthews
 *         Date: 12/7/12
 *         Time: 11:49 AM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class LabBatchTest {

    private String testLCSetTicketKey;
    private String pdoBusinessName;
    private List<String> pdoNames;
    private String workflowName;
    private Map<String, TwoDBarcodedTube> mapBarcodeToTube;

    @BeforeMethod
    public void setUp() {

        pdoBusinessName = "PD0-999";

        pdoNames = new ArrayList<String>();
        Collections.addAll(pdoNames, pdoBusinessName);

        workflowName = WorkflowName.EXOME_EXPRESS.getWorkflowName();
        mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();

        Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<String, ProductOrder>();

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
        ProductOrder productOrder = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123", new Product(
                "Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000, 20000, 100,
                40, null, null, true, workflowName, false, "agg type"),
                new ResearchProject(101L, "Test RP", "Test synopsis",
                        false));
        productOrder.setJiraTicketKey(pdoBusinessName);
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        mapKeyToProductOrder.put(pdoBusinessName, productOrder);

        List<String> vesselSampleList = new ArrayList<String>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");

        // starting rack
        for (int sampleIndex = 1; sampleIndex <= vesselSampleList.size(); sampleIndex++) {
            String barcode = "R" + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex;
            String bspStock = vesselSampleList.get(sampleIndex - 1);
            productOrderSamples.add(new ProductOrderSample(bspStock));
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        testLCSetTicketKey = "LCSET-321";
    }

    @AfterMethod
    public void tearDown() {

    }

    public void testDefaultLabBatch() {

        LabBatch testBatch = new LabBatch();

        Assert.assertNull(testBatch.getBatchName());

        Assert.assertNotNull(testBatch.getStartingBatchLabVessels());
        Assert.assertTrue(testBatch.getStartingBatchLabVessels().isEmpty());

        Assert.assertNotNull(testBatch.getLabEvents());
        Assert.assertTrue(testBatch.getLabEvents().isEmpty());

        Assert.assertTrue(testBatch.getActive());

        Assert.assertNull(testBatch.getJiraTicket());

        Assert.assertNull(testBatch.getCreatedOn());

    }

    public void testLabBatchCreation() {


        LabBatch testBatch = new LabBatch(LabBatch.generateBatchName(workflowName, pdoNames),
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);


        Assert.assertNotNull(testBatch.getBatchName());
        Assert.assertEquals(workflowName + ": " + pdoBusinessName, testBatch.getBatchName());

        Assert.assertNotNull(testBatch.getStartingBatchLabVessels());
        Assert.assertEquals(6, testBatch.getStartingBatchLabVessels().size());

        Assert.assertNotNull(testBatch.getLabEvents());
        Assert.assertTrue(testBatch.getLabEvents().isEmpty());

        Assert.assertTrue(testBatch.getActive());

        Assert.assertNull(testBatch.getJiraTicket());

        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

        Assert.assertNotNull(testBatch.getCreatedOn());
        Assert.assertEquals(formatter.format(new Date()), formatter.format(testBatch.getCreatedOn()));

        testBatch.setJiraTicket(new JiraTicket(JiraServiceProducer.stubInstance(), testLCSetTicketKey));

        Assert.assertNotNull(testBatch.getJiraTicket());

        Assert.assertNotNull(testBatch.getJiraTicket().getLabBatch());

        Assert.assertEquals(testBatch, testBatch.getJiraTicket().getLabBatch());

    }
}
