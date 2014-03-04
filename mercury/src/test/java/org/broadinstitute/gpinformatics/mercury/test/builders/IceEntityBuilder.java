package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds entity graphs for the Illumina Content Exome process.
 */
public class IceEntityBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final TubeFormation pondRegRack;
    private final String pondRegRackBarcode;
    private final List<String> pondRegTubeBarcodes;
    private String testPrefix;

    public IceEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
            LabEventFactory labEventFactory, LabEventHandler labEventHandler,
            TubeFormation pondRegRack, String pondRegRackBarcode,
            List<String> pondRegTubeBarcodes, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.pondRegRack = pondRegRack;
        this.pondRegRackBarcode = pondRegRackBarcode;
        this.pondRegTubeBarcodes = pondRegTubeBarcodes;
        this.testPrefix = testPrefix;
    }

    public IceEntityBuilder invoke() {
        IceJaxbBuilder iceJaxbBuilder = new IceJaxbBuilder(bettaLimsMessageTestFactory, testPrefix, pondRegRackBarcode,
                pondRegTubeBarcodes, testPrefix + "IceBait1", testPrefix + "IceBait2").invoke();

        LabEventTest.validateWorkflow("IcePoolingTransfer", pondRegRack);
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(pondRegRack.getLabel(), pondRegRack);
        for (TwoDBarcodedTube twoDBarcodedTube : pondRegRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(twoDBarcodedTube.getLabel(), twoDBarcodedTube);
        }
        LabEvent icePoolingTransfer = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIcePoolingTransfer(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(icePoolingTransfer);

        return this;
    }
}
