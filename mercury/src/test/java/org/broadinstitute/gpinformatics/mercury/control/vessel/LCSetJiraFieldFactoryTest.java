package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.testng.Assert;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Scott Matthews
 *         Date: 12/7/12
 *         Time: 1:20 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class LCSetJiraFieldFactoryTest {

    private String                             pdoBusinessName;
    private List<String>                       pdoNames;
    private String                             workflowName;
    private Map<String, TwoDBarcodedTube>      mapBarcodeToTube;
    private String                             rpSynopsis;
    private Map<String, CustomFieldDefinition> jiraFieldDefs;

    @BeforeMethod
    public void startUp() throws IOException {
        pdoBusinessName = "PDO-999";

        pdoNames = new ArrayList<String>();
        Collections.addAll(pdoNames, pdoBusinessName);

        workflowName = WorkflowName.EXOME_EXPRESS.getWorkflowName();
        mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();

        Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<String, ProductOrder>();

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
        rpSynopsis = "Test synopsis";
        ProductOrder productOrder = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123",
                                                     new Product("Test product",
                                                                 new ProductFamily("Test product family"), "test",
                                                                 "1234", null, null, 10000, 20000, 100, 40, null, null,
                                                                 true, workflowName, false, "agg type"),
                                                     new ResearchProject(101L, "Test RP", rpSynopsis, false));
        productOrder.setJiraTicketKey(pdoBusinessName);
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
            bspAliquot.addBucketEntry(new BucketEntry(bspAliquot, pdoBusinessName));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        jiraFieldDefs = JiraServiceProducer.stubInstance().getCustomFields();

    }

    @AfterMethod
    public void tearDown() {

    }

    public void testLCSetFieldGeneration() throws IOException {
        LabBatch testBatch = new LabBatch(LabBatch.generateBatchName(workflowName, pdoNames),
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        testBatch.setWorkflowName("Exome Express");

        Set<LabVessel> reworks = new HashSet<LabVessel>();
        reworks.add(new TwoDBarcodedTube("Rework1"));
        reworks.add(new TwoDBarcodedTube("Rework2"));
        testBatch.addReworks(reworks);

        int numSamples = testBatch.getStartingLabVessels().size() + testBatch.getReworks().size();

        AbstractBatchJiraFieldFactory testBuilder = AbstractBatchJiraFieldFactory
                .getInstance(CreateFields.ProjectType.LCSET_PROJECT, testBatch, AthenaClientProducer.stubInstance());

        Assert.assertEquals("6 samples from MyResearchProject PDO-999\n", testBuilder.generateDescription());

        Collection<CustomField> generatedFields = testBuilder.getCustomFields(jiraFieldDefs);

        Assert.assertEquals(6, generatedFields.size());

        for (CustomField currField : generatedFields) {
            if (currField.getFieldDefinition().getName()
                         .equals(LabBatch.RequiredSubmissionFields.WORK_REQUEST_IDS.getFieldName())) {
                Assert.assertEquals("N/A",(String) currField.getValue());
            }
            if (currField.getFieldDefinition().getName()
                         .equals(LabBatch.RequiredSubmissionFields.GSSR_IDS.getFieldName())) {
                for (LabVessel currVessel : testBatch.getStartingLabVessels()) {
                    for (String sampleName : currVessel.getSampleNames()) {
                        Assert.assertTrue(((String) currField.getValue()).contains(sampleName));
                    }
                }
                Assert.assertFalse(testBatch.getReworks().isEmpty());
                for (LabVessel currVessel : testBatch.getReworks()) {
                    for (String sampleName : currVessel.getSampleNames()) {
                        Assert.assertTrue(((String) currField.getValue()).contains(sampleName));
                    }
                }
            }
            if (currField.getFieldDefinition().getName()
                         .equals(LabBatch.RequiredSubmissionFields.LIBRARY_QC_SEQUENCING_REQUIRED.getFieldName())) {
                Assert.assertEquals(((CustomField.SelectOption) currField.getValue()).getId(), "-1");
            }
            if (currField.getFieldDefinition().getName().equals(LabBatch.RequiredSubmissionFields.NUMBER_OF_SAMPLES.getFieldName())) {
                Assert.assertEquals(numSamples, currField.getValue());
            }
            if (currField.getFieldDefinition().getName().equals(LabBatch.RequiredSubmissionFields.PROGRESS_STATUS.getFieldName())) {
                Assert.assertEquals(LCSetJiraFieldFactory.PROGRESS_STATUS, ((CustomField.ValueContainer)currField.getValue()).getValue());
            }
            if (currField.getFieldDefinition().getName().equals(LabBatch.RequiredSubmissionFields.PROTOCOL.getFieldName())) {
                WorkflowLoader wfLoader = new WorkflowLoader();
                WorkflowConfig wfConfig = wfLoader.load();
                AthenaClientService athenaSvc = AthenaClientProducer.stubInstance();

                ProductWorkflowDef workflowDef = wfConfig.getWorkflowByName(
                        athenaSvc.retrieveProductOrderDetails(pdoBusinessName).getProduct().getWorkflowName());

                Assert.assertEquals(workflowDef.getName() +":"+ workflowDef.getEffectiveVersion().getVersion(), currField.getValue());

            }
        }
    }

    @Test(enabled = false)
    public void test_sample_field_text_with_reworks() {
        String expectedText = "SM-1\n\nSM-2 (rework)";

        Set<LabVessel> newTubes = new HashSet<LabVessel>();
        Set<LabVessel> reworks = new HashSet<LabVessel>();
        LabVessel tube1 = new  TwoDBarcodedTube("000012");
        tube1.addSample(new MercurySample("SM-1"));
        LabVessel tube2 = new TwoDBarcodedTube("000033");
        tube2.addSample(new MercurySample("SM-2"));
        newTubes.add(tube1);
        reworks.add(tube2);

        LabBatch batch = new LabBatch("test",newTubes, LabBatch.LabBatchType.WORKFLOW);

        batch.addReworks(reworks);

        String actualText = LCSetJiraFieldFactory.buildSamplesListString(batch);

        assertThat(actualText.trim(),equalTo(expectedText.trim()));
    }

    @Test
    public void test_sample_field_text_no_reworks() {
        String sampleKey = "SM-123";

        Set<LabVessel> newTubes = new HashSet<LabVessel>();
        LabVessel tube = new TwoDBarcodedTube("000012");
        tube.addSample(new MercurySample(sampleKey));
        newTubes.add(tube);

        LabBatch batch = new LabBatch("test",newTubes, LabBatch.LabBatchType.WORKFLOW);

        String actualText = LCSetJiraFieldFactory.buildSamplesListString(batch);

        assertThat(actualText.trim(),equalTo(sampleKey.trim()));
    }

}
