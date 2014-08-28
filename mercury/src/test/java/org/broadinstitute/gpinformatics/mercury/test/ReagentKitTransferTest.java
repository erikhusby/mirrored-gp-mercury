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
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Test(enabled = false, groups = TestGroups.DATABASE_FREE)
public class ReagentKitTransferTest {
    BettaLimsMessageTestFactory bettaLimsMessageTestFactory = null;

    @BeforeTest
    public void setUp() {
        bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
    }

    public void testDenatureToReagentKit() {
        final long time = new Date().getTime();
        String denatureRackBarcode = "denatureRack" + time;
        String miSeqReagentKitBarcode = "reagentKit" + time;
        Map<String, RackOfTubes> mapBarcodeToSourceRackOfTubes = new HashMap<>();
        Map<String, BarcodedTube> mapBarcodeToSourceTube = new HashMap<>();

        Map<String, VesselPosition> sourceBarcodes = new HashMap<>();
        RackOfTubes denatureRack = new RackOfTubes(denatureRackBarcode, RackOfTubes.RackType.Matrix96);

        for (int i = 1; i <= 8; i++) {
            final String tubeBarcode = String.format("denatureTube0%s-%s", i, time);
            final String position = "A0" + i;
            sourceBarcodes.put(tubeBarcode, VesselPosition.valueOf(position));
            BarcodedTube tube = new BarcodedTube(tubeBarcode);
            Map<VesselPosition, BarcodedTube> positionMap = new HashMap<>();
            positionMap.put(VesselPosition.getByName(position), tube);
            TubeFormation tubeFormation = new TubeFormation(positionMap, RackOfTubes.RackType.Matrix96);
            mapBarcodeToSourceTube.put(tubeBarcode, tube);
            denatureRack.getTubeFormations().add(tubeFormation);
            mapBarcodeToSourceRackOfTubes.put(denatureRack.getLabel(), denatureRack);
        }

        final List<String> sourceBarcodeList = new ArrayList<>(sourceBarcodes.keySet());
        Collection<PlateCherryPickEvent> transferEventTypes = bettaLimsMessageTestFactory
                .buildCherryPickToReagentKit(
                        LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER.getName(), denatureRack,
                        miSeqReagentKitBarcode);

        for (PlateCherryPickEvent transferEventType : transferEventTypes) {
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
            final PlateType reagentKit = transferEventType.getPlate().get(0);
            MatcherAssert.assertThat(reagentKit.getBarcode(), Matchers.equalTo(miSeqReagentKitBarcode));
            MatcherAssert.assertThat(reagentKit.getPhysType(), Matchers.equalTo("MiseqReagentKit"));
            MatcherAssert.assertThat(reagentKit.getPhysType(),
                    Matchers.equalTo(StaticPlate.PlateType.MiSeqReagentKit.getAutomationName()));

            // test the kind of event returned
            MatcherAssert.assertThat(transferEventType.getEventType(),
                    Matchers.equalTo(LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER.getName()));
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateCherryPickEvent().add(transferEventType);
            final String message = BettaLimsMessageTestFactory.marshal(bettaLIMSMessage);
            Assert.assertFalse(message.isEmpty());
        }
    }
}
