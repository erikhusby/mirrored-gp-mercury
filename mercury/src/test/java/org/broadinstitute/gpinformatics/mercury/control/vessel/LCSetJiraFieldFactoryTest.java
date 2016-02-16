package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
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
    private Workflow workflow;
    private Map<String, BarcodedTube> mapBarcodeToTube;
    private String rpSynopsis;
    private Map<String, CustomFieldDefinition> jiraFieldDefs;
    private ProductOrder testProductOrder;
    @MockitoAnnotations.Mock
    private ProductOrderDao productOrderDao;
    private ProductOrder singleSampleOrder;
    private String testOrderKey;
    private String singleSampleTestOrder;

    @BeforeMethod
    public void startUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        testOrderKey = "PDO-999";
        testProductOrder =
                ProductOrderTestFactory.createDummyProductOrder(testOrderKey);
        singleSampleTestOrder = "PDO-7";
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

        List<String> vesselSampleList = new ArrayList<>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");
        Bucket bucket = new Bucket("Pico/Plating Bucket");
        // starting rack
        for (int sampleIndex = 1; sampleIndex <= vesselSampleList.size(); sampleIndex++) {
            ProductOrder productOrder = sampleIndex == 1 ? singleSampleOrder : testProductOrder;
            List<ProductOrderSample> samples = productOrder.getSamples();
            String barcode = "R" + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex;
            String bspStock = vesselSampleList.get(sampleIndex - 1);
            BarcodedTube bspAliquot = new BarcodedTube(barcode);
            MercurySample mercurySample = new MercurySample(bspStock, MercurySample.MetadataSource.BSP);
            mercurySample.addProductOrderSample(samples.get(0));
            bspAliquot.addSample(mercurySample);
            bucket.addEntry(productOrder, bspAliquot, BucketEntry.BucketEntryType.PDO_ENTRY,
                    Workflow.AGILENT_EXOME_EXPRESS);
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        jiraFieldDefs = JiraServiceProducer.stubInstance().getCustomFields();

    }

    @AfterMethod
    public void tearDown() {

    }

    public void testLCSetFieldGeneration() throws IOException {
        LabBatch testBatch = new LabBatch(LabBatch.generateBatchName(workflow, pdoNames),
                                          new HashSet<LabVessel>(mapBarcodeToTube.values()),
                                          LabBatch.LabBatchType.WORKFLOW);
        testBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        testBatch.setBatchDescription("Batch Test Description");

        Set<LabVessel> reworks = new HashSet<>();
        reworks.add(new BarcodedTube("Rework1"));
        reworks.add(new BarcodedTube("Rework2"));
        testBatch.addReworks(reworks);

        int numSamples = testBatch.getStartingBatchLabVessels().size();

        AbstractBatchJiraFieldFactory testBuilder = AbstractBatchJiraFieldFactory.getInstance(
                CreateFields.ProjectType.LCSET_PROJECT, testBatch, productOrderDao);

        Assert.assertEquals("1 sample with material types [] from MyResearchProject PDO-7\n5 samples with material types [] from MyResearchProject PDO-999\n",
                            testBuilder.generateDescription());

        Collection<CustomField> generatedFields = testBuilder.getCustomFields(jiraFieldDefs);

        Assert.assertEquals(7, generatedFields.size());

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
                Assert.assertEquals(numSamples, field.getValue());
            }
            if (fieldDefinitionName.equals(LabBatch.TicketFields.PROGRESS_STATUS.getName())) {
                Assert.assertEquals(LCSetJiraFieldFactory.PROGRESS_STATUS,
                                    ((CustomField.ValueContainer) field.getValue()).getValue());
            }
            if (fieldDefinitionName.equals(LabBatch.TicketFields.PROTOCOL.getName())) {
                WorkflowLoader workflowLoader = new WorkflowLoader();
                WorkflowConfig workflowConfig = workflowLoader.load();

                ProductWorkflowDef workflowDef = workflowConfig.getWorkflow(
                        testProductOrder.getProduct().getWorkflow());

                Assert.assertEquals(
                        workflowDef.getName() + ":" + workflowDef.getEffectiveVersion(testBatch.getCreatedOn())
                                                                 .getVersion(), field.getValue());

            }
        }
    }

    @Test
    public void test_sample_field_text_with_reworks() {
        String expectedText = "SM-1\n\nSM-2 (rework)";

        Set<LabVessel> newTubes = new HashSet<>();
        Set<LabVessel> reworks = new HashSet<>();
        LabVessel tube1 = new BarcodedTube("000012");
        tube1.addSample(new MercurySample("SM-1", MercurySample.MetadataSource.BSP));
        LabVessel tube2 = new BarcodedTube("000033");
        tube2.addSample(new MercurySample("SM-2", MercurySample.MetadataSource.BSP));
        newTubes.add(tube1);
        reworks.add(tube2);

        LabBatch batch = new LabBatch("test", newTubes, LabBatch.LabBatchType.WORKFLOW);

        batch.addReworks(reworks);

        String actualText = AbstractBatchJiraFieldFactory.buildSamplesListString(batch, null);

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

        String actualText = AbstractBatchJiraFieldFactory.buildSamplesListString(batch, null);

        assertThat(actualText.trim(), equalTo(sampleKey.trim()));
    }

    @Test
    public void test_sample_field_text_with_reworks_and_multiple_samples_per_tube() {
        String expectedText = "SM-1\nSM-3\n\nSM-2 (rework)\nSM-4 (rework)";

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

        batch.addReworks(reworks);

        String actualText = AbstractBatchJiraFieldFactory.buildSamplesListString(batch, null);

        assertThat(actualText.trim(), equalTo(expectedText.trim()));
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

        String actualText = AbstractBatchJiraFieldFactory.buildSamplesListString(batch, null);

        assertThat(actualText.trim(), equalTo(expectedText.trim()));
    }

}
