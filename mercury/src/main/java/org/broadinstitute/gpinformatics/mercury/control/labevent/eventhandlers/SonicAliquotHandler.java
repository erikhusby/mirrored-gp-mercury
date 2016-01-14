package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;
import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Handles a an aliquot message for Sonic.  The source rack is a mix of CRSP and BSP.  The BSP
 */
public class SonicAliquotHandler extends AbstractEventHandler {

    @Inject
    private SamplesDaughterPlateHandler samplesDaughterPlateHandler;

    @Override
    public void handleEvent(LabEvent targetEvent, StationEventType stationEvent) {
        // Clone the source event
        PlateTransferEventType plateTransferEventType = (PlateTransferEventType) stationEvent;
        StringWriter xml = new StringWriter();
        JAXB.marshal(plateTransferEventType, xml);
        StringReader reader = new StringReader(xml.toString());
        PlateTransferEventType unmarshal = JAXB.unmarshal(reader, plateTransferEventType.getClass());

        // Allow random access to source tubes
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        for (LabVessel labVessel : targetEvent.getSourceLabVessels()) {
            mapBarcodeToVessel.put(labVessel.getLabel(), labVessel);
        }

        // Remove source tubes that aren't in BSP
        Iterator<ReceptacleType> iterator = unmarshal.getSourcePositionMap().getReceptacle().iterator();
        Set<String> removePositions = new HashSet<>();
        while (iterator.hasNext()) {
            ReceptacleType receptacleType = iterator.next();
            LabVessel labVessel = mapBarcodeToVessel.get(receptacleType.getBarcode());
            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                if (sampleInstanceV2.getRootMercurySamples().iterator().next().getMetadataSource() !=
                        MercurySample.MetadataSource.BSP) {
                    iterator.remove();
                    removePositions.add(receptacleType.getPosition());
                }
            }
        }

        // Remove destination tubes that match source tube positions
        Iterator<ReceptacleType> destinationIterator = unmarshal.getPositionMap().getReceptacle().iterator();
        while (destinationIterator.hasNext()) {
            ReceptacleType receptacleType = destinationIterator.next();
            if (removePositions.contains(receptacleType.getPosition())) {
                destinationIterator.remove();
            }
        }

        // Forward to BSP
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateTransferEvent().add(unmarshal);
        samplesDaughterPlateHandler.postToBsp(bettaLIMSMessage, SamplesDaughterPlateHandler.BSP_TRANSFER_REST_URL);
    }
}
