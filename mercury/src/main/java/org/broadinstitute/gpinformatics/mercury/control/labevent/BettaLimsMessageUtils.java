package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods used by boundary and control EJBs (EJBs can't have static methods).
 */
public class BettaLimsMessageUtils {

    /**
     * Based on LabEventType, get tube barcodes for validation or routing
     *
     * @param plateCherryPickEvent from deck
     *
     * @return tube barcodes
     */
    public static Set<String> getBarcodesForCherryPick(PlateCherryPickEvent plateCherryPickEvent) {
        Set<String> barcodes = new HashSet<>();
        LabEventType.PlasticToValidate plasticToValidate =
                LabEventType.getByName(plateCherryPickEvent.getEventType()).getPlasticToValidate();
        if (plasticToValidate == LabEventType.PlasticToValidate.SOURCE ||
                plasticToValidate == LabEventType.PlasticToValidate.BOTH) {
            if (plateCherryPickEvent.getSourcePositionMap().isEmpty()) {
                for (PlateType plateType : plateCherryPickEvent.getSourcePlate()) {
                    barcodes.add(plateType.getBarcode());
                }
            } else {
                for (PositionMapType positionMapType : plateCherryPickEvent.getSourcePositionMap()) {
                    for (ReceptacleType receptacle : positionMapType.getReceptacle()) {
                        barcodes.add(receptacle.getBarcode());
                    }
                }
            }
        }
        if (plasticToValidate == LabEventType.PlasticToValidate.TARGET ||
                plasticToValidate == LabEventType.PlasticToValidate.BOTH) {
            if (plateCherryPickEvent.getPositionMap().isEmpty()) {
                for (PlateType plateType : plateCherryPickEvent.getPlate()) {
                    barcodes.add(plateType.getBarcode());
                }
            } else {
                for (PositionMapType positionMapType : plateCherryPickEvent.getPositionMap()) {
                    for (ReceptacleType receptacle : positionMapType.getReceptacle()) {
                        barcodes.add(receptacle.getBarcode());
                    }
                }
            }
        }
        return barcodes;
    }

    /**
     * Based on LabEventType, get plate barcodes for validation or routing
     *
     * @param plateEventType from deck
     *
     * @return plate barcodes
     */
    public static Set<String> getBarcodesForPlateEvent(PlateEventType plateEventType) {
        Set<String> barcodes = new HashSet<>();
        switch (LabEventType.getByName(plateEventType.getEventType()).getPlasticToValidate()) {
        case SOURCE:
            if (plateEventType.getPositionMap() == null) {
                barcodes.add(plateEventType.getPlate().getBarcode());
            } else {
                for (ReceptacleType position : plateEventType.getPositionMap().getReceptacle()) {
                    barcodes.add(position.getBarcode());
                }
            }
            break;
        }
        return barcodes;
    }

    public static Map<String, Set<StationEventType>> getBarcodesForPlateEvent(List<PlateEventType> plateEventTypes) {
        Collections.sort(plateEventTypes, new Comparator<PlateEventType>() {
            @Override
            public int compare(PlateEventType event1, PlateEventType event2) {
                return event1.getDisambiguator().compareTo(event2.getDisambiguator());
            }
        });
        Map<String, Set<StationEventType>> barcodeToPlateEvents = new HashMap<>();
        for (PlateEventType plateEventType: plateEventTypes) {
            switch (LabEventType.getByName(plateEventType.getEventType()).getPlasticToValidate()) {
            case SOURCE:
                String barcode = plateEventType.getPlate().getBarcode();
                Set<StationEventType> uniqueEventTypes;
                if (barcodeToPlateEvents.containsKey(barcode)) {
                    uniqueEventTypes = barcodeToPlateEvents.get(barcode);
                } else {
                    uniqueEventTypes = new LinkedHashSet<>();
                    barcodeToPlateEvents.put(barcode, uniqueEventTypes);
                }
                uniqueEventTypes.add(plateEventType);
                break;
            }
        }

        return barcodeToPlateEvents;
    }

    /**
     * Based on LabEventType, get plate barcodes for validation or routing
     *
     * @param plateTransferEventType from deck
     *
     * @return plate barcodes
     */
    public static Set<String> getBarcodesForPlateTransfer(PlateTransferEventType plateTransferEventType) {
        Set<String> barcodes = new HashSet<>();
        switch (LabEventType.getByName(plateTransferEventType.getEventType()).getPlasticToValidate()) {
        case SOURCE:
            if (plateTransferEventType.getSourcePositionMap() == null) {
                barcodes.add(plateTransferEventType.getSourcePlate().getBarcode());
            } else {
                for (ReceptacleType receptacleType : plateTransferEventType.getSourcePositionMap()
                        .getReceptacle()) {
                    barcodes.add(receptacleType.getBarcode());
                }
            }
            break;
        case TARGET:

            if (plateTransferEventType.getPositionMap() == null) {
                barcodes.add(plateTransferEventType.getPlate().getBarcode());
            } else {
                for (ReceptacleType targetReceptacle : plateTransferEventType.getPositionMap()
                        .getReceptacle()) {
                    barcodes.add(targetReceptacle.getBarcode());
                }
            }
            break;
        }
        return barcodes;
    }

    /**
     * Based on LabEventType, get tube or plate barcode for validation or routing
     *
     * @param receptaclePlateTransferEvent from deck
     *
     * @return plate or tube barcode
     */
    public static Set<String> getBarcodesForReceptaclePlateTransfer(
            ReceptaclePlateTransferEvent receptaclePlateTransferEvent) {
        Set<String> barcodes = new HashSet<>();
        switch (LabEventType.getByName(receptaclePlateTransferEvent.getEventType()).getPlasticToValidate()) {
        case SOURCE:
            barcodes.add(receptaclePlateTransferEvent.getSourceReceptacle().getBarcode());
            break;

        case TARGET:
            barcodes.add(receptaclePlateTransferEvent.getDestinationPlate().getBarcode());
            break;
        }
        return barcodes;
    }

    /**
     * Based on LabEventType, get tube barcodes for validation or routing
     *
     * @param receptacleEventType from deck
     *
     * @return tube barcodes
     */
    public static Set<String> getBarcodesForReceptacleEvent(ReceptacleEventType receptacleEventType) {
        Set<String> barcodes = new HashSet<>();
        switch (LabEventType.getByName(receptacleEventType.getEventType()).getPlasticToValidate()) {
        case SOURCE:
            barcodes.add(receptacleEventType.getReceptacle().getBarcode());
            break;
        }
        return barcodes;
    }

    /**
     * Based on LabEventType, get tube barcodes for validation or routing
     *
     * @param receptacleTransferEventType from deck
     *
     * @return tube barcodes
     */
    public static Set<String> getBarcodesForReceptacleTransferEvent(ReceptacleTransferEventType receptacleTransferEventType) {
        Set<String> barcodes = new HashSet<>();
        LabEventType.PlasticToValidate plasticToValidate =
                LabEventType.getByName(receptacleTransferEventType.getEventType()).getPlasticToValidate();
        if (plasticToValidate == LabEventType.PlasticToValidate.SOURCE ||
                plasticToValidate == LabEventType.PlasticToValidate.BOTH) {
            barcodes.add(receptacleTransferEventType.getSourceReceptacle().getBarcode());
        }
        if (plasticToValidate == LabEventType.PlasticToValidate.TARGET ||
                plasticToValidate == LabEventType.PlasticToValidate.BOTH) {
            barcodes.add(receptacleTransferEventType.getReceptacle().getBarcode());
        }
        return barcodes;
    }

}
