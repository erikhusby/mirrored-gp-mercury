package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
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
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;
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
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb.PARTIAL_FCT_MESSAGE;
import static org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb.SPLIT_DESIGNATION_MESSAGE;
import static org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation_.loadingTube;
import static org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation_.numberLanes;

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
        final Set<DesignationDto> designationDtos = new HashSet<>();
        final StringBuilder messages = new StringBuilder();
        final MessageReporter messageReporter = new MessageReporter() {
            @Override
            public String addMessage(String message, Object... arguments) {
                messages.append(String.format(message, arguments)).append("\n");
                return "";
            }
        };
        // Uses any old normalization transfer.
        final LabEvent labEvent = labBatchDao.findById(LabEvent.class, 184671L);

        // Sets up the flowcell types to be created.
        final IlluminaFlowcell.FlowcellType[] flowcellTypes = {
                IlluminaFlowcell.FlowcellType.MiSeqFlowcell,
                IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell
        };
        // Sets up the loading tubes to be put on flowcells.
        final Iterator<BarcodedTube> tubeIter = mapBarcodeToTube.values().iterator();
        final Map<LabVessel, Integer>[] vesselsLanes = new Map[]{
                new HashMap<LabVessel, Integer>() {{
                    put(tubeIter.next(), 3);
                }},
                new HashMap<LabVessel, Integer>() {{
                    put(tubeIter.next(), 5);
                    put(tubeIter.next(), 17);
                }}
        };
        // Sets up an lcset for each flowcell.
        final LabBatch[] lcsets = {
                new LabBatch("lcset0", Collections.EMPTY_SET, LabBatch.LabBatchType.WORKFLOW),
                new LabBatch("lcset1", Collections.EMPTY_SET, LabBatch.LabBatchType.WORKFLOW)
        };
        // The tube barcodes for each dto lane.
        List<String>[] tubeBarcodes = new List[]{
                new ArrayList<String>(),
                new ArrayList<String>()
        };
        String splitDtoBarcode = null;
        String splitDtoGrouping = null;

        // Makes action bean dtos that are queued designations.
        for (int idx = 0; idx < flowcellTypes.length; ++idx) {
            for (LabVessel loadingTube : vesselsLanes[idx].keySet()) {
                int numberLanes = vesselsLanes[idx].get(loadingTube);
                for (int i = 0; i < numberLanes; ++i) {
                    tubeBarcodes[idx].add(loadingTube.getLabel());
                }
                DesignationDto dto = new DesignationDto(loadingTube, Collections.singleton(labEvent),
                        "lcsetUrl", lcsets[idx], Collections.<String>emptyList(),
                        Collections.singleton("P-EX-0017"), "startingBatchVessels", "CLINICAL", 23,
                        flowcellTypes[idx], FlowcellDesignation.IndexType.DUAL,
                        167, numberLanes, 151, BigDecimal.TEN, false, new Date(),
                        FlowcellDesignation.Status.QUEUED);
                dto.setSelected(true);
                // Sets the dto with 17 lanes to be low priority. It will be split.
                if (numberLanes == 17) {
                    dto.setPriority(FlowcellDesignation.Priority.LOW);
                    splitDtoBarcode = dto.getBarcode();
                    splitDtoGrouping = (new DesignationDto.FctGrouping(dto)).toString();
                }
                designationDtos.add(dto);
            }
        }

        // Makes the FCTs.
        List<MutablePair<String, String>> fctUrls = labBatchEJB.makeFcts(designationDtos, "epolk", messageReporter);

        // Created the correct number of FCTs?
        int expectedNumberFcts = 0;
        for (int idx = 0; idx < flowcellTypes.length; ++idx) {
            expectedNumberFcts += tubeBarcodes[idx].size() / flowcellTypes[idx].getVesselGeometry().getRowCount();
        }
        Assert.assertEquals(fctUrls.size(), expectedNumberFcts);
        labBatchDao.flush();

        // Checks for the split dto.
        int splitDtoCount = 0;
        for (DesignationDto dto : designationDtos) {
            if (dto.getBarcode().equals(splitDtoBarcode)) {
                if (dto.isAllocated()) {
                    Assert.assertEquals((int)dto.getNumberLanes(), 11);
                } else {
                    ++splitDtoCount;
                    Assert.assertEquals((int)dto.getNumberLanes(), 6);
                    // Removes the split barcode from barcodes expected to be in FCTs.
                    for (int i = 0; i < 6; ++i) {
                        Assert.assertTrue(tubeBarcodes[1].remove(splitDtoBarcode), splitDtoBarcode);
                    }
                }
            } else {
                Assert.assertTrue(dto.isAllocated());
            }
        }
        Assert.assertEquals(splitDtoCount, 1);


        for (MutablePair<String, String> fctUrl : fctUrls) {
            String fctName = fctUrl.getLeft();
            Assert.assertTrue(fctUrl.getRight().contains(fctName), fctUrl.getRight() + " " + fctName);

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
                    Assert.assertTrue(tubeBarcodes[idx].remove(token), "At idx " + idx + " missing " + token);
                }
            }

        }

        // Verifies the one action bean message is about the split dto.
        String[] messageLines = messages.toString().split("\n");
        Assert.assertEquals(messageLines.length, 1);
        String splitMsg = MessageFormat.format(SPLIT_DESIGNATION_MESSAGE, splitDtoCount, splitDtoGrouping);
        Assert.assertEquals(messageLines[0], splitMsg);

        // Verifies the allocated loading tube barcodes all matched up with the expected ones.
        for (int idx = 0; idx < flowcellTypes.length; ++idx) {
            Assert.assertEquals(tubeBarcodes[idx].size(), 0,
                    "at idx " + idx + " found " + StringUtils.join(tubeBarcodes[idx], " "));
        }
    }

    // Tests grouping by numberCycles, readLength, indexType, regulatoryDesignation.
    @Test
    public void testDesignationFctGrouping() throws Exception {
        final Set<DesignationDto> designationDtos = new HashSet<>();
        final StringBuilder messages = new StringBuilder();
        final MessageReporter messageReporter = new MessageReporter() {
            @Override
            public String addMessage(String message, Object... arguments) {
                messages.append(String.format(message, arguments)).append("\n");
                return "";
            }
        };
        // Uses any old normalization transfer.
        final LabEvent labEvent = labBatchDao.findById(LabEvent.class, 184671L);
        final LabVessel loadingTube = mapBarcodeToTube.values().iterator().next();
        final IlluminaFlowcell.FlowcellType flowcellType = IlluminaFlowcell.FlowcellType.HiSeqFlowcell;
        final LabBatch lcset = new LabBatch("lcset0", Collections.EMPTY_SET, LabBatch.LabBatchType.WORKFLOW);

        DesignationDto[] dtos = new DesignationDto[]{
                new DesignationDto(loadingTube, Collections.singleton(labEvent), "lcsetUrl", lcset,
                        Collections.<String>emptyList(), Collections.singleton("P-EX-0017"), "startingBatchVessels",
                        "CLINICAL", 23, flowcellType, FlowcellDesignation.IndexType.DUAL, 167, 4, 151,
                        BigDecimal.TEN, false, new Date(), FlowcellDesignation.Status.QUEUED),
                new DesignationDto(loadingTube, Collections.singleton(labEvent), "lcsetUrl", lcset,
                        Collections.<String>emptyList(), Collections.singleton("P-EX-0017"), "startingBatchVessels",
                        "RESEARCH", 23, flowcellType, FlowcellDesignation.IndexType.DUAL, 167, 4, 151,
                        BigDecimal.TEN, false, new Date(), FlowcellDesignation.Status.QUEUED),
                new DesignationDto(loadingTube, Collections.singleton(labEvent), "lcsetUrl", lcset,
                        Collections.<String>emptyList(), Collections.singleton("P-EX-0017"), "startingBatchVessels",
                        "CLINICAL", 22, flowcellType, FlowcellDesignation.IndexType.DUAL, 167, 4, 151,
                        BigDecimal.TEN, false, new Date(), FlowcellDesignation.Status.QUEUED),
                new DesignationDto(loadingTube, Collections.singleton(labEvent), "lcsetUrl", lcset,
                        Collections.<String>emptyList(), Collections.singleton("P-EX-0017"), "startingBatchVessels",
                        "RESEARCH", 22, flowcellType, FlowcellDesignation.IndexType.DUAL, 167, 4, 151,
                        BigDecimal.TEN, false, new Date(), FlowcellDesignation.Status.QUEUED),
                new DesignationDto(loadingTube, Collections.singleton(labEvent), "lcsetUrl", lcset,
                        Collections.<String>emptyList(), Collections.singleton("P-EX-0017"), "startingBatchVessels",
                        "CLINICAL", 22, flowcellType, FlowcellDesignation.IndexType.DUAL, 168, 4, 151,
                        BigDecimal.TEN, false, new Date(), FlowcellDesignation.Status.QUEUED),
                new DesignationDto(loadingTube, Collections.singleton(labEvent), "lcsetUrl", lcset,
                        Collections.<String>emptyList(), Collections.singleton("P-EX-0017"), "startingBatchVessels",
                        "RESEARCH", 22, flowcellType, FlowcellDesignation.IndexType.DUAL, 168, 4, 151,
                        BigDecimal.TEN, false, new Date(), FlowcellDesignation.Status.QUEUED),
                new DesignationDto(loadingTube, Collections.singleton(labEvent), "lcsetUrl", lcset,
                        Collections.<String>emptyList(), Collections.singleton("P-EX-0017"), "startingBatchVessels",
                        "CLINICAL", 22, flowcellType, FlowcellDesignation.IndexType.DUAL, 168, 4, 152,
                        BigDecimal.TEN, false, new Date(), FlowcellDesignation.Status.QUEUED),
                new DesignationDto(loadingTube, Collections.singleton(labEvent), "lcsetUrl", lcset,
                        Collections.<String>emptyList(), Collections.singleton("P-EX-0017"), "startingBatchVessels",
                        "RESEARCH", 22, flowcellType, FlowcellDesignation.IndexType.DUAL, 168, 4, 152,
                        BigDecimal.TEN, false, new Date(), FlowcellDesignation.Status.QUEUED),
        };

        for (DesignationDto dto : dtos) {
            dto.setSelected(true);
        }

        // Should not combine any dto, so it should make no FCTs.
        Assert.assertTrue(CollectionUtils.isEmpty(labBatchEJB.makeFcts(designationDtos, "epolk", messageReporter)));

        // Verifies the 8 action bean messages are about the partial flowcells.
        List<String> messageLines = Arrays.asList(messages.toString().split("\n"));
        Assert.assertEquals(messageLines.size(), 8);
        for (DesignationDto dto : dtos) {
            String fctGroupingString = (new DesignationDto.FctGrouping(dto)).toString();
            String partialMsg = MessageFormat.format(LabBatchEjb.PARTIAL_FCT_MESSAGE, fctGroupingString, 4);
            Assert.assertTrue(messageLines.remove(partialMsg));
        }
        Assert.assertTrue(CollectionUtils.isEmpty(messageLines));
    }

}
