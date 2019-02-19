package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    private LabBatchDao labBatchDao;

    @Inject
    private LabVesselFactory labVesselFactory;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private UserBean userBean;

    /**
     * Get an imported batch, currently used only for testing
     *
     * @param batchName EX-1234
     *
     * @return JAX-RS DTO
     */
    @SuppressWarnings("FeatureEnvy")
    @GET
    @Path("{batchName}")
    @Produces({MediaType.APPLICATION_XML})
    public SampleImportBean getByBatchName(@PathParam("batchName") String batchName) {
        LabBatch labBatch = labBatchDao.findByName(batchName);
        if (labBatch == null) {
            return null;
        }

        Set<LabVessel> startingLabVessels = labBatch.getStartingBatchLabVessels();
        LabVessel firstLabVessel = startingLabVessels.iterator().next();
        LabEvent labEvent = firstLabVessel.getInPlaceEventsWithContainers().iterator().next();

        List<ChildVesselBean> childVesselBeans = new ArrayList<>();
        TubeFormation tubeFormation = (TubeFormation) firstLabVessel.getVesselContainers().iterator().next().getEmbedder();
        for (LabVessel startingLabVessel : startingLabVessels) {
            childVesselBeans.add(new ChildVesselBean(
                    startingLabVessel.getLabel(),
                    startingLabVessel.getMercurySamples().iterator().next().getSampleKey(),
                    startingLabVessel.getType().getName(),
                    tubeFormation.getContainerRole().getPositionOfVessel(startingLabVessel).name()));
        }

        RackOfTubes rackOfTubes = tubeFormation.getRacksOfTubes().iterator().next();
        List<ParentVesselBean> parentVesselBeans = new ArrayList<>();
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
     *
     * @param sampleImportBean JAX-RS DTO
     *
     * @return "Samples imported: " + batchName
     */
    @SuppressWarnings("FeatureEnvy")
    @POST
    public String importSamples(SampleImportBean sampleImportBean) {
        // todo jmt store the text of the message
        List<ParentVesselBean> parentVesselBeans = sampleImportBean.getParentVesselBeans();

        userBean.login(sampleImportBean.getUserName());

        if(userBean.getBspUser() ==  UserBean.UNKNOWN) {
            throw new ResourceException("A valid Username is required to complete this request",
                    Response.Status.UNAUTHORIZED);
        }

        Pair<List<LabVessel>,List<LabVessel>> labVessels = labVesselFactory.buildLabVessels(parentVesselBeans, sampleImportBean.getUserName(),
                sampleImportBean.getExportDate(), LabEventType.SAMPLE_IMPORT, MercurySample.MetadataSource.BSP);

        LabBatch labBatch = labBatchDao.findByName(sampleImportBean.getSourceSystemExportId());
        if (labBatch != null) {
            throw new RuntimeException(
                    "Export has already been received " + sampleImportBean.getSourceSystemExportId());
        }

        labBatchDao.persistAll(labVessels.getRight());

        String batchName = sampleImportBean.getSourceSystemExportId();
        labBatchDao.persist(new LabBatch(batchName, new HashSet<>(labVessels.getLeft()),
                LabBatch.LabBatchType.SAMPLES_IMPORT, sampleImportBean.getExportDate()));
        return "Samples imported: " + batchName;
    }

}
