package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * For importing data from Squid and BSP, creates a batch of tubes
 */
@Path("/labbatch")
@Stateless
public class LabBatchResource {

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabBatchDAO labBatchDAO;

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
        LabBatch labBatch = buildLabBatch(labBatchBean, mapBarcodeToTube, mapSampleToSample/*, null*/);
        labBatchDAO.persist(labBatch);
        labBatchDAO.flush();

        return "Batch persisted";
    }

    public LabBatch buildLabBatch(LabBatchBean labBatchBean, Map<String, TwoDBarcodedTube> mapBarcodeToTube,
            Map<MercurySample, MercurySample> mapBarcodeToSample/*, BasicProjectPlan projectPlan*/) {
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

//            if (tubeBean.getSampleBarcode() == null) {
                starters.add(twoDBarcodedTube);
//            } else {
//                BSPStartingSample bspStartingSample = mapBarcodeToSample.get(tubeBean.getSampleBarcode());
//                if(bspStartingSample == null) {
//                    bspStartingSample = new BSPStartingSample(tubeBean.getSampleBarcode() + ".aliquot"/*, projectPlan*/);
//                    mapBarcodeToSample.put(tubeBean.getSampleBarcode(), bspStartingSample);
//                }
//
//                starters.add(bspStartingSample);
//                projectPlan.addStarter(bspStartingSample);
//                projectPlan.addAliquotForStarter(bspStartingSample, twoDBarcodedTube);
//            }
        }
        LabBatch labBatch;
//        if(projectPlan == null) {
            labBatch = new LabBatch(labBatchBean.getBatchId(), starters);
//        } else {
//            labBatch = new LabBatch(projectPlan, labBatchBean.getBatchId(), starters);
//        }
        return labBatch;
    }
}
