package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.FCTJiraFieldFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraLaneInfo;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationUtils;
import org.broadinstitute.gpinformatics.mercury.presentation.workflow.CreateFctDto;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell;
import static org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell;
import static org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell.FlowcellType.HiSeqFlowcell;
import static org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell.FlowcellType.MiSeqFlowcell;

/**
 * Tests workflow and flowcell lab batch creation and accompanying JIRA tickets.
 */
@Test(groups = TestGroups.STANDARD)
public class LabBatchEjbStandardTest extends Arquillian {

    @Inject
    private LabBatchEjb labBatchEJB;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private UserTransaction utx;

    @Inject
    private JiraService jiraService;

    @Inject
    private LabBatchTestUtils labBatchTestUtils;

    @Inject
    private FlowcellDesignationEjb flowcellDesignationEjb;

    /**
     * Need this here because Arquillian CDI enricher does something strange with scopes <br/>
     * See note in BatchToJiraTest
     */
    @Inject
    private WorkflowConfig workflowConfig;

    private Bucket bucket;
    private boolean isClinical;
    private LabBatch labBatch;
    private LinkedHashMap<String, BarcodedTube> tubeMap = new LinkedHashMap<>();
    private LinkedHashMap<BarcodedTube, Integer> lanesPerTubeMap = new LinkedHashMap<>();

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

    @Test(groups = TestGroups.STANDARD)
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
                Workflow.ICE_CRSP, bucketIds, Collections.<Long>emptyList(),
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

        labBatchEJB.updateLabBatch(testFind.getJiraTicket().getTicketId(),
                Collections.singletonList(vessel.getBucketEntries().iterator().next().getBucketEntryId()),
                Collections.<Long>emptyList(), Collections.<Long>emptyList(),LabBatchEJBTest.BUCKET_NAME,
                MessageReporter.UNUSED, Collections.<String>emptyList());

        jiraIssue = jiraService.getIssue(testFind.getJiraTicket().getTicketName());

        Assert.assertEquals(jiraIssue.getSummary(), nameForBatch);
    }

    @Test(groups = TestGroups.STANDARD)
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
                Workflow.DNA_RNA_EXTRACTION_CELL_PELLETS, bucketIds, Collections.<Long>emptyList(),
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

        labBatchEJB.updateLabBatch(testFind.getJiraTicket().getTicketId(),
                Collections.singletonList(vessel.getBucketEntries().iterator().next().getBucketEntryId()),
                Collections.<Long>emptyList(), Collections.<Long>emptyList(), LabBatchEJBTest.EXTRACTION_BUCKET,
                MessageReporter.UNUSED, Collections.<String>emptyList());

        jiraIssue = jiraService.getIssue(testFind.getJiraTicket().getTicketName());

        Assert.assertEquals(jiraIssue.getSummary(), nameForBatch);
    }

    /** Tests FCT and JIRA ticket without using designations. */
    @Test(groups = TestGroups.STANDARD)
    public void testCreateFCTLabBatch() throws Exception {
        //Create FCT lab batch
        List<LabBatch.VesselToLanesInfo> laneInfos = new ArrayList<>();
        VesselPosition[] hiseq4000VesselPositions =
                HiSeq4000Flowcell.getVesselGeometry().getVesselPositions();
        List<VesselPosition> vesselPositionList = asList(hiseq4000VesselPositions);
        BarcodedTube barcodedTube = mapBarcodeToTube.entrySet().iterator().next().getValue();
        String lcset = "LCSET-0003";
        String productNames = "CP Human WES (150xMTC)" + DesignationDto.DELIMITER + "Controls";
        LabBatch.VesselToLanesInfo vesselToLanesInfo =
                new LabBatch.VesselToLanesInfo(vesselPositionList, BigDecimal.valueOf(13.11f), barcodedTube,
                        lcset, productNames, Collections.<FlowcellDesignation>emptyList());
        laneInfos.add(vesselToLanesInfo);
        LabBatch fctLabBatch = new LabBatch("Test FCT batch name", laneInfos,
                LabBatch.LabBatchType.FCT, HiSeq4000Flowcell);
        fctLabBatch.setBatchDescription(fctLabBatch.getBatchName());
        labBatchEJB.createLabBatch(fctLabBatch, "jowalsh", CreateFields.IssueType.HISEQ_4000,
                CreateFields.ProjectType.FCT_PROJECT);

        labBatchDao.flush();
        labBatchDao.clear();

        final String batchName = fctLabBatch.getBatchName();

        LabBatch testFind = labBatchDao.findByName(batchName);
        Assert.assertNotNull(testFind);
        Assert.assertNotNull(testFind.getJiraTicket());
        Assert.assertNotNull(testFind.getJiraTicket().getTicketName());
        Assert.assertEquals(testFind.getJiraTicket().getTicketName(), batchName);
        Assert.assertNotNull(testFind.getStartingBatchLabVessels());

        JiraIssue jiraIssue = jiraService.getIssue(batchName);
        String issueTypeValue = jiraIssue.getMappedField(LabBatch.TicketFields.ISSUE_TYPE_MAP.getName(),
                LabBatch.TicketFields.ISSUE_TYPE_NAME.getName());
        Assert.assertEquals(issueTypeValue, CreateFields.IssueType.HISEQ_4000.getJiraName(), batchName);

        Object laneInfoValue = jiraIssue.getField(LabBatch.TicketFields.LANE_INFO.getName());
        Assert.assertNotNull(laneInfoValue, batchName);
        String laneInfo = (String) laneInfoValue;
        Assert.assertTrue(laneInfo.startsWith(FCTJiraFieldFactory.LANE_INFO_HEADER), batchName);
        int counter = 1;
        for (JiraLaneInfo laneInfoRow : FCTJiraFieldFactory.parseJiraLaneInfo(laneInfo)) {
            Assert.assertEquals(laneInfoRow.getLane(), "LANE" + counter++, batchName);
            Assert.assertEquals(laneInfoRow.getLoadingVessel(), "R111111SM-423", batchName);
            Assert.assertEquals(laneInfoRow.getLoadingConc(), "13.109999656677246", batchName);
            Assert.assertEquals(laneInfoRow.getLcset(), "LCSET-0003", batchName);
            Assert.assertTrue(laneInfoRow.getProductNames().contains("CP Human WES (150xMTC)"),
                    laneInfoRow.getProductNames());
            Assert.assertTrue(laneInfoRow.getProductNames().contains("Controls"), laneInfoRow.getProductNames());
        }
    }

    /** Tests FCT and JIRA ticket using the CreateFctDto type of designations. */
    @Test(groups = TestGroups.STANDARD)
    public void testCreateFctFromCreateFctDtos() throws Exception {
        // Iterates on the loading tube barcodes to be used in the test.
        Iterator<String> barcodeIterator = mapBarcodeToTube.keySet().iterator();
        // Defines the number of tubes used for each test run, and the number of lanes to be allocated for each tube.
        int[][] numberLanes = {{3}, {1, 3}};
        // Defines the flowcell type to be used on each test run.
        IlluminaFlowcell.FlowcellType[] flowcellTypes = {MiSeqFlowcell, HiSeq2500Flowcell};

        for (int runIdx = 0; runIdx < numberLanes.length; ++runIdx) {

            setupForFctDtos(numberLanes[runIdx], barcodeIterator);

            // Makes CreateFCT action bean dtos.
            List<CreateFctDto> dtos = new ArrayList<>();
            String productName = runIdx == 1 ?
                    "Exome Express v2" + DesignationDto.DELIMITER + "Another Product" : "Exome Express v2";
            Map<String, String> barcodeToLcset = new HashMap<>();
            int laneCount = 0;

            for (LabVessel loadingTube : lanesPerTubeMap.keySet()) {
                CreateFctDto dto = new CreateFctDto();
                dto.setBarcode(loadingTube.getLabel());
                dto.setLcset(labBatch.getBatchName());
                dto.setProduct(productName);
                Assert.assertEquals(dto.getProduct(), productName); // (tests the split/join in the getter/setter.)
                dto.setNumberSamples(1);
                dto.setRegulatoryDesignation(isClinical ? DesignationUtils.CLINICAL : DesignationUtils.RESEARCH);
                dto.setReadLength(76);
                dto.setLoadingConc(BigDecimal.TEN);
                dto.setNumberLanes(lanesPerTubeMap.get(loadingTube));
                laneCount += lanesPerTubeMap.get(loadingTube);
                dtos.add(dto);
                barcodeToLcset.put(dto.getBarcode(), dto.getLcset());
            }

            // Makes the FCTs.
            final StringBuilder messages = new StringBuilder();
            List<LabBatch> fcts = labBatchEJB.makeFcts(dtos, flowcellTypes[runIdx], "epolk", new MessageReporter() {
                @Override
                public String addMessage(String message, Object... arguments) {
                    messages.append(String.format(message, arguments)).append("\n");
                    return "";
                }
            });
            Assert.assertFalse(messages.toString().contains(" invalid "), messages.toString());
            Assert.assertEquals(fcts.size(), laneCount / flowcellTypes[runIdx].getVesselGeometry().getCapacity());

            // Checks the lane info on the jira ticket.
            for (LabBatch fct : fcts) {
                JiraIssue jiraIssue = jiraService.getIssue(fct.getJiraTicket().getTicketName());
                Assert.assertNotNull(jiraIssue);
                String laneInfo = (String) (jiraIssue.getField(LabBatch.TicketFields.LANE_INFO.getName()));
                Assert.assertNotNull(laneInfo);
                for (JiraLaneInfo laneInfoRow : FCTJiraFieldFactory.parseJiraLaneInfo(laneInfo)) {
                    Assert.assertTrue(tubeMap.containsKey(laneInfoRow.getLoadingVessel()));
                    Assert.assertEquals(laneInfoRow.getLoadingConc(), "10");
                    Assert.assertEquals(laneInfoRow.getLcset(), barcodeToLcset.get(laneInfoRow.getLoadingVessel()));
                    // Jira ticket has '\\' delimited product names, not '<br/>'.
                    Assert.assertEquals(laneInfoRow.getProductNames(),
                            FCTJiraFieldFactory.replaceDtoDelimiter(productName));
                }
            }
        }
    }

    /** Tests FCT and JIRA ticket using the DesignationDto type of designations. */
    @Test
    public void testFctsFromDesignations() throws Exception {
        // Iterates on the loading tube barcodes to be used in the test.
        Iterator<String> barcodeIterator = mapBarcodeToTube.keySet().iterator();
        // Defines the number of tubes used for each test run, and the number of lanes to be allocated for each tube.
        int[][] numberLanes = {{3}, {5, 17}};
        // Defines the flowcell type to be used on each test run.
        IlluminaFlowcell.FlowcellType[] flowcellTypes = {MiSeqFlowcell, HiSeq4000Flowcell};

        for (int runIdx = 0; runIdx < numberLanes.length; ++runIdx) {

            setupForFctDtos(numberLanes[runIdx], barcodeIterator);

            String productName = "Exome Express v2";
            String splitDtoBarcode = null;
            String splitDtoGrouping = null;
            Map<String, String> barcodeToLcset = new HashMap<>();
            int laneCount = 0;
            // Makes action bean dtos that are queued designations.
            List<DesignationDto> designationDtos = new ArrayList<>();
            for (LabVessel loadingTube : lanesPerTubeMap.keySet()) {
                DesignationDto dto = new DesignationDto();
                dto.setBarcode(loadingTube.getLabel());
                dto.setLcset(labBatch.getBatchName());
                dto.setProduct(productName);
                Assert.assertEquals(dto.getProduct(), productName);
                dto.setNumberSamples(1);
                dto.setRegulatoryDesignation(isClinical ? DesignationUtils.CLINICAL : DesignationUtils.RESEARCH);
                dto.setSequencerModel(flowcellTypes[runIdx]);
                dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
                dto.setReadLength(76);
                dto.setLoadingConc(BigDecimal.TEN);
                dto.setNumberLanes(lanesPerTubeMap.get(loadingTube));
                laneCount += lanesPerTubeMap.get(loadingTube);
                dto.setStatus(FlowcellDesignation.Status.QUEUED);
                dto.setPoolTest(false);
                dto.setPairedEndRead(true);
                dto.setSelected(true);

                // Sets the dto with 17 lanes to be low priority. It will be split.
                if (dto.getNumberLanes() == 17) {
                    dto.setPriority(FlowcellDesignation.Priority.LOW);
                    splitDtoBarcode = dto.getBarcode();
                    splitDtoGrouping = LabBatchEjb.dtoGroupDescription(dto);
                }
                designationDtos.add(dto);
                barcodeToLcset.put(dto.getBarcode(), dto.getLcset());
            }

            // Creates FlowcellDesignations from the dtos.
            Assert.assertFalse(designationDtos.isEmpty());
            DesignationUtils.updateDesignationsAndDtos(designationDtos,
                    EnumSet.allOf(FlowcellDesignation.Status.class), flowcellDesignationEjb);

            Set<String> expectedBarcodes = new HashSet<>(barcodeToLcset.keySet());
            Set<Long> expectedDesignationIds = new HashSet<>();

            // Checks the persisted flowcell designations against their dtos.
            for (DesignationDto dto : designationDtos) {
                Assert.assertNotNull(dto.getDesignationId());
                FlowcellDesignation flowcellDesignation = labBatchDao.findById(FlowcellDesignation.class,
                        dto.getDesignationId());
                Assert.assertNotNull(flowcellDesignation);
                expectedDesignationIds.add(flowcellDesignation.getDesignationId());
                Assert.assertFalse(dto.getAllocatedLanes() == dto.getNumberLanes());
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
                Assert.assertEquals(testDtos.get(0).getProduct(), dto.getProduct());
                Assert.assertEquals(testDtos.get(0).getReadLength(), dto.getReadLength());
                Assert.assertEquals(testDtos.get(0).getSequencerModel(), dto.getSequencerModel());
                Assert.assertEquals(testDtos.get(0).getTubeType(), dto.getTubeType());
                Assert.assertEquals(testDtos.get(0).getTubeDate(), dto.getTubeDate());
            }
            Assert.assertEquals(expectedDesignationIds.size(), numberLanes[runIdx].length);

            for (DesignationDto dto : designationDtos) {
                dto.setSelected(true);
            }

            // Makes the FCTs.
            final StringBuilder messages = new StringBuilder();
            List<MutablePair<String, String>> fctUrls = labBatchEJB.makeFcts(designationDtos, "epolk",
                    new MessageReporter() {
                        @Override
                        public String addMessage(String message, Object... arguments) {
                            messages.append(String.format(message, arguments)).append("\n");
                            return "";
                        }
                    }, false);
            Assert.assertFalse(messages.toString().contains(" invalid "), messages.toString());

            // Created the correct number of FCTs?
            Assert.assertEquals(fctUrls.size(), laneCount / flowcellTypes[runIdx].getVesselGeometry().getCapacity());
            labBatchDao.flush();

            // Checks for the split dto.
            int splitDtoCount = 0;
            for (DesignationDto dto : designationDtos) {
                if (dto.getBarcode().equals(splitDtoBarcode)) {
                    if (dto.getAllocatedLanes() == dto.getNumberLanes()) {
                        Assert.assertEquals((int) dto.getNumberLanes(), 11);
                    } else {
                        ++splitDtoCount;
                        Assert.assertEquals((int) dto.getNumberLanes(), 6);
                    }
                } else {
                    Assert.assertTrue(dto.getAllocatedLanes() == dto.getNumberLanes());
                }
            }
            Assert.assertEquals(splitDtoCount, splitDtoBarcode == null ? 0 : 1);
            Set<String> foundBarcodes = new HashSet<>();
            Set<Long> foundDesignationIds = new HashSet<>();

            for (MutablePair<String, String> fctUrl : fctUrls) {
                String fctName = fctUrl.getLeft();
                Assert.assertTrue(fctUrl.getRight().contains(fctName), fctUrl.getRight() + " " + fctName);

                LabBatch fctLabBatch = labBatchDao.findByName(fctName);
                Assert.assertNotNull(fctLabBatch);

                Assert.assertEquals(fctLabBatch.getFlowcellType(), flowcellTypes[runIdx]);

                // Gets the per-lane allocations from the jira ticket.
                JiraIssue jiraIssue = jiraService.getIssue(fctLabBatch.getJiraTicket().getTicketName());
                Assert.assertNotNull(jiraIssue);
                String laneInfo = (String) (jiraIssue.getField(LabBatch.TicketFields.LANE_INFO.getName()));
                Assert.assertNotNull(laneInfo);
                for (JiraLaneInfo laneInfoRow : FCTJiraFieldFactory.parseJiraLaneInfo(laneInfo)) {
                    Assert.assertEquals(laneInfoRow.getLoadingConc(), "10");
                    Assert.assertEquals(laneInfoRow.getLcset(), barcodeToLcset.get(laneInfoRow.getLoadingVessel()));
                    Assert.assertEquals(laneInfoRow.getProductNames(), productName);
                    foundBarcodes.add(laneInfoRow.getLoadingVessel());
                }

                // Gets the starting batch vessels' designation ids.
                for (LabBatchStartingVessel startingVessel : fctLabBatch.getLabBatchStartingVessels()) {
                    Assert.assertNotNull(startingVessel.getFlowcellDesignation());
                    foundDesignationIds.add(startingVessel.getFlowcellDesignation().getDesignationId());
                }
            }
            if (splitDtoCount > 0) {
                // Verifies the one action bean message is about the split dto.
                String[] messageLines = messages.toString().split("\n");
                Assert.assertEquals(messageLines.length, 1, messages.toString());
                String splitMsg =
                        MessageFormat.format(LabBatchEjb.SPLIT_DESIGNATION_MESSAGE, splitDtoCount, splitDtoGrouping);
                Assert.assertEquals(messageLines[0], splitMsg);
            }

            // Verifies the allocated loading tube barcodes all matched up with the expected ones.
            Assert.assertTrue(CollectionUtils.disjunction(foundBarcodes, expectedBarcodes).isEmpty(), "Mismatches: " +
                    StringUtils.join(CollectionUtils.disjunction(foundBarcodes, expectedBarcodes), ", "));
            // Verifies the starting batch flowcell designations are correct.
            Assert.assertTrue(CollectionUtils.disjunction(foundDesignationIds, expectedDesignationIds).isEmpty(),
                    "Mismatches: " +
                    StringUtils.join(CollectionUtils.disjunction(foundDesignationIds, expectedDesignationIds), ", "));
        }
    }

    /** Tests fct grouping of dto. */
    @Test(groups = TestGroups.STANDARD)
    public void testDesignationFctGrouping() throws Exception {
        final StringBuilder messages = new StringBuilder();
        final MessageReporter messageReporter = new MessageReporter() {
            @Override
            public String addMessage(String message, Object... arguments) {
                messages.append(String.format(message, arguments)).append("\n");
                return "";
            }
        };

        final IlluminaFlowcell.FlowcellType flowcellType = HiSeqFlowcell;
        final LabBatch lcset = new LabBatch("lcset0", Collections.EMPTY_SET, LabBatch.LabBatchType.WORKFLOW);
        List<DesignationDto> dtos = new ArrayList<>();
        final int numberLanes = 1;

        for (int index = 0; index < 6; ++index) {

            // dtos for indexes 0, 1, 2, 3 form singleton groupings.
            // dtos for indexes 4 & 5 will both be in one group.
            // None of the groupings will have enough lanes to form a complete FCT.

            String regulatoryDesignation = (index > 0) ? DesignationUtils.CLINICAL : DesignationUtils.RESEARCH;
            FlowcellDesignation.IndexType indexType = (index > 1) ?
                    FlowcellDesignation.IndexType.DUAL : FlowcellDesignation.IndexType.SINGLE;
            boolean pairedEnd = (index > 2);
            int readLength = (index > 3) ? 151 : 152;

            DesignationDto dto = new DesignationDto();
            dto.setBarcode("tube label " + index);
            dto.setLcset(lcset.getBatchName());
            dto.setProduct("Exome Express v2");
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

        // Creates FlowcellDesignations from the dtos.
        Assert.assertFalse(dtos.isEmpty());
        DesignationUtils.updateDesignationsAndDtos(dtos, EnumSet.allOf(FlowcellDesignation.Status.class),
                flowcellDesignationEjb);

        for (DesignationDto dto : dtos) {
            dto.setSelected(true);
        }

        // Should make no FCTs.
        Assert.assertTrue(CollectionUtils.isEmpty(labBatchEJB.makeFcts(dtos, "epolk", messageReporter, false)));

        // Should have no dto validation errors.
        Assert.assertFalse(messages.toString().contains(" invalid "), messages.toString());

        // Verifies there are 5 action bean messages are about the partial flowcells.
        List<String> messageLines = new ArrayList(asList(messages.toString().split("\n")));
        for (int index = 0; index < 5; ++index) {
            DesignationDto dto = dtos.get(index);
            int emptyLanes = flowcellType.getVesselGeometry().getVesselPositions().length -
                             (index < 4 ? numberLanes : 2 * numberLanes);
            String msg = MessageFormat.format(LabBatchEjb.PARTIAL_FCT_MESSAGE, LabBatchEjb.dtoGroupDescription(dto),
                    emptyLanes);
            Assert.assertTrue(messageLines.remove(msg), "Expected: " + msg);
        }
        Assert.assertTrue(CollectionUtils.isEmpty(messageLines), "Unexpected: " + StringUtils.join(messageLines, ", "));
    }

    @Test(groups = TestGroups.STANDARD)
    public void testDesignationMix() throws Exception {
        final StringBuilder messages = new StringBuilder();
        MessageReporter messageReporter = new MessageReporter() {
            @Override
            public String addMessage(String message, Object... arguments) {
                messages.append(String.format(message, arguments)).append("\n");
                return "";
            }
        };

        // Should fail to put Exome mix of clinical and research on one FCT.
        List<MutablePair<String, String>> list = designationErrorHelper(messages, messageReporter,
                Pair.of("Exome Express v2", DesignationUtils.CLINICAL),
                Pair.of("Exome Express v2", DesignationUtils.RESEARCH));
        Assert.assertEquals(list.size(), 0);
        String[] message = messages.toString().split("\\n");
        // Verfies two partially filled FCTs.
        Assert.assertTrue(message[0].contains("makes partially filled FCT with 1 empty lane"), message[0]);
        Assert.assertTrue(message[1].contains("makes partially filled FCT with 1 empty lane"), message[1]);

        // Should fail to validate an Exome mixed designation.
        messages.setLength(0);
        list = designationErrorHelper(messages, messageReporter,
                Pair.of("Exome Express v2", DesignationUtils.MIXED),
                Pair.of("Exome Express v2", DesignationUtils.MIXED));
        Assert.assertEquals(list.size(), 0, messages.toString());
        Assert.assertTrue(messages.toString().contains("has invalid regulatory designation (Clinical and Research)"),
                messages.toString());

        // Makes Genome mixed designation FCT just fine.
        messages.setLength(0);
        list = designationErrorHelper(messages, messageReporter,
                Pair.of("CLIA PCR-Free Whole Genome", DesignationUtils.CLINICAL),
                Pair.of("PCR-Free Human WGS - 30x v1", DesignationUtils.RESEARCH));
        Assert.assertEquals(list.size(), 1, messages.toString());

        // Validates a Genome mixed designation just fine.
        messages.setLength(0);
        list = designationErrorHelper(messages, messageReporter,
                Pair.of("CLIA PCR-Free Whole Genome", DesignationUtils.MIXED),
                Pair.of("PCR-Free Human WGS - 30x v1", DesignationUtils.MIXED));
        Assert.assertEquals(list.size(), 1, messages.toString());
        Assert.assertEquals(messages.length(), 0, messages.toString());
    }

    private List<MutablePair<String, String>> designationErrorHelper(StringBuilder messages,
            MessageReporter messageReporter, final Pair<String, String>... productAndRegulatoryDesignations) {

        final LabVessel loadingTube = mapBarcodeToTube.values().iterator().next();
        final IlluminaFlowcell.FlowcellType flowcellType = HiSeq2500Flowcell;
        final LabBatch lcset = new LabBatch("lcset0", Collections.EMPTY_SET, LabBatch.LabBatchType.WORKFLOW);
        List<DesignationDto> dtos = new ArrayList<DesignationDto>() {{
            for (Pair<String, String> productAndRegulatoryDesignation : productAndRegulatoryDesignations) {
                DesignationDto dto = new DesignationDto();
                dto.setBarcode(loadingTube.getLabel());
                dto.setLcset(lcset.getBatchName());
                dto.setProduct(productAndRegulatoryDesignation.getLeft());
                dto.setNumberSamples(1);
                dto.setRegulatoryDesignation(productAndRegulatoryDesignation.getRight());
                dto.setSequencerModel(flowcellType);
                dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
                dto.setReadLength(151);
                dto.setLoadingConc(BigDecimal.TEN);
                dto.setNumberLanes(1);
                dto.setStatus(FlowcellDesignation.Status.QUEUED);
                dto.setPoolTest(false);
                dto.setPairedEndRead(true);
                dto.setSelected(true);
                add(dto);
            }
        }};

        // Creates FlowcellDesignations from the dtos.
        Assert.assertFalse(dtos.isEmpty());
        DesignationUtils.updateDesignationsAndDtos(dtos, EnumSet.allOf(FlowcellDesignation.Status.class),
                flowcellDesignationEjb);

        for (DesignationDto dto : dtos) {
            dto.setSelected(true);
        }
        return labBatchEJB.makeFcts(dtos, "epolk", messageReporter, false);
    }

    /**
     * Puts loading tubes in an LCSET (the flowcell's linked lcset) as setup for a flowcell batch test.
     * @param numberLanes defines both how many loading tubes, and how many lanes to allocate for each tube.
     * @param barcodeIterator is used to pick the loading tube barcode.
     */
    private void setupForFctDtos(int[] numberLanes, Iterator<String> barcodeIterator) throws Exception {

        tubeMap.clear();
        lanesPerTubeMap.clear();
        for (int i = 0; i < numberLanes.length; ++i) {
            String barcode = barcodeIterator.next();
            tubeMap.put(barcode, mapBarcodeToTube.get(barcode));
            lanesPerTubeMap.put(mapBarcodeToTube.get(barcode), numberLanes[i]);
        }

        // Buckets the loading tubes and creates an LCSET.
        bucket = labBatchTestUtils.putTubesInSpecificBucket(LabBatchEJBTest.BUCKET_NAME,
                BucketEntry.BucketEntryType.PDO_ENTRY, tubeMap);

        isClinical = false;
        List<Long> bucketIds = new ArrayList<>();
        for (BarcodedTube barcodedTube : tubeMap.values()) {
            for (BucketEntry bucketEntry : barcodedTube.getBucketEntries()) {
                bucketIds.add(bucketEntry.getBucketEntryId());
                isClinical = bucketEntry.getProductOrder().getResearchProject().getRegulatoryDesignation().
                        isClinical();
            }
        }

        labBatch = labBatchEJB.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                Workflow.AGILENT_EXOME_EXPRESS, bucketIds, Collections.<Long>emptyList(),
                "Batch_" + System.currentTimeMillis(), "", new Date(), null, "epolk",
                LabBatchEJBTest.BUCKET_NAME, MessageReporter.UNUSED, Collections.<String>emptyList());

        Assert.assertEquals(labBatch.getLabBatchStartingVessels().size(), bucketIds.size());
    }
}
