package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import freemarker.template.utility.StringUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Row;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.FingerprintingPlateProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ControlReagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
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
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes.RackType.Matrix96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;

/**
 * Tests the action bean.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class FingerprintingSpreadsheetActionBeanTest {
    private Map<VesselPosition, BarcodedTube> initialPicoMap;
    private Map<VesselPosition, BarcodedTube> rearrayMap;
    private TubeFormation initialPicoRack;
    private TubeFormation rearrayRack;
    private StaticPlate initalPicoPlate;
    private StaticPlate fpPlate;
    private ControlDao controlDao = EasyMock.createNiceMock(ControlDao.class);
    private FingerprintingSpreadsheetActionBean actionBean;
    // These controls do not have to match the actual control names found in the Mercury db.
    private Control negativeControl = new Control("WATER_CONTROL", Control.ControlType.NEGATIVE);
    private Control positiveControl = new Control("NA12878", Control.ControlType.POSITIVE);
    private ControlReagent fillerReagent = new ControlReagent("NA12878", "nolot", new Date(),
            new Control("NA12878", Control.ControlType.POSITIVE));

    @BeforeMethod
    public void setUp() throws Exception {
        Date now = new Date();
        Date later = new Date(now.getTime() + 10000L);

        // Makes an initial pico Matrix96 rack with 1 positive control, 1 negative control, and 90 samples.
        // Samples above SM-159 will be high concentration and will be FP pico'd.
        initialPicoMap = new HashMap<>();
        int i = 0;
        for (VesselPosition position : Matrix96.getVesselGeometry().getVesselPositions()) {
            String idValue = String.valueOf(100 + i);
            MercurySample mercurySample = new MercurySample("SM-" + idValue, MercurySample.MetadataSource.MERCURY);
            String collaboratorSampleId =
                    (i == 0) ? negativeControl.getCollaboratorSampleId() :
                    (i == 1) ? positiveControl.getCollaboratorSampleId() :
                    "Patient-" + idValue;
            mercurySample.addMetadata(collaboratorSampleIdMetadata(collaboratorSampleId));
            BarcodedTube tube = new BarcodedTube("tube" + idValue);
            tube.addSample(mercurySample);
            tube.addMetric(new LabMetric(new BigDecimal(i), LabMetric.MetricType.INITIAL_PICO,
                    LabMetric.LabUnit.NG_PER_UL, position.name(), now));
            initialPicoMap.put(position, tube);
            ++i;
        }
        initialPicoRack = new TubeFormation(initialPicoMap, Matrix96);

        // Puts a volume on the initial pico plate transfer event, which should not be picked up.
        initalPicoPlate = new StaticPlate("initialPicoPlate", Eppendorf96);
        LabEvent initPicoTransfer = doSectionTransfer(initialPicoRack, initalPicoPlate);
        initPicoTransfer.addMetadata(new LabEventMetadata(LabEventMetadata.LabEventMetadataType.Volume, "11.1"));

        // Makes a rearray rack with 1 positive control, 1 negative control, 58 Initial pico quant samples,
        // 30 FP pico quant aliquots, and 6 tubes of NA12878 filler.
        rearrayMap = new HashMap<>();
        i = 0;
        for (VesselPosition position : Matrix96.getVesselGeometry().getVesselPositions()) {
            BarcodedTube rearrayTube;
            if (i < 60) {
                rearrayTube = initialPicoMap.get(position);
            } else if (i < 91) {
                // Dilutes the high concentration tubes and puts an FP Pico metric on the new tubes.
                rearrayTube = new BarcodedTube("tube" + (200 + i));
                BarcodedTube initialTube = initialPicoMap.get(position);
                rearrayTube.addAllSamples(initialTube.getMercurySamples());
                BigDecimal conc = new BigDecimal("5." + StringUtil.leftPad(String.valueOf(i), 3, '0'));
                rearrayTube.addMetric(new LabMetric(conc, LabMetric.MetricType.FINGERPRINT_PICO,
                        LabMetric.LabUnit.NG_PER_UL, position.name(), later));
            } else {
                // Fills empty positions with non-control NA12878, which has no sample, no SM-id, no quant value.
                rearrayTube = new BarcodedTube("tube" + (300 + i));
                rearrayTube.addReagent(fillerReagent);
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

        actionBean = new FingerprintingSpreadsheetActionBean();
        actionBean.setContext(new CoreActionBeanContext());
        actionBean.setControlDao(controlDao);

        EasyMock.reset(controlDao);
    }

    public void testBarcodeSubmit() throws Exception {
        EasyMock.expect(controlDao.findBySampleId(negativeControl.getCollaboratorSampleId())).andReturn(negativeControl).anyTimes();
        EasyMock.expect(controlDao.findBySampleId(positiveControl.getCollaboratorSampleId())).andReturn(positiveControl).anyTimes();
        EasyMock.expect(controlDao.findBySampleId(EasyMock.anyObject(String.class))).andReturn(null).anyTimes();
        EasyMock.replay(controlDao);

        // Generates a spreadsheet.
        actionBean.staticPlate = fpPlate;
        actionBean.barcodeSubmit();

        // Collects the participant ids from the first sheet.
        List<String> participantIds = new ArrayList<>();
        int smCount = 0;
        for (Row row : actionBean.workbook.getSheet("Participants")) {
            // Skips the header row and last row that has nulls.
            if (row.getCell(0) == null || row.getCell(0).getStringCellValue().startsWith("Participant")) {
                continue;
            }
            String participantId = row.getCell(0).getStringCellValue();
            if (participantId.startsWith("SM-")) {
                ++smCount;
            }
            participantIds.add(participantId);
            Assert.assertEquals("0", row.getCell(1).getStringCellValue());
        }
        Assert.assertTrue(participantIds.contains(FingerprintingPlateProcessor.NEGATIVE_CONTROL));
        Assert.assertTrue(participantIds.contains(FingerprintingPlateProcessor.NA12878));
        Assert.assertEquals(90, smCount);

        // Checks the data on the second sheet against the tubes in rearray rack.
        Set<String> tubePrefixes = new HashSet<>();
        Iterator<String> participantIdsIter = participantIds.iterator();
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

            // Checks the participantId matches across sheets, and column consistency.
            Assert.assertEquals(participantIdsIter.next(), participantId);
            Assert.assertEquals(participantId, rootId);
            Assert.assertEquals(participantId, sampleId);
            Assert.assertEquals("22.2", volume);

            // The tube to check against.
            BarcodedTube tube = rearrayMap.get(VesselPosition.getByName(position));
            tubePrefixes.add(tube.getLabel().substring(0, 5));
            if (tube.getLabel().substring(0, 5).endsWith("3")) {
                Assert.assertEquals(FingerprintingPlateProcessor.NA12878, participantId);
                Assert.assertEquals(0, tube.getMetrics().size());
                Assert.assertEquals(conc, "0");
            } else {
                Assert.assertEquals(1, tube.getMetrics().size());
                Assert.assertEquals(conc, tube.getMetrics().iterator().next().getValue().toString());
            }

        }

        Assert.assertTrue(tubePrefixes.remove("tube1"));
        Assert.assertTrue(tubePrefixes.remove("tube2"));
        Assert.assertTrue(tubePrefixes.remove("tube3"));
        Assert.assertTrue(CollectionUtils.isEmpty(tubePrefixes));

        // Checks row counts.
        Assert.assertFalse(participantIdsIter.hasNext());
        Assert.assertEquals(Matrix96.getVesselGeometry().getVesselPositions().length, participantIds.size());

        EasyMock.verify(controlDao);
    }

    public void test48SampleCount() throws Exception {
        EasyMock.expect(controlDao.findBySampleId(EasyMock.anyObject(String.class))).andReturn(null).anyTimes();
        EasyMock.replay(controlDao);

        final int fullPlateSampleCount = 96;
        final int halfPlateSampleCount = 48;
        // Makes a half-sized fp plate from the last half of the full plate.
        List<VesselPosition> positions = new ArrayList<>(rearrayMap.keySet());
        for (VesselPosition position : positions.subList(halfPlateSampleCount, fullPlateSampleCount)) {
            rearrayMap.remove(position);
        }
        Assert.assertEquals(halfPlateSampleCount, rearrayMap.size());
        rearrayRack = new TubeFormation(rearrayMap, Matrix96);
        fpPlate = new StaticPlate("fpPlate48", Eppendorf96);
        LabEvent rearrayTransfer = doSectionTransfer(rearrayRack, fpPlate);
        // Provide no volume information as lab event metadata.

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
            String volume = row.getCell(4).getStringCellValue();
            Assert.assertEquals("0", volume);
            ++rowCount;
        }
        Assert.assertEquals(halfPlateSampleCount, rowCount);
        EasyMock.verify(controlDao);
    }

    public void test95SampleCount() throws Exception {
        EasyMock.expect(controlDao.findBySampleId(EasyMock.anyObject(String.class))).andReturn(null).anyTimes();
        EasyMock.replay(controlDao);

        // Removes two tubes and the action bean should make a validation error.
        rearrayMap.remove(VesselPosition.A01);
        rearrayMap.remove(VesselPosition.A02);
        rearrayRack = new TubeFormation(rearrayMap, Matrix96);
        fpPlate = new StaticPlate("fpPlateBad", Eppendorf96);
        LabEvent rearrayTransfer = doSectionTransfer(rearrayRack, fpPlate);
        rearrayTransfer.addMetadata(new LabEventMetadata(LabEventMetadata.LabEventMetadataType.Volume, "33.3"));

        actionBean.staticPlate = fpPlate;
        actionBean.barcodeSubmit();
        Assert.assertEquals(1, actionBean.getContext().getValidationErrors().size());
        Assert.assertNull(actionBean.workbook);
        EasyMock.verify(controlDao);
    }

    /** Returns sample metadata with the specified collaborator sample id. */
    private Set<Metadata> collaboratorSampleIdMetadata(final String collaboratorSampleId) {
        return new HashSet<Metadata>() {{
            add(new Metadata(Metadata.Key.SAMPLE_ID, collaboratorSampleId));
        }};
    }

}
