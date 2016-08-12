package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.DesignationLoadingTube;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationLoadingTubeActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationRowDto;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = TestGroups.STANDARD)
public class LabBatchEjbStandardTest extends Arquillian {

    @Inject
    private LabBatchEjb labBatchEJB;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private BucketDao bucketDao;

    @Inject
    private UserTransaction utx;

    @Inject
    private ProductDao productDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BarcodedTubeDao tubeDao;

    @Inject
    private JiraService jiraService;

    @Inject
    LabBatchTestUtils labBatchTestUtils;

    private Bucket bucket;

    private LinkedHashMap<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();

    @BeforeMethod
    public void setUp() throws Exception {

        if (utx == null) {
            return;
        }

        utx.begin();

        List<String> vesselSampleList = new ArrayList<>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");

        mapBarcodeToTube = labBatchTestUtils.initializeTubes(vesselSampleList);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    @Test
    public void testUpdateLabBatch() throws Exception {

        bucket = labBatchTestUtils.putTubesInSpecificBucket(LabBatchEJBTest.BUCKET_NAME,
                BucketEntry.BucketEntryType.PDO_ENTRY, mapBarcodeToTube);

        Date today = new Date();

        SimpleDateFormat formatDate = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss::SSSS a");

        String nameForBatch = formatDate.format(today) + " - Does this get Overwritten";

        List<Long> bucketIds = new ArrayList<>();
        for (BarcodedTube barcodedTube : mapBarcodeToTube.values()) {
            for (BucketEntry bucketEntry : barcodedTube.getBucketEntries()) {
                bucketIds.add(bucketEntry.getBucketEntryId());
            }
        }

        LabBatch testBatch = labBatchEJB.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                Workflow.ICE_CRSP.getWorkflowName(), bucketIds, Collections.<Long>emptyList(),
                nameForBatch, "", new Date(), null, "scottmat", LabBatchEJBTest.BUCKET_NAME,
                MessageReporter.UNUSED, Collections.<String>emptyList());

        final String batchName = testBatch.getBatchName();

        labBatchDao.flush();
        labBatchDao.clear();

        LabBatch testFind = labBatchDao.findByName(batchName);

        JiraIssue jiraIssue = jiraService.getIssue(testFind.getJiraTicket().getTicketName());

        System.out.println("Jira ticket ID is... " + testFind.getJiraTicket().getTicketName());
        Assert.assertEquals(jiraIssue.getSummary(), nameForBatch);


        Assert.assertNotNull(testFind);
        Assert.assertNotNull(testFind.getJiraTicket());
        Assert.assertNotNull(testFind.getJiraTicket().getTicketName());
        Assert.assertNotNull(testFind.getStartingBatchLabVessels());
        Assert.assertEquals(6, testFind.getStartingBatchLabVessels().size());
        Assert.assertEquals(testFind.getBatchName(), testFind.getJiraTicket().getTicketName());

        List<String> vesselSampleList = new ArrayList<>(1);

        Collections.addAll(vesselSampleList, "SM-423RS");

        LinkedHashMap<String, BarcodedTube> newMapBarcodeToTube = labBatchTestUtils.initializeTubes(vesselSampleList);

        bucket = labBatchTestUtils.putTubesInSpecificBucket(LabBatchEJBTest.BUCKET_NAME,
                BucketEntry.BucketEntryType.PDO_ENTRY, newMapBarcodeToTube);

        LabVessel vessel = newMapBarcodeToTube.values().iterator().next();

        labBatchEJB.addToLabBatch(testFind.getJiraTicket().getTicketId(),
                Collections.singletonList(vessel.getBucketEntries().iterator().next().getBucketEntryId()),
                Collections.<Long>emptyList(), LabBatchEJBTest.BUCKET_NAME, MessageReporter.UNUSED,
                Collections.<String>emptyList());

        jiraIssue = jiraService.getIssue(testFind.getJiraTicket().getTicketName());

        Assert.assertEquals(jiraIssue.getSummary(), nameForBatch);
    }

    @Test
    public void testUpdateExtractionLabBatch() throws Exception {

        bucket = labBatchTestUtils.putTubesInSpecificBucket(LabBatchEJBTest.EXTRACTION_BUCKET,
                BucketEntry.BucketEntryType.PDO_ENTRY, mapBarcodeToTube);

        Date today = new Date();

        SimpleDateFormat formatDate = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss::SSSS a");

        String nameForBatch = formatDate.format(today) + " - Does this get Overwritten";

        List<Long> bucketIds = new ArrayList<>();

        for (BarcodedTube barcodedTube : mapBarcodeToTube.values()) {
            for (BucketEntry bucketEntry : barcodedTube.getBucketEntries()) {
                bucketIds.add(bucketEntry.getBucketEntryId());
            }
        }

        LabBatch testBatch = labBatchEJB.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                Workflow.DNA_RNA_EXTRACTION_CELL_PELLETS.getWorkflowName(), bucketIds, Collections.<Long>emptyList(),
                nameForBatch, "", new Date(), null, "scottmat", LabBatchEJBTest.EXTRACTION_BUCKET,
                MessageReporter.UNUSED, Collections.<String>emptyList());

        final String batchName = testBatch.getBatchName();

        labBatchDao.flush();
        labBatchDao.clear();

        LabBatch testFind = labBatchDao.findByName(batchName);

        JiraIssue jiraIssue = jiraService.getIssue(testFind.getJiraTicket().getTicketName());

        System.out.println("Jira ticket ID is... " + testFind.getJiraTicket().getTicketName());
        Assert.assertEquals(jiraIssue.getSummary(), nameForBatch);


        Assert.assertNotNull(testFind);
        Assert.assertNotNull(testFind.getJiraTicket());
        Assert.assertNotNull(testFind.getJiraTicket().getTicketName());
        Assert.assertNotNull(testFind.getStartingBatchLabVessels());
        Assert.assertEquals(6, testFind.getStartingBatchLabVessels().size());
        Assert.assertEquals(testFind.getBatchName(), testFind.getJiraTicket().getTicketName());

        List<String> vesselSampleList = new ArrayList<>(1);

        Collections.addAll(vesselSampleList, "SM-423RS");

        LinkedHashMap<String, BarcodedTube> newMapBarcodeToTube = labBatchTestUtils.initializeTubes(vesselSampleList);

        bucket = labBatchTestUtils.putTubesInSpecificBucket(LabBatchEJBTest.EXTRACTION_BUCKET,
                BucketEntry.BucketEntryType.PDO_ENTRY, newMapBarcodeToTube);

        LabVessel vessel = newMapBarcodeToTube.values().iterator().next();

        labBatchEJB.addToLabBatch(testFind.getJiraTicket().getTicketId(),
                Collections.singletonList(vessel.getBucketEntries().iterator().next().getBucketEntryId()),
                Collections.<Long>emptyList(), LabBatchEJBTest.EXTRACTION_BUCKET, MessageReporter.UNUSED,
                Collections.<String>emptyList());

        jiraIssue = jiraService.getIssue(testFind.getJiraTicket().getTicketName());

        Assert.assertEquals(jiraIssue.getSummary(), nameForBatch);
    }

    @Test
    public void testCreateFCTLabBatch() throws Exception {
        //Create FCT lab batch
        List<LabBatch.VesselToLanesInfo> laneInfos = new ArrayList<>();
        VesselPosition[] hiseq4000VesselPositions =
                IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell.getVesselGeometry().getVesselPositions();
        List<VesselPosition> vesselPositionList = Arrays.asList(hiseq4000VesselPositions);
        BarcodedTube barcodedTube = mapBarcodeToTube.entrySet().iterator().next().getValue();
        LabBatch.VesselToLanesInfo vesselToLanesInfo =
                new LabBatch.VesselToLanesInfo(vesselPositionList, BigDecimal.valueOf(13.11f), barcodedTube);
        laneInfos.add(vesselToLanesInfo);
        LabBatch fctLabBatch = new LabBatch("Test FCT batch name", laneInfos,
                LabBatch.LabBatchType.FCT, IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
        fctLabBatch.setBatchDescription(fctLabBatch.getBatchName());
        labBatchEJB.createLabBatch(fctLabBatch, "jowalsh", CreateFields.IssueType.FLOWCELL,
                CreateFields.ProjectType.FCT_PROJECT);

        labBatchDao.flush();
        labBatchDao.clear();

        final String batchName = fctLabBatch.getBatchName();

        LabBatch testFind = labBatchDao.findByName(batchName);

        JiraIssue jiraIssue = jiraService.getIssue(testFind.getJiraTicket().getTicketName());

        System.out.println("FCT Jira ticket ID is... " + testFind.getJiraTicket().getTicketName());

        Object laneInfoValue = jiraIssue.getField(LabBatch.TicketFields.LANE_INFO.getName());
        Assert.assertNotNull(laneInfoValue);
        String laneInfo = (String) laneInfoValue;
        String expectedLaneInfo = "||Lane||Loading Vessel||Loading Concentration||\n"
                                  + "|LANE1|R111111SM-423|13.109999656677246|\n"
                                  + "|LANE2|R111111SM-423|13.109999656677246|\n"
                                  + "|LANE3|R111111SM-423|13.109999656677246|\n"
                                  + "|LANE4|R111111SM-423|13.109999656677246|\n"
                                  + "|LANE5|R111111SM-423|13.109999656677246|\n"
                                  + "|LANE6|R111111SM-423|13.109999656677246|\n"
                                  + "|LANE7|R111111SM-423|13.109999656677246|\n"
                                  + "|LANE8|R111111SM-423|13.109999656677246|\n";
        Assert.assertEquals(laneInfo, expectedLaneInfo);
        Assert.assertNotNull(testFind);
        Assert.assertNotNull(testFind.getJiraTicket());
        Assert.assertNotNull(testFind.getJiraTicket().getTicketName());
        Assert.assertNotNull(testFind.getStartingBatchLabVessels());
        Assert.assertEquals(testFind.getBatchName(), testFind.getJiraTicket().getTicketName());
    }


    @Test
    public void testFctsFromDesignations() throws Exception {
        final List<DesignationRowDto> designationDtos = new ArrayList<>();
        final StringBuilder messages = new StringBuilder();
        final MessageReporter messageReporter = new MessageReporter() {
            @Override
            public String addMessage(String message, Object... arguments) {
                messages.append(String.format(message, arguments)).append("\n");
                return "";
            }
        };
        final LabEvent labEvent = new LabEvent(LabEventType.NORMALIZATION_TRANSFER, new Date(),
                "machine", 1L, 1234L, "program");

        // Array index corresponds to flowcell type.
        final IlluminaFlowcell.FlowcellType[] flowcellTypes = {
                IlluminaFlowcell.FlowcellType.MiSeqFlowcell,
                IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell
        };
        // Array index corresponds to flowcell type.
        final Iterator<BarcodedTube> tubeIter = mapBarcodeToTube.values().iterator();
        final Map<LabVessel, Integer>[] vesselsLanes = new Map[]{
                new HashMap<LabVessel, Integer>() {{
                    put(tubeIter.next(), 3);
                }},
                new HashMap<LabVessel, Integer>() {{
                    put(tubeIter.next(), 5);
                    put(tubeIter.next(), 11);
                }}
        };
        // Array index corresponds to flowcell type.
        final LabBatch[] lcsets = {
                new LabBatch("lcset0", Collections.EMPTY_SET, LabBatch.LabBatchType.WORKFLOW),
                new LabBatch("lcset1", Collections.EMPTY_SET, LabBatch.LabBatchType.WORKFLOW)
        };
        // Array index corresponds to flowcell type.
        List<String>[] expectedBarcodes = new List[]{
                new ArrayList<String>(),
                new ArrayList<String>()
        };

        // Makes action bean dtos for queued designations having a mix of flowcell types.
        for (int idx = 0; idx < flowcellTypes.length; ++idx) {
            for (LabVessel loadingTube : vesselsLanes[idx].keySet()) {
                int numberLanes = vesselsLanes[idx].get(loadingTube);
                for (int i = 0; i < numberLanes; ++i) {
                    // The expected loading tube barcodes, repeated according to their lane counts.
                    expectedBarcodes[idx].add(loadingTube.getLabel());
                }
                DesignationRowDto dto = new DesignationRowDto(loadingTube, Collections.singleton(labEvent),
                        "lcsetUrl", lcsets[idx], Collections.<String>emptyList(),
                        Collections.singleton("P-EX-0017"), "startingBatchVessels", "CLINICAL", 23,
                        flowcellTypes[idx], DesignationLoadingTube.IndexType.DUAL,
                        167, numberLanes, 151, BigDecimal.TEN, false, new Date(),
                        DesignationLoadingTube.Status.QUEUED);
                dto.setSelected(true);
                designationDtos.add(dto);
            }
        }

        // Makes the FCTs.
        List<DesignationLoadingTubeActionBean.FctDto> fctDtos = labBatchEJB.makeFcts(designationDtos, "epolk",
                messageReporter);

        // Created the correct number of FCTs?
        int expectedNumberFcts = 0;
        for (int idx = 0; idx < flowcellTypes.length; ++idx) {
            expectedNumberFcts += expectedBarcodes[idx].size() /
                              flowcellTypes[idx].getVesselGeometry().getVesselPositions().length;
        }
        Assert.assertEquals(fctDtos.size(), expectedNumberFcts);
        labBatchDao.flush();

        for (DesignationLoadingTubeActionBean.FctDto fctDto : fctDtos) {
            String fctName = fctDto.getName();
            Assert.assertTrue(fctDto.getUrl().contains(fctName), fctDto.getUrl() + " " + fctName);

            LabBatch fctLabBatch = labBatchDao.findByName(fctName);
            Assert.assertNotNull(fctLabBatch);

            // Gets the flowcell index for doing lookups.
            int idx;
            for (idx = 0; idx < flowcellTypes.length; ++idx) {
                if (fctLabBatch.getFlowcellType() == flowcellTypes[idx]) {
                    break;
                }
            }

            Assert.assertEquals(fctLabBatch.getLabBatchType(),
                    flowcellTypes[idx] == IlluminaFlowcell.FlowcellType.MiSeqFlowcell ?
                            LabBatch.LabBatchType.MISEQ : LabBatch.LabBatchType.FCT);

            // Gets the per-lane allocations from the jira ticket.
            JiraIssue jiraIssue = jiraService.getIssue(fctLabBatch.getJiraTicket().getTicketName());
            Assert.assertNotNull(jiraIssue);
            String laneInfo = (String) (jiraIssue.getField(LabBatch.TicketFields.LANE_INFO.getName()));
            Assert.assertNotNull(laneInfo);

            // Removes the allocated loading tube barcodes from the expected ones.
            for (String token : laneInfo.split("\\|")) {
                if (token != null && token.contains("SM-")) {
                    Assert.assertTrue(expectedBarcodes[idx].remove(token), "At idx " + idx + " missing " + token);
                }
            }
        }
        // Verifies no error messages.
        Assert.assertTrue(messages.length() == 0, "Unexpected messages: '" + messages.toString() + "'");

        // Verifies the allocated loading tube barcodes all matched up with the expected ones.
        for (int idx = 0; idx < flowcellTypes.length; ++idx) {
            Assert.assertEquals(expectedBarcodes[idx].size(), 0,
                    "at idx " + idx + " found " + StringUtils.join(expectedBarcodes[idx], " "));
        }
    }
}
