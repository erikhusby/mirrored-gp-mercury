package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
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

    @Inject
    private LabMetricRunDao labMetricRunDao;

    @POST
    public String uploadQuantRun(VesselMetricRunBean vesselMetricRunBean) {
        // fetch existing run and vessels
        List<String> barcodes = new ArrayList<>();
        for (VesselMetricBean vesselMetricBean : vesselMetricRunBean.getVesselMetricBeans()) {
            barcodes.add(vesselMetricBean.getBarcode());
        }
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
        LabMetricRun labMetricRun = buildLabMetricRun(vesselMetricRunBean, mapBarcodeToVessel);
        labMetricRunDao.persist(labMetricRun);
        labMetricRunDao.flush();

        return "Quant run persisted";
    }

    @Path("fetchQuantForVessel")
    @Produces("text/plain")
    @GET
    public BigDecimal fetchQuantForVessel(@QueryParam("metricType") LabMetric.MetricType metricType, @QueryParam("barcode") String barcode ) {
        LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
        BigDecimal value = null;
        if( labVessel != null ) {
            if( labVessel.getContainerRole() != null ) {
                throw new RuntimeException("Resource does not handle container vessels");
            }
            ArrayList<LabMetric> metrics = new ArrayList<>();

            for( LabMetric metric : labVessel.getMetrics() ) {
                if( metric.getName() == metricType ) {
                    metrics.add( metric );
                }
            }
            if( metrics.size() > 0 ) {
                // If more than 1, get latest only!
                if( metrics.size() > 1 ) {
                    Collections.sort(metrics);
                }
                value = metrics.get( metrics.size() - 1 ).getValue();
            }
        } else {
            throw new RuntimeException("No LabVessel for barcode");
        }
        if( value != null ) {
            return value;
        } else {
            throw new RuntimeException("No metrics for LabVessel");
        }

    }


    @DaoFree
    public LabMetricRun buildLabMetricRun(VesselMetricRunBean vesselMetricRunBean,
            Map<String, LabVessel> mapBarcodeToVessel) {
        // todo jmt pass in existing LabMetricRun, if any
        LabMetric.MetricType metricType = LabMetric.MetricType.getByDisplayName(vesselMetricRunBean.getQuantType());
        LabMetricRun labMetricRun = new LabMetricRun(vesselMetricRunBean.getRunName(), vesselMetricRunBean.getRunDate(),
                metricType);

        for (VesselMetricBean vesselMetricBean : vesselMetricRunBean.getVesselMetricBeans()) {
            LabVessel labVessel = mapBarcodeToVessel.get(vesselMetricBean.getBarcode());
            if(labVessel == null) {
                throw new RuntimeException("Failed to find vessel for barcode " + vesselMetricBean.getBarcode());
            }
            LabMetric labMetric = new LabMetric(new BigDecimal(vesselMetricBean.getValue()), metricType,
                    LabMetric.LabUnit.getByDisplayName(vesselMetricBean.getUnit()),
                    vesselMetricBean.getContainerPosition(), vesselMetricRunBean.getRunDate());
            labVessel.addMetric(labMetric);
            labMetricRun.addMetric(labMetric);
        }
        return labMetricRun;
    }
}
