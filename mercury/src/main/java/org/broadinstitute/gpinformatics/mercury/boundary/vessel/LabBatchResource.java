package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.*;

/**
 * For importing data from Squid and BSP, creates a batch of tubes
 */
@Path("/labbatch")
@Stateful
@RequestScoped
public class LabBatchResource {

    public static final String BSP_BATCH_PREFIX = "BP";

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabBatchDAO labBatchDAO;

    @Inject
    private JiraService jiraService;

    @POST
    public String createLabBatch(LabBatchBean labBatchBean) {
        List<String> tubeBarcodes = new ArrayList<String>();
        List<MercurySample> mercurySampleKeys = new ArrayList<MercurySample>();
        for (TubeBean tubeBean : labBatchBean.getTubeBeans()) {
            tubeBarcodes.add(tubeBean.getBarcode());
            if(tubeBean.getSampleBarcode() != null && tubeBean.getProductOrderKey() != null) {
                mercurySampleKeys.add(new MercurySample(tubeBean.getProductOrderKey(), tubeBean.getSampleBarcode()));
            }
        }

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = twoDBarcodedTubeDAO.findByBarcodes(tubeBarcodes);
        Map<MercurySample, MercurySample> mapSampleToSample = mercurySampleDao.findByMercurySample(mercurySampleKeys);
        LabBatch labBatch = buildLabBatch(labBatchBean, mapBarcodeToTube, mapSampleToSample);

        if(!labBatchBean.getBatchId().startsWith(BSP_BATCH_PREFIX)) {
            JiraTicket jiraTicket = new JiraTicket(jiraService, labBatchBean.getBatchId());
            labBatch.setJiraTicket(jiraTicket);
            jiraTicket.setLabBatch(labBatch);
        }
        labBatchDAO.persist(labBatch);
        labBatchDAO.flush();

        return "Batch persisted";
    }

    /**
     * DAO-free method to build a LabBatch entity
     * @param labBatchBean JAXB
     * @param mapBarcodeToTube from database
     * @param mapBarcodeToSample from database
     * @return entity
     */
    @DaoFree
    public LabBatch buildLabBatch(LabBatchBean labBatchBean, Map<String, TwoDBarcodedTube> mapBarcodeToTube,
            Map<MercurySample, MercurySample> mapBarcodeToSample) {
        Set<LabVessel> starters = new HashSet<LabVessel>();
        for (TubeBean tubeBean : labBatchBean.getTubeBeans()) {
            TwoDBarcodedTube twoDBarcodedTube = mapBarcodeToTube.get(tubeBean.getBarcode());
            if (twoDBarcodedTube == null) {
                twoDBarcodedTube = new TwoDBarcodedTube(tubeBean.getBarcode());
                mapBarcodeToTube.put(tubeBean.getBarcode(), twoDBarcodedTube);
            }

            if(tubeBean.getSampleBarcode() != null && tubeBean.getProductOrderKey() != null) {
                MercurySample mercurySampleKey = new MercurySample(tubeBean.getProductOrderKey(), tubeBean.getSampleBarcode());
                MercurySample mercurySample = mapBarcodeToSample.get(mercurySampleKey);
                if(mercurySample == null) {
                    mercurySample = mercurySampleKey;
                    mapBarcodeToSample.put(mercurySampleKey, mercurySample);
                }
                twoDBarcodedTube.addSample(mercurySample);
            }
            starters.add(twoDBarcodedTube);
        }
        return new LabBatch(labBatchBean.getBatchId(), starters, labBatchBean.getBatchId().startsWith(BSP_BATCH_PREFIX) ?
                LabBatch.LabBatchType.BSP : LabBatch.LabBatchType.WORKFLOW);
    }

}
