package org.broadinstitute.gpinformatics.mercury.control.vessel;

import com.google.common.collect.Lists;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Test FCT ticket creation
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class FCTJiraFieldFactoryTest {

    public static final String FLOWCELL_4000_TICKET = "FCT-5";

    private Map<String, CustomFieldDefinition> jiraFieldDefs;

    private String laneInfo = "||Lane||Loading Vessel||Loading Concentration||\n"
                              + "|LANE1|DenatureTube01|16.219999313354492|\n"
                              + "|LANE2|DenatureTube01|16.219999313354492|\n"
                              + "|LANE3|DenatureTube01|16.219999313354492|\n"
                              + "|LANE4|DenatureTube01|16.219999313354492|\n"
                              + "|LANE5|DenatureTube02|12.220000267028809|\n"
                              + "|LANE6|DenatureTube02|12.220000267028809|\n"
                              + "|LANE7|DenatureTube02|12.220000267028809|\n"
                              + "|LANE8|DenatureTube02|12.220000267028809|\n";

    @BeforeMethod
    public void startUp() throws IOException {
        jiraFieldDefs = JiraServiceProducer.stubInstance().getCustomFields();
    }

    public void testLCSetFieldGeneration() throws IOException {
        VesselPosition[] hiseq4000VesselPositions =
                IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell.getVesselGeometry().getVesselPositions();
        List<VesselPosition> vesselPositionList = Arrays.asList(hiseq4000VesselPositions);
        List<List<VesselPosition>> partition = Lists.partition(vesselPositionList, hiseq4000VesselPositions.length / 2);
        List<VesselPosition> vesselPositions1 = partition.get(0);
        List<VesselPosition> vesselPositions2 = partition.get(1);
        List<LabBatch.VesselToLanesInfo> vesselToLanesInfos = new ArrayList<>();

        LabBatch.VesselToLanesInfo vesselToLanesInfo = new LabBatch.VesselToLanesInfo(
                vesselPositions1, BigDecimal.valueOf(16.22f), new BarcodedTube("DenatureTube01"));

        LabBatch.VesselToLanesInfo vesselToLanesInfo2 = new LabBatch.VesselToLanesInfo(
                vesselPositions2, BigDecimal.valueOf(12.22f), new BarcodedTube("DenatureTube02"));

        vesselToLanesInfos.add(vesselToLanesInfo);
        vesselToLanesInfos.add(vesselToLanesInfo2);

        LabBatch labBatch = new LabBatch(FLOWCELL_4000_TICKET, vesselToLanesInfos,
                LabBatch.LabBatchType.FCT, IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);

        AbstractBatchJiraFieldFactory testBuilder = AbstractBatchJiraFieldFactory.getInstance(
                CreateFields.ProjectType.FCT_PROJECT, labBatch, null);

        Collection<CustomField> generatedFields = testBuilder.getCustomFields(jiraFieldDefs);

        Assert.assertEquals(1, generatedFields.size());

        for (CustomField field : generatedFields) {

            String fieldDefinitionName = field.getFieldDefinition().getName();
            if (fieldDefinitionName.equals(LabBatch.TicketFields.LANE_INFO.getName())) {
                Assert.assertEquals(laneInfo, (String) field.getValue());
            }
        }
    }

}
