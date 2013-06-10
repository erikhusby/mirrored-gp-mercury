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
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MiSeqReagentKitJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String sourceRackBarcode;
    private final Map<String, VesselPosition> sourceTubeBarcodes;
    private final String targetPlateBarcode;
    private final String plateType;
    private LabEvent denatureToMiSeqReagentKitJaxb;
    private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();

    public MiSeqReagentKitJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String sourceRackBarcode,
                                      Map<String, VesselPosition> sourceTubeBarcodes,
                                      String targetPlateBarcode, String plateType) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.sourceRackBarcode = sourceRackBarcode;
        this.sourceTubeBarcodes = sourceTubeBarcodes;
        this.targetPlateBarcode = targetPlateBarcode;
        this.plateType = plateType;
    }

    public MiSeqReagentKitJaxbBuilder invoke() {
        Map<String,TubeFormation> mapBarcodeToSorceTubeFormation=new HashMap<>();
        Map<String,RackOfTubes> mapBarcodeToSourceRackOfTubes=new HashMap<>();
        Map<String,TwoDBarcodedTube> mapBarcodeToSourceTube=new HashMap<>();


        for (String sourceBarcode : sourceTubeBarcodes.keySet()) {

        }
        PlateCherryPickEvent denatureJaxb = bettaLimsMessageTestFactory
                .buildCherryPickToReagentKit(LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER.getName(),
                         mapBarcodeToSourceRackOfTubes, mapBarcodeToSourceTube,
                        targetPlateBarcode);
        BettaLIMSMessage denatureMessage = bettaLimsMessageTestFactory.addMessage(messageList, denatureJaxb);


        return this;
    }

    public LabEvent getDenatureToMiSeqReagentKitJaxb() {
        return denatureToMiSeqReagentKitJaxb;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }
}
