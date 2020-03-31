package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.MetricReworkDisposition;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;

/**
 * Tests the PicoDispositionActionBean
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class PicoDispositionActionBeanTest {

    public void testInvalidVesselAndPosition() {
        PicoDispositionActionBean picoDispositionActionBean = new PicoDispositionActionBean();

        BarcodedTubeDao mockBarcodedTubeDao = createMock(BarcodedTubeDao.class);
        expect(mockBarcodedTubeDao.findByBarcode("1194638779")).andReturn(new BarcodedTube("1194638779"));
        expect(mockBarcodedTubeDao.findByBarcode("XX234RR")).andReturn(null);
        picoDispositionActionBean.setBarcodedTubeDao(mockBarcodedTubeDao);
        replay(mockBarcodedTubeDao);
        // Bad barcode, bad position
        picoDispositionActionBean.setRackScanJson("{\"scans\":[{\"position\":\"R99\",\"barcode\":\"1194638779\"},{\"position\":\"A06\",\"barcode\":\"XX234RR\"}]}");

        String resultJson = picoDispositionActionBean.getListItemsJson();

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode out = mapper.readTree(resultJson);
            JsonNode errNode = out.get("errors");
            Assert.assertTrue(errNode.isArray(), "Expected an array of error messages");
            String[] errs = mapper.convertValue(errNode, String[].class);
            Assert.assertEquals(errs.length, 2, "Expected 2 error messages");
        } catch (Exception e) {
            Assert.fail("JSON parsing exception thrown", e);
        }
    }

    public void testVesselMetrics() {
        PicoDispositionActionBean picoDispositionActionBean = new PicoDispositionActionBean();

        Date now = new Date();

        Map<String, LabMetric> mapBarcodeToMetric = new HashMap<>();
        Map<String, BarcodedTube> mapBarcodeToTube = new HashMap<>();

        BarcodedTube tube;
        LabMetric labMetric;
        LabMetricDecision decision;

        // REPEAT
        tube = new BarcodedTube("1194638817");
        tube.setVolume(BigDecimal.valueOf(51));
        tube.addSample(new MercurySample("SM-JACU1", MercurySample.MetadataSource.MERCURY));
        labMetric = new LabMetric(BigDecimal.valueOf(3.66), LabMetric.MetricType.INITIAL_PICO, LabMetric.LabUnit.NG_PER_UL, VesselPosition.A01, now);
        labMetric.setLabVessel(tube);
        decision = new LabMetricDecision(LabMetricDecision.Decision.REPEAT, now, 01L, labMetric);
        decision.setReworkDisposition(MetricReworkDisposition.BAD_TRIP_READS);
        labMetric.setLabMetricDecision(decision);
        tube.getMetrics().add(labMetric);
        mapBarcodeToMetric.put(tube.getLabel(), labMetric);
        mapBarcodeToTube.put(tube.getLabel(), tube);

        // PASS
        tube = new BarcodedTube("1194638806");
        tube.setVolume(BigDecimal.valueOf(50));
        tube.addSample(new MercurySample("SM-JACVO", MercurySample.MetadataSource.MERCURY));
        labMetric = new LabMetric(BigDecimal.valueOf(7.36), LabMetric.MetricType.INITIAL_PICO, LabMetric.LabUnit.NG_PER_UL, VesselPosition.A02, now);
        labMetric.setLabVessel(tube);
        decision = new LabMetricDecision(LabMetricDecision.Decision.PASS, now, 01L, labMetric);
        decision.setReworkDisposition(MetricReworkDisposition.PASS);
        labMetric.setLabMetricDecision(decision);
        tube.getMetrics().add(labMetric);
        mapBarcodeToMetric.put(tube.getLabel(), labMetric);
        mapBarcodeToTube.put(tube.getLabel(), tube);

        // NORM
        tube = new BarcodedTube("1194638519");
        tube.setVolume(BigDecimal.valueOf(54));
        tube.addSample(new MercurySample("SM-JADAI", MercurySample.MetadataSource.MERCURY));
        labMetric = new LabMetric(BigDecimal.valueOf(101.00), LabMetric.MetricType.INITIAL_PICO, LabMetric.LabUnit.NG_PER_UL, VesselPosition.A08, now);
        labMetric.setLabVessel(tube);
        decision = new LabMetricDecision(LabMetricDecision.Decision.REPEAT, now, 01L, labMetric);
        decision.setReworkDisposition(MetricReworkDisposition.NORM_IN_TUBE);
        labMetric.setLabMetricDecision(decision);
        tube.getMetrics().add(labMetric);
        mapBarcodeToMetric.put(tube.getLabel(), labMetric);
        mapBarcodeToTube.put(tube.getLabel(), tube);

        // UNDILUTED
        tube = new BarcodedTube("1194638853");
        tube.setVolume(BigDecimal.valueOf(100));
        tube.addSample(new MercurySample("SM-JACVI", MercurySample.MetadataSource.MERCURY));
        labMetric = new LabMetric(BigDecimal.valueOf(3.00), LabMetric.MetricType.INITIAL_PICO, LabMetric.LabUnit.NG_PER_UL, VesselPosition.H01, now);
        labMetric.setLabVessel(tube);
        decision = new LabMetricDecision(LabMetricDecision.Decision.REPEAT, now, 01L, labMetric);
        decision.setReworkDisposition(MetricReworkDisposition.UNDILUTED);
        labMetric.setLabMetricDecision(decision);
        tube.getMetrics().add(labMetric);
        mapBarcodeToMetric.put(tube.getLabel(), labMetric);
        mapBarcodeToTube.put(tube.getLabel(), tube);

        BarcodedTubeDao mockBarcodedTubeDao = createMock(BarcodedTubeDao.class);
        for (Map.Entry<String, BarcodedTube> labelToVessel : mapBarcodeToTube.entrySet()) {
            expect(mockBarcodedTubeDao.findByBarcode(labelToVessel.getKey())).andReturn(labelToVessel.getValue());
        }
        picoDispositionActionBean.setBarcodedTubeDao(mockBarcodedTubeDao);
        replay(mockBarcodedTubeDao);
        // Bad barcode, bad position
        picoDispositionActionBean.setRackScanJson("{\"scans\":[{\"position\":\"A01\",\"barcode\":\"1194638817\"},{\"position\":\"A02\",\"barcode\":\"1194638806\"},{\"position\":\"A08\",\"barcode\":\"1194638519\"},{\"position\":\"H01\",\"barcode\":\"1194638853\"}]}");

        String resultJson = picoDispositionActionBean.getListItemsJson();

        ObjectMapper mapper = new ObjectMapper();
        List<PicoDispositionActionBean.ListItem> items = null;
        try {
            items = mapper.readValue(resultJson, new TypeReference<List<PicoDispositionActionBean.ListItem>>() {
            });
            Assert.assertEquals(items.size(), 4, "Expected 4 metric list item");
        } catch (Exception e) {
            Assert.fail("JSON parsing exception thrown", e);
        }

        for (PicoDispositionActionBean.ListItem item : items) {
            LabMetric metric = mapBarcodeToMetric.get(item.getBarcode());
            LabMetricDecision metricDecision = metric.getLabMetricDecision();

            PicoDispositionActionBean.DestinationRackType rackType = null;
            switch (metricDecision.getReworkDisposition()) {
                case BAD_TRIP_READS:
                    rackType = PicoDispositionActionBean.DestinationRackType.REPEAT;
                    break;
                case NORM_IN_TUBE:
                    rackType = PicoDispositionActionBean.DestinationRackType.NORM;
                    break;
                case UNDILUTED:
                    rackType = PicoDispositionActionBean.DestinationRackType.UNDILUTED;
                    break;
                case PASS:
                    rackType = PicoDispositionActionBean.DestinationRackType.NONE;
                    break;
                default:
                    rackType = null;
            }
            Assert.assertEquals(item.getDestinationRackType(), rackType);
            Assert.assertEquals(item.getPosition(), metric.getVesselPosition().name());
            Assert.assertEquals(item.getSampleId(), metric.getLabVessel().getMercurySamples().iterator().next().getSampleKey());
            Assert.assertTrue(item.getVolume().compareTo(metric.getLabVessel().getVolume()) == 0);
            Assert.assertTrue(item.getConcentration().compareTo(metric.getValue()) == 0);
        }
    }
}
