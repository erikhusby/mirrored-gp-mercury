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

package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class has methods for transferring stuff from one Vessel to Another One.
 */
@Stateful
@RequestScoped
public class VesselTransferBean {
    @Inject
    LabEventFactory labEventFactory;

    public PlateCherryPickEvent denatureToReagentKitTransfer(String denatureRackBarcode,
                                                             List<String> denatureTubeBarcodes,
                                                             String reagentKitBarcode, String username,
                                                             String stationName) {

        final String eventType = LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER.getName();


        PlateCherryPickEvent event = new PlateCherryPickEvent();
        event.setEventType(eventType);
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        try {
            event.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar));
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        event.setDisambiguator(1L);
        event.setOperator(username);
        event.setStation(stationName);

        List<LabEventFactory.CherryPick> cherryPicks = new ArrayList<>();
        Map<String, VesselPosition> sourceBarcodes = new HashMap<>();
        for (String barcode : denatureTubeBarcodes) {
            if (denatureRackBarcode.isEmpty()) {
                denatureRackBarcode = "DenatureRack" + barcode;
            }
            sourceBarcodes.put(barcode, VesselPosition.valueOf(barcode));
            LabEventFactory.CherryPick cherryPick =
                    new LabEventFactory.CherryPick(barcode, VesselPosition.valueOf(barcode).name(), reagentKitBarcode,
                            MiSeqReagentKit.LOADING_WELL.name());
            cherryPicks.add(cherryPick);
        }

        labEventFactory.buildCherryPickRackToPlateDbFree(event,
                denatureRackBarcode, sourceBarcodes,
                reagentKitBarcode,
                MiSeqReagentKit.PlateType.MiSeqReagentKit.getDisplayName(), cherryPicks);

        return event;
    }
}
