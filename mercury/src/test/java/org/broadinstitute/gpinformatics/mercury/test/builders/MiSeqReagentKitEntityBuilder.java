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
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.HashMap;
import java.util.Map;

public class MiSeqReagentKitEntityBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final TubeFormation denatureRack;
    private final String mySeqReagentKitBarcode;
    private MiSeqReagentKit miSeqReagentKit;
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

        mapBarcodeToSourceTubeFormation.put(denatureRack.getLabel(), denatureRack);
        for (VesselPosition vesselPosition : denatureRack.getVesselGeometry().getVesselPositions()) {
            final TwoDBarcodedTube tube =
                    denatureRack.getContainerRole().getVesselAtPosition(vesselPosition);
            if (tube != null) {
                denatureRackMap.put(tube.getLabel(), vesselPosition);
                mapPositionToTube.put(vesselPosition, tube);
                mapBarcodeToSourceTube.put(tube.getLabel(), tube);
            }

        }

        miSeqReagentKitTransferJaxb = new MiSeqReagentKitJaxbBuilder(
                bettaLimsMessageTestFactory, denatureRack.getLabel(), mapBarcodeToSourceRackOfTubes,mapBarcodeToSourceTube,
                mySeqReagentKitBarcode, StaticPlate.PlateType.MiSeqReagentKit.getDisplayName()
        ).invoke();


        final PlateCherryPickEvent denatureJaxb = miSeqReagentKitTransferJaxb.getDenatureJaxb();
        LabEvent miSeqReagentKitTransferEntity = labEventFactory
                        .buildCherryPickRackToReagentKitDbFree(denatureJaxb,mapBarcodeToSourceTubeFormation,mapBarcodeToSourceRackOfTubes,mapBarcodeToSourceTube);
        labEventHandler.processEvent(miSeqReagentKitTransferEntity);
        //asserts
//        miSeqReagentKit =
//                (MiSeqReagentKit) miSeqReagentKitTransferJaxb


        LabEventTest.validateWorkflow(LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER.getName(), miSeqReagentKit);

        return this;
    }

    public MiSeqReagentKit getMiSeqReagentKit() {
        return miSeqReagentKit;
    }
}
