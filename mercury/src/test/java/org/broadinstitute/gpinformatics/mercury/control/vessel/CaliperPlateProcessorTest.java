package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Test parsing of Caliper file.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class CaliperPlateProcessorTest {

    public static final String CALIPER_OUTPUT_CSV = "CaliperOutput.csv";
    public static final String PLATE_BARCODE = "000010553069";

    @Test
    public void testBasic() {
        InputStream testSpreadSheet = VarioskanParserTest.getSpreadsheet(CALIPER_OUTPUT_CSV);
        try {
            CaliperPlateProcessor caliperPlateProcessor = new CaliperPlateProcessor();
            CaliperPlateProcessor.CaliperRun run  = caliperPlateProcessor.parse(testSpreadSheet);
            Assert.assertEquals(run.getPlateWellResultMarkers().size(), 96);
            Assert.assertFalse(caliperPlateProcessor.getMessageCollection().hasErrors());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                testSpreadSheet.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    public void testCreateRun() {
        InputStream testSpreadSheet = VarioskanParserTest.getSpreadsheet(CALIPER_OUTPUT_CSV);
        try {
            VesselEjb vesselEjb = new VesselEjb();

            CaliperPlateProcessor caliperPlateProcessor = new CaliperPlateProcessor();
            CaliperPlateProcessor.CaliperRun caliperRun = caliperPlateProcessor.parse(testSpreadSheet);

            Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();

            Map<VesselPosition, BarcodedTube> mapPositionToTube = buildTubesAndTransfers(mapBarcodeToPlate,
                    PLATE_BARCODE, "");

            MessageCollection messageCollection = new MessageCollection();
            Set<LabEventMetadata> metadata = new HashSet<>();
            LabMetricRun labMetricRun = vesselEjb.createRNACaliperRunDaoFree(LabMetric.MetricType.INITIAL_RNA_CALIPER,
                    caliperRun, mapBarcodeToPlate, 101L, messageCollection, metadata).getLeft();
            Assert.assertFalse(messageCollection.hasErrors());
            Assert.assertEquals(labMetricRun.getLabMetrics().size(), 2 * 96);
            Assert.assertEquals(mapPositionToTube.get(VesselPosition.A01).getMetrics().iterator().next().getValue(),
                    new BigDecimal("9.80"));
            Assert.assertEquals(mapPositionToTube.get(VesselPosition.A02).getMetrics().iterator().next().getValue(),
                    new BigDecimal("10.00"));
            Assert.assertEquals(mapPositionToTube.get(VesselPosition.A03).getMetrics().iterator().next().getValue(),
                    new BigDecimal("0.00")); //NA's are 0
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                testSpreadSheet.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static Map<VesselPosition, BarcodedTube> buildTubesAndTransfers(Map<String, StaticPlate> mapBarcodeToPlate,
                                                                           String plate1Barcode, String tubePrefix) {
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        for (VesselPosition vesselPosition : RackOfTubes.RackType.Matrix96.getVesselGeometry().getVesselPositions()) {
            BarcodedTube barcodedTube = new BarcodedTube(tubePrefix + vesselPosition.toString());
            barcodedTube.setVolume(new BigDecimal("75"));
            mapPositionToTube.put(vesselPosition, barcodedTube);
        }

        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);

        StaticPlate staticPlate1 = new StaticPlate(plate1Barcode, StaticPlate.PlateType.Eppendorf384);
        mapBarcodeToPlate.put(staticPlate1.getLabel(), staticPlate1);

        LabEvent labEvent1 = new LabEvent(LabEventType.RNA_CALIPER_SETUP, new Date(), "BATMAN", 1L, 101L,
                "Bravo");
        labEvent1.getSectionTransfers().add(new SectionTransfer(tubeFormation.getContainerRole(), SBSSection.ALL96,
                null, staticPlate1.getContainerRole(), SBSSection.P384_96TIP_1INTERVAL_A1, null, labEvent1));

        return mapPositionToTube;
    }
}