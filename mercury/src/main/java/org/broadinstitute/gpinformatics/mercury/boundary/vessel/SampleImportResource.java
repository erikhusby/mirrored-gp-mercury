package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JAX-RS web service to import samples from other systems, e.g. BSP
 */
@Path("/sampleimport")
@Stateful
@RequestScoped
public class SampleImportResource {

    @Inject
    private LabBatchDAO labBatchDAO;

    @Inject
    private LabVesselFactory labVesselFactory;

    @Inject
    private BSPUserList bspUserList;

    /**
     * Get an imported batch, currently used only for testing
     * @param batchName EX-1234
     * @return JAX-RS DTO
     */
    @SuppressWarnings("FeatureEnvy")
    @GET
    @Path("{batchName}")
    @Produces({MediaType.APPLICATION_XML})
    public SampleImportBean getByBatchName(@PathParam("batchName") String batchName) {
        LabBatch labBatch = labBatchDAO.findByName(batchName);
        if(labBatch == null) {
            return null;
        }

        Set<LabVessel> startingLabVessels = labBatch.getStartingLabVessels();
        LabVessel firstLabVessel = startingLabVessels.iterator().next();
        LabEvent labEvent = firstLabVessel.getInPlaceEvents().iterator().next();

        List<ChildVesselBean> childVesselBeans = new ArrayList<ChildVesselBean>();
        TubeFormation tubeFormation = (TubeFormation) firstLabVessel.getContainers().iterator().next().getEmbedder();
        for (LabVessel startingLabVessel : startingLabVessels) {
            childVesselBeans.add(new ChildVesselBean(
                    startingLabVessel.getLabel(),
                    startingLabVessel.getMercurySamples().iterator().next().getSampleKey(),
                    startingLabVessel.getType().getName(),
                    tubeFormation.getContainerRole().getPositionOfVessel(startingLabVessel).name()));
        }

        RackOfTubes rackOfTubes = tubeFormation.getRacksOfTubes().iterator().next();
        List<ParentVesselBean> parentVesselBeans = new ArrayList<ParentVesselBean>();
        parentVesselBeans.add(new ParentVesselBean(rackOfTubes.getLabel(), null, rackOfTubes.getType().getName(),
                childVesselBeans));
        BspUser bspUser = bspUserList.getById(labEvent.getEventOperator());
        return new SampleImportBean(
                "BSP",
                labBatch.getBatchName(),
                labEvent.getEventDate(),
                parentVesselBeans,
                bspUser.getUsername());
    }

    /**
     * Register samples from BSP, usually a rack of tubes
     * @param sampleImportBean JAX-RS DTO
     * @return "Samples imported: " + batchName
     */
    @SuppressWarnings("FeatureEnvy")
    @POST
    public String importSamples(SampleImportBean sampleImportBean) {
        // todo jmt store the text of the message
        List<ParentVesselBean> parentVesselBeans = sampleImportBean.getParentVesselBeans();

        List<LabVessel> labVessels = labVesselFactory.buildLabVessels(parentVesselBeans, sampleImportBean.getUserName(),
                sampleImportBean.getExportDate(), LabEventType.SAMPLE_IMPORT);

        LabBatch labBatch = labBatchDAO.findByName(sampleImportBean.getSourceSystemExportId());
        if (labBatch != null) {
            throw new RuntimeException("Export has already been received " + sampleImportBean.getSourceSystemExportId());
        }
        String batchName = sampleImportBean.getSourceSystemExportId();
        labBatchDAO.persist(new LabBatch(batchName, new HashSet<LabVessel>(labVessels),
                LabBatch.LabBatchType.SAMPLES_IMPORT));
        return "Samples imported: " + batchName;
    }

}
