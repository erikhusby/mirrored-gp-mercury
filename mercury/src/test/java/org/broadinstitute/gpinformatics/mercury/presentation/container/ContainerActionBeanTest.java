package org.broadinstitute.gpinformatics.mercury.presentation.container;

import net.sourceforge.stripes.mock.MockRoundtrip;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.presentation.StripesMockTestUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.boundary.storage.StorageEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.persistence.LockModeType;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = TestGroups.DATABASE_FREE)
public class ContainerActionBeanTest {

    private ContainerActionBean actionBean;
    private BarcodedTube barcodedTube;
    private RackOfTubes rackOfTubes;
    private StorageLocation storageLocation;
    private LabVesselDao mockLabVesselDao;
    private LabEventDao mockLabEventDao;
    private MockRoundtrip roundTrip;
    private StorageLocationDao mockStorageLocationDao;
    private StorageEjb mockStorageEjb;

    @BeforeMethod
    public void setUp() throws Exception {
        actionBean = new ContainerActionBean();
        actionBean.setContext(new CoreActionBeanContext());
        barcodedTube = new BarcodedTube("testTube");
        rackOfTubes = new RackOfTubes("testRack", RackOfTubes.RackType.Matrix48SlotRack2mL);
        actionBean.setRackOfTubes(rackOfTubes);
        storageLocation = new StorageLocation("GageRack_A1", StorageLocation.LocationType.SLOT, null);
        mockLabVesselDao = mock(LabVesselDao.class);
        mockStorageEjb = mock( StorageEjb.class );
        when(mockLabVesselDao.findByIdentifier(barcodedTube.getLabel())).thenReturn(barcodedTube);
        when(mockLabVesselDao.findByIdentifier(rackOfTubes.getLabel())).thenReturn(rackOfTubes);
        when(mockLabVesselDao.findSingleSafely(BarcodedTube.class, BarcodedTube_.label, barcodedTube.getLabel(), LockModeType.NONE)).thenReturn(barcodedTube);
        actionBean.setLabVesselDao(mockLabVesselDao);

        UserBean userBean = Mockito.mock(UserBean.class);
        BspUser qaDudeUser = new BSPUserList.QADudeUser(RoleType.PM.name(), 1L);
        Mockito.when(userBean.getBspUser()).thenReturn(qaDudeUser);
        actionBean.setUserBean(userBean);
        mockLabEventDao = mock(LabEventDao.class);
        actionBean.setLabEventDao(mockLabEventDao);
        mockStorageLocationDao = mock(StorageLocationDao.class);
        actionBean.setStorageLocationDao(mockStorageLocationDao);
        actionBean.setStorageEjb(mockStorageEjb);
        roundTrip = StripesMockTestUtils.createMockRoundtrip(ContainerActionBean.class);
    }

    @Test
    public void testBuildPositionMapEmptyRack() {
        actionBean.buildPositionMappingForInStorage();
        Assert.assertEquals(actionBean.getMapPositionToVessel().isEmpty(), true);
    }

    @Test
    public void testBuildPositionMapWithEvent() {
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        mapPositionToTube.put(VesselPosition.A01, barcodedTube);
        LabEvent event = addEventToRack(rackOfTubes, mapPositionToTube, LabEventType.STORAGE_CHECK_IN, storageLocation);
        when(mockLabEventDao.findInPlaceByAncillaryVessel(rackOfTubes)).thenReturn(Collections.singletonList(event));
        when(mockStorageEjb.findLatestCheckInEvent(rackOfTubes) ).thenReturn(event);
        actionBean.buildPositionMappingForInStorage();
        Assert.assertEquals(actionBean.getMapPositionToVessel().get(VesselPosition.A01), barcodedTube);
    }

    @Test
    public void testBuildPositionMapHandleRearrays() {
        // Put checkout date an hour later than now
        // (otherwise if within a millisecond, date based treemap iterator will be non-deterministic)
        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.HOUR, 1);
        Date checkoutDate = cal.getTime();

        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        mapPositionToTube.put(VesselPosition.A01, barcodedTube);
        addEventToRack(rackOfTubes, mapPositionToTube, LabEventType.STORAGE_CHECK_IN, storageLocation);

        mapPositionToTube = new HashMap<>();
        mapPositionToTube.put(VesselPosition.A05, barcodedTube);
        TubeFormation rearrayFormation = new TubeFormation(mapPositionToTube, rackOfTubes.getRackType());
        RackOfTubes newRearrayRack = new RackOfTubes("RearrayRack", RackOfTubes.RackType.Matrix48SlotRack2mL);
        rearrayFormation.addRackOfTubes(newRearrayRack);
        newRearrayRack.getTubeFormations().add(rearrayFormation);
        LabEvent labEvent2 = new LabEvent(LabEventType.STORAGE_CHECK_OUT, checkoutDate, "UnitTest", 1L, 1L, "UnitTest");
        labEvent2.setInPlaceLabVessel(rearrayFormation);
        labEvent2.setAncillaryInPlaceVessel(newRearrayRack);
        labEvent2.setStorageLocation(storageLocation);
        rearrayFormation.addInPlaceEvent(labEvent2);

        actionBean.buildPositionMappingForInStorage();
        Assert.assertEquals(actionBean.getMapPositionToVessel().isEmpty(), true);
    }

    @Test
    public void testSaveToLocation() throws Exception {
        actionBean.setStorageLocation(storageLocation);
        actionBean.saveContainer();
        Assert.assertEquals(actionBean.getContext().getValidationErrors().size(), 1);

        List<ReceptacleType> receptacleTypeList = new ArrayList<>();
        ReceptacleType receptacleType = new ReceptacleType();
        receptacleType.setBarcode(barcodedTube.getLabel());
        receptacleType.setPosition(VesselPosition.A01.name());
        receptacleTypeList.add(receptacleType);
        actionBean.setReceptacleTypes(receptacleTypeList);

        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        mapPositionToTube.put(VesselPosition.A01, barcodedTube);
        LabEvent event = addEventToRack(rackOfTubes, mapPositionToTube, LabEventType.STORAGE_CHECK_IN, storageLocation);
        when(mockLabEventDao.findInPlaceByAncillaryVessel(rackOfTubes)).thenReturn(Collections.singletonList(event));
        when(mockStorageEjb.findLatestCheckInEvent(rackOfTubes) ).thenReturn(event);

        actionBean.buildPositionMappingForInStorage();
        MessageCollection messageCollection = new MessageCollection();
        actionBean.handleSaveContainer(messageCollection);
        Assert.assertEquals(messageCollection.hasErrors(), false);
        Assert.assertEquals(rackOfTubes.getStorageLocation(), null);
    }

    private LabEvent addEventToRack(RackOfTubes rackOfTubes, Map<VesselPosition, BarcodedTube> mapPositionToTube,
                                LabEventType labEventType, StorageLocation storageLocation ) {

        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, rackOfTubes.getRackType());
        tubeFormation.addRackOfTubes(rackOfTubes);
        rackOfTubes.getTubeFormations().add(tubeFormation);
        // Use DAOFree
        LabEvent labEvent = ( new StorageEjb() ).createStorageEvent( labEventType, tubeFormation, storageLocation, rackOfTubes, 1L );

        return labEvent;
    }
}