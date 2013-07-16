package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.apache.commons.lang3.StringUtils;
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
 * The specific handler for the
 * {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.DENATURE_TO_DILUTION_TRANSFER} message.
 * <p/>
 * This handler will validate the relation ship between an FCT ticket specified for a denature tube and the
 * relationship both of these entities will have to the targetted dilution tube.
 */
public class DenatureToDilutionTubeHandler extends AbstractEventHandler {

    public static final String FCT_METADATA_NAME = "FCT";

    @Override
    public void handleEvent(LabEvent targetEvent, StationEventType stationEvent) {

        PlateCherryPickEvent castEvent = OrmUtil.proxySafeCast(stationEvent, PlateCherryPickEvent.class);

        Map<String, List<MetadataType>> mapReceptacleToMetadata = new HashMap<>();

        // Collect all meta data for the target Receptacles.  It is here that the FCT Ticket will be referenced
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

        //Loop through the cherry picks.  it is here that we will find the specific denature-> flowcell relationships
        for (CherryPickTransfer transfer : targetEvent.getCherryPickTransfers()) {

            LabVessel denatureTube =
                    transfer.getSourceVesselContainer().getVesselAtPosition(transfer.getSourcePosition());
            LabVessel dilutionTube =
                    transfer.getTargetVesselContainer().getVesselAtPosition(transfer.getTargetPosition());

            Collection<LabBatch> fctBatches = denatureTube.getAllLabBatches(LabBatch.LabBatchType.FCT);

            //Get FCT Ticket for which the Dilution tube is targeted
            String fctTicket = "";
            for (MetadataType dilutionMetadata : mapReceptacleToMetadata.get(dilutionTube.getLabel())) {
                if (FCT_METADATA_NAME.equals(dilutionMetadata.getName())) {
                    fctTicket = dilutionMetadata.getValue();
                    break;
                }
            }

            //Error out if there is no FCT ticket for the dilution tube
            if (StringUtils.isBlank(fctTicket)) {
                throw new ResourceException("The message does not have an FCT Ticket associated with dilution tube " +
                                            dilutionTube.getLabel(),
                        Response.Status.BAD_REQUEST);
            }

            // Check if the dilution tube is associated with another FCT Ticket
            for (LabBatchStartingVessel dilutionFctAssociations : dilutionTube.getDilutionReferences()) {
                if (!fctTicket.equals(dilutionFctAssociations.getLabBatch().getBusinessKey())) {
                    throw new ResourceException("The dilution tube " + dilutionTube.getLabel() +
                                                " is already associated with another FCT Ticket: " +
                                                dilutionFctAssociations.getLabBatch().getBusinessKey(),
                            Response.Status.BAD_REQUEST);
                } else {
                    return;
                }
            }


            /*
             * Check the FCT Tickets of the denature:
             * -- If we find the Batch corresponding to the fct ticket and there is no dilution tube currently
             *      assigned, assign the cherry pick dilution tube to the batch association
             * -- If we find the Batch corresponding to the fct ticket and there is an existing dilution tube currently
             *     assigned, throw an exception
             * -- If we do not find any fct tickets associated with the denature, throw an exception that the
             *     association was not previously done
             */
            boolean foundTicket = false;
            for (LabBatch denatureFctTicket : fctBatches) {
                if (fctTicket.equals(denatureFctTicket.getBusinessKey())) {
                    foundTicket = true;
                    for (LabBatchStartingVessel fctVesselAssociation : denatureFctTicket.getLabBatchStartingVessels()) {
                        if (denatureTube.equals(fctVesselAssociation.getLabVessel())) {

                            if (fctVesselAssociation.getDilutionVessel() == null) {
                                fctVesselAssociation.setDilutionVessel(dilutionTube);
                            } else if (!fctVesselAssociation.getDilutionVessel().equals(dilutionTube)) {
                                throw new ResourceException("This FCT is associated with a different dilution tube " +
                                                            " for the given Denature", Response.Status.BAD_REQUEST);
                            }
                        }
                    }
                }
            }

            if (!foundTicket) {
                throw new ResourceException("The denature tube " + denatureTube.getLabel() +
                                            " is not associated with the given FCT.", Response.Status.NOT_FOUND);
            }
        }
    }
}
