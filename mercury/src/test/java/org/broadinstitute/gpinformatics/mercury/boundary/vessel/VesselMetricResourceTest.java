package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Database free test of the Vessel Metric web service run builder utility
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class VesselMetricResourceTest {

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testBuildLabMetricRun() {
        VesselMetricResource vesselMetricResource = new VesselMetricResource();

        ArrayList<VesselMetricBean> vesselMetricBeans = new ArrayList<>();
        String barcode = "1234";
        vesselMetricBeans.add(new VesselMetricBean(barcode, "1.2", "ng/uL"));
        VesselMetricRunBean vesselMetricRunBean = new VesselMetricRunBean("TestRun", new Date(),  "Initial Pico", vesselMetricBeans);

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(barcode, new BarcodedTube(barcode));
        LabMetricRun labMetricRun = vesselMetricResource.buildLabMetricRun(vesselMetricRunBean, mapBarcodeToVessel);

        Assert.assertEquals(labMetricRun.getLabMetrics().size(), 1, "Wrong number of metrics");
        LabMetric labMetric = labMetricRun.getLabMetrics().iterator().next();
        Assert.assertEquals(labMetric.getLabVessel().getLabel(), barcode, "Wrong barcode");
    }
}
