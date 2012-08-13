package org.broadinstitute.sequel.boundary.labevent;

import org.broadinstitute.sequel.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.sequel.entity.OrmUtil;
import org.broadinstitute.sequel.entity.labevent.GenericLabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.VesselContainer;
import org.broadinstitute.sequel.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.sequel.entity.vessel.VesselPosition;
import org.broadinstitute.sequel.entity.workflow.LabBatch;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.ArrayList;
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
    public List<LabEventBean> transfersByBatchId(@PathParam("batchId")String batchId) {
        LabBatch labBatch = labBatchDAO.findByName(batchId);
        Set<GenericLabEvent> labEvents = labBatch.getLabEvents();
        List<LabEventBean> labEventBeans = buildLabEventBeans(labEvents);
        return labEventBeans;
    }

    public List<LabEventBean> buildLabEventBeans(Set<GenericLabEvent> labEvents) {
        List<LabEventBean> labEventBeans = new ArrayList<LabEventBean>();
        for (GenericLabEvent labEvent : labEvents) {
            LabEventBean labEventBean = new LabEventBean(
                    labEvent.getLabEventType().getName(),
                    labEvent.getEventLocation(),
                    labEvent.getEventOperator().getLogin());
            labEventBean.setBatchId(labEvent.getLabBatch().getBatchName());

            // todo jmt rationalize these?  Each side can be a vessel, or a vessel + section, or a vessel + position
            labEvent.getCherryPickTransfers();
            labEvent.getSectionTransfers();
            labEvent.getVesselToSectionTransfers();
            labEvent.getVesselToVesselTransfers();

            for (LabVessel sourceLabVessel : labEvent.getSourceLabVessels()) {
                labEventBean.getSources().add(buildLabVesselBean(sourceLabVessel));
            }

            for (LabVessel targetLabVessel : labEvent.getTargetLabVessels()) {
                labEventBean.getTargets().add(buildLabVesselBean(targetLabVessel));
            }

            TransferBean transferBean = new TransferBean();
            labEventBean.getTransfers().add(transferBean);
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
                        if(labBatch.getStarters().contains(labVessel)) {
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
        }
        LabVesselBean labVesselBean = new LabVesselBean(labVesselEntity.getLabel(), type);
        if(OrmUtil.proxySafeIsInstance(labVesselEntity, VesselContainerEmbedder.class)) {
            VesselContainer vesselContainer = OrmUtil.proxySafeCast(labVesselEntity, VesselContainerEmbedder.class).getVesselContainer();
            if(OrmUtil.proxySafeIsInstance(labVesselEntity, StaticPlate.class)) {
                StaticPlate staticPlate = OrmUtil.proxySafeCast(labVesselEntity, StaticPlate.class);
                Iterator<String> positionNames = staticPlate.getPlateType().getVesselGeometry().getPositionNames();
                while (positionNames.hasNext()) {
                    String positionName  =  positionNames.next();
                    labVesselBean.getMapPositionToLabVessel().put(positionName, new LabVesselBean(null, LabVessel.CONTAINER_TYPE.PLATE_WELL.name()));
                }
            } else {
                Set<Map.Entry<VesselPosition, LabVessel>> entrySet = vesselContainer.getMapPositionToVessel().entrySet();
                for (Map.Entry<VesselPosition, LabVessel> positionToLabVessel : entrySet) {
                    labVesselBean.getMapPositionToLabVessel().put(positionToLabVessel.getKey().name(), buildLabVesselBean(positionToLabVessel.getValue()));
                }
            }
            for (Map.Entry<String, LabVesselBean> positionLabVesselBeanEntry : labVesselBean.getMapPositionToLabVessel().entrySet()) {
                StarterCriteria starterCriteria = new StarterCriteria();
                vesselContainer.evaluateCriteria(VesselPosition.getByName(positionLabVesselBeanEntry.getKey()), starterCriteria,
                        VesselContainer.TraversalDirection.Ancestors, null, 0);
                if (starterCriteria.getStarter() != null) {
                    positionLabVesselBeanEntry.getValue().setStarter(starterCriteria.getStarter().getLabel());
                }
            }
        }
        return labVesselBean;
    }

    // Section definitions
}
