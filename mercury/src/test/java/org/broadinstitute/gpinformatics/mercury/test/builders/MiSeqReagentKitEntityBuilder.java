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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.Assert;

import java.util.HashMap;
import java.util.Map;

public class MiSeqReagentKitEntityBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final TubeFormation denatureRack;
    private final String mySeqReagentKitBarcode;
    private MiSeqReagentKit miSeqReagentKit;
    MiSeqReagentKitJaxbBuilder reagentKitTransferJaxb;

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
        Map<String, VesselPosition> denatureRackMap = new HashMap<>();
        String denatureBarcode = null;
        for (VesselPosition vesselPosition : denatureRack.getVesselGeometry().getVesselPositions()) {
            final TwoDBarcodedTube tube = denatureRack.getContainerRole().getVesselAtPosition(vesselPosition);
            if (tube != null) {
                denatureBarcode = tube.getLabel();
                denatureRackMap.put(denatureBarcode, vesselPosition);
            }

        }

        reagentKitTransferJaxb =
                new MiSeqReagentKitJaxbBuilder(bettaLimsMessageTestFactory, denatureBarcode, denatureRackMap,
                        mySeqReagentKitBarcode).invoke();
        final PlateCherryPickEvent plateCherryPickEvent =
                reagentKitTransferJaxb.getDenatureToReagentKitJaxb().getPlateCherryPickEvent().get(0);
        Assert.assertNotNull(plateCherryPickEvent);
        Assert.assertEquals(plateCherryPickEvent.getSource().get(0).getBarcode(),"DenatureRack"+mySeqReagentKitBarcode);

        Assert.assertEquals(plateCherryPickEvent.getPlate().getBarcode(),mySeqReagentKitBarcode);

        return this;
    }

    public MiSeqReagentKit getMiSeqReagentKit() {
        return miSeqReagentKit;
    }
}
