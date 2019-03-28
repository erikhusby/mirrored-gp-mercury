package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Scott Matthews
 *         Date: 12/7/12
 *         Time: 1:20 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class LCSetJiraFieldFactoryTest {

    private List<String> pdoNames;
    private String workflow;
    private Map<String, BarcodedTube> mapBarcodeToTube;
    private String rpSynopsis;
    private Map<String, CustomFieldDefinition> jiraFieldDefs;
    private ProductOrder testProductOrder;
    @MockitoAnnotations.Mock
    private ProductOrderDao productOrderDao;
    private ProductOrder singleSampleOrder;
    private String testOrderKey;
    private String singleSampleTestOrder = "PDO-7";
    public Bucket bucket;

    @BeforeMethod
    public void startUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        testOrderKey = "PDO-999";
        testProductOrder =
                ProductOrderTestFactory.createDummyProductOrder(5,testOrderKey);
        singleSampleOrder = ProductOrderTestFactory.createDummyProductOrder(singleSampleTestOrder);

        Mockito.when(productOrderDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] arguments = invocationOnMock.getArguments();
                ProductOrder foundOrder = null;
                if ((arguments[0]).equals(testOrderKey)) {
                    foundOrder = testProductOrder;
                } else {
                    foundOrder = singleSampleOrder;
                }
                return foundOrder;
            }
        });

        pdoNames = new ArrayList<>();
        Collections.addAll(pdoNames, testProductOrder.getBusinessKey());

        workflow = Workflow.AGILENT_EXOME_EXPRESS;
        mapBarcodeToTube = new LinkedHashMap<>();

        List<ProductOrderSample> vesselSampleList = new ArrayList<>();

        final List<ProductOrderSample> testProductOrderSamples = testProductOrder.getSamples();
        CollectionUtils.addAll(vesselSampleList, singleSampleOrder.getSamples());
        CollectionUtils.addAll(vesselSampleList, testProductOrderSamples);
        bucket = new Bucket("Pico/Plating Bucket");
        // starting rack
        for (int sampleIndex = 1; sampleIndex <= vesselSampleList.size(); sampleIndex++) {
            String barcode = "R" + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex;
            ProductOrderSample currentProductOrderSample = vesselSampleList.get(sampleIndex - 1);

            String bspStock = currentProductOrderSample.getName();

            BarcodedTube bspAliquot = new BarcodedTube(barcode);
            MercurySample mercurySample = new MercurySample(bspStock, MercurySample.MetadataSource.BSP);
            mercurySample.addProductOrderSample(currentProductOrderSample);
            mercurySample.addLabVessel(bspAliquot);
            bucket.addEntry(currentProductOrderSample.getProductOrder(), bspAliquot,
                    BucketEntry.BucketEntryType.PDO_ENTRY, new Date());
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        testProductOrderSamples.get(testProductOrderSamples.size()-2)
                .setManualOnRisk(new RiskCriterion(RiskCriterion.RiskCriteriaType.CONCENTRATION, Operator.GREATER_THAN, "20"),
                        "Test risk on the second to last sample to ensure proper display");

        testProductOrderSamples.get(testProductOrderSamples.size()-1)
                .setManualOnRisk(new RiskCriterion(RiskCriterion.RiskCriteriaType.FFPE, Operator.IS, "true"),
                        "Test risk on the final sample to ensure proper display");

        jiraFieldDefs = JiraServiceTestProducer.stubInstance().getCustomFields();

    }

    @AfterMethod
    public void tearDown() {

    }

    public void testLCSetFieldGeneration() throws IOException {
        LabBatch testBatch = new LabBatch(LabBatch.generateBatchName(workflow, pdoNames),
                                          new HashSet<LabVessel>(mapBarcodeToTube.values()),
                                          LabBatch.LabBatchType.WORKFLOW);
        for (BucketEntry bucketEntry : bucket.getBucketEntries()) {
            testBatch.addBucketEntry(bucketEntry);
        }

        testBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        testBatch.setBatchDescription("Batch Test Description");
        WorkflowConfig workflowConfig = new WorkflowLoader().load();

        Set<LabVessel> reworks = new HashSet<>();
        reworks.add(testProductOrder.getSamples().get(testProductOrder.getSamples().size()-3).getMercurySample().getLabVessel().iterator().next());
        reworks.add(testProductOrder.getSamples().get(testProductOrder.getSamples().size()-4).getMercurySample().getLabVessel().iterator().next());
        testBatch.addReworks(reworks);

        int numSamples = testBatch.getStartingBatchLabVessels().size();

        AbstractBatchJiraFieldFactory testBuilder = AbstractBatchJiraFieldFactory.getInstance(
                CreateFields.ProjectType.LCSET_PROJECT, testBatch, null, productOrderDao, workflowConfig);

        Assert.assertEquals(testBuilder.generateDescription(),
                "1 sample with material types [] from MyResearchProject PDO-7\n5 samples with material types [] from MyResearchProject PDO-999\n");

        Collection<CustomField> generatedFields = testBuilder.getCustomFields(jiraFieldDefs);

        Assert.assertEquals(generatedFields.size(), 10);

        for (CustomField field : generatedFields) {

            String fieldDefinitionName = field.getFieldDefinition().getName();
            if (fieldDefinitionName.equals(LabBatch.TicketFields.WORK_REQUEST_IDS.getName())) {
                Assert.assertEquals("N/A", (String) field.getValue());
            }
            if (fieldDefinitionName.equals(LabBatch.TicketFields.GSSR_IDS.getName())) {
                for (LabVessel currVessel : testBatch.getStartingBatchLabVessels()) {
                    for (String sampleName : currVessel.getSampleNames()) {
                        Assert.assertTrue(((String) field.getValue()).contains(sampleName));
                    }
                }
                Assert.assertFalse(testBatch.getReworks().isEmpty());
                for (LabVessel currVessel : testBatch.getReworks()) {
                    for (String sampleName : currVessel.getSampleNames()) {
                        Assert.assertTrue(((String) field.getValue()).contains(sampleName));
                    }
                }
            }
            if (fieldDefinitionName.equals(LabBatch.TicketFields.LIBRARY_QC_SEQUENCING_REQUIRED.getName())) {
                Assert.assertEquals(((CustomField.SelectOption) field.getValue()).getId(), "-1");
            }
            if (fieldDefinitionName.equals(LabBatch.TicketFields.NUMBER_OF_SAMPLES.getName())) {
                Assert.assertEquals(field.getValue(), numSamples);
            }
            if (fieldDefinitionName.equals(LabBatch.TicketFields.PROGRESS_STATUS.getName())) {
                Assert.assertEquals(LCSetJiraFieldFactory.PROGRESS_STATUS,
                                    ((CustomField.ValueContainer) field.getValue()).getValue());
            }
            if (fieldDefinitionName.equals(LabBatch.TicketFields.PROTOCOL.getName())) {

                ProductWorkflowDef workflowDef = workflowConfig.getWorkflow(
                        testProductOrder.getProduct().getWorkflowName());

                Assert.assertEquals(
                        workflowDef.getName() + ":" + workflowDef.getEffectiveVersion(testBatch.getCreatedOn())
                                                                 .getVersion(), field.getValue());
            }
            if (fieldDefinitionName.equals(LabBatch.TicketFields.SAMPLES_ON_RISK.getName())) {
                Assert.assertEquals(field.getValue(),
                        testProductOrder.getSamples().get(testProductOrder.getSamples().size()-1).getName()
                        + "\n"
                        + testProductOrder.getSamples().get(testProductOrder.getSamples().size()-2).getName()
                );
            }
            if (fieldDefinitionName.equals(LabBatch.TicketFields.RISK_CATEGORIZED_SAMPLES.getName())) {

                // Multiple risk items returned in field value are in a non-deterministic order
                String pdoSampleRisk = "*"+testProductOrder.getSamples().get(testProductOrder.getSamples().size()-1).getRiskItems().iterator().next().getRiskCriterion().getCalculationString()+"*\n"
                        +testProductOrder.getSamples().get(testProductOrder.getSamples().size()-1).getName()+"\n";
                int length = pdoSampleRisk.length();
                Assert.assertTrue(field.getValue().toString().contains(pdoSampleRisk));

                pdoSampleRisk = "*"+testProductOrder.getSamples().get(testProductOrder.getSamples().size()-2).getRiskItems().iterator().next().getRiskCriterion().getCalculationString()+"*\n"
                        +testProductOrder.getSamples().get(testProductOrder.getSamples().size()-2).getName()+"\n";
                length += pdoSampleRisk.length();
                Assert.assertTrue(field.getValue().toString().contains(pdoSampleRisk));

                Assert.assertEquals( length, field.getValue().toString().length());
            }

            if (fieldDefinitionName.equals(LabBatch.TicketFields.REWORK_SAMPLES.getName())) {
                for (LabVessel rework : reworks) {
                    for(SampleInstanceV2 sampleInstance : rework.getSampleInstancesV2()) {

                        Assert.assertTrue(((String) field.getValue()).contains(sampleInstance.getSingleProductOrderSample().getName()));
                    }
                }
            }
        }
    }

    @Test
    public void test_sample_field_text_with_reworks() {
        String expectedText = "SM-1\nSM-2";

        Set<LabVessel> newTubes = new HashSet<>();
        Set<LabVessel> reworks = new HashSet<>();
        LabVessel tube1 = new BarcodedTube("000012");
        tube1.addSample(new MercurySample("SM-1", MercurySample.MetadataSource.BSP));
        LabVessel tube2 = new BarcodedTube("000033");
        tube2.addSample(new MercurySample("SM-2", MercurySample.MetadataSource.BSP));
        newTubes.add(tube1);
        reworks.add(tube2);

        LabBatch batch = new LabBatch("test", newTubes, LabBatch.LabBatchType.WORKFLOW);
        batch.addBucketEntry(new BucketEntry(tube1, testProductOrder, bucket, BucketEntry.BucketEntryType.PDO_ENTRY, 1));

        batch.addReworks(reworks);
        batch.addBucketEntry(new BucketEntry(tube2, testProductOrder, bucket, BucketEntry.BucketEntryType.REWORK_ENTRY,
                1));

        String actualText = AbstractBatchJiraFieldFactory.buildSamplesListString(batch, true);
        assertThat(actualText.trim(), equalTo(expectedText.trim()));
    }

    @Test
    public void test_sample_field_text_no_reworks() {
        String sampleKey = "SM-123";

        Set<LabVessel> newTubes = new HashSet<>();
        LabVessel tube = new BarcodedTube("000012");
        tube.addSample(new MercurySample(sampleKey, MercurySample.MetadataSource.BSP));
        newTubes.add(tube);

        LabBatch batch = new LabBatch("test", newTubes, LabBatch.LabBatchType.WORKFLOW);
        batch.addBucketEntry(new BucketEntry(tube, testProductOrder, new Bucket("Test"),
                BucketEntry.BucketEntryType.PDO_ENTRY, 1));

        String actualText = AbstractBatchJiraFieldFactory.buildSamplesListString(batch, true);
        assertThat(actualText.trim(), equalTo(sampleKey.trim()));
    }

    @Test
    public void test_sample_field_text_with_reworks_and_multiple_samples_per_tube() {
        Set<LabVessel> newTubes = new HashSet<>();
        Set<LabVessel> reworks = new HashSet<>();
        LabVessel tube1 = new BarcodedTube("000012");
        LabVessel sourceTube11 = new BarcodedTube("0000121");
        LabVessel sourceTube12 = new BarcodedTube("0000122");
        sourceTube11.addSample(new MercurySample("SM-1", MercurySample.MetadataSource.BSP));
        sourceTube12.addSample(new MercurySample("SM-3", MercurySample.MetadataSource.BSP));
        new VesselToVesselTransfer(sourceTube11, tube1,
                new LabEvent(LabEventType.EXTRACT_CELL_SUSP_TO_MATRIX, new Date(),"test", 1L, 1L,"Test"));
        new VesselToVesselTransfer(sourceTube12, tube1,
                new LabEvent(LabEventType.EXTRACT_CELL_SUSP_TO_MATRIX, new Date(),"test", 2L, 1L,"Test"));
        tube1.addSample(new MercurySample("SM-5", MercurySample.MetadataSource.BSP));

        LabVessel tube2 = new BarcodedTube("000033");
        LabVessel sourceTube21 = new BarcodedTube("0000331");
        LabVessel sourceTube22 = new BarcodedTube("0000332");
        sourceTube21.addSample(new MercurySample("SM-2", MercurySample.MetadataSource.BSP));
        sourceTube22.addSample(new MercurySample("SM-4", MercurySample.MetadataSource.BSP));
        new VesselToVesselTransfer(sourceTube21, tube2,
                new LabEvent(LabEventType.EXTRACT_CELL_SUSP_TO_MATRIX, new Date(),"test", 3L, 1L,"Test"));
        new VesselToVesselTransfer(sourceTube22, tube2,
                new LabEvent(LabEventType.EXTRACT_CELL_SUSP_TO_MATRIX, new Date(),"test", 4L, 1L,"Test"));

        newTubes.add(tube1);
        reworks.add(tube2);

        LabBatch batch = new LabBatch("test", newTubes, LabBatch.LabBatchType.WORKFLOW);
        batch.addBucketEntry(new BucketEntry(tube1, testProductOrder, bucket, BucketEntry.BucketEntryType.PDO_ENTRY,
                1));

        batch.addReworks(reworks);
        batch.addBucketEntry(new BucketEntry(tube2, testProductOrder, bucket, BucketEntry.BucketEntryType.REWORK_ENTRY,
                1));

        // Test nearest sample names.
        String actualText = AbstractBatchJiraFieldFactory.buildSamplesListString(batch, true);
        assertThat(actualText, equalTo("SM-5\nSM-2\nSM-4\n"));

        // Test earliest sample names.
        assertThat(AbstractBatchJiraFieldFactory.buildSamplesListString(batch, false),
                equalTo("SM-1\nSM-3\nSM-2\nSM-4\n"));
    }

    @Test
    public void test_sample_field_text_no_reworks_and_multiple_samples_per_tube() {
        String sampleKey = "SM-123";
        String sampleKey2 = "SM-1234";
        String expectedText = sampleKey + "\n" + sampleKey2;

        Set<LabVessel> newTubes = new HashSet<>();
        LabVessel tube = new BarcodedTube("000012");
        LabVessel sourceTube1 = new BarcodedTube("0000121");
        LabVessel sourceTube2 = new BarcodedTube("0000122");
        sourceTube1.addSample(new MercurySample(sampleKey, MercurySample.MetadataSource.BSP));
        sourceTube2.addSample(new MercurySample(sampleKey2, MercurySample.MetadataSource.BSP));
        new VesselToVesselTransfer(sourceTube1, tube,
                new LabEvent(LabEventType.EXTRACT_CELL_SUSP_TO_MATRIX, new Date(),"test", 1L, 1L,"Test"));
        new VesselToVesselTransfer(sourceTube2, tube,
                new LabEvent(LabEventType.EXTRACT_CELL_SUSP_TO_MATRIX, new Date(),"test", 2L, 1L,"Test"));
        newTubes.add(tube);

        LabBatch batch = new LabBatch("test", newTubes, LabBatch.LabBatchType.WORKFLOW);
        batch.addBucketEntry(new BucketEntry(tube, testProductOrder, new Bucket("test"),
                BucketEntry.BucketEntryType.PDO_ENTRY, 1));

        String actualText = AbstractBatchJiraFieldFactory.buildSamplesListString(batch, true);
        assertThat(actualText.trim(), equalTo(expectedText.trim()));
    }
}
