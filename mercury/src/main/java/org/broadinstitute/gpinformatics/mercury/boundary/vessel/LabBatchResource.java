package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * For importing data from Squid and BSP, creates a batch of tubes
 */
@SuppressWarnings("FeatureEnvy")
@Path("/labbatch")
@Stateful
@RequestScoped
public class LabBatchResource {

    static final String BSP_BATCH_PREFIX = "BP";

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabVesselFactory labVesselFactory;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private JiraService jiraService;

    public LabBatchResource() {
    }

    /** Constructor used for test purposes. */
    public LabBatchResource(LabVesselFactory labVesselFactory) {
        this.labVesselFactory = labVesselFactory;
    }

    @POST
    public String createLabBatch(LabBatchBean labBatchBean) {
        LabBatch labBatch = null;
        if (labBatchBean.getParentVesselBean() != null) {
            labBatch = createLabBatchByParentVessel(labBatchBean);
        } else {
            List<String> tubeBarcodes = new ArrayList<>();
            List<MercurySample> mercurySampleKeys = new ArrayList<>();
            for (TubeBean tubeBean : labBatchBean.getTubeBeans()) {
                tubeBarcodes.add(tubeBean.getBarcode());
                if (tubeBean.getSampleBarcode() != null) {
                    mercurySampleKeys
                            .add(new MercurySample(tubeBean.getSampleBarcode(), MercurySample.MetadataSource.BSP));
                }
            }

            Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(tubeBarcodes);
            Map<MercurySample, MercurySample> mapSampleToSample =
                    mercurySampleDao.findByMercurySample(mercurySampleKeys);
            labBatch = buildLabBatch(labBatchBean, mapBarcodeToTube, mapSampleToSample);
        }

        if (!labBatchBean.getBatchId().startsWith(BSP_BATCH_PREFIX)) {
            JiraTicket jiraTicket = new JiraTicket(jiraService, labBatchBean.getBatchId());
            labBatch.setJiraTicket(jiraTicket);
            jiraTicket.setLabBatch(labBatch);
        }
        labBatchDao.persist(labBatch);
        labBatchDao.flush();

        return "Batch persisted";
    }

    /**
     * Build a LabBatch entity from a LabBatchBean with a ParentVesselBean
     */
    public LabBatch createLabBatchByParentVessel(LabBatchBean labBatchBean) {
        List<LabVessel> labVessels = labVesselFactory.buildLabVessels(
                Arrays.asList(labBatchBean.getParentVesselBean()), labBatchBean.getUsername(),
                new Date(), null, MercurySample.MetadataSource.BSP);

        Set<LabVessel> labVesselSet = new HashSet<>(labVessels);
        return new LabBatch(labBatchBean.getBatchId(), labVesselSet,
                labBatchBean.getBatchId().startsWith(BSP_BATCH_PREFIX) ?
                        LabBatch.LabBatchType.BSP : LabBatch.LabBatchType.WORKFLOW);
    }

    /**
     * DAO-free method to build a LabBatch entity
     *
     * @param labBatchBean       JAXB
     * @param mapBarcodeToTube   from database
     * @param mapBarcodeToSample from database
     *
     * @return entity
     */
    @DaoFree
    public LabBatch buildLabBatch(LabBatchBean labBatchBean, Map<String, BarcodedTube> mapBarcodeToTube,
                                  Map<MercurySample, MercurySample> mapBarcodeToSample) {
        Set<LabVessel> starters = new HashSet<>();
        for (TubeBean tubeBean : labBatchBean.getTubeBeans()) {
            BarcodedTube barcodedTube = mapBarcodeToTube.get(tubeBean.getBarcode());
            if (barcodedTube == null) {
                barcodedTube = new BarcodedTube(tubeBean.getBarcode());
                mapBarcodeToTube.put(tubeBean.getBarcode(), barcodedTube);
            }

            if (tubeBean.getSampleBarcode() != null) {
                MercurySample mercurySampleKey = new MercurySample(tubeBean.getSampleBarcode(),
                        MercurySample.MetadataSource.BSP);
                MercurySample mercurySample = mapBarcodeToSample.get(mercurySampleKey);
                if (mercurySample == null) {
                    mercurySample = mercurySampleKey;
                    mapBarcodeToSample.put(mercurySampleKey, mercurySample);
                }
                barcodedTube.addSample(mercurySample);
            }
            starters.add(barcodedTube);
        }
        return new LabBatch(labBatchBean.getBatchId(), starters,
                labBatchBean.getBatchId().startsWith(BSP_BATCH_PREFIX) ?
                        LabBatch.LabBatchType.BSP : LabBatch.LabBatchType.WORKFLOW);
    }

}
