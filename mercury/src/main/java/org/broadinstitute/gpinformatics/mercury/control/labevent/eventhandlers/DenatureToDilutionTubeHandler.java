package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.enterprise.context.Dependent;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The specific handler for the DENATURE_TO_DILUTION_TRANSFER message.
 * <p/>
 * This handler will validate the relationship between an FLOWCELL_TICKET ticket specified for a denature tube and the
 * relationship both of these entities will have to the targetted dilution tube.
 */
@Dependent
public class DenatureToDilutionTubeHandler extends AbstractEventHandler {

    public static final String FCT_METADATA_NAME = "FCT";

    public DenatureToDilutionTubeHandler() {
    }

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
            LabVessel dilutionTube;
            if (transfer.getTargetVesselContainer().getEmbedder().getType() == LabVessel.ContainerType.STRIP_TUBE) {
                dilutionTube = transfer.getTargetVesselContainer().getEmbedder();
            } else {
                dilutionTube = transfer.getTargetVesselContainer().getVesselAtPosition(transfer.getTargetPosition());
            }

            List<LabBatch> fctBatches = new ArrayList<>();
            LabVesselSearchDefinition.VesselBatchTraverserCriteria
                    downstreamBatchFinder = new LabVesselSearchDefinition.VesselBatchTraverserCriteria();
            denatureTube.evaluateCriteria(downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Ancestors);

            for (LabBatch labBatch: downstreamBatchFinder.getLabBatches()) {
                if(labBatch.getLabBatchType() == LabBatch.LabBatchType.FCT) {
                    fctBatches.add(labBatch);
                }
            }

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
                }
            }


            /*
             * Check the FCT Tickets of the denature:
             * -- If we find the Batch corresponding to the fct ticket and there is no dilution tube currently
             *      assigned, assign the cherry pick dilution tube to the batch association
             * -- If we find the Batch corresponding to the fct ticket and there is an existing dilution tube currently
             *     assigned, throw an exception
             * -- If we find the batch corresponding to the fct ticket but we don't find the starting vessel
             *     in the lab batch then check each of the denature tube's ancestors
             * -- If we do not find any fct tickets associated with the denature, throw an exception that the
             *     association was not previously done
             */
            boolean foundTicket = false;
            for (LabBatch fctLabBatch : fctBatches) {
                if (fctTicket.equals(fctLabBatch.getBusinessKey())) {
                    foundTicket = true;
                    LabBatchStartingVessel startingVessel = findStartingVessel(fctLabBatch, denatureTube, transfer);
                    if (startingVessel != null) {
                        if (startingVessel.getDilutionVessel() == null) {
                            startingVessel.setDilutionVessel(dilutionTube);
                        } else if (!startingVessel.getDilutionVessel().equals(dilutionTube)) {
                            throw new ResourceException(fctLabBatch.getBatchName() + " starting tube " +
                                    startingVessel.getLabVessel().getLabel() +
                                    " was previously associated with dilution tube " +
                                    startingVessel.getDilutionVessel().getLabel() +
                                    " and cannot be reassociated with " + dilutionTube.getLabel(),
                                    Response.Status.BAD_REQUEST);
                        }
                    } else {
                        throw new ResourceException("Cannot find the " + fctLabBatch.getBatchName() +
                                " starting tube ancestor of " + denatureTube.getLabel(), Response.Status.BAD_REQUEST);
                    }
                }
            }

            if (!foundTicket) {
                throw new ResourceException("The denature tube " + denatureTube.getLabel() +
                                            " is not associated with the given FCT.", Response.Status.NOT_FOUND);
            }
        }
    }

    /**
     * Returns a labBatchStartingVessel for this FCT batch by looking at the loading tube and its ancestors,
     * or returns null if none found.
     */
    private LabBatchStartingVessel findStartingVessel(LabBatch fctLabBatch, LabVessel loadingTube,
            CherryPickTransfer transfer) {

        boolean mustMatchLane = fctLabBatch.getFlowcellType() != null &&
                (fctLabBatch.getFlowcellType() == IlluminaFlowcell.FlowcellType.NovaSeqFlowcell ||
                        fctLabBatch.getFlowcellType() == IlluminaFlowcell.FlowcellType.NovaSeqS4Flowcell);

        for (LabBatchStartingVessel labBatchStartingVessel : fctLabBatch.getLabBatchStartingVessels()) {
            boolean tubeMatches = loadingTube.equals(labBatchStartingVessel.getLabVessel());
            boolean laneMatches = !mustMatchLane || (labBatchStartingVessel.getVesselPosition() != null &&
                    (transfer.getTargetPosition().name().charAt(0) - 'A' + 1) ==
                            Integer.parseInt(StringUtils.substringAfter(
                                    labBatchStartingVessel.getVesselPosition().name(), "LANE")));
            if (tubeMatches && laneMatches) {
                return labBatchStartingVessel;
            }
        }
        if (!loadingTube.getContainers().isEmpty()) {
            LabVessel tubeFormation = loadingTube.getContainers().iterator().next();
            List<LabVessel.VesselEvent> ancestors = tubeFormation.getContainerRole().getAncestors(loadingTube);
            if (ancestors != null && !ancestors.isEmpty()) {
                LabVessel.VesselEvent vesselEvent = ancestors.get(0);
                LabVessel ancestorTube = vesselEvent.getSourceLabVessel();
                // Recursively calls this method on the ancestor tube.
                return findStartingVessel(fctLabBatch, ancestorTube, transfer);
            }
        }
        return null;
    }
}
