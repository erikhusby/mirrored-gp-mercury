package org.broadinstitute.gpinformatics.mercury.presentation.storage;

import com.opencsv.stream.reader.LineReader;
import net.sourceforge.stripes.action.Message;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.validation.ValidationError;
import net.sourceforge.stripes.validation.ValidationErrors;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.presentation.StripesMockTestUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemOfRecord;
import org.broadinstitute.gpinformatics.mercury.boundary.storage.StorageEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.junit.Assert;
import org.mockito.AdditionalMatchers;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@Test(groups = TestGroups.DATABASE_FREE, singleThreaded = true)
public class SrsPickWorkspaceActionBeanTest extends BaseEventTest {

    private LabBatchDao labBatchDaoMock;
    private LabVesselDao labVesselDaoMock;
    private StorageLocationDao storageLocationDaoMock;
    private StorageEjb storageEjbMock;
    private UserBean testUserBean;

    LabBatch srsBatch01, srsBatch02, srsBatch03;

    private RackOfTubes rackOfTubes01, rackOfTubes02, rackOfTubes03, rackOfTubes04;
    private TubeFormation tubes01, tubes02, tubes03, tubes04;

    StorageLocation rack1, rack2;
    StorageLocation slot1, slot2;

    @BeforeClass
    public void initClass() {
        BspUser testUser = new BSPUserList.QADudeUser("Test", BSPManagerFactoryStub.QA_DUDE_USER_ID);
        testUserBean = new UserBean();
        try {
            Field bspUserField = UserBean.class.getDeclaredField("bspUser");
            bspUserField.setAccessible(true);
            bspUserField.set(testUserBean, testUser);
        } catch (Exception ex) {
            System.out.println("Failure enabling reflection on bspUser: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    @BeforeMethod
    public void setUp() {

        try {
            expectedRouting = SystemOfRecord.System.MERCURY;
            super.setUp();

            labBatchDaoMock = mock(LabBatchDao.class);
            labVesselDaoMock = mock(LabVesselDao.class);
            storageLocationDaoMock = mock(StorageLocationDao.class);
            storageEjbMock = mock(StorageEjb.class);

            // ******** Build out some SRS batches **********
            Field batchIdField = LabBatch.class.getDeclaredField("labBatchId");
            batchIdField.setAccessible(true);

            Field storageIdField = StorageLocation.class.getDeclaredField("storageLocationId");
            storageIdField.setAccessible(true);

            // Put a couple dummy batches in place, oldest first
            List<LabBatch> initialBatches = new ArrayList<>();
            srsBatch01 = new LabBatch("SRS Batch 01", Collections.emptySet(), LabBatch.LabBatchType.SRS);
            batchIdField.set(srsBatch01, 1L);
            initialBatches.add(srsBatch01);
            when(labBatchDaoMock.findById(LabBatch.class, 1L)).thenReturn(srsBatch01);

            srsBatch02 = new LabBatch("SRS Batch 02", Collections.emptySet(), LabBatch.LabBatchType.SRS);
            batchIdField.set(srsBatch02, 2L);
            initialBatches.add(srsBatch02);
            when(labBatchDaoMock.findById(LabBatch.class, 2L)).thenReturn(srsBatch02);

            srsBatch03 = new LabBatch("SRS Batch 03", Collections.emptySet(), LabBatch.LabBatchType.SRS);
            srsBatch03.setActive(false);
            batchIdField.set(srsBatch03, 3L);
            initialBatches.add(srsBatch03);
            when(labBatchDaoMock.findById(LabBatch.class, 3L)).thenReturn(srsBatch03);

            when(labBatchDaoMock.findByTypeAndActiveStatus(LabBatch.LabBatchType.SRS, Boolean.TRUE)).thenReturn(initialBatches);

            // Build out some slots, no hierarchy required in this use case
            slot1 = new StorageLocation("Slot 1", StorageLocation.LocationType.SLOT, rack1);
            slot1.setStorageCapacity(1);
            storageIdField.set(slot1, 1000L);
            slot2 = new StorageLocation("Slot 2", StorageLocation.LocationType.SLOT, rack2);
            slot2.setStorageCapacity(1);
            storageIdField.set(slot2, 2000L);
//            slot3 = new StorageLocation("Slot 3", StorageLocation.LocationType.SLOT, rack3);
//            slot3.setStorageCapacity(1);
//            storageIdField.set(slot3, 3000L);

            // *********** Build out racks for batches *************
            rackOfTubes01 = new RackOfTubes("rack01", RackOfTubes.RackType.Matrix96);
            rackOfTubes02 = new RackOfTubes("rack02", RackOfTubes.RackType.Matrix96);
            rackOfTubes03 = new RackOfTubes("rack03", RackOfTubes.RackType.Matrix96);
            rackOfTubes04 = new RackOfTubes("rack04", RackOfTubes.RackType.Matrix96);

            Map<VesselPosition, BarcodedTube> layout01 = new HashMap<>();
            Map<VesselPosition, BarcodedTube> layout02 = new HashMap<>();
            Map<VesselPosition, BarcodedTube> layout03 = new HashMap<>();
            Map<VesselPosition, BarcodedTube> layout04 = new HashMap<>();

            BarcodedTube tube;
            for (VesselPosition pos : VesselGeometry.G12x8.getVesselPositions()) {
                // Batch 01 gets one rack
                tube = new BarcodedTube("tube_a_" + pos, BarcodedTube.BarcodedTubeType.MatrixTube);
                tube.setStorageLocation(slot1);
                layout01.put(pos, tube);
                srsBatch01.getLabBatchStartingVessels().add(new LabBatchStartingVessel(tube, srsBatch01));
                // Pick verify needs to find rack01 tubes!
                when(labVesselDaoMock.findByIdentifier(tube.getLabel())).thenReturn(tube);

                // Batch 02 gets two racks
                tube = new BarcodedTube("tube_b_" + pos, BarcodedTube.BarcodedTubeType.MatrixTube);
                tube.setStorageLocation(slot2);
                layout02.put(pos, tube);
                srsBatch02.getLabBatchStartingVessels().add(new LabBatchStartingVessel(tube, srsBatch02));

                tube = new BarcodedTube("tube_c_" + pos, BarcodedTube.BarcodedTubeType.MatrixTube);
                layout03.put(pos, tube);
                srsBatch02.getLabBatchStartingVessels().add(new LabBatchStartingVessel(tube, srsBatch02));

                tube = new BarcodedTube("tube_d_" + pos, BarcodedTube.BarcodedTubeType.MatrixTube);
                layout04.put(pos, tube);
                srsBatch03.getLabBatchStartingVessels().add(new LabBatchStartingVessel(tube, srsBatch03));
            }

            tubes01 = new TubeFormation(layout01, RackOfTubes.RackType.Matrix96);
            rackOfTubes01.getTubeFormations().add(tubes01);
            rackOfTubes01.setStorageLocation(slot1);
            tubes02 = new TubeFormation(layout02, RackOfTubes.RackType.Matrix96);
            rackOfTubes02.getTubeFormations().add(tubes02);
            rackOfTubes02.setStorageLocation(slot2);
            tubes03 = new TubeFormation(layout03, RackOfTubes.RackType.Matrix96);
            rackOfTubes01.getTubeFormations().add(tubes03);
            tubes04 = new TubeFormation(layout04, RackOfTubes.RackType.Matrix96);
            rackOfTubes04.getTubeFormations().add(tubes04);

            // Check in rack01
            when(storageLocationDaoMock.findById(StorageLocation.class, 1000L)).thenReturn(slot1);
            when(storageLocationDaoMock.getLocationTrail(slot1)).thenReturn("Trail to slot1");
            LabEvent rack01_inPlace = new LabEvent(LabEventType.STORAGE_CHECK_IN, new Date(), "TEST"
                    , 1L, BSPManagerFactoryStub.QA_DUDE_USER_ID, "");
            rack01_inPlace.setAncillaryInPlaceVessel(rackOfTubes01);
            rack01_inPlace.setInPlaceLabVessel(tubes01);
            rack01_inPlace.setStorageLocation(slot1);
            tubes01.addInPlaceEvent(rack01_inPlace);
            rackOfTubes01.getAncillaryInPlaceEvents().add(rack01_inPlace);
            Assert.assertEquals(rackOfTubes01.getAncillaryInPlaceEvents().size(), 1);
            Assert.assertEquals(rackOfTubes01.getStorageLocation().getLabel(), "Slot 01");

            // Check in rack02
            when(storageLocationDaoMock.findById(StorageLocation.class, 2000L)).thenReturn(slot2);
            when(storageLocationDaoMock.getLocationTrail(slot2)).thenReturn("Trail to slot2");
            LabEvent rack02_inPlace = new LabEvent(LabEventType.IN_PLACE, new Date(), "TEST"
                    , 1L, BSPManagerFactoryStub.QA_DUDE_USER_ID, "");
            rack02_inPlace.setAncillaryInPlaceVessel(rackOfTubes02);
            rack02_inPlace.setInPlaceLabVessel(tubes02);
            rack01_inPlace.setStorageLocation(slot2);
            tubes02.addInPlaceEvent(rack02_inPlace);
            rackOfTubes02.getAncillaryInPlaceEvents().add(rack02_inPlace);
            Assert.assertEquals(rackOfTubes02.getAncillaryInPlaceEvents().size(), 1);

            // Scan layout of rack03, no storage location
            LabEvent rack03_inPlace = new LabEvent(LabEventType.IN_PLACE, new Date(), "TEST"
                    , 1L, BSPManagerFactoryStub.QA_DUDE_USER_ID, "");
            rack03_inPlace.setAncillaryInPlaceVessel(rackOfTubes03);
            rack03_inPlace.setInPlaceLabVessel(tubes03);
            tubes03.addInPlaceEvent(rack03_inPlace);
            rackOfTubes03.getAncillaryInPlaceEvents().add(rack03_inPlace);
            Assert.assertEquals(rackOfTubes03.getAncillaryInPlaceEvents().size(), 1);

            // Scan layout of rack04
            LabEvent rack04_inPlace = new LabEvent(LabEventType.IN_PLACE, new Date(), "TEST"
                    , 1L, BSPManagerFactoryStub.QA_DUDE_USER_ID, "");
            rack04_inPlace.setAncillaryInPlaceVessel(rackOfTubes04);
            rack04_inPlace.setInPlaceLabVessel(tubes04);
            tubes04.addInPlaceEvent(rack04_inPlace);
            rackOfTubes04.getAncillaryInPlaceEvents().add(rack04_inPlace);
            Assert.assertEquals(rackOfTubes04.getAncillaryInPlaceEvents().size(), 1);


            when(storageLocationDaoMock.findSingle(LabVessel.class, LabVessel_.label, "666")).thenReturn(null);
            when(storageLocationDaoMock.findSingle(LabVessel.class, LabVessel_.label, rackOfTubes01.getLabel())).thenReturn(rackOfTubes01);
            when(storageLocationDaoMock.findSingle(LabVessel.class, LabVessel_.label, rackOfTubes02.getLabel())).thenReturn(rackOfTubes02);

            when(storageEjbMock.findLatestRackAndLayout(any(LabVessel.class))).thenCallRealMethod();
            when(storageEjbMock.tryPersistRackOrTubes(any(LabVessel.class))).thenCallRealMethod();
            // Pick verify needs to find rackOfTubes01!
            when(labVesselDaoMock.findByIdentifier(rackOfTubes01.getLabel())).thenReturn(rackOfTubes01);
            when(storageEjbMock.isStorageEvent(any(LabEvent.class))).thenCallRealMethod();

            // Need events timestamped in sequence
            Thread.sleep(1000);

        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Failure in test setup: " + ex.getMessage());
        }
    }

    /**
     * Default event handler, gathers batch list
     */
    public void testDefault() throws Exception {
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(PickWorkspaceActionBean.class
                , storageLocationDaoMock, storageEjbMock, labBatchDaoMock, labVesselDaoMock);
        trip.execute();
        PickWorkspaceActionBean actionBean = trip.getActionBean(PickWorkspaceActionBean.class);
        trip.getContext().getFilters().get(0).destroy();
        String json = actionBean.getBatchSelectionList();
        JsonArray result = Json.createReader(new StringReader(json)).readArray();
        Assert.assertTrue("Expected 3 lab batches", result.size() == 3);
        // Order should be descending, last is inactive
        JsonObject batch = result.getJsonObject(2);
        Assert.assertTrue("Expected batch 03", batch.getInt("batchId") == 3);
        Assert.assertFalse("Expected inactive batch", batch.getBoolean("active"));
        batch = result.getJsonObject(1);
        Assert.assertTrue("Expected batch 02", batch.getInt("batchId") == 2);
        Assert.assertTrue("Expected active batch", batch.getBoolean("active"));
        batch = result.getJsonObject(0);
        Assert.assertTrue("Expected batch 02", batch.getInt("batchId") == 1);
        Assert.assertTrue("Expected active batch", batch.getBoolean("active"));
    }

    /**
     * Test batch selection, batch 01 has 1 plate, and batch 02 has 2 plates
     */
    public void testProcessBatches() throws Exception {

        // Get batch list for round trip
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(PickWorkspaceActionBean.class
                , storageLocationDaoMock, storageEjbMock, labBatchDaoMock, labVesselDaoMock);
        trip.execute();
        PickWorkspaceActionBean actionBean = trip.getActionBean(PickWorkspaceActionBean.class);
        trip.getContext().getFilters().get(0).destroy();
        JsonArray batchList = Json.createReader(new StringReader(actionBean.getBatchSelectionList())).readArray();

        // Now select 1 batch (first 2 are active), and submit selection
        JsonObject batch01 = batchList.getJsonObject(0);
        JsonObject batch02 = batchList.getJsonObject(1);
        JsonArray batchSelection = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("batchId", batch01.get("batchId"))
                        .add("batchName", batch01.get("batchName"))
                        .add("wasSelected", batch01.get("wasSelected"))
                        .add("selected", true)
                        .add("active", batch01.get("active"))
                )
                .add(Json.createObjectBuilder()
                        .add("batchId", batch02.get("batchId"))
                        .add("batchName", batch02.get("batchName"))
                        .add("wasSelected", batch02.get("wasSelected"))
                        .add("selected", true)
                        .add("active", batch02.get("active"))
                )
                // Recycle this one unchanged
                .add(batchList.getJsonObject(2))
                .build();

        trip = StripesMockTestUtils.createMockRoundtrip(PickWorkspaceActionBean.class
                , storageLocationDaoMock, storageEjbMock, labBatchDaoMock, labVesselDaoMock);
        trip.setParameter("batchSelectionList", batchSelection.toString());
        trip.setParameter("pickerData", "[]");

        trip.execute(PickWorkspaceActionBean.EVT_PROCESS_BATCHES);
        actionBean = trip.getActionBean(PickWorkspaceActionBean.class);
        trip.getContext().getFilters().get(0).destroy();

        // Check batches
        batchList = Json.createReader(new StringReader(actionBean.getBatchSelectionList())).readArray();
        Assert.assertTrue("Expected 3 lab batches", batchList.size() == 3);
        // Order should be descending, first is selected
        batch01 = batchList.getJsonObject(0);
        Assert.assertTrue("Expected selected batch", batch01.getBoolean("selected"));
        Assert.assertTrue("Expected selected batch", batch01.getBoolean("wasSelected"));
        Assert.assertTrue("Expected batch 01", batch01.getInt("batchId") == 1);
        List<Message> messages = actionBean.getStripesMessages();
        Assert.assertTrue("Expected 2 messages, got " + messages.size(), messages.size() == 2);
        String message = messages.get(0).getMessage(Locale.US);
        Assert.assertTrue("Unexpected message: " + message, message.contains(" added") || message.contains("No available"));
        message = messages.get(1).getMessage(null);
        Assert.assertTrue("Unexpected message: " + message, message.contains(" added") || message.contains("No available"));

        // Check vessel count
        Assert.assertEquals("Vessel count not as expected", srsBatch01.getLabBatchStartingVessels().size() + srsBatch02.getLabBatchStartingVessels().size(), actionBean.getPickSampleCount());

        JsonArray pickList = Json.createReader(new StringReader(actionBean.getPickerData())).readArray();
        Assert.assertTrue("Batch/rack count should be 3", pickList.size() == 3);

        JsonObject jsBatch01 = pickList.getJsonObject(0);
        Assert.assertEquals("Batch 01 expected", jsBatch01.getString("batchName"), "SRS Batch 01");
        Assert.assertEquals("Slot 01 expected", jsBatch01.getInt("storageLocId"), 1000);
        Assert.assertEquals("Rack 01 expected", jsBatch01.getString("sourceVessel"), "rack01");
        JsonArray jsRack01 = jsBatch01.getJsonArray("pickerVessels");
        Assert.assertTrue("Vessel count should be 96", jsRack01.size() == 96);

        JsonObject jsBatch02 = pickList.getJsonObject(1);
        Assert.assertEquals("Batch 02 expected", jsBatch02.getString("batchName"), "SRS Batch 02");
        Assert.assertEquals("Slot 02 expected", jsBatch02.getInt("storageLocId"), 2000);
        Assert.assertEquals("Rack 02 expected", jsBatch02.getString("sourceVessel"), "rack02");
        JsonArray jsRack02 = jsBatch02.getJsonArray("pickerVessels");
        Assert.assertTrue("Vessel count should be 96", jsRack02.size() == 96);

        JsonObject jsBatch03 = pickList.getJsonObject(2);
        Assert.assertEquals("Batch 02 expected", jsBatch03.getString("batchName"), "SRS Batch 02");
        Assert.assertTrue("Null location expected", jsBatch03.isNull("storageLocId"));
        Assert.assertEquals("Rack 03 expected", jsBatch03.getString("sourceVessel"), "rack03");
        JsonArray jsRack03 = jsBatch03.getJsonArray("pickerVessels");
        Assert.assertTrue("Vessel count should be 96", jsRack03.size() == 96);

    }

    /**
     * Test XL20 pick list creation, same setup as testProcessBatches() except select and break up batch 01 into 2 destination plates
     */
    public void testXL20Pick() throws Exception {

        // Get batch list for round trip
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(PickWorkspaceActionBean.class
                , storageLocationDaoMock, storageEjbMock, labBatchDaoMock, labVesselDaoMock);
        trip.execute();
        PickWorkspaceActionBean actionBean = trip.getActionBean(PickWorkspaceActionBean.class);
        trip.getContext().getFilters().get(0).destroy();
        JsonArray batchList = Json.createReader(new StringReader(actionBean.getBatchSelectionList())).readArray();

        // Now select 1 batch and submit selection
        JsonObject batch01 = batchList.getJsonObject(0);
        JsonArray batchSelection = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("batchId", batch01.get("batchId"))
                        .add("batchName", batch01.get("batchName"))
                        .add("wasSelected", batch01.get("wasSelected"))
                        .add("selected", true)
                        .add("active", batch01.get("active"))
                )
                // Recycle these 2 unchanged
                .add(batchList.getJsonObject(1))
                .add(batchList.getJsonObject(2))
                .build();
        trip = StripesMockTestUtils.createMockRoundtrip(PickWorkspaceActionBean.class
                , storageLocationDaoMock, storageEjbMock, labBatchDaoMock, labVesselDaoMock);
        trip.setParameter("batchSelectionList", batchSelection.toString());
        trip.setParameter("pickerData", "[]");
        trip.execute(PickWorkspaceActionBean.EVT_PROCESS_BATCHES);
        actionBean = trip.getActionBean(PickWorkspaceActionBean.class);
        trip.getContext().getFilters().get(0).destroy();
        // Hold batches
        batchList = Json.createReader(new StringReader(actionBean.getBatchSelectionList())).readArray();

        // Json picklist is immutable
        // Build a new picklist populated with source and destination for srsBatch01 rack
        JsonArrayBuilder pickListBuilder = Json.createArrayBuilder();
        JsonObjectBuilder batchBuilder = Json.createObjectBuilder();
        batchBuilder.add("batchId", srsBatch01.getLabBatchId());
        batchBuilder.add("batchName", srsBatch01.getBatchName());
        batchBuilder.add("storageLocId", slot1.getStorageLocationId());
        batchBuilder.add("storageLocPath", storageLocationDaoMock.getLocationTrail(slot1));
        batchBuilder.add("sourceVessel", rackOfTubes01.getLabel());
        JsonArrayBuilder vesselListBuilder = Json.createArrayBuilder();
        int i = 0;
        for (LabVessel vessel : tubes01.getContainerRole().getContainedVessels()) {
            if (i < 48) {
                VesselPosition pos = VesselGeometry.G12x8.getVesselPositions()[i];
                JsonObjectBuilder vesselBuilder = Json.createObjectBuilder();
                vesselBuilder.add("sourceVessel", vessel.getLabel());
                vesselBuilder.add("sourcePosition", pos.name());
                vesselBuilder.add("targetVessel", "target01");
                vesselBuilder.add("targetPosition", pos.name());
                vesselListBuilder.add(vesselBuilder.build());
            } else {
                VesselPosition pos = VesselGeometry.G12x8.getVesselPositions()[i];
                JsonObjectBuilder vesselBuilder = Json.createObjectBuilder();
                vesselBuilder.add("sourceVessel", vessel.getLabel());
                vesselBuilder.add("sourcePosition", pos.name());
                vesselBuilder.add("targetVessel", "target02");
                vesselBuilder.add("targetPosition", pos.name());
                vesselListBuilder.add(vesselBuilder.build());
            }
            i++;
        }
        batchBuilder.add("pickerVessels", vesselListBuilder.build());
        JsonArray pickList = pickListBuilder.add(batchBuilder.build()).build();

        // Send empty list for XL20
        trip = StripesMockTestUtils.createMockRoundtrip(PickWorkspaceActionBean.class
                , storageLocationDaoMock, storageEjbMock, labBatchDaoMock, labVesselDaoMock);
        trip.setParameter("batchSelectionList", batchList.toString());
        trip.setParameter("pickerData", "[]");
        trip.execute(PickWorkspaceActionBean.EVT_DOWNLOAD_XFER_FILE);
        String csv = trip.getOutputString();
        trip.getContext().getFilters().get(0).destroy();
        Assert.assertTrue("Expected empty XL20 pick list", csv.length() == 0);
        Assert.assertTrue("Expected 1 validation error for no picker vessels",
                trip.getValidationErrors().get(ValidationErrors.GLOBAL_ERROR).size() == 1);

        // Send full picker list of a single rack for XL20
        trip = StripesMockTestUtils.createMockRoundtrip(PickWorkspaceActionBean.class
                , storageLocationDaoMock, storageEjbMock, labBatchDaoMock, labVesselDaoMock);
        trip.setParameter("batchSelectionList", batchSelection.toString());
        trip.setParameter("pickerData", pickList.toString());
        trip.execute(PickWorkspaceActionBean.EVT_DOWNLOAD_XFER_FILE);
        LineReader csvReader = new LineReader(new BufferedReader(new StringReader(trip.getOutputString())), false);
        trip.getContext().getFilters().get(0).destroy();
        int lineCount = 0, target01Count = 0, target02Count = 0;
        Set<String> target01Locs = new HashSet<>(), target02Locs = new HashSet<>(), srcTubes = new HashSet<>(), srcLocs = new HashSet<>();
        while (true) {
            String line = csvReader.readLine();
            if (line == null || line.length() == 0) {
                break;
            }
            lineCount++;
            String[] data = line.split(",");
            Assert.assertTrue("Expected 5 elements", data.length == 5);
            Assert.assertTrue("Expected source rack barcode", rackOfTubes01.getLabel().equals(data[0]));
            srcLocs.add(data[1]);
            srcTubes.add(data[2]);
            if ("target01".equals(data[3])) {
                target01Count++;
                target01Locs.add(data[4]);
            } else {
                target02Count++;
                target02Locs.add(data[4]);
            }
        }
        Assert.assertTrue("Expected 96 CSV lines", lineCount == 96);
        Assert.assertTrue("Expected 48 destination 01 lines", target01Count == 48);
        Assert.assertTrue("Expected 48 destination 02 lines", target02Count == 48);
        Assert.assertTrue("Expected 96 CSV lines", lineCount == 96);
        Assert.assertTrue("Expected 96 distinct source tubes", srcTubes.size() == 96);
        Assert.assertTrue("Expected 96 distinct source positions", srcLocs.size() == 96);
        Assert.assertTrue("Expected 48 distinct destination 01 positions", target01Locs.size() == 48);
        Assert.assertTrue("Expected 48 distinct destination 02 positions", target02Locs.size() == 48);
    }

    /**
     * Test pick completion, same setup as testXL20Pick(), break up batch 01 into 2 destination plates
     */
    public void testValidatePicks() throws Exception {

        final List<LabEvent> inPlaceEvents = new ArrayList<>();
        // Capture all created events
        when(this.storageEjbMock.createDisambiguatedStorageEvent(eq(LabEventType.IN_PLACE), any(LabVessel.class), AdditionalMatchers.or(isNull(StorageLocation.class), any(StorageLocation.class)), any(LabVessel.class), anyLong(), any(Date.class), anyLong()))
                .thenAnswer(
                        (Answer) invocation -> {
                            LabEvent event = (LabEvent) invocation.callRealMethod();
                            inPlaceEvents.add(event);
                            return event;
                        });

        // Get batch list for round trip
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(PickWorkspaceActionBean.class
                , storageLocationDaoMock, this.storageEjbMock, labBatchDaoMock, labVesselDaoMock);
        trip.execute();
        PickWorkspaceActionBean actionBean = trip.getActionBean(PickWorkspaceActionBean.class);
        trip.getContext().getFilters().get(0).destroy();
        JsonArray batchList = Json.createReader(new StringReader(actionBean.getBatchSelectionList())).readArray();

        // Now select 1 batch and submit selection
        JsonObject batch01 = batchList.getJsonObject(0);
        JsonArray batchSelection = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("batchId", batch01.get("batchId"))
                        .add("batchName", batch01.get("batchName"))
                        .add("wasSelected", batch01.get("wasSelected"))
                        .add("selected", true)
                        .add("active", batch01.get("active"))
                )
                // Recycle these 2 unchanged
                .add(batchList.getJsonObject(1))
                .add(batchList.getJsonObject(2))
                .build();
        trip = StripesMockTestUtils.createMockRoundtrip(PickWorkspaceActionBean.class
                , storageLocationDaoMock, this.storageEjbMock, labBatchDaoMock, labVesselDaoMock);
        trip.setParameter("batchSelectionList", batchSelection.toString());
        trip.setParameter("pickerData", "[]");
        trip.execute(PickWorkspaceActionBean.EVT_PROCESS_BATCHES);
        actionBean = trip.getActionBean(PickWorkspaceActionBean.class);
        trip.getContext().getFilters().get(0).destroy();

        // Build scan data for destination racks
        JsonArrayBuilder scanListBuilder = Json.createArrayBuilder();
        JsonObjectBuilder scanBuilder01 = Json.createObjectBuilder(), scanBuilder02 = Json.createObjectBuilder();
        scanBuilder01.add("scannerName", "Simulator");
        scanBuilder01.add("scanDate", "02/19/2020 12:21:04");
        scanBuilder01.add("rackBarcode", "target01");
        scanBuilder01.add("scanUser", "QADudeTest");
        scanBuilder02.add("scannerName", "Simulator");
        scanBuilder02.add("scanDate", "02/19/2020 12:25:04");
        scanBuilder02.add("rackBarcode", "target02");
        scanBuilder01.add("scanUser", "QADudeTest");
        JsonArrayBuilder scanTubeListBuilder01 = Json.createArrayBuilder(), scanTubeListBuilder02 = Json.createArrayBuilder();

        // Json picklist is immutable
        // Build a new picklist populated with source and destination for srsBatch01 rack
        JsonArrayBuilder pickListBuilder = Json.createArrayBuilder();
        JsonObjectBuilder batchBuilder = Json.createObjectBuilder();
        batchBuilder.add("batchId", srsBatch01.getLabBatchId());
        batchBuilder.add("batchName", srsBatch01.getBatchName());
        batchBuilder.add("storageLocId", slot1.getStorageLocationId());
        batchBuilder.add("storageLocPath", storageLocationDaoMock.getLocationTrail(slot1));
        batchBuilder.add("sourceVessel", rackOfTubes01.getLabel());
        JsonArrayBuilder vesselListBuilder = Json.createArrayBuilder();
        int i = 0;
        for (LabVessel vessel : tubes01.getContainerRole().getContainedVessels()) {
            if (i < 48) {
                VesselPosition pos = VesselGeometry.G12x8.getVesselPositions()[i];
                JsonObjectBuilder vesselBuilder = Json.createObjectBuilder();
                vesselBuilder.add("sourceVessel", vessel.getLabel());
                vesselBuilder.add("sourcePosition", pos.name());
                vesselBuilder.add("targetVessel", "target01");
                vesselBuilder.add("targetPosition", pos.name());
                vesselListBuilder.add(vesselBuilder.build());
                JsonObjectBuilder scanTubeBuilder01 = Json.createObjectBuilder();
                scanTubeBuilder01.add("position", pos.name());
                scanTubeBuilder01.add("barcode", vessel.getLabel());
                scanTubeListBuilder01.add(scanTubeBuilder01.build());

            } else {
                VesselPosition pos = VesselGeometry.G12x8.getVesselPositions()[i];
                JsonObjectBuilder vesselBuilder = Json.createObjectBuilder();
                vesselBuilder.add("sourceVessel", vessel.getLabel());
                vesselBuilder.add("sourcePosition", pos.name());
                vesselBuilder.add("targetVessel", "target02");
                vesselBuilder.add("targetPosition", pos.name());
                vesselListBuilder.add(vesselBuilder.build());
                JsonObjectBuilder scanTubeBuilder02 = Json.createObjectBuilder();
                scanTubeBuilder02.add("position", pos.name());
                scanTubeBuilder02.add("barcode", vessel.getLabel());
                scanTubeListBuilder02.add(scanTubeBuilder02.build());
            }
            i++;
        }
        batchBuilder.add("pickerVessels", vesselListBuilder.build());
        JsonArray pickList = pickListBuilder.add(batchBuilder.build()).build();

        scanListBuilder.add(scanBuilder01.add("scans", scanTubeListBuilder01.build()).build());
        scanListBuilder.add(scanBuilder02.add("scans", scanTubeListBuilder02.build()).build());
        JsonArray scanList = scanListBuilder.build();

        // Send full picker list for validation
        trip = StripesMockTestUtils.createMockRoundtrip(PickWorkspaceActionBean.class
                , storageLocationDaoMock, this.storageEjbMock, labBatchDaoMock, labVesselDaoMock, testUserBean);
        trip.setParameter("pickerData", pickList.toString());
        trip.setParameter("scanData", scanList.toString());
        trip.execute(PickWorkspaceActionBean.EVT_REGISTER_TRANSFERS);
        actionBean = trip.getActionBean(PickWorkspaceActionBean.class);
        trip.getContext().getFilters().get(0).destroy();

        List<Message> messages = actionBean.getStripesMessages();
        List<ValidationError> errors = trip.getValidationErrors().get(ValidationErrors.GLOBAL_ERROR);
        Assert.assertTrue("Expected 3 success messages", messages.size() == 3);
        Assert.assertTrue("Expected no validation errors", errors == null || errors.size() == 0);

        // Should have created 3 scan events, source and 2 destinations
        Assert.assertTrue("Expected 3 events", inPlaceEvents.size() == 3);
        Set<String> labels = new HashSet<>();
        Set<LabVessel> tubes = new HashSet<>();

        // First 2 are targets of 48 vessels each
        LabEvent event = inPlaceEvents.get(0);
        Assert.assertTrue("Expected IN_PLACE event", LabEventType.IN_PLACE.equals(event.getLabEventType()));
        labels.add(event.getAncillaryInPlaceVessel().getLabel());
        tubes.addAll(event.getInPlaceLabVessel().getContainerRole().getContainedVessels());
        Assert.assertTrue("Expected 48 tubes", tubes.size() == 48);

        event = inPlaceEvents.get(1);
        Assert.assertTrue("Expected IN_PLACE event", LabEventType.IN_PLACE.equals(event.getLabEventType()));
        labels.add(event.getAncillaryInPlaceVessel().getLabel());
        tubes.addAll(event.getInPlaceLabVessel().getContainerRole().getContainedVessels());
        Assert.assertTrue("Expected 96 tubes total", tubes.size() == 96);

        Assert.assertTrue("Expected 2 target racks", labels.size() == 2 && labels.contains("target01") && labels.contains("target02"));

        // third is depleted source
        event = inPlaceEvents.get(2);
        Assert.assertTrue("Expected IN_PLACE event", LabEventType.IN_PLACE.equals(event.getLabEventType()));
        Assert.assertTrue("Expected source rack", event.getAncillaryInPlaceVessel().getLabel().equals("rack01"));
        Assert.assertTrue("Expected empty rack", event.getInPlaceLabVessel().getContainerRole().getContainedVessels().size() == 0);

    }
}