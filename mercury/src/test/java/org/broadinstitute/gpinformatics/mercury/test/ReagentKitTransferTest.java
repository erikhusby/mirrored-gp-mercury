/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettalimsMessageBeanTest;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class ReagentKitTransferTest {
    BettaLimsMessageTestFactory bettaLimsMessageTestFactory = null;

    @BeforeTest
    public void setUp() {
        bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
    }

    @DataProvider(name = "denatureTubeDataProvider")
    public Object[][] getDataProvider() {
        final long time = new Date().getTime();
        String denatureRackBarcode = "denatureRack" + time;
        String miSeqReagentKitBarcode = "reagentKit" + time;
        List<LabEventFactory.CherryPick> cherryPicks = new ArrayList<>();
        Map<String, VesselPosition> sourceBarcodes = new HashMap<>();
        for (int i = 1; i <= 8; i++) {
            final String tubeBarcode = String.format("denatureTube0%s-%s", i, time);
            String position = "A0" + i;
            sourceBarcodes.put(tubeBarcode, VesselPosition.valueOf(position));
            LabEventFactory.CherryPick cherryPick = new LabEventFactory.CherryPick(
                    tubeBarcode, position, miSeqReagentKitBarcode, MiSeqReagentKit.LOADING_WELL.name());
            cherryPicks.add(cherryPick);
        }

        return new Object[][]{
                {denatureRackBarcode, sourceBarcodes, miSeqReagentKitBarcode, cherryPicks},
                {null, sourceBarcodes, miSeqReagentKitBarcode, cherryPicks}
        };
    }

    public void testDenatureToReagentKit() {
        final long time = new Date().getTime();
        String denatureRackBarcode = "denatureRack" + time;
        String miSeqReagentKitBarcode = "reagentKit" + time;
        List<LabEventFactory.CherryPick> cherryPicks = new ArrayList<>();
        Map<String, VesselPosition> sourceBarcodes = new HashMap<>();
        RackOfTubes denatureRack=new RackOfTubes(denatureRackBarcode, RackOfTubes.RackType.Matrix96);

        for (int i = 1; i <= 8; i++) {
            final String tubeBarcode = String.format("denatureTube0%s-%s", i, time);
            final String position = "A0" + i;
            sourceBarcodes.put(tubeBarcode, VesselPosition.valueOf(position));
            LabEventFactory.CherryPick cherryPick = new LabEventFactory.CherryPick(
                    tubeBarcode, position, miSeqReagentKitBarcode, MiSeqReagentKit.LOADING_WELL.name());
            cherryPicks.add(cherryPick);
            final TwoDBarcodedTube tube = new TwoDBarcodedTube(tubeBarcode);
            TubeFormation tubeFormation = new TubeFormation(new HashMap < VesselPosition, TwoDBarcodedTube >()
                    {{

                    put(VesselPosition.getByName(position), tube);}}, RackOfTubes.RackType.Matrix96);
            tubeFormation.setVesselContainer(new VesselContainer<TwoDBarcodedTube>(tube));
            denatureRack.getTubeFormations().add(tubeFormation);
        }

        final PlateCherryPickEvent plateCherryPickEvent = new PlateCherryPickEvent();
        plateCherryPickEvent.setEventType(LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER.getName());

        final List<String> sourceBarcodeList = new ArrayList(sourceBarcodes.keySet());

        Map<String, TubeFormation> mapBarcodeToSourceTubeFormation = new HashMap<>();
        Map<String, RackOfTubes> mapBarcodeToSourceRackOfTubes = new HashMap<>();
        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube = new HashMap<>();
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<>();
        Map<String, VesselPosition> denatureRackMap = new HashMap<>();

        mapBarcodeToSourceRackOfTubes.put(denatureRack.getLabel(), denatureRack);
        for (TubeFormation tubeFormation : denatureRack.getTubeFormations()) {
            mapBarcodeToSourceTubeFormation.put(tubeFormation.getLabel(), tubeFormation);
            for (VesselPosition vesselPosition : tubeFormation.getVesselGeometry().getVesselPositions()) {
                final LabVessel tube =
                        tubeFormation.getContainerRole().getEmbedder();
                denatureRackMap.put(tube.getLabel(), vesselPosition);
//                mapPositionToTube.put(vesselPosition, tube);
//                mapBarcodeToSourceTube.put(tube.getLabel(), tube);

            }
        }

        PlateCherryPickEvent transferEventType = bettaLimsMessageTestFactory
                .buildCherryPickToReagentKit(
                        LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER.getName(),mapBarcodeToSourceRackOfTubes,
                        mapBarcodeToSourceTube,miSeqReagentKitBarcode);

        // test the source denature tube
        MatcherAssert.assertThat(transferEventType.getSourcePlate(), Matchers.hasSize(1));
        final PlateType plateType = transferEventType.getSourcePlate().get(0);
        MatcherAssert.assertThat(plateType.getBarcode(), Matchers.equalTo(denatureRackBarcode));
        MatcherAssert.assertThat(plateType.getPhysType(), Matchers.equalTo("TubeRack"));

        // test the source denature tube map
        MatcherAssert.assertThat(transferEventType.getSourcePositionMap(), Matchers.hasSize(1));
        final PositionMapType sourceMap = transferEventType.getSourcePositionMap().get(0);
        MatcherAssert.assertThat(sourceMap.getBarcode(), Matchers.equalTo(denatureRackBarcode));
        MatcherAssert.assertThat(sourceMap.getReceptacle(), Matchers.hasSize(8));
        for (ReceptacleType receptacle : transferEventType.getSourcePositionMap().get(0).getReceptacle()) {
            MatcherAssert.assertThat(sourceBarcodeList, Matchers.hasItem(receptacle.getBarcode()));
        }

        // Test the created kit.
        final PlateType reagentKit = transferEventType.getPlate();
        MatcherAssert.assertThat(reagentKit.getBarcode(), Matchers.equalTo(miSeqReagentKitBarcode));
        MatcherAssert.assertThat(reagentKit.getPhysType(), Matchers.equalTo("MiseqReagentKit"));
        MatcherAssert.assertThat(reagentKit.getPhysType(),
                Matchers.equalTo(StaticPlate.PlateType.MiSeqReagentKit.getDisplayName()));

        // test the kind of event returned
        MatcherAssert.assertThat(transferEventType.getEventType(),
                Matchers.equalTo(LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER.getName()));
        BettaLIMSMessage bettaLIMSMessage  = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateCherryPickEvent().add(transferEventType);
        final String message = BettalimsMessageBeanTest.marshalMessage(bettaLIMSMessage);
        Assert.assertFalse(message.isEmpty());
    }
}
