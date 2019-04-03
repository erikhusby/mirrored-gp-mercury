package org.broadinstitute.gpinformatics.mercury.control.vessel;

import com.google.common.collect.Lists;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactoryTest.BARCODE_SUFFIX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test FCT ticket creation
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class FCTJiraFieldFactoryTest extends BaseEventTest {

    public static final String FLOWCELL_4000_TICKET = "FCT-5";

    private Map<String, CustomFieldDefinition> jiraFieldDefs;

    @BeforeMethod
    public void startUp() throws IOException {
        jiraFieldDefs = JiraServiceTestProducer.stubInstance().getCustomFields();
    }

    public void testFCTSetFieldGeneration() throws IOException {

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        Date runDate = new Date();
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        workflowBatch.setCreatedOn(new Date());

        //Build Event History
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, BARCODE_SUFFIX);
        expectedRouting = SystemRouter.System.MERCURY;
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                String.valueOf(runDate.getTime()),
                BARCODE_SUFFIX, true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), BARCODE_SUFFIX);
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                        exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                        exomeExpressShearingEntityBuilder.getShearingPlate(), BARCODE_SUFFIX);
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                        libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                        libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), BARCODE_SUFFIX);

        QtpEntityBuilder qtpEntityBuilder =
                runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                        hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                        hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                        "1");

        BarcodedTube denatureTube =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);

        VesselPosition[] hiseq4000VesselPositions =
                IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell.getVesselGeometry().getVesselPositions();
        List<VesselPosition> vesselPositionList = Arrays.asList(hiseq4000VesselPositions);
        List<List<VesselPosition>> partition = Lists.partition(vesselPositionList, hiseq4000VesselPositions.length / 2);
        List<VesselPosition> vesselPositions1 = partition.get(0);
        List<VesselPosition> vesselPositions2 = partition.get(1);
        List<LabBatch.VesselToLanesInfo> vesselToLanesInfos = new ArrayList<>();

        LabBatch.VesselToLanesInfo vesselToLanesInfo = new LabBatch.VesselToLanesInfo(
                vesselPositions1, BigDecimal.valueOf(16.22f), denatureTube, workflowBatch.getBatchName(),
                productOrder.getProduct().getProductName(), Collections.<FlowcellDesignation>emptyList());

        LabBatch.VesselToLanesInfo vesselToLanesInfo2 = new LabBatch.VesselToLanesInfo(
                vesselPositions2, BigDecimal.valueOf(12.22f), denatureTube, workflowBatch.getBatchName(),
                productOrder.getProduct().getProductName(), Collections.<FlowcellDesignation>emptyList());

        vesselToLanesInfos.add(vesselToLanesInfo);
        vesselToLanesInfos.add(vesselToLanesInfo2);

        LabBatch labBatch = new LabBatch(FLOWCELL_4000_TICKET, vesselToLanesInfos,
                LabBatch.LabBatchType.FCT, IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);

        FlowcellDesignationEjb flowcellDesignationEjb = mock(FlowcellDesignationEjb.class);
        SequencingTemplateFactory sequencingTemplateFactory = new SequencingTemplateFactory();
        sequencingTemplateFactory.setWorkflowConfig(new WorkflowLoader().load());
        sequencingTemplateFactory.setFlowcellDesignationEjb(flowcellDesignationEjb);
        when(flowcellDesignationEjb.getFlowcellDesignations(labBatch)).thenReturn(Collections.emptyList());

        AbstractBatchJiraFieldFactory testBuilder = AbstractBatchJiraFieldFactory.getInstance(
                CreateFields.ProjectType.FCT_PROJECT, labBatch, sequencingTemplateFactory, null, null);

        Collection<CustomField> testFields = testBuilder.getCustomFields(jiraFieldDefs);
        Assert.assertEquals(2, testFields.size());
        CustomField testField = findCustomField(LabBatch.TicketFields.LANE_INFO, testFields);
        Assert.assertEquals(testField.getFieldDefinition().getName(), LabBatch.TicketFields.LANE_INFO.getName());
        String laneInfo = (String) testField.getValue();
        Assert.assertTrue(laneInfo.startsWith(FCTJiraFieldFactory.LANE_INFO_HEADER));
        int counter = 1;
        for (JiraLaneInfo jiraLaneInfo : FCTJiraFieldFactory.parseJiraLaneInfo(laneInfo)) {
            if (counter < 5) {
                Assert.assertEquals(jiraLaneInfo.getLane(), "LANE" + counter++);
                Assert.assertEquals(jiraLaneInfo.getLoadingVessel(), "DenatureTube10");
                Assert.assertEquals(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(jiraLaneInfo.getLoadingConc())),
                        new BigDecimal("16.22"));
                Assert.assertEquals(jiraLaneInfo.getLcset(), "LCSET1");
            } else {
                Assert.assertEquals(jiraLaneInfo.getLane(), "LANE" + counter++);
                Assert.assertEquals(jiraLaneInfo.getLoadingVessel(), "DenatureTube10");
                Assert.assertEquals(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(jiraLaneInfo.getLoadingConc())),
                        new BigDecimal("12.22"));
                Assert.assertEquals(jiraLaneInfo.getLcset(), "LCSET1");
            }
        }

        CustomField readStructureField = findCustomField(LabBatch.TicketFields.READ_STRUCTURE, testFields);
        Assert.assertEquals((String) readStructureField.getValue(), "76x8x8x76");
    }

    private CustomField findCustomField(LabBatch.TicketFields ticketField, Collection<CustomField> testFields) {
        for (CustomField customField: testFields) {
            if (customField.getFieldDefinition().getName().equals(ticketField.getName())) {
                return customField;
            }
        }
        return null;
    }
}

