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

package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.util.HashMap;
import java.util.Map;

public class MiSeqReagentKitEntityBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final TubeFormation denatureRack;
    private final String mySeqReagentKitBarcode;

    MiSeqReagentKitJaxbBuilder miSeqReagentKitTransferJaxb;

    public MiSeqReagentKitEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                        LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                        TubeFormation denatureRack, String mySeqReagentKitBarcode) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.denatureRack = denatureRack;
        this.mySeqReagentKitBarcode = mySeqReagentKitBarcode;
    }

    public MiSeqReagentKitEntityBuilder invoke() {
        Map<String, TubeFormation> mapBarcodeToSourceTubeFormation = new HashMap<>();
        Map<String, RackOfTubes> mapBarcodeToSourceRackOfTubes = new HashMap<>();
        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube = new HashMap<>();
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<>();
        Map<String, VesselPosition> denatureRackMap = new HashMap<>();

        for (RackOfTubes rackOfTubes : denatureRack.getRacksOfTubes()) {
            mapBarcodeToSourceRackOfTubes.put(denatureRack.getLabel(), rackOfTubes);


            for (TubeFormation tubeFormation : rackOfTubes.getTubeFormations()) {
                mapBarcodeToSourceTubeFormation.put(tubeFormation.getLabel(),tubeFormation);
                for (VesselPosition vesselPosition : tubeFormation.getVesselGeometry().getVesselPositions()) {
                    final TwoDBarcodedTube tube =
                            tubeFormation.getContainerRole().getVesselAtPosition(vesselPosition);
                    denatureRackMap.put(tube.getLabel(),vesselPosition);
                    mapPositionToTube.put(vesselPosition,tube);
                    mapBarcodeToSourceTube.put(tube.getLabel(), tube);

                }

            }

        }

        miSeqReagentKitTransferJaxb = new MiSeqReagentKitJaxbBuilder(
                bettaLimsMessageTestFactory,denatureRack.getLabel(),denatureRackMap,
                mySeqReagentKitBarcode,StaticPlate.PlateType.MiSeqReagentKit.getDisplayName()
        ).invoke();


        LabEvent miSeqReagentKitTransferEntity = miSeqReagentKitTransferJaxb.getDenatureToMiSeqReagentKitJaxb();

        labEventHandler.processEvent(miSeqReagentKitTransferEntity);
        //asserts
        MiSeqReagentKit reagentKit =
                (MiSeqReagentKit) miSeqReagentKitTransferEntity.getCherryPickTransfers().iterator().next().getTargetVesselContainer()
                        .getEmbedder();


//        LabEventTest.validateWorkflow(LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER.getName(), reagentKit);

        return this;
    }

    public MiSeqReagentKitJaxbBuilder getMiSeqReagentKitTransferJaxb() {
        return miSeqReagentKitTransferJaxb;
    }

    public String getMySeqReagentKitBarcode() {
        return mySeqReagentKitBarcode;
    }
}
