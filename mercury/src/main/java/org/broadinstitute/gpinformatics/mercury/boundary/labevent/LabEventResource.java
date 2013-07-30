package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
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
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private LabBatchDAO labBatchDAO;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BSPUserList bspUserList;

    /**
     * Default implementation of the LabEventRefDataFetcher that gets real data from BSP.
     */
    private class DefaultLabEventRefDataFetcher implements LabEventFactory.LabEventRefDataFetcher {

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
    public LabEventResponseBean transfersByBatchId(@PathParam("batchId") String batchId) {
        LabBatch labBatch = labBatchDAO.findByName(batchId);
        if (labBatch == null) {
            throw new RuntimeException("Batch not found: " + batchId);
        }
        List<LabEvent> labEventsByTime = new ArrayList<>(labBatch.getLabEvents());
        Collections.sort(labEventsByTime, LabEvent.BY_EVENT_DATE);
        List<LabEventBean> labEventBeans =
                buildLabEventBeans(labEventsByTime, new DefaultLabEventRefDataFetcher());
        return new LabEventResponseBean(labEventBeans);
    }




    @Path("/transfersToFirstAncestorRack/{plateBarcodes}")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    /**
     * Find all LabEvents for transfers from the specified plate barcodes back to ancestor Matrix racks.
     */
    public LabEventResponseBean transfersToFirstAncestorRack(@PathParam("plateBarcodes") @Nonnull String plateBarcodes) {
        String[] barcodes = StringUtils.split(plateBarcodes, ",");
        Map<String, LabVessel> byBarcodes = labVesselDao.findByBarcodes(Arrays.asList(barcodes));

        List<LabEvent> labEvents = new ArrayList<>();

        for (LabVessel labVessel : byBarcodes.values()) {
            // Not checking that all queried barcodes were accounted for in the results, that is up to the caller.
            if (labVessel == null) {
                continue;
            }

            Set<List<LabEvent>> setOfLabEventLists =
                    VesselContainer.shortestPathsToVesselsSatisfyingPredicate(labVessel, VesselContainer.IS_LAB_VESSEL_A_RACK);

            // Flatten the result as the current caller does not expect more than one List of transfers to be found
            // per query barcode.
            for (List<LabEvent> labEventList : setOfLabEventLists) {
                labEvents.addAll(labEventList);
            }
        }

        Collections.sort(labEvents, LabEvent.BY_EVENT_DATE);

        List<LabEventBean> labEventBeans =
                buildLabEventBeans(labEvents, new DefaultLabEventRefDataFetcher());

        return new LabEventResponseBean(labEventBeans);
    }



    public List<LabEventBean> buildLabEventBeans(List<LabEvent> labEvents,
                                                 LabEventFactory.LabEventRefDataFetcher dataFetcherHelper) {
        List<LabEventBean> labEventBeans = new ArrayList<>();

        for (LabEvent labEvent : labEvents) {
            BspUser operator = dataFetcherHelper.getOperator(labEvent.getEventOperator());
            LabEventBean labEventBean = new LabEventBean(
                    labEvent.getLabEventType().getName(),
                    labEvent.getEventLocation(),
                    operator == null ? "Unknown user: " + labEvent.getEventOperator() : operator.getUsername(),
                    labEvent.getEventDate());
            for (Reagent reagent : labEvent.getReagents()) {
                labEventBean.getReagents().add(new ReagentBean(reagent.getName(), reagent.getLot()));
            }

            labEventBean.setBatchId(labEvent.getLabBatch().getBatchName());

            // todo jmt rationalize these?  Each side can be a vessel, or a vessel + section, or a vessel + position
            for (CherryPickTransfer cherryPickTransfer : labEvent.getCherryPickTransfers()) {
                TransferBean transferBean = new TransferBean();
                transferBean.setType("CherryPickTransfer");
                transferBean.setSourceBarcode(getLabel(cherryPickTransfer.getSourceVesselContainer().getEmbedder()));
                transferBean.setSourcePosition(cherryPickTransfer.getSourcePosition().name());
                transferBean.setTargetBarcode(getLabel(cherryPickTransfer.getTargetVesselContainer().getEmbedder()));
                transferBean.setTargetPosition(cherryPickTransfer.getTargetPosition().name());
                labEventBean.getTransfers().add(transferBean);
            }
            for (SectionTransfer sectionTransfer : labEvent.getSectionTransfers()) {
                TransferBean transferBean = new TransferBean();
                transferBean.setType("SectionTransfer");
                transferBean.setSourceBarcode(getLabel(sectionTransfer.getSourceVesselContainer().getEmbedder()));
                transferBean.setSourceSection(sectionTransfer.getSourceSection().getSectionName());
                transferBean.setTargetBarcode(getLabel(sectionTransfer.getTargetVesselContainer().getEmbedder()));
                transferBean.setTargetSection(sectionTransfer.getTargetSection().getSectionName());
                labEventBean.getTransfers().add(transferBean);
            }
            for (VesselToSectionTransfer vesselToSectionTransfer : labEvent.getVesselToSectionTransfers()) {
                TransferBean transferBean = new TransferBean();
                transferBean.setType("VesselToSectionTransfer");
                transferBean.setSourceBarcode(vesselToSectionTransfer.getSourceVessel().getLabel());
                transferBean
                        .setTargetBarcode(getLabel(vesselToSectionTransfer.getTargetVesselContainer().getEmbedder()));
                transferBean.setTargetSection(vesselToSectionTransfer.getTargetSection().getSectionName());
                labEventBean.getTransfers().add(transferBean);
            }
            for (VesselToVesselTransfer vesselToVesselTransfer : labEvent.getVesselToVesselTransfers()) {
                TransferBean transferBean = new TransferBean();
                transferBean.setType("VesselToVesselTransfer");
                transferBean.setSourceBarcode(vesselToVesselTransfer.getSourceVessel().getLabel());
                transferBean.setTargetBarcode(vesselToVesselTransfer.getTargetLabVessel().getLabel());
                labEventBean.getTransfers().add(transferBean);
            }

            for (LabVessel sourceLabVessel : labEvent.getSourceLabVessels()) {
                labEventBean.getSources().add(buildLabVesselBean(sourceLabVessel));
            }

            for (LabVessel targetLabVessel : labEvent.getTargetLabVessels()) {
                labEventBean.getTargets().add(buildLabVesselBean(targetLabVessel));
            }

            labEventBeans.add(labEventBean);
        }
        return labEventBeans;
    }

    // todo jmt make this work for sample starters
    static class StarterCriteria implements TransferTraverserCriteria {
        private LabVessel starter;

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (context.getLabVessel() != null) {
                if (context.getLabVessel().getTransfersTo().isEmpty()) {
                    for (LabBatch labBatch : context.getLabVessel().getLabBatches()) {
                        if (labBatch.getStartingBatchLabVessels().contains(context.getLabVessel())) {
                            starter = context.getLabVessel();
                            return TraversalControl.StopTraversing;
                        }
                    }
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public LabVessel getStarter() {
            return starter;
        }
    }

    private LabVesselBean buildLabVesselBean(LabVessel labVesselEntity) {
        // todo jmt need to hide on-the-fly creation of plate wells
        String type = labVesselEntity.getType().name();
        if (labVesselEntity.getType() == LabVessel.ContainerType.STATIC_PLATE) {
            type = OrmUtil.proxySafeCast(labVesselEntity, StaticPlate.class).getPlateType().getDisplayName();
        } else if (labVesselEntity.getType() == LabVessel.ContainerType.RACK_OF_TUBES) {
            type = OrmUtil.proxySafeCast(labVesselEntity, RackOfTubes.class).getRackType().getDisplayName();
        } else if (labVesselEntity.getType() == LabVessel.ContainerType.TUBE_FORMATION) {
            type = OrmUtil.proxySafeCast(labVesselEntity, TubeFormation.class).getRackType().getDisplayName();
        }
        String label = getLabel(labVesselEntity);
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
                                positionName, buildLabVesselBean(labVessel)));
                    }
                }
            } else {
                @SuppressWarnings("unchecked")
                Set<Map.Entry<VesselPosition, LabVessel>> entrySet =
                        vesselContainer.getMapPositionToVessel().entrySet();
                for (Map.Entry<VesselPosition, LabVessel> positionToLabVessel : entrySet) {
                    labVesselBean.getLabVesselPositionBeans().add(new LabVesselPositionBean(
                            positionToLabVessel.getKey().name(), buildLabVesselBean(positionToLabVessel.getValue())));
                }
            }
            for (LabVesselPositionBean labVesselPositionBean : labVesselBean.getLabVesselPositionBeans()) {
                StarterCriteria starterCriteria = new StarterCriteria();
                vesselContainer.evaluateCriteria(VesselPosition.getByName(labVesselPositionBean.getPosition()),
                        starterCriteria,
                        TransferTraverserCriteria.TraversalDirection.Ancestors, null, 0);
                if (starterCriteria.getStarter() != null) {
                    labVesselPositionBean.getLabVesselBean().setStarter(starterCriteria.getStarter().getLabel());
                }
            }
        }
        return labVesselBean;
    }

    /**
     * Returns label of vessel, including special handling for tube formations.
     *
     * @param labVesselEntity plastic
     *
     * @return label
     */
    private String getLabel(LabVessel labVesselEntity) {
        String label;
        if (labVesselEntity.getType() == LabVessel.ContainerType.TUBE_FORMATION) {
            // The "label" for a tube formation is the hash of the tube barcodes and their formations, so return the
            // label for the rack.
            label = OrmUtil.proxySafeCast(labVesselEntity, TubeFormation.class).getRacksOfTubes().iterator().next()
                    .getLabel();
        } else {
            label = labVesselEntity.getLabel();
        }
        return label;
    }

    // Section definitions
}
