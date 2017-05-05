package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import freemarker.template.utility.StringUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.FingerprintingPlateFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
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

    private static final int FULL_RACK_COUNT = Matrix96.getVesselGeometry().getVesselPositions().length;
    private static final BigDecimal BD_60 = new BigDecimal("60.0");

    @BeforeMethod
    public void setUp() throws Exception {
        Date now = new Date();
        Date later = new Date(now.getTime() + 10000L);

        // Makes an initial pico Matrix96 rack with 1 positive control, 1 negative control, and 94 samples.
        // Samples above SM-159 will be high concentration and will be FP pico'd.
        initialPicoMap = new HashMap<>();
        for (int i = 0; i < FULL_RACK_COUNT; ++i) {
            VesselPosition position = Matrix96.getVesselGeometry().getVesselPositions()[i];
            String idValue = String.valueOf(100 + i);
            MercurySample mercurySample = new MercurySample("SM-" + idValue, MercurySample.MetadataSource.MERCURY);
            String collaboratorSampleId =
                    (i == 0) ? negativeControl.getCollaboratorParticipantId() :
                    (i == 1) ? positiveControl.getCollaboratorParticipantId() :
                    "Patient-" + idValue;
            mercurySample.addMetadata(collaboratorSampleIdMetadata(collaboratorSampleId));
            BarcodedTube tube = new BarcodedTube("tube" + idValue);
            tube.addSample(mercurySample);
            tube.addMetric(new LabMetric(new BigDecimal(i), LabMetric.MetricType.INITIAL_PICO,
                    LabMetric.LabUnit.NG_PER_UL, position.name(), now));
            initialPicoMap.put(position, tube);
        }
        initialPicoRack = new TubeFormation(initialPicoMap, Matrix96);

        // Puts a volume on the initial pico plate transfer event, which should not be picked up.
        initalPicoPlate = new StaticPlate("initialPicoPlate", Eppendorf96);
        LabEvent initPicoTransfer = doSectionTransfer(initialPicoRack, initalPicoPlate);
        initPicoTransfer.addMetadata(new LabEventMetadata(LabEventMetadata.LabEventMetadataType.Volume, "11.1"));

        // Dilutes the high concentration tubes and puts an FP Pico metric on the new aliquot tubes.
        Map<VesselPosition, BarcodedTube> aliquotSourceMap = new HashMap<>();
        Map<VesselPosition, BarcodedTube> aliquotDestMap = new HashMap<>();
        int aliquotPositionIdx = 0;
        for (int i = 60; i < 91; ++i) {
            VesselPosition picoPosition = Matrix96.getVesselGeometry().getVesselPositions()[i];
            VesselPosition aliquotPosition = Matrix96.getVesselGeometry().getVesselPositions()[aliquotPositionIdx];

            BarcodedTube initialTube = initialPicoMap.get(picoPosition);
            Assert.assertEquals(initialTube.getMetrics().size(), 1);
            Assert.assertTrue(BD_60.compareTo(initialTube.getMetrics().iterator().next().getValue()) <= 0);

            aliquotSourceMap.put(aliquotPosition, initialTube);

            BarcodedTube aliquotTube = new BarcodedTube("tube" + (200 + i));
            aliquotTube.addAllSamples(initialTube.getMercurySamples());
            BigDecimal conc = new BigDecimal("5." + StringUtil.leftPad(String.valueOf(i), 3, '0'));
            aliquotTube.addMetric(new LabMetric(conc, LabMetric.MetricType.FINGERPRINT_PICO,
                    LabMetric.LabUnit.NG_PER_UL, aliquotPosition.name(), later));

            aliquotDestMap.put(aliquotPosition, aliquotTube);

            ++aliquotPositionIdx;
        }
        TubeFormation aliquotSourceRack = new TubeFormation(aliquotSourceMap, Matrix96);
        TubeFormation aliquotDestRack = new TubeFormation(aliquotDestMap, Matrix96);
        LabEvent aliquotTransfer = doSectionTransfer(aliquotSourceRack, aliquotDestRack);

        // Makes a rearray rack with 1 positive control, 1 negative control, 58 Initial pico quant samples,
        // 30 FP pico quant aliquots, and 6 tubes of NA12878 filler.
        rearrayMap = new HashMap<>();
        aliquotPositionIdx = 0;
        for (int i = 0; i < FULL_RACK_COUNT; ++i) {
            VesselPosition picoPosition = Matrix96.getVesselGeometry().getVesselPositions()[i];
            VesselPosition aliquotPosition = Matrix96.getVesselGeometry().getVesselPositions()[aliquotPositionIdx];
            VesselPosition rearrayPosition = Matrix96.getVesselGeometry().getVesselPositions()[i];
            BarcodedTube rearrayTube;
            if (i < 60) {
                rearrayTube = initialPicoMap.get(picoPosition);
            } else if (i < 91) {
                rearrayTube = aliquotDestMap.get(aliquotPosition);
                Assert.assertNotNull(rearrayTube, "at idx " + i + " (aliquot idx " + aliquotPositionIdx + ")");
                ++aliquotPositionIdx;
            } else {
                // Fills empty positions with non-control NA12878, which has no sample, no SM-id, no quant value.
                rearrayTube = new BarcodedTube("tube" + (300 + i));
                rearrayTube.addReagent(fillerReagent);
            }
            rearrayMap.put(rearrayPosition, rearrayTube);
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
        EasyMock.expect(controlDao.findByCollaboratorParticipantId(negativeControl.getCollaboratorParticipantId())).andReturn(negativeControl).anyTimes();
        EasyMock.expect(controlDao.findByCollaboratorParticipantId(positiveControl.getCollaboratorParticipantId())).andReturn(positiveControl).anyTimes();
        EasyMock.expect(controlDao.findByCollaboratorParticipantId(EasyMock.anyObject(String.class))).andReturn(null).anyTimes();
        EasyMock.replay(controlDao);

        // Generates a spreadsheet.
        actionBean.setStaticPlate(fpPlate);
        actionBean.barcodeSubmit();

        // Should have no validation errors and a workbook.
        Assert.assertEquals(actionBean.getErrorMessages().size(), 0, StringUtils.join(actionBean.getErrorMessages(), "; "));
        Assert.assertNotNull(actionBean.getWorkbook());

        // Collects the participant ids from the first sheet.
        List<String> participantIds = new ArrayList<>();
        int smCount = 0;
        for (Row row : actionBean.getWorkbook().getSheet("Participants")) {
            // Skips the header row and last row that has nulls.
            if (row.getCell(0) == null || row.getCell(0).getStringCellValue().startsWith("Participant")) {
                continue;
            }
            String participantId = row.getCell(0).getStringCellValue();
            if (participantId.startsWith("SM-")) {
                ++smCount;
                participantIds.add(participantId);
            } else if (participantId.startsWith("negative")) {
                Assert.fail("Negative controls shouldn't be put into participant sheet");
            } else if (participantId.startsWith("NA12878")) {
                Assert.fail("Positive controls shouldn't be put into participant sheet");
            }
            Assert.assertEquals(row.getCell(1).getStringCellValue(), "0");
        }
        Assert.assertFalse(participantIds.contains(FingerprintingPlateFactory.NEGATIVE_CONTROL));
        Assert.assertFalse(participantIds.contains(FingerprintingPlateFactory.NA12878));
        Assert.assertEquals(90, smCount);

        // Checks the data on the second sheet against the tubes in rearray rack.
        Set<String> tubePrefixes = new HashSet<>();
        Iterator<String> participantIdsIter = participantIds.iterator();
        for (Row row : actionBean.getWorkbook().getSheet("Plate")) {
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
            if (!participantId.startsWith("negative") && !participantId.startsWith("NA12878")) {
                Assert.assertEquals(participantId, participantIdsIter.next());
            }
            Assert.assertEquals(rootId, participantId);
            Assert.assertEquals(sampleId, participantId);
            Assert.assertEquals(volume, "22.2");

            // The tube to check against.
            BarcodedTube tube = rearrayMap.get(VesselPosition.getByName(position));
            tubePrefixes.add(tube.getLabel().substring(0, 5));
            if (tube.getLabel().substring(0, 5).endsWith("3")) {
                Assert.assertEquals(FingerprintingPlateFactory.NA12878, participantId);
                Assert.assertEquals(0, tube.getMetrics().size());
                Assert.assertEquals(conc, "20");
            } else {
                Assert.assertEquals(tube.getMetrics().size(), 1);
                Assert.assertEquals(tube.getMetrics().iterator().next().getValue().toString(), conc);
            }

        }

        Assert.assertTrue(tubePrefixes.remove("tube1"));
        Assert.assertTrue(tubePrefixes.remove("tube2"));
        Assert.assertTrue(tubePrefixes.remove("tube3"));
        Assert.assertTrue(CollectionUtils.isEmpty(tubePrefixes));

        // Checks row counts.
        Assert.assertFalse(participantIdsIter.hasNext());
        Assert.assertEquals(participantIds.size(), 90);

        EasyMock.verify(controlDao);
    }

    public void testRework() throws Exception {
        EasyMock.expect(controlDao.findByCollaboratorParticipantId(EasyMock.anyObject(String.class))).andReturn(null).anyTimes();
        EasyMock.replay(controlDao);

        final int fullPlateSampleCount = 96;
        final int halfPlateSampleCount = 48;
        // Makes a half-sized fp plate from the last half of the full plate.
        List<VesselPosition> positions = new ArrayList<>(rearrayMap.keySet());
        for (VesselPosition position : positions.subList(halfPlateSampleCount, fullPlateSampleCount)) {
            rearrayMap.remove(position);
        }
        Assert.assertEquals(rearrayMap.size(), halfPlateSampleCount);
        rearrayRack = new TubeFormation(rearrayMap, Matrix96);
        fpPlate = new StaticPlate("fpPlate48", Eppendorf96);
        LabEvent setupEvent = doSectionTransfer(LabEventType.FINGERPRINTING_PLATE_SETUP, rearrayRack, fpPlate);
        LabEvent pondRegEvent = doSectionTransfer(LabEventType.POND_REGISTRATION, rearrayRack, fpPlate);
        LabEvent reworkEvent = doSectionTransfer(LabEventType.FINGERPRINTING_PLATE_SETUP, rearrayRack, fpPlate);
        // Provide no volume information as lab event metadata.

        actionBean.setStaticPlate(fpPlate);
        actionBean.barcodeSubmit();

        Assert.assertEquals(actionBean.getErrorMessages().size(), 0, StringUtils.join(actionBean.getErrorMessages(), "; "));
        Assert.assertNotNull(actionBean.getWorkbook());

        // 48 sample rows in the spreadsheet.
        int rowCount = 0;
        for (Row row : actionBean.getWorkbook().getSheet("Participants")) {
            // Skips the header row and last row that has nulls.
            if (row.getCell(0) == null || row.getCell(0).getStringCellValue().startsWith("Participant")) {
                continue;
            }
            String participantId = row.getCell(0).getStringCellValue();
            if (participantId.startsWith("SM-")) {
                Assert.fail("Reworked samples shouldn't be put into participant sheet");
            } else if (participantId.startsWith("negative")) {
                Assert.fail("Negative controls shouldn't be put into participant sheet");
            } else if (participantId.startsWith("NA12878")) {
                Assert.fail("Positive controls shouldn't be put into participant sheet");
            } else {
                rowCount++;
            }
        }
        // Reworks, NA12878, and negative controls should be excluded which is the whole set after pond transfer
        Assert.assertEquals(rowCount, 0);
        EasyMock.verify(controlDao);
    }

    public void test48SampleCount() throws Exception {
        EasyMock.expect(controlDao.findByCollaboratorParticipantId(EasyMock.anyObject(String.class))).andReturn(null).anyTimes();
        EasyMock.replay(controlDao);

        final int fullPlateSampleCount = 96;
        final int halfPlateSampleCount = 48;
        // Makes a half-sized fp plate from the last half of the full plate.
        List<VesselPosition> positions = new ArrayList<>(rearrayMap.keySet());
        for (VesselPosition position : positions.subList(halfPlateSampleCount, fullPlateSampleCount)) {
            rearrayMap.remove(position);
        }
        Assert.assertEquals(rearrayMap.size(), halfPlateSampleCount);
        rearrayRack = new TubeFormation(rearrayMap, Matrix96);
        fpPlate = new StaticPlate("fpPlate48", Eppendorf96);
        LabEvent rearrayTransfer = doSectionTransfer(rearrayRack, fpPlate);
        // Provide no volume information as lab event metadata.

        actionBean.setStaticPlate(fpPlate);
        actionBean.barcodeSubmit();

        // Should have no errors and a workbook.
        Assert.assertEquals(actionBean.getErrorMessages().size(), 0, StringUtils.join(actionBean.getErrorMessages(), "; "));
        Assert.assertNotNull(actionBean.getWorkbook());

        // 48 sample rows in the spreadsheet.
        int rowCount = 0;
        for (Row row : actionBean.getWorkbook().getSheet("Plate")) {
            // Skips the header row and last row that has nulls.
            if (row.getCell(0) == null || row.getCell(0).getStringCellValue().startsWith("Well Position")) {
                continue;
            }
            String volume = row.getCell(4).getStringCellValue();
            Assert.assertEquals(volume,"0");
            ++rowCount;
        }
        Assert.assertEquals(rowCount, halfPlateSampleCount);
        EasyMock.verify(controlDao);
    }

    public void test46SampleCount() throws Exception {
        EasyMock.expect(controlDao.findByCollaboratorParticipantId(EasyMock.anyObject(String.class))).andReturn(null)
                .anyTimes();
        EasyMock.replay(controlDao);

        // Removes two tubes and the action bean should make a validation error.
        List<VesselPosition> positions = new ArrayList<>(rearrayMap.keySet());
        for (VesselPosition position : positions.subList(46, 96)) {
            rearrayMap.remove(position);
        }
        rearrayRack = new TubeFormation(rearrayMap, Matrix96);
        fpPlate = new StaticPlate("fpPlateBad", Eppendorf96);
        LabEvent rearrayTransfer = doSectionTransfer(rearrayRack, fpPlate);
        rearrayTransfer.addMetadata(new LabEventMetadata(LabEventMetadata.LabEventMetadataType.Volume, "33.3"));

        actionBean.setStaticPlate(fpPlate);
        actionBean.barcodeSubmit();
        // Should have no creation errors, one validation error, and no workbook.
        Assert.assertEquals(actionBean.getErrorMessages().size(), 0, StringUtils.join(actionBean.getErrorMessages(), "; "));
        Assert.assertEquals(actionBean.getContext().getValidationErrors().size(), 1);
        Assert.assertNull(actionBean.getWorkbook());
        EasyMock.verify(controlDao);
    }

    /** Returns sample metadata with the specified collaborator sample id. */
    private Set<Metadata> collaboratorSampleIdMetadata(final String collaboratorSampleId) {
        return new HashSet<Metadata>() {{
            add(new Metadata(Metadata.Key.SAMPLE_ID, collaboratorSampleId));
            add(new Metadata(Metadata.Key.PATIENT_ID, collaboratorSampleId));
        }};
    }

}
