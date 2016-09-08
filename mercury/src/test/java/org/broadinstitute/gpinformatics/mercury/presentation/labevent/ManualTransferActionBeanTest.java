package org.broadinstitute.gpinformatics.mercury.presentation.labevent;

import net.sourceforge.stripes.mock.MockRoundtrip;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.presentation.StripesMockTestUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.*;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Database free test of action bean.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ManualTransferActionBeanTest {

    /**
     * Verify that choosing XStain creates many events, each with many reagents.
     */
    public void testXStainInit() {
        String eventType = "InfiniumXStain";
        ManualTransferActionBean actionBean = chooseEvent(eventType);

        List<StationEventType> stationEvents = actionBean.getStationEvents();
        LabEventType.ManualTransferDetails manualTransferDetails =
                LabEventType.INFINIUM_XSTAIN.getManualTransferDetails();
        Assert.assertEquals(stationEvents.size(), manualTransferDetails.getNumEvents());
        Assert.assertEquals(stationEvents.get(0).getEventType(), eventType);
        Assert.assertTrue(stationEvents.get(0) instanceof PlateEventType);
        PlateEventType plateEventType = (PlateEventType) stationEvents.get(0);

        int numReagentFields = 0;
        for (int i : manualTransferDetails.getReagentFieldCounts()) {
            numReagentFields += i;
        }
        Assert.assertEquals(plateEventType.getReagent().size(), numReagentFields);
    }

    /**
     * Simulate choosing an event, and return the resulting action bean.
     */
    private ManualTransferActionBean chooseEvent(String eventType) {
        try {
            MockRoundtrip roundTrip = StripesMockTestUtils.createMockRoundtrip(ManualTransferActionBean.class);
            roundTrip.setParameter("stationEvents[0].eventType", eventType);
            roundTrip.execute(ManualTransferActionBean.CHOOSE_EVENT_TYPE_ACTION);
            return roundTrip.getActionBean(ManualTransferActionBean.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test rack to plate transfer.
     */
    public void testShearingTransfer() {
        // Get the skeleton station event
        String eventType = "ShearingTransfer";
        ManualTransferActionBean initActionBean = chooseEvent(eventType);
        List<StationEventType> stationEvents = initActionBean.getStationEvents();

        ManualTransferActionBean actionBean = new ManualTransferActionBean();
        actionBean.setContext(new CoreActionBeanContext());

        // The action bean needs the user, to set the operator field in the event
        UserBean userBean = Mockito.mock(UserBean.class);
        BspUser qaDudeUser = new BSPUserList.QADudeUser(RoleType.PM.name(), 1L);
        Mockito.when(userBean.getBspUser()).thenReturn(qaDudeUser);
        actionBean.setUserBean(userBean);

        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        actionBean.setLabVesselDao(labVesselDao);

        // Set reagent
        actionBean.setManualTransferDetails(LabEventType.SHEARING_TRANSFER.getManualTransferDetails());
        PlateTransferEventType plateTransferEventType = (PlateTransferEventType) stationEvents.get(0);
        ReagentType reagentType = plateTransferEventType.getReagent().get(0);
        reagentType.setBarcode("asdf");
        reagentType.setExpiration(new Date());

        // Set source rack, and mock DAO call
        PlateType sourcePlate = plateTransferEventType.getSourcePlate();
        String sourceBarcode = "SourceRack";
        sourcePlate.setBarcode(sourceBarcode);
        Mockito.when(labVesselDao.findByIdentifier(sourceBarcode)).thenReturn(
                new StaticPlate(sourceBarcode, StaticPlate.PlateType.Plate96Well200));

        // Add a tube
        PositionMapType sourcePositionMap = new PositionMapType();
        ReceptacleType receptacleType = new ReceptacleType();
        receptacleType.setPosition("A01");
        final String tubeBarcode = "tube1";
        receptacleType.setBarcode(tubeBarcode);
        Mockito.when(labVesselDao.findByBarcodes(Arrays.asList(tubeBarcode))).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(tubeBarcode, new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075));
                }});
        sourcePositionMap.getReceptacle().add(receptacleType);

        // Check that empty positions are filtered out
        receptacleType = new ReceptacleType();
        receptacleType.setPosition("A02");
        sourcePositionMap.getReceptacle().add(receptacleType);
        plateTransferEventType.setSourcePositionMap(sourcePositionMap);

        // Set destination plate, and mock DAO call
        PlateType destPlateType = plateTransferEventType.getPlate();
        String destBarcode = "DestPlate";
        destPlateType.setBarcode(destBarcode);
        Mockito.when(labVesselDao.findByIdentifier(destBarcode)).thenReturn(
                new StaticPlate(destBarcode, StaticPlate.PlateType.Plate96Well200));
        actionBean.setStationEvents(stationEvents);

        BettaLIMSMessage bettaLIMSMessage = actionBean.buildBettaLIMSMessage();
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
        Assert.assertNotNull(bettaLIMSMessage);
        PositionMapType sourcePositionMap1 = bettaLIMSMessage.getPlateTransferEvent().get(0).getSourcePositionMap();
        Assert.assertEquals(sourcePositionMap1.getReceptacle().size(), 1);
    }

    /**
     *  Test cherry pick pooling transfer.
     */
    public void testCherryPickTransfer() {
        // Get the skeleton station event
        String eventType = "PoolingTransfer";
        ManualTransferActionBean initActionBean = chooseEvent(eventType);
        List<StationEventType> stationEvents = initActionBean.getStationEvents();

        ManualTransferActionBean actionBean = new ManualTransferActionBean();
        actionBean.setContext(new CoreActionBeanContext());

        // The action bean needs the user, to set the operator field in the event
        UserBean userBean = Mockito.mock(UserBean.class);
        BspUser qaDudeUser = new BSPUserList.QADudeUser(RoleType.PM.name(), 1L);
        Mockito.when(userBean.getBspUser()).thenReturn(qaDudeUser);
        actionBean.setUserBean(userBean);

        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        actionBean.setLabVesselDao(labVesselDao);

        // Set reagent
        actionBean.setManualTransferDetails(LabEventType.POOLING_TRANSFER.getManualTransferDetails());
        PlateCherryPickEvent plateTransferEventType = (PlateCherryPickEvent) stationEvents.get(0);
        ReagentType reagentType = plateTransferEventType.getReagent().get(0);
        reagentType.setBarcode("asdf");
        reagentType.setExpiration(new Date());

        // Add source tubes. The test requires at least one valid tube position.
        PositionMapType sourcePositionMap = new PositionMapType();
        ReceptacleType receptacleType = new ReceptacleType();
        receptacleType.setPosition("A02");

        final String tubeBarcode = "tube1";
        receptacleType.setBarcode(tubeBarcode);
        Mockito.when(labVesselDao.findByBarcodes(Arrays.asList(tubeBarcode))).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(tubeBarcode, new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075));
                }});
        sourcePositionMap.getReceptacle().add(receptacleType);


        // Add destination tubes. The test requires at least one valid tube position.
        PositionMapType positionMap = new PositionMapType();
        receptacleType = new ReceptacleType();
        receptacleType.setPosition("A04");
        final String barcode = "tube2";
        receptacleType.setBarcode(barcode);
        positionMap.getReceptacle().add(receptacleType);

        plateTransferEventType.getPositionMap().add(0, positionMap);
        plateTransferEventType.getSourcePositionMap().add(0, sourcePositionMap);
        actionBean.setStationEvents(stationEvents);

        //Inject a fake JSON string to simulate the input from the pooling transfer screen.
        actionBean.setCherryPickJSON("[{\"sourceIDs\":[\"B02_src_RcpBcd0_13\"],\"targetIDs\":[\"A01_dest_RcpBcd0_0\"],\"sourceBarcodes\":[\"1077064182\"],\"targetBarcodes\":[\"1076205469\"],\"targetFCT\":[null],\"targetPositions\":[1]}]");
        BettaLIMSMessage bettaLIMSMessage = actionBean.buildBettaLIMSMessage();
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
        Assert.assertNotNull(bettaLIMSMessage);
    }

    /**
     *  Test cherry pick strip tube transfer.
     */
    public void testStripTubeTransfer() {
        // Get the skeleton station event
        String eventType = "StripTubeBTransfer";
        ManualTransferActionBean initActionBean = chooseEvent(eventType);
        List<StationEventType> stationEvents = initActionBean.getStationEvents();

        ManualTransferActionBean actionBean = new ManualTransferActionBean();
        actionBean.setContext(new CoreActionBeanContext());

        // The action bean needs the user, to set the operator field in the event
        UserBean userBean = Mockito.mock(UserBean.class);
        BspUser qaDudeUser = new BSPUserList.QADudeUser(RoleType.PM.name(), 1L);
        Mockito.when(userBean.getBspUser()).thenReturn(qaDudeUser);
        actionBean.setUserBean(userBean);

        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        actionBean.setLabVesselDao(labVesselDao);

        // Tubes and reagents are only set for validation purposes.
        // Set reagent
        actionBean.setManualTransferDetails(LabEventType.STRIP_TUBE_B_TRANSFER.getManualTransferDetails());
        PlateCherryPickEvent plateTransferEventType = (PlateCherryPickEvent) stationEvents.get(0);
        ReagentType reagentType = plateTransferEventType.getReagent().get(0);
        reagentType.setBarcode("asdf");
        reagentType.setExpiration(new Date());

        // Add source tubes. The test requires at least one valid tube position.
        PositionMapType sourcePositionMap = new PositionMapType();
        ReceptacleType receptacleType = new ReceptacleType();
        receptacleType.setPosition("A02");
        final String tubeBarcode = "tube1";
        receptacleType.setBarcode(tubeBarcode);
        Mockito.when(labVesselDao.findByBarcodes(Arrays.asList(tubeBarcode))).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(tubeBarcode, new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075));
                }});
        sourcePositionMap.getReceptacle().add(receptacleType);

        // Add destination tubes. The test requires at least one valid tube position.
        PositionMapType positionMap = new PositionMapType();
        receptacleType = new ReceptacleType();
        receptacleType.setPosition("A04");
        final String barcode = "tube2";
        receptacleType.setBarcode(barcode);
        positionMap.getReceptacle().add(receptacleType);

        plateTransferEventType.getPositionMap().add(0, positionMap);
        plateTransferEventType.getSourcePositionMap().add(0, sourcePositionMap);

        actionBean.setStationEvents(stationEvents);

        //Inject a fake JSON string to simulate the input from the strip tube transfer screen.
        actionBean.setCherryPickJSON("[{\"sourceIDs\":[\"A01_src_RcpBcd0_0\"],\"targetIDs\":[\"A01_dest_RcpBcd0_0\",\"B01_dest_RcpBcd0_0\",\"C01_dest_RcpBcd0_0\",\"D01_dest_RcpBcd0_0\",\"E01_dest_RcpBcd0_0\",\"F01_dest_RcpBcd0_0\",\"G01_dest_RcpBcd0_0\",\"H01_dest_RcpBcd0_0\"],\"sourceBarcodes\":[\"DenatureTube08120952080\"],\"targetBarcodes\":[\"StripTube0812095208ADXX1\",\"StripTube0812095208ADXX1\",\"StripTube0812095208ADXX1\",\"StripTube0812095208ADXX1\",\"StripTube0812095208ADXX1\",\"StripTube0812095208ADXX1\",\"StripTube0812095208ADXX1\",\"StripTube0812095208ADXX1\"],\"targetFCT\":[\"FCT-32386\",\"FCT-32386\",\"FCT-32386\",\"FCT-32386\",\"FCT-32386\",\"FCT-32386\",\"FCT-32386\",\"FCT-32386\"],\"targetPositions\":[1,2,3,4,5,6,7,8]}]");
        BettaLIMSMessage bettaLIMSMessage = actionBean.buildBettaLIMSMessage();
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
        Assert.assertNotNull(bettaLIMSMessage);
    }
}