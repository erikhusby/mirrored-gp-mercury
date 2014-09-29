package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import freemarker.template.utility.StringUtil;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Row;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.doSectionTransfer;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes.RackType.HamiltonSampleCarrier24;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes.RackType.Matrix96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;

/**
 * Tests the action bean.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class FingerprintingSpreadsheetActionBeanTest extends TestCase {
    private Map<VesselPosition, BarcodedTube> initialPicoMap;
    private Map<VesselPosition, BarcodedTube> rearrayMap;
    private TubeFormation initialPicoRack;
    private TubeFormation rearrayRack;
    private StaticPlate initalPicoPlate;
    private StaticPlate fpPlate;
    private static final BigDecimal BD_60 = new BigDecimal("60.0");

    public void setUp() throws Exception {
        super.setUp();
        Date now = new Date();
        Date later = new Date(now.getTime() + 10000L);

        // Makes a Matrix96 rack with 90 tubes.
        initialPicoMap = new HashMap<>();
        int i = 0;
        for (VesselPosition position : Matrix96.getVesselGeometry().getVesselPositions()) {
            String id = String.valueOf(100 + i);
            BarcodedTube tube = new BarcodedTube("tube" + id);
            tube.addSample(new MercurySample("SM-" + id, MercurySample.MetadataSource.MERCURY));
            // The pico quants are done on a plate derived from these tubes but the metric is put on the tubes.
            tube.addMetric(new LabMetric(new BigDecimal(1 + i), LabMetric.MetricType.INITIAL_PICO,
                    LabMetric.LabUnit.NG_PER_UL, position.name(), now));
            initialPicoMap.put(position, tube);
            ++i;
            if (i > 89) {
                break;
            }
        }
        initialPicoRack = new TubeFormation(initialPicoMap, Matrix96);
        // Puts a volume on the initial pico plate transfer event, which should not be picked up.
        initalPicoPlate = new StaticPlate("initialPicoPlate", Eppendorf96);
        LabEvent initPicoTransfer = doSectionTransfer(initialPicoRack, initalPicoPlate);
        initPicoTransfer.addMetadata(new LabEventMetadata(LabEventMetadata.LabEventMetadataType.Volume, "11.1"));

        // The rearray rack contains tubes of initial pico'd samples, diluted aliquots, and NA12878 filler.
        rearrayMap = new HashMap<>();
        i = 0;
        for (VesselPosition position : Matrix96.getVesselGeometry().getVesselPositions()) {
            BarcodedTube rearrayTube = initialPicoMap.get(position);
            // Dilutes the high concentration tubes and puts an FP Pico metric on the new tubes.
            if (rearrayTube != null && rearrayTube.getMetrics().iterator().next().getValue().compareTo(BD_60) >= 0) {
                String id = String.valueOf(200 + i);
                rearrayTube = new BarcodedTube("tube" + id);
                BigDecimal conc = new BigDecimal("5." + StringUtil.leftPad(String.valueOf(i), 3, '0'));
                rearrayTube.addMetric(new LabMetric(conc, LabMetric.MetricType.FINGERPRINT_PICO,
                        LabMetric.LabUnit.NG_PER_UL, position.name(), later));
            }
            if (rearrayTube == null) {
                // Fills empty positions with NA12878.
                String id = String.valueOf(300 + i);
                rearrayTube = new BarcodedTube("tube" + id);
                rearrayTube.addMetric(new LabMetric(BigDecimal.ZERO, LabMetric.MetricType.FINGERPRINT_PICO,
                        LabMetric.LabUnit.NG_PER_UL, position.name(), later));
            }
            rearrayMap.put(position, rearrayTube);
            ++i;
        }
        rearrayRack = new TubeFormation(rearrayMap, Matrix96);

        // Makes the FP plate as a "stamp" (section transfer) of the rearray rack.
        // Adds a volume metadata on the transfer event.
        fpPlate = new StaticPlate("fpPlate", Eppendorf96);
        LabEvent rearrayTransfer = doSectionTransfer(rearrayRack, fpPlate);
        rearrayTransfer.addMetadata(new LabEventMetadata(LabEventMetadata.LabEventMetadataType.Volume, "22.2"));
    }

    public void testBarcodeSubmit() throws Exception {
        // Generates a spreadsheet.
        FingerprintingSpreadsheetActionBean actionBean = new FingerprintingSpreadsheetActionBean();
        actionBean.staticPlate = fpPlate;
        actionBean.barcodeSubmit();

        // Collects the participant ids from the first sheet.
        List<String> participantIds = new ArrayList<>();
        for (Row row : actionBean.workbook.getSheet("Participants")) {
            // Skips the header row and last row that has nulls.
            if (row.getCell(0) == null || row.getCell(0).getStringCellValue().startsWith("Participant")) {
                continue;
            }
            participantIds.add(row.getCell(0).getStringCellValue());
        }
        Iterator<String> participantIdsIter = participantIds.iterator();

        // Checks the data on the second sheet.
        Set<String> tubePrefixes = new HashSet<>();
        for (Row row : actionBean.workbook.getSheet("Plate")) {
            // Skips the header row and last row that has nulls.
            if (row.getCell(0) == null || row.getCell(0).getStringCellValue().startsWith("Well Position")) {
                continue;
            }

            String position = row.getCell(0).getStringCellValue();
            String participantId = row.getCell(1).getStringCellValue();
            String rootId = row.getCell(2).getStringCellValue();
            String sampleId = row.getCell(3).getStringCellValue();
            String volume = row.getCell(4).getStringCellValue();
            String conc = row.getCell(5).getStringCellValue();

            Assert.assertEquals(participantIdsIter.next(), participantId);
            Assert.assertEquals(participantId, rootId);
            Assert.assertEquals(participantId, sampleId);
            Assert.assertEquals("22.2", volume);

            BarcodedTube tube = rearrayMap.get(VesselPosition.getByName(position));
            tubePrefixes.add(tube.getLabel().substring(0, 5));
            Assert.assertEquals(1, tube.getMetrics().size());
            BigDecimal value = tube.getMetrics().iterator().next().getValue();
            Assert.assertEquals(conc, value.toString());
        }

        Assert.assertTrue(tubePrefixes.remove("tube1"));
        Assert.assertTrue(tubePrefixes.remove("tube2"));
        Assert.assertTrue(tubePrefixes.remove("tube3"));
        Assert.assertTrue(CollectionUtils.isEmpty(tubePrefixes));

        // Checks the row counts.
        Assert.assertFalse(participantIdsIter.hasNext());
        Assert.assertEquals(Matrix96.getVesselGeometry().getVesselPositions().length, participantIds.size());
    }

    public void test48SampleCount() throws Exception {
        final int fullPlateSampleCount = 96;
        final int halfPlateSampleCount = 48;
        // Makes a half-sized fp plate.
        List<VesselPosition> positions = new ArrayList<>(rearrayMap.keySet());
        for (VesselPosition position : positions.subList(halfPlateSampleCount, fullPlateSampleCount)) {
            rearrayMap.remove(position);
        }
        Assert.assertEquals(halfPlateSampleCount, rearrayMap.size());
        rearrayRack = new TubeFormation(rearrayMap, Matrix96);
        fpPlate = new StaticPlate("fpPlate48", Eppendorf96);
        LabEvent rearrayTransfer = doSectionTransfer(rearrayRack, fpPlate);
        rearrayTransfer.addMetadata(new LabEventMetadata(LabEventMetadata.LabEventMetadataType.Volume, "33.3"));

        FingerprintingSpreadsheetActionBean actionBean = new FingerprintingSpreadsheetActionBean();
        actionBean.setContext(new CoreActionBeanContext());
        actionBean.staticPlate = fpPlate;
        actionBean.barcodeSubmit();

        // No validation errors.
        Assert.assertEquals(0, actionBean.getContext().getValidationErrors().size());
        // 48 sample rows in the spreadsheet.
        int rowCount = 0;
        for (Row row : actionBean.workbook.getSheet("Plate")) {
            // Skips the header row and last row that has nulls.
            if (row.getCell(0) == null || row.getCell(0).getStringCellValue().startsWith("Well Position")) {
                continue;
            }
            ++rowCount;
        }
        Assert.assertEquals(halfPlateSampleCount, rowCount);
    }

    public void test95SampleCount() throws Exception {

        // Removes a tube, and the action bean should make a validation error.
        VesselPosition position = rearrayMap.keySet().iterator().next();
        rearrayMap.remove(position);
        rearrayRack = new TubeFormation(rearrayMap, Matrix96);
        fpPlate = new StaticPlate("fpPlateBad", Eppendorf96);
        LabEvent rearrayTransfer = doSectionTransfer(rearrayRack, fpPlate);
        rearrayTransfer.addMetadata(new LabEventMetadata(LabEventMetadata.LabEventMetadataType.Volume, "33.3"));

        FingerprintingSpreadsheetActionBean actionBean = new FingerprintingSpreadsheetActionBean();
        actionBean.setContext(new CoreActionBeanContext());
        actionBean.staticPlate = fpPlate;
        actionBean.barcodeSubmit();
        Assert.assertEquals(1, actionBean.getContext().getValidationErrors().size());
        Assert.assertNull(actionBean.workbook);
    }
}
