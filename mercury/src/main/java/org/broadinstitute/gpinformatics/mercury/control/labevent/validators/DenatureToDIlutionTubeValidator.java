package org.broadinstitute.gpinformatics.mercury.control.labevent.validators;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class DenatureToDilutionTubeValidator extends AbstractEventValidator {

    public static final String FCT_METADATA_NAME = "FCT";

    @Override
    public void validateEvent(LabEvent targetEvent, StationEventType stationEvent) {

        PlateCherryPickEvent castEvent = OrmUtil.proxySafeCast(stationEvent, PlateCherryPickEvent.class);

        Map<String, List<MetadataType>> mapReceptacleToMetadata = new HashMap<>();

        for (PositionMapType targetPosition : castEvent.getPositionMap()) {
            for (ReceptacleType targetReceptacle : targetPosition.getReceptacle()) {
                for (MetadataType metadataType : targetReceptacle.getMetadata()) {
                    if (!mapReceptacleToMetadata.containsKey(targetReceptacle.getBarcode())) {
                        mapReceptacleToMetadata.put(targetReceptacle.getBarcode(), new ArrayList<MetadataType>());
                    }

                    mapReceptacleToMetadata.get(targetReceptacle.getBarcode()).add(metadataType);
                }
            }
        }

        for (CherryPickTransfer transfer : targetEvent.getCherryPickTransfers()) {
            LabVessel denatureTube =
                    transfer.getSourceVesselContainer().getVesselAtPosition(transfer.getSourcePosition());
            LabVessel dilutionTube =
                    transfer.getTargetVesselContainer().getVesselAtPosition(transfer.getTargetPosition());

            Collection<LabBatch> fctBatches = denatureTube.getLabBatchesOfType(LabBatch.LabBatchType.FCT);

            String fctTicket = "";
            for (MetadataType dilutionMetadata : mapReceptacleToMetadata.get(dilutionTube.getLabel())) {
                if (FCT_METADATA_NAME.equals(dilutionMetadata.getName())) {
                    fctTicket = dilutionMetadata.getValue();
                    break;
                }
            }

            boolean foundTicket = false;
            for (LabBatch denatureFctTicket : fctBatches) {
                if (fctTicket.equals(denatureFctTicket.getBusinessKey())) {
                    foundTicket = true;
                    for (LabBatchStartingVessel fctVesselAssociation : denatureFctTicket.getLabBatchStartingVessels()) {
                        if (denatureTube.equals(fctVesselAssociation.getLabVessel())) {

                            if(fctVesselAssociation.getDilutionVessel() == null) {
                                fctVesselAssociation.setDilutionVessel(dilutionTube);
                            } else if(!fctVesselAssociation.getDilutionVessel().equals(dilutionTube)) {
                                throw new ResourceException("This FCT is associated with a different dilution tube " +
                                                            "for the given Denature", Response.Status.BAD_REQUEST);
                            }
                        }
                    }
                }
            }

            if(!foundTicket) {
                throw new ResourceException("The denature tube " + denatureTube.getLabel() +
                                            " is not associated with the given FCT.", Response.Status.NOT_FOUND);
            }
        }
    }
}
