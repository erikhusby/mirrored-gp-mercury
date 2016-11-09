package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.broadinstitute.bsp.client.util.MessageCollection;
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
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationUtils;
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

import static org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb.SPLIT_DESIGNATION_MESSAGE;

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

    private final List<DesignationDto> testDtos = new ArrayList<>();

    private DesignationUtils designationUtils = new DesignationUtils(new DesignationUtils.Caller() {
        @Override
        public DesignationDto getMultiEdit() {
            return null;
        }

        @Override
        public void setMultiEdit(DesignationDto dto) {

        }

        @Override
        public List<DesignationDto> getDtos() {
            return testDtos;
        }
    });

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
        final LabEvent labEvent = getAnyNormEvent().iterator().next();

        // Sets up the flowcell types to be created.
        final IlluminaFlowcell.FlowcellType[] flowcellTypes = {
                IlluminaFlowcell.FlowcellType.MiSeqFlowcell,
                IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell
        };
        // Sets up the loading tubes to be put on flowcells.
        final LinkedHashMap<String, BarcodedTube>[] tubeMaps = new LinkedHashMap[]{
                new LinkedHashMap<>(), new LinkedHashMap<>()};
        for (Map.Entry<String, BarcodedTube> entry : mapBarcodeToTube.entrySet()) {
            if (tubeMaps[0].size() < 1) {
                tubeMaps[0].put(entry.getKey(), entry.getValue());
            } else if (tubeMaps[1].size() < 2) {
                tubeMaps[1].put(entry.getKey(), entry.getValue());
            }
        }

        final Map<LabVessel, Integer>[] vesselsLanes = new Map[]{
                new HashMap<LabVessel, Integer>() {{
                    put(tubeMaps[0].values().iterator().next(), 3);
                }},
                new HashMap<LabVessel, Integer>() {{
                    Iterator<BarcodedTube> iterator = tubeMaps[1].values().iterator();
                    put(iterator.next(), 5);
                    put(iterator.next(), 17);
                }}
        };

        bucket = labBatchTestUtils.putTubesInSpecificBucket(LabBatchEJBTest.EXTRACTION_BUCKET,
                BucketEntry.BucketEntryType.PDO_ENTRY, mapBarcodeToTube);

        boolean[] isClinicals = {false, false};

        // Sets up an lcset for each flowcell.
        LabBatch[] labBatches = new LabBatch[tubeMaps.length];
        for (int idx = 0; idx < tubeMaps.length; ++idx) {
            List<Long> bucketIds = new ArrayList<>();
            for (BarcodedTube barcodedTube : tubeMaps[idx].values()) {
                for (BucketEntry bucketEntry : barcodedTube.getBucketEntries()) {
                    bucketIds.add(bucketEntry.getBucketEntryId());
                    isClinicals[idx] = bucketEntry.getProductOrder().getResearchProject().getRegulatoryDesignation().
                            isClinical();
                }
            }

            labBatches[idx] = labBatchEJB.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                    Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName(), bucketIds, Collections.<Long>emptyList(),
                    "Batch_" + idx + System.currentTimeMillis(), "", new Date(), null, "epolk",
                    LabBatchEJBTest.EXTRACTION_BUCKET, MessageReporter.UNUSED, Collections.<String>emptyList());

            Assert.assertEquals(labBatches[idx].getLabBatchStartingVessels().size(), bucketIds.size());
        };

        // The tube barcode for each flowcell lane.
        List<String>[] barcodePerLane = new List[]{
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
                    barcodePerLane[idx].add(loadingTube.getLabel());
                }
                DesignationDto dto = new DesignationDto();
                dto.setBarcode(loadingTube.getLabel());
                dto.setEvents(Collections.singleton(labEvent));
                dto.setLcset(labBatches[idx].getBatchName());
                dto.setProductNames(Collections.singletonList("Exome Express v2"));
                dto.setNumberSamples(1);
                dto.setRegulatoryDesignation(isClinicals[idx] ? DesignationUtils.CLINICAL : DesignationUtils.RESEARCH);
                dto.setSequencerModel(flowcellTypes[idx]);
                dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
                dto.setReadLength(76);
                dto.setLoadingConc(BigDecimal.TEN);
                dto.setNumberLanes(numberLanes);
                dto.setStatus(FlowcellDesignation.Status.QUEUED);
                dto.setPoolTest(false);
                dto.setPairedEndRead(true);
                dto.setSelected(true);

                // Sets the dto with 17 lanes to be low priority. It will be split.
                if (numberLanes == 17) {
                    dto.setPriority(FlowcellDesignation.Priority.LOW);
                    splitDtoBarcode = dto.getBarcode();
                    splitDtoGrouping = dto.fctGrouping();
                }
                designationDtos.add(dto);
            }
        }

        // Makes the FCTs.
        List<MutablePair<String, String>> fctUrls = labBatchEJB.makeFcts(designationDtos, "epolk", messageReporter);
        Assert.assertFalse(messages.toString().contains(" invalid "), messages.toString());

        // Created the correct number of FCTs?
        int expectedNumberFcts = 0;
        for (int idx = 0; idx < flowcellTypes.length; ++idx) {
            expectedNumberFcts += barcodePerLane[idx].size() / flowcellTypes[idx].getVesselGeometry().getRowCount();
        }
        Assert.assertEquals(fctUrls.size(), expectedNumberFcts);
        labBatchDao.flush();

        // Checks the persisted flowcell designations against their dtos.
        Assert.assertFalse(designationDtos.isEmpty());
        for (DesignationDto dto : designationDtos) {
            Assert.assertNotNull(dto.getDesignationId());
            FlowcellDesignation flowcellDesignation = labBatchDao.findById(FlowcellDesignation.class,
                    dto.getDesignationId());
            Assert.assertNotNull(flowcellDesignation);
            Assert.assertEquals(dto.isAllocated(),
                    (flowcellDesignation.getStatus() == FlowcellDesignation.Status.IN_FCT));
            Assert.assertNull(flowcellDesignation.getChosenLcset());

            // Tests that a dto can be made from the persisted flowcell designation.
            MessageCollection errors = new MessageCollection();
            testDtos.clear();
            designationUtils.makeDtosFromDesignations(Collections.singleton(flowcellDesignation), errors);
            Assert.assertFalse(errors.hasErrors(), StringUtils.join(errors, "\n"));
            Assert.assertEquals(testDtos.size(), 1);
            Assert.assertEquals(testDtos.get(0).getBarcode(), dto.getBarcode());
            Assert.assertEquals(testDtos.get(0).getLcset(), dto.getLcset());
            Assert.assertEquals(testDtos.get(0).getStatus(), dto.getStatus());
            Assert.assertEquals(testDtos.get(0).getIndexType(), dto.getIndexType());
            Assert.assertEquals(testDtos.get(0).getPairedEndRead(), dto.getPairedEndRead());
            Assert.assertEquals(testDtos.get(0).getRegulatoryDesignation(), dto.getRegulatoryDesignation());
            Assert.assertEquals(testDtos.get(0).getLoadingConc(), dto.getLoadingConc());
            Assert.assertEquals(testDtos.get(0).getNumberLanes(), dto.getNumberLanes());
            Assert.assertEquals(testDtos.get(0).getNumberSamples(), dto.getNumberSamples());
            Assert.assertEquals(testDtos.get(0).getPoolTest(), dto.getPoolTest());
            Assert.assertEquals(testDtos.get(0).getPriority(), dto.getPriority());
            Assert.assertEquals(testDtos.get(0).getProductNameJoin(), dto.getProductNameJoin());
            Assert.assertEquals(testDtos.get(0).getReadLength(), dto.getReadLength());
            Assert.assertEquals(testDtos.get(0).getSequencerModel(), dto.getSequencerModel());
            Assert.assertEquals(testDtos.get(0).getTubeEventId(), dto.getTubeEventId());
        }

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
                        Assert.assertTrue(barcodePerLane[1].remove(splitDtoBarcode), splitDtoBarcode);
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
                    Assert.assertTrue(barcodePerLane[idx].remove(token), "At idx " + idx + " missing " + token);
                }
            }

        }

        // Verifies the one action bean message is about the split dto.
        String[] messageLines = messages.toString().split("\n");
        Assert.assertEquals(messageLines.length, 1, messages.toString());
        String splitMsg = MessageFormat.format(SPLIT_DESIGNATION_MESSAGE, splitDtoCount, splitDtoGrouping);
        Assert.assertEquals(messageLines[0], splitMsg);

        // Verifies the allocated loading tube barcodes all matched up with the expected ones.
        for (int idx = 0; idx < flowcellTypes.length; ++idx) {
            Assert.assertEquals(barcodePerLane[idx].size(), 0,
                    "at idx " + idx + " found " + StringUtils.join(barcodePerLane[idx], " "));
        }

    }

    /** Tests fct grouping of dto. */
    @Test
    public void testDesignationFctGrouping() throws Exception {
        final StringBuilder messages = new StringBuilder();
        final MessageReporter messageReporter = new MessageReporter() {
            @Override
            public String addMessage(String message, Object... arguments) {
                messages.append(String.format(message, arguments)).append("\n");
                return "";
            }
        };

        final IlluminaFlowcell.FlowcellType flowcellType = IlluminaFlowcell.FlowcellType.HiSeqFlowcell;
        final LabBatch lcset = new LabBatch("lcset0", Collections.EMPTY_SET, LabBatch.LabBatchType.WORKFLOW);
        List<DesignationDto> dtos = new ArrayList<>();
        final int numberLanes = 1;
        List<LabEvent> labEvents = getAnyNormEvent();

        for (int index = 0; index < 6; ++index) {
            if (CollectionUtils.isNotEmpty(labEvents.get(index).getSourceVesselTubes())) {
                LabVessel loadingTube = labEvents.get(index).getSourceVesselTubes().iterator().next();

                // dtos for indexes 0, 1, 2, 3 form singleton groupings.
                // dtos for indexes 4 & 5 will both be in one group.
                // None of the groupings will have enough lanes to form a complete FCT.

                String regulatoryDesignation = (index > 0) ? DesignationUtils.CLINICAL : DesignationUtils.RESEARCH;
                FlowcellDesignation.IndexType indexType = (index > 1) ?
                        FlowcellDesignation.IndexType.DUAL : FlowcellDesignation.IndexType.SINGLE;
                boolean pairedEnd = (index > 2);
                int readLength = (index > 3) ? 151 : 152;

                DesignationDto dto = new DesignationDto();
                dto.setBarcode(loadingTube.getLabel());
                dto.setEvents(Collections.singleton(labEvents.get(index)));
                dto.setLcset(lcset.getBatchName());
                dto.setProductNames(Collections.singletonList("Exome Express v2"));
                dto.setNumberSamples(1);
                dto.setRegulatoryDesignation(regulatoryDesignation);
                dto.setSequencerModel(flowcellType);
                dto.setIndexType(indexType);
                dto.setReadLength(readLength);
                dto.setLoadingConc(BigDecimal.TEN);
                dto.setNumberLanes(numberLanes);
                dto.setStatus(FlowcellDesignation.Status.QUEUED);
                dto.setPoolTest(false);
                dto.setPairedEndRead(pairedEnd);
                dto.setSelected(true);

                dtos.add(dto);
            }
        }

        // Should make no FCTs.
        Assert.assertTrue(CollectionUtils.isEmpty(labBatchEJB.makeFcts(dtos, "epolk", messageReporter)));

        // Should have no dto validation errors.
        Assert.assertFalse(messages.toString().contains(" invalid "), messages.toString());

        // Verifies there are 5 action bean messages are about the partial flowcells.
        List<String> messageLines = new ArrayList(Arrays.asList(messages.toString().split("\n")));
        for (int index = 0; index < 5; ++index) {
            DesignationDto dto = dtos.get(index);
            int emptyLanes = flowcellType.getVesselGeometry().getVesselPositions().length -
                             (index < 4 ? numberLanes : 2 * numberLanes);
            String msg = MessageFormat.format(LabBatchEjb.PARTIAL_FCT_MESSAGE, dto.fctGrouping(), emptyLanes);

            Assert.assertTrue(messageLines.remove(msg), "Expected: " + msg);
        }
        Assert.assertTrue(CollectionUtils.isEmpty(messageLines), "Unexpected: " + StringUtils.join(messageLines, ", "));
    }

    @Test
    public void testDesignationError() throws Exception {
        final StringBuilder messages = new StringBuilder();
        final MessageReporter messageReporter = new MessageReporter() {
            @Override
            public String addMessage(String message, Object... arguments) {
                messages.append(String.format(message, arguments)).append("\n");
                return "";
            }
        };
        final LabEvent labEvent = getAnyNormEvent().iterator().next();
        final LabVessel loadingTube = mapBarcodeToTube.values().iterator().next();
        final IlluminaFlowcell.FlowcellType flowcellType = IlluminaFlowcell.FlowcellType.MiSeqFlowcell;
        final LabBatch lcset = new LabBatch("lcset0", Collections.EMPTY_SET, LabBatch.LabBatchType.WORKFLOW);
        List<DesignationDto> dtos = new ArrayList<DesignationDto>() {{
            for (int i = 0; i < 2; ++i) {
                DesignationDto dto = new DesignationDto();
                dto.setBarcode(loadingTube.getLabel());
                dto.setEvents(Collections.singleton(labEvent));
                dto.setLcset(lcset.getBatchName());
                dto.setProductNames(Collections.singletonList("Exome Express v2"));
                dto.setNumberSamples(1);
                dto.setRegulatoryDesignation(i == 0 ? DesignationUtils.CLINICAL : DesignationUtils.RESEARCH);
                dto.setSequencerModel(flowcellType);
                dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
                dto.setReadLength(151);
                dto.setLoadingConc(BigDecimal.TEN);
                dto.setNumberLanes(i);
                dto.setStatus(FlowcellDesignation.Status.QUEUED);
                dto.setPoolTest(false);
                dto.setPairedEndRead(true);
                dto.setSelected(true);
                add(dto);
            }
        }};

        // Normally would make an FCT but the error should prevent it.
        Assert.assertTrue(CollectionUtils.isEmpty(labBatchEJB.makeFcts(dtos, "epolk", messageReporter)));

        // Verifies the action bean message is about the number of lanes.
        List<String> messageLines = Arrays.asList(messages.toString().split("\n"));
        Assert.assertEquals(messageLines.size(), 1, messages.toString());
        String msg = MessageFormat.format(LabBatchEjb.DESIGNATION_ERROR_MSG, loadingTube.getLabel());
        Assert.assertTrue(messageLines.get(0).startsWith(msg), messages.toString());
        Assert.assertTrue(messageLines.get(0).contains("number of lanes"), messages.toString());
    }

    private List<LabEvent> getAnyNormEvent() {
        return labBatchDao.findList(LabEvent.class, LabEvent_.labEventType, LabEventType.NORMALIZATION_TRANSFER);
    }

}
