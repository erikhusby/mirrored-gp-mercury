package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JAX-RS web service for vessel quantifications
 */
@SuppressWarnings("FeatureEnvy")
@Path("/vesselmetric")
@Stateful
@RequestScoped
public class VesselMetricResource {

    @Inject
    private LabVesselDao labVesselDao;

    @POST
    public String uploadQuantRun(VesselMetricRunBean vesselMetricRunBean) {
        // fetch existing run and vessels
        vesselMetricRunBean.getRunName();
        vesselMetricRunBean.getQuantType();
        List<String> barcodes = new ArrayList<String>();
        for (VesselMetricBean vesselMetricBean : vesselMetricRunBean.getVesselMetricBeans()) {
            barcodes.add(vesselMetricBean.getBarcode());
        }
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
        buildLabMetricRun(vesselMetricRunBean, mapBarcodeToVessel);

        return "Quant run persisted";
    }

    @DaoFree
    public LabMetricRun buildLabMetricRun(VesselMetricRunBean vesselMetricRunBean,
            Map<String, LabVessel> mapBarcodeToVessel) {
        for (VesselMetricBean vesselMetricBean : vesselMetricRunBean.getVesselMetricBeans()) {
            LabVessel labVessel = mapBarcodeToVessel.get(vesselMetricBean.getBarcode());
            if(labVessel == null) {
                throw new RuntimeException("Failed to find vessel for barcode " + vesselMetricBean.getBarcode());
            }
        }
        return new LabMetricRun(vesselMetricRunBean.getRunName(), vesselMetricRunBean.getRunDate(),
                LabMetric.MetricType.getByDisplayName(vesselMetricRunBean.getQuantType()));
    }
}
