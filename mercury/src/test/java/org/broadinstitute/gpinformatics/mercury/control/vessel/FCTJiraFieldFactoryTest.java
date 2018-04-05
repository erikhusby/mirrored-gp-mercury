package org.broadinstitute.gpinformatics.mercury.control.vessel;

import com.google.common.collect.Lists;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Test FCT ticket creation
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class FCTJiraFieldFactoryTest {

    public static final String FLOWCELL_4000_TICKET = "FCT-5";

    private Map<String, CustomFieldDefinition> jiraFieldDefs;

    @BeforeMethod
    public void startUp() throws IOException {
        jiraFieldDefs = JiraServiceTestProducer.stubInstance().getCustomFields();
    }

    public void testFCTSetFieldGeneration() throws IOException {
        VesselPosition[] hiseq4000VesselPositions =
                IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell.getVesselGeometry().getVesselPositions();
        List<VesselPosition> vesselPositionList = Arrays.asList(hiseq4000VesselPositions);
        List<List<VesselPosition>> partition = Lists.partition(vesselPositionList, hiseq4000VesselPositions.length / 2);
        List<VesselPosition> vesselPositions1 = partition.get(0);
        List<VesselPosition> vesselPositions2 = partition.get(1);
        List<LabBatch.VesselToLanesInfo> vesselToLanesInfos = new ArrayList<>();

        LabBatch.VesselToLanesInfo vesselToLanesInfo = new LabBatch.VesselToLanesInfo(
                vesselPositions1, BigDecimal.valueOf(16.22f), new BarcodedTube("DenatureTube01"), "LCSET-0001",
                "Express Somatic Human WES (Deep Coverage) v1", Collections.<FlowcellDesignation>emptyList());

        LabBatch.VesselToLanesInfo vesselToLanesInfo2 = new LabBatch.VesselToLanesInfo(
                vesselPositions2, BigDecimal.valueOf(12.22f), new BarcodedTube("DenatureTube02"), "LCSET-0002",
                "Product 1" + DesignationDto.DELIMITER + "Product 2" + DesignationDto.DELIMITER + "Product 3",
                Collections.<FlowcellDesignation>emptyList());

        vesselToLanesInfos.add(vesselToLanesInfo);
        vesselToLanesInfos.add(vesselToLanesInfo2);

        LabBatch labBatch = new LabBatch(FLOWCELL_4000_TICKET, vesselToLanesInfos,
                LabBatch.LabBatchType.FCT, IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);

        AbstractBatchJiraFieldFactory testBuilder = AbstractBatchJiraFieldFactory.getInstance(
                CreateFields.ProjectType.FCT_PROJECT, labBatch, null, null);

        Collection<CustomField> testFields = testBuilder.getCustomFields(jiraFieldDefs);
        Assert.assertEquals(1, testFields.size());
        CustomField testField = testFields.iterator().next();
        Assert.assertEquals(testField.getFieldDefinition().getName(), LabBatch.TicketFields.LANE_INFO.getName());
        String laneInfo = (String) testField.getValue();
        Assert.assertTrue(laneInfo.startsWith(FCTJiraFieldFactory.LANE_INFO_HEADER));
        int counter = 1;
        for (JiraLaneInfo jiraLaneInfo : FCTJiraFieldFactory.parseJiraLaneInfo(laneInfo)) {
            if (counter < 5) {
                Assert.assertEquals(jiraLaneInfo.getLane(), "LANE" + counter++);
                Assert.assertEquals(jiraLaneInfo.getLoadingVessel(), "DenatureTube01");
                Assert.assertEquals(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(jiraLaneInfo.getLoadingConc())),
                        new BigDecimal("16.22"));
                Assert.assertEquals(jiraLaneInfo.getLcset(), "LCSET-0001");
                Assert.assertEquals(jiraLaneInfo.getProductNames(), "Express Somatic Human WES (Deep Coverage) v1");
            } else {
                Assert.assertEquals(jiraLaneInfo.getLane(), "LANE" + counter++);
                Assert.assertEquals(jiraLaneInfo.getLoadingVessel(), "DenatureTube02");
                Assert.assertEquals(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(jiraLaneInfo.getLoadingConc())),
                        new BigDecimal("12.22"));
                Assert.assertEquals(jiraLaneInfo.getLcset(), "LCSET-0002");
                String[] productNames = jiraLaneInfo.getProductNames().split(FCTJiraFieldFactory.JIRA_CRLF);
                Assert.assertEquals(productNames.length, 3);
                Assert.assertEquals(productNames, new String[]{"Product 1", "Product 2", "Product 3"});
            }
        }
    }
}

