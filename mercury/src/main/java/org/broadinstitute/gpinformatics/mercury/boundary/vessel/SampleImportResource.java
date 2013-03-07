package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.HashSet;
import java.util.List;

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

    /**
     * Register samples from BSP, usually a rack of tubes
     * @param sampleImportBean JAX-RS DTO
     * @return "Samples imported: " + batchName
     */
    @SuppressWarnings("FeatureEnvy")
    @POST
    public String importSamples(SampleImportBean sampleImportBean) {
        List<ParentVesselBean> parentVesselBeans = sampleImportBean.getParentVesselBeans();

        List<LabVessel> labVessels = labVesselFactory.buildLabVessels(parentVesselBeans, sampleImportBean.getUserName(),
                sampleImportBean.getExportDate(), LabEventType.SAMPLE_RECEIPT);

        LabBatch labBatch = labBatchDAO.findByName(sampleImportBean.getSourceSystemExportId());
        if (labBatch != null) {
            throw new RuntimeException("Export has already been received " + sampleImportBean.getSourceSystemExportId());
        }
        String batchName = sampleImportBean.getSourceSystemExportId();
        labBatchDAO.persist(new LabBatch(batchName, new HashSet<LabVessel>(labVessels),
                LabBatch.LabBatchType.SAMPLES_RECEIPT));
        return "Samples imported: " + batchName;
    }

}
