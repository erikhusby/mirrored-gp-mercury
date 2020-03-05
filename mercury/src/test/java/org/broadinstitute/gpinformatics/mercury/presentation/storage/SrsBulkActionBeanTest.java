package org.broadinstitute.gpinformatics.mercury.presentation.storage;

import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.validation.ValidationErrors;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.presentation.StripesMockTestUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemOfRecord;
import org.broadinstitute.gpinformatics.mercury.boundary.storage.StorageEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
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
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = TestGroups.DATABASE_FREE, singleThreaded = true)
public class SrsBulkActionBeanTest extends BaseEventTest {

    private StorageLocationDao storageLocationDaoMock;
    private StorageEjb storageEjbMock;
    private UserBean testUserBean;

    private Field storageIdField;
    private Field vesselIdField;
    private Field vesselEventCountField;

    private RackOfTubes rackOfTubes01, rackOfTubes02;
    private TubeFormation tubes01, tubes02;

    StorageLocation parent;
    StorageLocation shelf1, shelf2;
    StorageLocation rack1, rack2, rack3, rack4;
    StorageLocation slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8;

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
        expectedRouting = SystemOfRecord.System.MERCURY;
        super.setUp();

        storageLocationDaoMock = mock(StorageLocationDao.class);
        storageEjbMock = mock(StorageEjb.class);

        try {
            storageIdField = StorageLocation.class.getDeclaredField("storageLocationId");
            storageIdField.setAccessible(true);
            vesselIdField = LabVessel.class.getDeclaredField("labVesselId");
            vesselIdField.setAccessible(true);
            vesselEventCountField = LabVessel.class.getDeclaredField("inPlaceEventsCount");
            vesselEventCountField.setAccessible(true);
        } catch (Exception ex) {
            System.out.println("Failure enabling reflection: " + ex.getMessage());
            ex.printStackTrace();
        }

        parent = new StorageLocation("Freezer", StorageLocation.LocationType.FREEZER, null);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(1))).thenReturn(parent);

        shelf1 = new StorageLocation("Shelf 1", StorageLocation.LocationType.SHELF, parent);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(10))).thenReturn(shelf1);
        rack1 = new StorageLocation("Rack 1", StorageLocation.LocationType.GAUGERACK, shelf1);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(100))).thenReturn(rack1);
        slot1 = new StorageLocation("Slot 1", StorageLocation.LocationType.SLOT, rack1);
        slot1.setStorageCapacity(1);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(1000))).thenReturn(slot1);
        when(storageLocationDaoMock.getLocationTrail(slot1)).thenReturn("Trail to slot1");
        // Bypass criteria query
        when(storageLocationDaoMock.getSlotStoredContainerCount(slot1)).thenReturn(1);
        slot2 = new StorageLocation("Slot 2", StorageLocation.LocationType.SLOT, rack1);
        slot2.setStorageCapacity(1);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(2000))).thenReturn(slot2);
        when(storageLocationDaoMock.getLocationTrail(slot2)).thenReturn("Trail to slot2");
        rack2 = new StorageLocation("Rack 2", StorageLocation.LocationType.GAUGERACK, shelf1);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(200))).thenReturn(rack2);
        slot3 = new StorageLocation("Slot 3", StorageLocation.LocationType.SLOT, rack2);
        slot3.setStorageCapacity(1);
        when(storageLocationDaoMock.getLocationTrail(slot3)).thenReturn("Trail to slot3");
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(3000))).thenReturn(slot3);
        slot4 = new StorageLocation("Slot 4", StorageLocation.LocationType.SLOT, rack2);
        slot4.setStorageCapacity(1);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(4000))).thenReturn(slot4);
        when(storageLocationDaoMock.getLocationTrail(slot4)).thenReturn("Trail to slot4");

        shelf2 = new StorageLocation("Shelf 2", StorageLocation.LocationType.SHELF, parent);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(20))).thenReturn(shelf2);
        rack3 = new StorageLocation("Rack 1", StorageLocation.LocationType.GAUGERACK, shelf2);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(300))).thenReturn(rack3);
        slot5 = new StorageLocation("Slot 5", StorageLocation.LocationType.SLOT, rack3);
        slot5.setStorageCapacity(1);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(5000))).thenReturn(slot5);
        slot6 = new StorageLocation("Slot 6", StorageLocation.LocationType.SLOT, rack3);
        slot6.setStorageCapacity(1);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(6000))).thenReturn(slot6);
        rack4 = new StorageLocation("Rack 2", StorageLocation.LocationType.GAUGERACK, shelf2);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(400))).thenReturn(rack4);
        slot7 = new StorageLocation("Slot 7", StorageLocation.LocationType.SLOT, rack4);
        slot7.setStorageCapacity(1);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(7000))).thenReturn(slot7);
        slot8 = new StorageLocation("Slot 8", StorageLocation.LocationType.SLOT, rack4);
        slot8.setStorageCapacity(1);
        when(storageLocationDaoMock.findById(StorageLocation.class, Long.valueOf(8000))).thenReturn(slot8);

        parent.setChildrenStorageLocation(new HashSet(Arrays.asList(shelf1, shelf2)));
        shelf1.setChildrenStorageLocation(new HashSet(Arrays.asList(rack1, rack2)));
        rack1.setChildrenStorageLocation(new HashSet(Arrays.asList(slot1, slot2)));
        rack2.setChildrenStorageLocation(new HashSet(Arrays.asList(slot3, slot4)));
        shelf2.setChildrenStorageLocation(new HashSet(Arrays.asList(rack3, rack4)));
        rack3.setChildrenStorageLocation(new HashSet(Arrays.asList(slot5, slot6)));
        rack4.setChildrenStorageLocation(new HashSet(Arrays.asList(slot7, slot8)));

        try {
            storageIdField.set(parent, new Long(1L));

            storageIdField.set(shelf1, new Long(10L));
            storageIdField.set(shelf2, new Long(20L));

            storageIdField.set(rack1, new Long(100L));
            storageIdField.set(rack2, new Long(200L));
            storageIdField.set(rack3, new Long(300L));
            storageIdField.set(rack4, new Long(400L));

            storageIdField.set(slot1, new Long(1000L));
            storageIdField.set(slot2, new Long(2000L));
            storageIdField.set(slot3, new Long(3000L));
            storageIdField.set(slot4, new Long(4000L));
            storageIdField.set(slot5, new Long(5000L));
            storageIdField.set(slot6, new Long(6000L));
            storageIdField.set(slot7, new Long(7000L));
            storageIdField.set(slot8, new Long(8000L));

        } catch (Exception ex) {
            System.out.println("Failure using reflection to set storageLocationId: " + ex.getMessage());
            ex.printStackTrace();
        }

        rackOfTubes01 = new RackOfTubes("rack01", RackOfTubes.RackType.Matrix96);
        rackOfTubes01.setStorageLocation(slot1);

        rackOfTubes02 = new RackOfTubes("rack01", RackOfTubes.RackType.Matrix96);
        Map<VesselPosition, BarcodedTube> layout01 = new HashMap<>();
        Map<VesselPosition, BarcodedTube> layout02 = new HashMap<>();

        for (VesselPosition pos : VesselGeometry.G12x8.getVesselPositions()) {
            BarcodedTube tube01 = new BarcodedTube("tube_a_" + pos, BarcodedTube.BarcodedTubeType.MatrixTube);
            tube01.setStorageLocation(slot1);
            BarcodedTube tube02 = new BarcodedTube("tube_b_" + pos, BarcodedTube.BarcodedTubeType.MatrixTube);
            layout01.put(pos, tube01);
            layout02.put(pos, tube02);
        }

        tubes01 = new TubeFormation(layout01, RackOfTubes.RackType.Matrix96);
        rackOfTubes01.getTubeFormations().add(tubes01);
        tubes02 = new TubeFormation(layout02, RackOfTubes.RackType.Matrix96);
        rackOfTubes02.getTubeFormations().add(tubes02);

        // Check in rack01
        LabEvent rack01_inPlace = new LabEvent(LabEventType.STORAGE_CHECK_IN, new Date(), "TEST"
                , 1L, BSPManagerFactoryStub.QA_DUDE_USER_ID, "");
        rack01_inPlace.setAncillaryInPlaceVessel(rackOfTubes01);
        rack01_inPlace.setInPlaceLabVessel(tubes01);
        rackOfTubes01.getInPlaceLabEvents().add(rack01_inPlace);
        Assert.assertEquals(rackOfTubes01.getAncillaryInPlaceEvents().size(), 1);
        Assert.assertEquals(rackOfTubes01.getStorageLocation().getLabel(), "Slot 01");

        // Scan layout of rack02
        LabEvent rack02_inPlace = new LabEvent(LabEventType.IN_PLACE, new Date(), "TEST"
                , 1L, BSPManagerFactoryStub.QA_DUDE_USER_ID, "");
        rack02_inPlace.setAncillaryInPlaceVessel(rackOfTubes02);
        rack02_inPlace.setInPlaceLabVessel(tubes02);
        rackOfTubes02.getInPlaceLabEvents().add(rack02_inPlace);
        Assert.assertEquals(rackOfTubes02.getAncillaryInPlaceEvents().size(), 1);

        when(storageLocationDaoMock.findSingle(LabVessel.class, LabVessel_.label, "666")).thenReturn(null);
        when(storageLocationDaoMock.findSingle(LabVessel.class, LabVessel_.label, rackOfTubes01.getLabel())).thenReturn(rackOfTubes01);
        when(storageLocationDaoMock.findSingle(LabVessel.class, LabVessel_.label, rackOfTubes02.getLabel())).thenReturn(rackOfTubes02);

    }

    /**
     * Not much here, default event handler <br/>
     * (Storage tree nodes set up by StorageLocationActionBean tested elsewhere)
     */
    public void testInitCheckIn() throws Exception {
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(BulkStorageOpsActionBean.class
                , storageLocationDaoMock, storageEjbMock);
        trip.execute();
        BulkStorageOpsActionBean actionBean = trip.getActionBean(BulkStorageOpsActionBean.class);
        trip.getContext().getFilters().get(0).destroy();
        Assert.assertTrue("Action bean stage INIT expected", "INIT".equals(actionBean.getCheckInPhase()));
    }

    /**
     * Mimics the functionality of validating a selection of locations to determine if any have available locations
     * to use for bulk check-in
     */
    public void testCheckinValidateNullLocation() throws Exception {
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(BulkStorageOpsActionBean.class, storageLocationDaoMock, storageEjbMock);
        trip.execute(BulkStorageOpsActionBean.EVT_VALIDATED_CHECK_IN);
        BulkStorageOpsActionBean actionBean = trip.getActionBean(BulkStorageOpsActionBean.class);
        trip.getContext().getFilters().get(0).destroy();
        Assert.assertTrue("Action bean stage INIT expected", "INIT".equals(actionBean.getCheckInPhase()));
        Assert.assertFalse("Expected errors on action bean validation when no locations selected", trip.getValidationErrors().isEmpty());
    }

    public void testCheckinValidateNoLocation() throws Exception {
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(BulkStorageOpsActionBean.class, storageLocationDaoMock, storageEjbMock);
        trip.setParameter("proposedLocationIds", "");
        trip.execute(BulkStorageOpsActionBean.EVT_VALIDATED_CHECK_IN);
        BulkStorageOpsActionBean actionBean = trip.getActionBean(BulkStorageOpsActionBean.class);
        trip.getContext().getFilters().get(0).destroy();
        Assert.assertTrue("Action bean stage INIT expected", "INIT".equals(actionBean.getCheckInPhase()));
        Assert.assertFalse("Expected errors on action bean validation when no locations selected", trip.getValidationErrors().isEmpty());
    }

    public void testCheckinValidateUnknownLocation() throws Exception {
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(BulkStorageOpsActionBean.class, storageLocationDaoMock, storageEjbMock);
        trip.setParameter("proposedLocationIds", "666");
        trip.execute(BulkStorageOpsActionBean.EVT_VALIDATED_CHECK_IN);
        BulkStorageOpsActionBean actionBean = trip.getActionBean(BulkStorageOpsActionBean.class);
        trip.getContext().getFilters().get(0).destroy();
        Assert.assertTrue("Action bean stage INIT expected", "INIT".equals(actionBean.getCheckInPhase()));
        Assert.assertTrue("Expected errors on action bean validation when no locations selected", trip.getValidationErrors().size() > 0);
    }

    /**
     * parent -> shelf 1 -> racks 1 and 2 should yield slot2, slot3, and slot4 (slot1 populated in setUp())
     */
    public void testCheckinValidateSomeLocations() throws Exception {
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(BulkStorageOpsActionBean.class, storageLocationDaoMock, storageEjbMock);
        trip.setParameter("proposedLocationIds", "100,200");
        trip.execute(BulkStorageOpsActionBean.EVT_VALIDATED_CHECK_IN);
        BulkStorageOpsActionBean actionBean = trip.getActionBean(BulkStorageOpsActionBean.class);
        trip.getContext().getFilters().get(0).destroy();
        Assert.assertTrue("Action bean stage READY expected", "READY".equals(actionBean.getCheckInPhase()));
        Assert.assertTrue("Expected 1 validation error for the full slot1 location",
                trip.getValidationErrors().get(ValidationErrors.GLOBAL_ERROR).size() == 1);
        Assert.assertTrue("Action bean expected 3 of 4 open slots", actionBean.getValidLocations().size() == 3);

        Set<Long> expectedLocations = new HashSet<>(Arrays.asList(2000L, 3000L, 4000L));
        for (StorageLocation loc : actionBean.getValidLocations()) {
            Assert.assertTrue("Unexpected available storage location", expectedLocations.contains(loc.getStorageLocationId()));
        }
    }

    /**
     * Populate slot 2 and look for space in a full rack
     */
    public void testCheckinValidateFullLocations() throws Exception {
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(BulkStorageOpsActionBean.class, storageLocationDaoMock, storageEjbMock);
        when(storageLocationDaoMock.getSlotStoredContainerCount(slot1)).thenReturn(1);
        when(storageLocationDaoMock.getSlotStoredContainerCount(slot2)).thenReturn(1);
        trip.setParameter("proposedLocationIds", rack1.getStorageLocationId().toString());
        trip.execute(BulkStorageOpsActionBean.EVT_VALIDATED_CHECK_IN);
        BulkStorageOpsActionBean actionBean = trip.getActionBean(BulkStorageOpsActionBean.class);
        trip.getContext().getFilters().get(0).destroy();
        Assert.assertTrue("Action bean stage INIT expected", "INIT".equals(actionBean.getCheckInPhase()));
        Assert.assertTrue("Expected 3 validation errors for all slots full",
                actionBean.getValidationErrors().get(ValidationErrors.GLOBAL_ERROR).size() == 3);
        Assert.assertTrue("Action bean expected no open slots", actionBean.getValidLocations().size() == 0);
    }

    /**
     * Try a bulk check-in without location
     */
    public void testCheckInNoLocation() throws Exception {
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(BulkStorageOpsActionBean.class, storageLocationDaoMock, storageEjbMock);
        trip.addParameter("storageLocationId", null);
        trip.addParameter("barcode", "");
        trip.execute(BulkStorageOpsActionBean.EVT_CHECK_IN);
        JsonObject result = Json.createReader(new StringReader(trip.getOutputString())).readObject();
        trip.getContext().getFilters().get(0).destroy();
        Assert.assertEquals("Result status should be 'danger'", "danger", result.getString("status"));
        String feedback = result.getString("feedbackMsg");
        Assert.assertTrue("Unexpected result message: " + feedback, feedback.contains("No storage location specified"));
    }

    /**
     * Try a bulk check-in with unknown rack
     */
    public void testCheckInNoRack() throws Exception {
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(BulkStorageOpsActionBean.class, storageLocationDaoMock, storageEjbMock);
        trip.addParameter("storageLocationId", "1000");
        trip.addParameter("barcode", "666");
        trip.execute(BulkStorageOpsActionBean.EVT_CHECK_IN);
        JsonObject result = Json.createReader(new StringReader(trip.getOutputString())).readObject();
        trip.getContext().getFilters().get(0).destroy();
        Assert.assertEquals("Result status should be 'danger'", "danger", result.getString("status"));
        String feedback = result.getString("feedbackMsg");
        Assert.assertTrue("Unexpected result message: " + feedback, feedback.contains("not found"));
    }

    /**
     * Try a valid bulk check-in, rack has ancillary in place event associated with it
     */
    public void testCheckIn() throws Exception {
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(BulkStorageOpsActionBean.class
                , storageLocationDaoMock, storageEjbMock, testUserBean);
        trip.addParameter("storageLocationId", "1000");
        trip.addParameter("barcode", rackOfTubes02.getLabel());
        trip.execute(BulkStorageOpsActionBean.EVT_CHECK_IN);
        JsonObject result = Json.createReader(new StringReader(trip.getOutputString())).readObject();
        trip.getContext().getFilters().get(0).destroy();
        Assert.assertEquals("Result status should be 'success'", "success", result.getString("status"));
        String feedback = result.getString("feedbackMsg");
        Assert.assertTrue("Unexpected result message: " + feedback, feedback.contains(rackOfTubes02.getLabel()));
    }

    /**
     * Try a valid bulk check-in, rack has ancillary in place event associated with it
     */
    public void testCheckOut() throws Exception {
        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(BulkStorageOpsActionBean.class
                , storageLocationDaoMock, storageEjbMock, testUserBean);
        trip.addParameter("barcode", rackOfTubes01.getLabel());
        // TODO: JMS I give up, time is being hoovered up because the StorageLocation I added has disappeared:  "Rack not in storage."
        rackOfTubes01.setStorageLocation(slot1);
        when(storageLocationDaoMock.findSingle(LabVessel.class, LabVessel_.label, rackOfTubes01.getLabel())).thenReturn(rackOfTubes01);
        trip.execute(BulkStorageOpsActionBean.EVT_CHECK_OUT);
        JsonObject result = Json.createReader(new StringReader(trip.getOutputString())).readObject();
        trip.getContext().getFilters().get(0).destroy();
        Assert.assertEquals("Result status should be 'success'", "success", result.getString("status"));
        String feedback = result.getString("feedbackMsg");
        Assert.assertTrue("Unexpected result message: " + feedback, feedback.contains(rackOfTubes01.getLabel()));
    }

}