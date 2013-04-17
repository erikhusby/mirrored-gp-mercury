package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods used by boundary and control EJBs (EJBs can't have static methods).
 */
public class BettalimsMessageUtils {

    /**
     * Based on LabEventType, get tube barcodes for validation or routing
     * @param plateCherryPickEvent    from deck
     * @return tube barcodes
     */
    public static Set<String> getBarcodesForCherryPick(PlateCherryPickEvent plateCherryPickEvent) {
        Set<String> barcodes = new HashSet<String>();
        switch (LabEventType.getByName(plateCherryPickEvent.getEventType()).getPlasticToValidate()) {
            case SOURCE:
                for (PositionMapType positionMapType : plateCherryPickEvent.getSourcePositionMap()) {
                    for (ReceptacleType receptacle : positionMapType.getReceptacle()) {
                        barcodes.add(receptacle.getBarcode());
                    }
                }
                break;
        }
        return barcodes;
    }

    /**
     * Based on LabEventType, get plate barcodes for validation or routing
     * @param plateEventType from deck
     * @return plate barcodes
     */
    public static Set<String> getBarcodesForPlateEvent(PlateEventType plateEventType) {
        Set<String> barcodes = new HashSet<String>();
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

    /**
     * Based on LabEventType, get plate barcodes for validation or routing
     * @param plateTransferEventType from deck
     * @return plate barcodes
     */
    public static Set<String> getBarcodesForPlateTransfer(PlateTransferEventType plateTransferEventType) {
        Set<String> barcodes = new HashSet<String>();
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
     * @param receptaclePlateTransferEvent from deck
     * @return plate or tube barcode
     */
    public static Set<String> getBarcodesForReceptaclePlateTransfer(ReceptaclePlateTransferEvent receptaclePlateTransferEvent) {
        Set<String> barcodes = new HashSet<String>();
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
     * @param receptacleEventType from deck
     * @return tube barcodes
     */
    public static Set<String> getBarcodesForReceptacleEvent(ReceptacleEventType receptacleEventType) {
        Set<String> barcodes = new HashSet<String>();
        switch (LabEventType.getByName(receptacleEventType.getEventType()).getPlasticToValidate()) {
            case SOURCE:
                barcodes.add(receptacleEventType.getReceptacle().getBarcode());
                break;
        }
        return barcodes;
    }

}
