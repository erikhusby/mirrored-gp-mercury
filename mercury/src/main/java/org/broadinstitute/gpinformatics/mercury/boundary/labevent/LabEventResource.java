package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A JAX-RS web service to return Transfers etc., by various criteria
 */
@Path("/labevent")
@Stateless
public class LabEventResource {

    @Inject
    private LabBatchDAO labBatchDAO;

    @Path("/batch/{batchId}")
    @GET
    @Produces({MediaType.APPLICATION_XML})
    public LabEventResponseBean transfersByBatchId(@PathParam("batchId")String batchId) {
        LabBatch labBatch = labBatchDAO.findByName(batchId);
        List<LabEvent> labEventsByTime = new ArrayList<LabEvent>(labBatch.getLabEvents());
        Collections.sort(labEventsByTime, LabEvent.byEventDate);
        List<LabEventBean> labEventBeans = buildLabEventBeans(labEventsByTime);
        return new LabEventResponseBean(labEventBeans);
    }

    public List<LabEventBean> buildLabEventBeans(List<LabEvent> labEvents) {
        List<LabEventBean> labEventBeans = new ArrayList<LabEventBean>();
        for (LabEvent labEvent : labEvents) {
            LabEventBean labEventBean = new LabEventBean(
                    labEvent.getLabEventType().getName(),
                    labEvent.getEventLocation(),
                    labEvent.getEventOperator().getLogin(),
                    labEvent.getEventDate());
            labEventBean.setBatchId(labEvent.getLabBatch().getBatchName());

            // todo jmt rationalize these?  Each side can be a vessel, or a vessel + section, or a vessel + position
            for (CherryPickTransfer cherryPickTransfer : labEvent.getCherryPickTransfers()) {
                TransferBean transferBean = new TransferBean();
                transferBean.setType("CherryPickTransfer");
                transferBean.setSourceBarcode(cherryPickTransfer.getSourceVesselContainer().getEmbedder().getLabel());
                transferBean.setSourcePosition(cherryPickTransfer.getSourcePosition().name());
                transferBean.setTargetBarcode(cherryPickTransfer.getTargetVesselContainer().getEmbedder().getLabel());
                transferBean.setTargetPosition(cherryPickTransfer.getTargetPosition().name());
                labEventBean.getTransfers().add(transferBean);
            }
            for (SectionTransfer sectionTransfer : labEvent.getSectionTransfers()) {
                TransferBean transferBean = new TransferBean();
                transferBean.setType("SectionTransfer");
                transferBean.setSourceBarcode(sectionTransfer.getSourceVesselContainer().getEmbedder().getLabel());
                transferBean.setSourceSection(sectionTransfer.getSourceSection().getSectionName());
                transferBean.setTargetBarcode(sectionTransfer.getTargetVesselContainer().getEmbedder().getLabel());
                transferBean.setTargetSection(sectionTransfer.getTargetSection().getSectionName());
                labEventBean.getTransfers().add(transferBean);
            }
            for (VesselToSectionTransfer vesselToSectionTransfer : labEvent.getVesselToSectionTransfers()) {
                TransferBean transferBean = new TransferBean();
                transferBean.setType("VesselToSectionTransfer");
                transferBean.setSourceBarcode(vesselToSectionTransfer.getSourceVessel().getLabel());
                transferBean.setTargetBarcode(vesselToSectionTransfer.getTargetVesselContainer().getEmbedder().getLabel());
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
    static class StarterCriteria implements VesselContainer.TransferTraverserCriteria {
        private LabVessel starter;

        @Override
        public TraversalControl evaluateVessel(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            if (labVessel != null) {
                if(labVessel.getTransfersTo().isEmpty()) {
                    for (LabBatch labBatch : labVessel.getLabBatches()) {
                        if(labBatch.getStartingLabVessels().contains(labVessel)) {
                            starter = labVessel;
                            return TraversalControl.StopTraversing;
                        }
                    }
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        public LabVessel getStarter() {
            return starter;
        }
    }

    private LabVesselBean buildLabVesselBean(LabVessel labVesselEntity) {
        // todo jmt need to hide on-the-fly creation of plate wells
        String type = labVesselEntity.getType().name();
        if(type.equals(LabVessel.CONTAINER_TYPE.STATIC_PLATE.name())) {
            type = OrmUtil.proxySafeCast(labVesselEntity, StaticPlate.class).getPlateType().getDisplayName();
        } else if(type.equals(LabVessel.CONTAINER_TYPE.RACK_OF_TUBES.name())) {
            type = OrmUtil.proxySafeCast(labVesselEntity, RackOfTubes.class).getRackType().getDisplayName();
        }
        LabVesselBean labVesselBean = new LabVesselBean(labVesselEntity.getLabel(), type);
        VesselContainer vesselContainer = labVesselEntity.getContainerRole();
        if(vesselContainer != null) {
            if(OrmUtil.proxySafeIsInstance(labVesselEntity, StaticPlate.class)) {
                StaticPlate staticPlate = OrmUtil.proxySafeCast(labVesselEntity, StaticPlate.class);
                Iterator<String> positionNames = staticPlate.getPlateType().getVesselGeometry().getPositionNames();
                while (positionNames.hasNext()) {
                    String positionName  =  positionNames.next();
                    labVesselBean.getLabVesselPositionBeans().add(new LabVesselPositionBean(
                            positionName, new LabVesselBean(null, LabVessel.CONTAINER_TYPE.PLATE_WELL.name())));
                }
            } else if(OrmUtil.proxySafeIsInstance(labVesselEntity, RackOfTubes.class)) {
                RackOfTubes rackOfTubes = OrmUtil.proxySafeCast(labVesselEntity, RackOfTubes.class);
                Iterator<String> positionNames = rackOfTubes.getRackType().getVesselGeometry().getPositionNames();
                while (positionNames.hasNext()) {
                    String positionName  =  positionNames.next();
                    LabVessel labVessel = (LabVessel) vesselContainer.getMapPositionToVessel().get(VesselPosition.getByName(positionName));
                    labVesselBean.getLabVesselPositionBeans().add(new LabVesselPositionBean(
                            positionName, buildLabVesselBean(labVessel)));
                }
            } else {
                Set<Map.Entry<VesselPosition, LabVessel>> entrySet = vesselContainer.getMapPositionToVessel().entrySet();
                for (Map.Entry<VesselPosition, LabVessel> positionToLabVessel : entrySet) {
                    labVesselBean.getLabVesselPositionBeans().add(new LabVesselPositionBean(
                            positionToLabVessel.getKey().name(), buildLabVesselBean(positionToLabVessel.getValue())));
                }
            }
            for (LabVesselPositionBean labVesselPositionBean : labVesselBean.getLabVesselPositionBeans()) {
                StarterCriteria starterCriteria = new StarterCriteria();
                vesselContainer.evaluateCriteria(VesselPosition.getByName(labVesselPositionBean.getPosition()), starterCriteria,
                        VesselContainer.TraversalDirection.Ancestors, null, 0);
                if (starterCriteria.getStarter() != null) {
                    labVesselPositionBean.getLabVesselBean().setStarter(starterCriteria.getStarter().getLabel());
                }
            }
        }
        return labVesselBean;
    }

    // Section definitions
}
