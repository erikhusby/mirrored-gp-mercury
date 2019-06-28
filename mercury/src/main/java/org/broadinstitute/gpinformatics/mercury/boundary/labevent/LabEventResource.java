package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventRefDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A JAX-RS web service to return Transfers etc., by various criteria.
 */
@Path("/labevent")
@Stateful
@RequestScoped
public class LabEventResource {

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BSPUserList bspUserList;

    /**
     * Default implementation of the LabEventRefDataFetcher that gets real data from BSP.
     */
    private class DefaultLabEventRefDataFetcher implements LabEventRefDataFetcher {

        @Override
        public BspUser getOperator(String userId) {
            return bspUserList.getByUsername(userId);
        }

        @Override
        public BspUser getOperator(Long bspUserId) {
            return bspUserList.getById(bspUserId);
        }

        @Override
        public LabBatch getLabBatch(String labBatchName) {
            return null;
        }
    }


    @Path("/batch/{batchId}")
    @GET
    @Produces({MediaType.APPLICATION_XML})
    /**
     * Find all LabEvents for the specified batch ID.
     */
    public LabEventResponseBean labEventsByBatchId(@PathParam("batchId") String batchId) {
        LabBatch labBatch = labBatchDao.findByName(batchId);
        if (labBatch == null) {
            throw new RuntimeException("Batch not found: " + batchId);
        }
        List<LabEvent> labEventsByTime = new ArrayList<>(labBatch.getLabEvents());
        Collections.sort(labEventsByTime, LabEvent.BY_EVENT_DATE);
        List<LabEventBean> labEventBeans =
                buildLabEventBeans(labEventsByTime, new DefaultLabEventRefDataFetcher());
        return new LabEventResponseBean(labEventBeans);
    }


    /**
     * Return any in-place LabEvents that correspond to reagent additions for any of the specified plate barcodes.
     * This does <b>not</b> return reagents that are part of transfer events.  This is added specifically to support
     * batchless Pico where the Pico reagent is not part of a transfer and events are not grouped by a batch.
     */
    @Path("/inPlaceReagentEvents")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public LabEventResponseBean inPlaceReagentEventsByPlateBarcodes(
            @QueryParam("plateBarcodes") @Nonnull List<String> plateBarcodes) {

        Collection<LabVessel> labVessels = labVesselDao.findByBarcodes(plateBarcodes).values();

        List<LabEvent> labEvents = new ArrayList<>();

        for (LabVessel labVessel : labVessels) {
            if (labVessel != null) {
                for (LabEvent labEvent : labVessel.getInPlaceEventsWithContainers()) {
                    if (!labEvent.getReagents().isEmpty()) {
                        labEvents.add(labEvent);
                    }
                }
            }
        }

        Collections.sort(labEvents, LabEvent.BY_EVENT_DATE);

        List<LabEventBean> labEventBeans =
                buildLabEventBeans(labEvents, new DefaultLabEventRefDataFetcher());

        return new LabEventResponseBean(labEventBeans);
    }


    @Path("/transfersToFirstAncestorRack")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    /**
     * Find all LabEvents for transfers from the specified plate barcodes back to ancestor Matrix racks.
     */
    public LabEventResponseBean transfersToFirstAncestorRack(
            @QueryParam("plateBarcodes") @Nonnull List<String> plateBarcodes) {
        Collection<LabVessel> labVessels = labVesselDao.findByBarcodes(plateBarcodes).values();

        // This is a Set as LabEvents should be unique in the results.
        Set<LabEvent> labEvents = new HashSet<>();

        for (LabVessel labVessel : labVessels) {
            // This is not checking that all queried barcodes were accounted for in the results,
            // that is up to the caller.
            if (labVessel == null || labVessel.getContainerRole() == null) {
                continue;
            }

            VesselContainer<?> vesselContainer = labVessel.getContainerRole();

            List<List<LabEvent>> resultsForThisVessel =
                    vesselContainer.shortestPathsToVesselsSatisfyingPredicate(VesselContainer.IS_LAB_VESSEL_A_RACK);
            // Flatten the result to get an Iterable of LabEvents, keep unique copies by putting the LabEvents
            // in a Set.
            labEvents.addAll(Sets.newHashSet(Iterables.concat(resultsForThisVessel)));
        }

        List<LabEvent> sortedLabEvents = new ArrayList<>(labEvents);
        Collections.sort(sortedLabEvents, LabEvent.BY_EVENT_DATE);

        List<LabEventBean> labEventBeans =
                buildLabEventBeans(sortedLabEvents, new DefaultLabEventRefDataFetcher());

        return new LabEventResponseBean(labEventBeans);
    }


    public List<LabEventBean> buildLabEventBeans(List<LabEvent> labEvents,
                                                 LabEventRefDataFetcher dataFetcherHelper) {
        List<LabEventBean> labEventBeans = new ArrayList<>();

        for (LabEvent labEvent : labEvents) {
            BspUser operator = dataFetcherHelper.getOperator(labEvent.getEventOperator());
            LabEventBean labEventBean = new LabEventBean(
                    labEvent.getLabEventType().getName(),
                    labEvent.getEventLocation(),
                    operator == null ? "Unknown user: " + labEvent.getEventOperator() : operator.getUsername(),
                    labEvent.getEventDate());
            for (Reagent reagent : labEvent.getReagents()) {
                labEventBean.getReagents().add(
                        new ReagentBean(reagent.getName(), reagent.getLot(), reagent.getExpiration()));
            }

            for(LabEventMetadata labEventMetadata : labEvent.getLabEventMetadatas()) {
                labEventBean.getMetadatas().add(new MetadataBean(
                        labEventMetadata.getLabEventMetadataType().getDisplayName(),
                        labEventMetadata.getValue()));
            }

            LabBatch labBatch = labEvent.getLabBatch();
            labEventBean.setBatchId(labBatch != null ? labBatch.getBatchName() : null);

            Map<String, String> mapTubeFormationLabelToRackLabel = new HashMap<>();
            // todo jmt rationalize these?  Each side can be a vessel, or a vessel + section, or a vessel + position
            for (CherryPickTransfer cherryPickTransfer : labEvent.getCherryPickTransfers()) {
                TransferBean transferBean = new TransferBean();
                transferBean.setType("CherryPickTransfer");
                String sourceBarcode = getLabel(cherryPickTransfer.getAncillarySourceVessel(),
                        cherryPickTransfer.getSourceVesselContainer());
                transferBean.setSourceBarcode(sourceBarcode);
                mapTubeFormationLabelToRackLabel.put(
                        cherryPickTransfer.getSourceVesselContainer().getEmbedder().getLabel(),
                        sourceBarcode);
                transferBean.setSourcePosition(cherryPickTransfer.getSourcePosition().name());

                String targetBarcode = getLabel(cherryPickTransfer.getAncillaryTargetVessel(),
                        cherryPickTransfer.getTargetVesselContainer());
                transferBean.setTargetBarcode(targetBarcode);
                mapTubeFormationLabelToRackLabel.put(
                        cherryPickTransfer.getTargetVesselContainer().getEmbedder().getLabel(),
                        targetBarcode);
                transferBean.setTargetPosition(cherryPickTransfer.getTargetPosition().name());
                labEventBean.getTransfers().add(transferBean);
            }
            for (SectionTransfer sectionTransfer : labEvent.getSectionTransfers()) {
                TransferBean transferBean = new TransferBean();
                transferBean.setType("SectionTransfer");
                String sourceBarcode = getLabel(sectionTransfer.getAncillarySourceVessel(),
                        sectionTransfer.getSourceVesselContainer());
                transferBean.setSourceBarcode(sourceBarcode);
                mapTubeFormationLabelToRackLabel.put(
                        sectionTransfer.getSourceVesselContainer().getEmbedder().getLabel(),
                        sourceBarcode);
                transferBean.setSourceSection(sectionTransfer.getSourceSection().getSectionName());

                String targetBarcode = getLabel(sectionTransfer.getAncillaryTargetVessel(),
                        sectionTransfer.getTargetVesselContainer());
                transferBean.setTargetBarcode(targetBarcode);
                mapTubeFormationLabelToRackLabel.put(
                        sectionTransfer.getTargetVesselContainer().getEmbedder().getLabel(),
                        targetBarcode);
                transferBean.setTargetSection(sectionTransfer.getTargetSection().getSectionName());
                labEventBean.getTransfers().add(transferBean);
            }
            for (VesselToSectionTransfer vesselToSectionTransfer : labEvent.getVesselToSectionTransfers()) {
                TransferBean transferBean = new TransferBean();
                transferBean.setType("VesselToSectionTransfer");
                transferBean.setSourceBarcode(vesselToSectionTransfer.getSourceVessel().getLabel());
                String targetBarcode = getLabel(vesselToSectionTransfer.getAncillaryTargetVessel(),
                        vesselToSectionTransfer.getTargetVesselContainer());
                transferBean.setTargetBarcode(targetBarcode);
                mapTubeFormationLabelToRackLabel.put(
                        vesselToSectionTransfer.getTargetVesselContainer().getEmbedder().getLabel(),
                        targetBarcode);
                transferBean.setTargetSection(vesselToSectionTransfer.getTargetSection().getSectionName());
                labEventBean.getTransfers().add(transferBean);
            }
            for (VesselToVesselTransfer vesselToVesselTransfer : labEvent.getVesselToVesselTransfers()) {
                TransferBean transferBean = new TransferBean();
                transferBean.setType("VesselToVesselTransfer");
                transferBean.setSourceBarcode(vesselToVesselTransfer.getSourceVessel().getLabel());
                transferBean.setTargetBarcode(vesselToVesselTransfer.getTargetVessel().getLabel());
                labEventBean.getTransfers().add(transferBean);
            }

            for (LabVessel sourceLabVessel : labEvent.getSourceLabVessels()) {
                labEventBean.getSources().add(buildLabVesselBean(sourceLabVessel, mapTubeFormationLabelToRackLabel));
            }

            for (LabVessel targetLabVessel : labEvent.getTargetLabVessels()) {
                labEventBean.getTargets().add(buildLabVesselBean(targetLabVessel, mapTubeFormationLabelToRackLabel));
            }

            labEventBeans.add(labEventBean);
        }
        return labEventBeans;
    }

    // todo jmt make this work for sample starters
    static class StarterCriteria extends TransferTraverserCriteria {
        private LabVessel starter;

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {

            // Ancestry only!
            if( context.getTraversalDirection() != TraversalDirection.Ancestors ) {
                throw new IllegalStateException("LabEventResource.StarterCriteria supports ancestry traversal only.");
            }

            LabVessel contextVessel = context.getContextVessel();

            if (contextVessel != null) {
                if (contextVessel.getTransfersTo().isEmpty()) {
                    for (LabBatch labBatch : contextVessel.getLabBatches()) {
                        if (labBatch.getStartingBatchLabVessels().contains(contextVessel)) {
                            starter = contextVessel;
                            return TraversalControl.StopTraversing;
                        }
                    }
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public LabVessel getStarter() {
            return starter;
        }
    }

    private LabVesselBean buildLabVesselBean(LabVessel labVesselEntity,
            Map<String, String> mapTubeFormationLabelToRackLabel) {
        // todo jmt need to hide on-the-fly creation of plate wells
        String type = labVesselEntity.getType().name();
        String label = null;
        if (labVesselEntity.getType() == LabVessel.ContainerType.STATIC_PLATE) {
            type = OrmUtil.proxySafeCast(labVesselEntity, StaticPlate.class).getPlateType().getAutomationName();
        } else if (labVesselEntity.getType() == LabVessel.ContainerType.RACK_OF_TUBES) {
            type = OrmUtil.proxySafeCast(labVesselEntity, RackOfTubes.class).getRackType().getDisplayName();
        } else if (labVesselEntity.getType() == LabVessel.ContainerType.TUBE_FORMATION) {
            type = OrmUtil.proxySafeCast(labVesselEntity, TubeFormation.class).getRackType().getDisplayName();
            label = mapTubeFormationLabelToRackLabel.get(labVesselEntity.getLabel());
        }
        if (label == null) {
            label = labVesselEntity.getLabel();
        }
        LabVesselBean labVesselBean = new LabVesselBean(label, type);
        VesselContainer vesselContainer = labVesselEntity.getContainerRole();
        if (vesselContainer != null) {
            if (OrmUtil.proxySafeIsInstance(labVesselEntity, StaticPlate.class)) {
                StaticPlate staticPlate = OrmUtil.proxySafeCast(labVesselEntity, StaticPlate.class);
                Iterator<String> positionNames = staticPlate.getPlateType().getVesselGeometry().getPositionNames();
                while (positionNames.hasNext()) {
                    String positionName = positionNames.next();
                    labVesselBean.getLabVesselPositionBeans().add(new LabVesselPositionBean(
                            positionName, new LabVesselBean(null, LabVessel.ContainerType.PLATE_WELL.name())));
                }
            } else if (OrmUtil.proxySafeIsInstance(labVesselEntity, TubeFormation.class)) {
                TubeFormation tubeFormation = OrmUtil.proxySafeCast(labVesselEntity, TubeFormation.class);
                Iterator<String> positionNames = tubeFormation.getRackType().getVesselGeometry().getPositionNames();
                while (positionNames.hasNext()) {
                    String positionName = positionNames.next();
                    LabVessel labVessel = vesselContainer.getVesselAtPosition(VesselPosition.getByName(positionName));
                    if (labVessel != null) {
                        labVesselBean.getLabVesselPositionBeans().add(new LabVesselPositionBean(
                                positionName, buildLabVesselBean(labVessel, mapTubeFormationLabelToRackLabel)));
                    }
                }
            } else {
                @SuppressWarnings("unchecked")
                Set<Map.Entry<VesselPosition, LabVessel>> entrySet =
                        vesselContainer.getMapPositionToVessel().entrySet();
                for (Map.Entry<VesselPosition, LabVessel> positionToLabVessel : entrySet) {
                    labVesselBean.getLabVesselPositionBeans().add(new LabVesselPositionBean(
                            positionToLabVessel.getKey().name(), buildLabVesselBean(positionToLabVessel.getValue(),
                            mapTubeFormationLabelToRackLabel)));
                }
            }
            for (LabVesselPositionBean labVesselPositionBean : labVesselBean.getLabVesselPositionBeans()) {
                StarterCriteria starterCriteria = new StarterCriteria();
                vesselContainer.evaluateCriteria(VesselPosition.getByName(labVesselPositionBean.getPosition()),
                        starterCriteria,
                        TransferTraverserCriteria.TraversalDirection.Ancestors, 0);
                if (starterCriteria.getStarter() != null) {
                    labVesselPositionBean.getLabVesselBean().setStarter(starterCriteria.getStarter().getLabel());
                }
            }
        }
        return labVesselBean;
    }

    /**
     * Returns label of vessel, including special handling for tube formations.
     */
    private String getLabel(LabVessel ancillaryVessel, VesselContainer<?> vesselContainer) {
        String label;
        if (ancillaryVessel == null) {
            if (vesselContainer.getEmbedder().getType() == LabVessel.ContainerType.TUBE_FORMATION) {
                // The "label" for a tube formation is the hash of the tube barcodes and their formations, so return the
                // label for the rack.
                label = OrmUtil.proxySafeCast(vesselContainer.getEmbedder(), TubeFormation.class).
                        getRacksOfTubes().iterator().next().getLabel();
            } else {
                label = vesselContainer.getEmbedder().getLabel();
            }
        } else {
            label = ancillaryVessel.getLabel();
        }
        return label;
    }

}
