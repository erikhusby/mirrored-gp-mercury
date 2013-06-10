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
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

/**
 * This class has methods for transferring stuff from one Vessel to Another One.
 */
@Stateful
@RequestScoped
public class VesselTransferBean {
    @Inject
    LabEventFactory labEventFactory;
    @Inject
    LabEventDao labEventDao;
    @Inject
    RackOfTubesDao rackOfTubesDao;
    @Inject
    LabVesselDao labVesselDao;

    /**
     * Create a vessel transfer for denature to reagent kit.
     *
     * @param denatureRackBarcode The barcode of denature rack. If null a fake one will be created.
     * @param denatureBarcodeMap  A Map of denature tube barcodes and location on the denature rack.
     * @param reagentKitBarcode   The barcode of reagent kit that will receive the transfer.
     * @param username            Name of the user performing the action.
     * @param stationName         Where the transfer occurred (such as UI)
     *
     * @return The newly created event.
     */
    public LabEvent denatureToReagentKitTransfer(@Nullable String denatureRackBarcode,
                                                 Map<String, VesselPosition> denatureBarcodeMap,
                                                 String reagentKitBarcode, String username,
                                                 String stationName) {

        final String eventType = LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER.getName();


        PlateCherryPickEvent transferEvent = new PlateCherryPickEvent();
        transferEvent.setEventType(eventType);
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        try {
            transferEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar));
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        transferEvent.setDisambiguator(1L);
        transferEvent.setOperator(username);
        transferEvent.setStation(stationName);

        Map<String, TubeFormation> mapBarcodeToSourceTubeFormation = new HashMap<>();
        Map<String, RackOfTubes> mapBarcodeToSourceRackOfTubes = new HashMap<>();
        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube = new HashMap<>();
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<>();
        for (Map.Entry<String, VesselPosition> item : denatureBarcodeMap.entrySet()) {
            final String tubeBarcode = item.getKey();
            final TwoDBarcodedTube sourceTube = new TwoDBarcodedTube(tubeBarcode);
            final VesselPosition vesselPosition = item.getValue();
            mapPositionToTube.put(vesselPosition, sourceTube);
            mapBarcodeToSourceTube.put(tubeBarcode, sourceTube);
        }

        RackOfTubes denatureRack = null;
        if (denatureRackBarcode != null) {
            denatureRack = rackOfTubesDao.findByBarcode(denatureRackBarcode);
        } else {
            denatureRackBarcode= "DenatureRack" + reagentKitBarcode;
        }

        for (TubeFormation tubeFormation : denatureRack.getTubeFormations()) {
            mapBarcodeToSourceTubeFormation.put(denatureRackBarcode, tubeFormation);
        }

        mapBarcodeToSourceRackOfTubes.put(denatureRackBarcode, denatureRack);

        LabEvent labEvent =
                labEventFactory.buildCherryPickRackToReagentKitDbFree(transferEvent, mapBarcodeToSourceTubeFormation,
                        mapBarcodeToSourceRackOfTubes, mapBarcodeToSourceTube);
        labEventDao.persist(labEvent);
        return labEvent;
    }
}
