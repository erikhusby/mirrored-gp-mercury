package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class MiSeqReagentKitEntityBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final String reagentBlockBarcode;
    private final TubeFormation denatureRack;
    private final String flowcellBarcode;
    private MiSeqReagentKit reagentKit;


    public MiSeqReagentKitEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                        LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                        String reagentBlockBarcode, TubeFormation denatureRack, String flowcellBarcode) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.reagentBlockBarcode = reagentBlockBarcode;
        this.denatureRack = denatureRack;
        this.flowcellBarcode = flowcellBarcode;
    }

    public MiSeqReagentKitEntityBuilder invoke() {

        Map<VesselPosition, BarcodedTube> mapPositionToVessel = denatureRack.getContainerRole().getMapPositionToVessel();

        Map<String, VesselPosition> mapBarcodeToPosition = new HashMap<>();

        for (Map.Entry<VesselPosition, BarcodedTube> vesselToPosition : mapPositionToVessel.entrySet()) {
            mapBarcodeToPosition.put(vesselToPosition.getValue().getLabel(), vesselToPosition.getKey());
        }

        final MiSeqReagentKitJaxbBuilder jaxbBuilder =
                new MiSeqReagentKitJaxbBuilder(mapBarcodeToPosition,reagentBlockBarcode, flowcellBarcode, bettaLimsMessageTestFactory);
        jaxbBuilder.invoke();
        PlateCherryPickEvent reagentkitXfer = jaxbBuilder.getDenatureToReagentKitJaxb();


        LabEvent reagentKitEvent = labEventFactory.buildFromBettaLims(reagentkitXfer);
        labEventHandler.processEvent(reagentKitEvent );

        reagentKit = (MiSeqReagentKit) reagentKitEvent.getTargetLabVessels().iterator().next();

        Assert.assertNotNull(reagentKit);
        Assert.assertEquals(reagentBlockBarcode, reagentKit.getLabel());

        return this;
    }

    public MiSeqReagentKit getReagentKit() {
        return reagentKit;
    }
}
