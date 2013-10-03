package org.broadinstitute.gpinformatics.mercury.control.vessel;

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
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

    private String pdoBusinessName;
    private List<String> pdoNames;
    private Workflow workflow;
    private Map<String, TwoDBarcodedTube> mapBarcodeToTube;
    private String rpSynopsis;
    private Map<String, CustomFieldDefinition> jiraFieldDefs;

    @BeforeMethod
    public void startUp() throws IOException {
        pdoBusinessName = "PDO-999";

        pdoNames = new ArrayList<>();
        Collections.addAll(pdoNames, pdoBusinessName);

        workflow = Workflow.AGILENT_EXOME_EXPRESS;
        mapBarcodeToTube = new LinkedHashMap<>();

        List<String> vesselSampleList = new ArrayList<>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");

        // starting rack
        for (int sampleIndex = 1; sampleIndex <= vesselSampleList.size(); sampleIndex++) {
            String barcode = "R" + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex;
            String bspStock = vesselSampleList.get(sampleIndex - 1);
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(bspStock));
            bspAliquot.addBucketEntry(new BucketEntry(bspAliquot, sampleIndex == 1 ? "PDO-7" : pdoBusinessName,
                    BucketEntry.BucketEntryType.PDO_ENTRY));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        jiraFieldDefs = JiraServiceProducer.stubInstance().getCustomFields();

    }

    @AfterMethod
    public void tearDown() {

    }

    public void testLCSetFieldGeneration() throws IOException {
        LabBatch testBatch = new LabBatch(LabBatch.generateBatchName(workflow, pdoNames),
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        testBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        testBatch.setBatchDescription("Batch Test Description");

        Set<LabVessel> reworks = new HashSet<>();
        reworks.add(new TwoDBarcodedTube("Rework1"));
        reworks.add(new TwoDBarcodedTube("Rework2"));
        testBatch.addReworks(reworks);

        int numSamples = testBatch.getStartingBatchLabVessels().size();

        AbstractBatchJiraFieldFactory testBuilder = AbstractBatchJiraFieldFactory.getInstance(
                CreateFields.ProjectType.getLcsetProjectType(), testBatch, AthenaClientProducer.stubInstance());

        Assert.assertEquals("1 samples from MyResearchProject PDO-7\n5 samples from MyResearchProject PDO-999\n",
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
                AthenaClientService athenaClientService = AthenaClientProducer.stubInstance();

                ProductWorkflowDef workflowDef = workflowConfig.getWorkflow(
                        athenaClientService.retrieveProductOrderDetails(pdoBusinessName).getProduct().getWorkflow());

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
        LabVessel tube1 = new TwoDBarcodedTube("000012");
        tube1.addSample(new MercurySample("SM-1"));
        LabVessel tube2 = new TwoDBarcodedTube("000033");
        tube2.addSample(new MercurySample("SM-2"));
        newTubes.add(tube1);
        reworks.add(tube2);

        LabBatch batch = new LabBatch("test", newTubes, LabBatch.LabBatchType.WORKFLOW);

        batch.addReworks(reworks);

        String actualText = LCSetJiraFieldFactory.buildSamplesListString(batch, null);

        assertThat(actualText.trim(), equalTo(expectedText.trim()));
    }

    @Test
    public void test_sample_field_text_no_reworks() {
        String sampleKey = "SM-123";

        Set<LabVessel> newTubes = new HashSet<>();
        LabVessel tube = new TwoDBarcodedTube("000012");
        tube.addSample(new MercurySample(sampleKey));
        newTubes.add(tube);

        LabBatch batch = new LabBatch("test", newTubes, LabBatch.LabBatchType.WORKFLOW);

        String actualText = LCSetJiraFieldFactory.buildSamplesListString(batch, null);

        assertThat(actualText.trim(), equalTo(sampleKey.trim()));
    }

}
