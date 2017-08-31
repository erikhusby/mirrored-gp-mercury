package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test the functionality in the Vessel Metric web service
 */
@Test(groups = TestGroups.STANDARD)
public class VesselMetricResourceTest extends RestServiceContainerTest {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    /**
     * Run builder functionality (DAO free)
     */
    @Test
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

    @Test(dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testQuantFetcher(@ArquillianResource URL baseUrl)
            throws Exception {
        // Service added via GPLIM-4931 so use the tube and quant type in the ticket to verify
        String barcode = "0212942357";
        LabMetric.MetricType quantType = LabMetric.MetricType.VIIA_QPCR;

        WebResource resource = makeWebResource(baseUrl, "fetchQuantForVessel");

        String result = resource
                .queryParam("metricType", LabMetric.MetricType.VIIA_QPCR.name())
                .queryParam("barcode", barcode).get(String.class);
        Assert.assertEquals("1.38", result, "Unexpected quant value" );

        try {
            result = resource
                    .queryParam("metricType", LabMetric.MetricType.VIIA_QPCR.name())
                    .queryParam("barcode", barcode + "XX").get(String.class);
        } catch( UniformInterfaceException uie ) {
            Assert.assertEquals(uie.getResponse().getEntity(String.class), "No LabVessel for barcode", "Unexpected error message" );
        }

        try {
            result = resource
                .queryParam("metricType", LabMetric.MetricType.INITIAL_PICO.name())
                .queryParam("barcode", barcode).get(String.class);
        } catch( UniformInterfaceException uie ) {
            Assert.assertEquals(uie.getResponse().getEntity(String.class), "No metrics for LabVessel", "Unexpected error message");
        }
    }

    @Override
    protected String getResourcePath() {
        return "vesselmetric";
    }
}
