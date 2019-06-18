package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ConcentrationAndVolumeAndWeightType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LibraryDataType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.PlateTransferType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SampleInfoType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.WellAndSourceTubeType;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.doSectionTransfer;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.makeTubeFormation;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Tests for LimsQueries boundary interface.
 *
 * @author breilly
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class LimsQueriesTest {

    private StaticPlateDao staticPlateDao;
    private LabVesselDao labVesselDao;
    private BarcodedTubeDao barcodedTubeDao;
    private LimsQueries limsQueries;

    private StaticPlate plate3;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setup() {
        // todo jmt mocks could be removed by small refactoring into @DaoFree methods
        staticPlateDao = createMock(StaticPlateDao.class);
        labVesselDao = createMock(LabVesselDao.class);
        barcodedTubeDao = createMock(BarcodedTubeDao.class);

        plate3 = new StaticPlate("plate3", Eppendorf96);

        doSectionTransfer(makeTubeFormation(new BarcodedTube("tube")), plate3);
        doSectionTransfer(new StaticPlate("plate1", Eppendorf96), plate3);
        doSectionTransfer(new StaticPlate("plate2", Eppendorf96), plate3);
        limsQueries = new LimsQueries(staticPlateDao, labVesselDao, barcodedTubeDao, null);
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchLibraryDetailsByTubeBarcode() {
        String[] sampleKey = {"SM-1111", "SM-2222"};
        String barcode = "3333";

        // Make a tube that contains a pool of two pdo samples from two different research projects.
        BarcodedTube poolTube = new BarcodedTube(barcode);
        for (int i = 0; i < sampleKey.length; ++i) {
            ProductOrderSample productOrderSample = new ProductOrderSample(sampleKey[i]);
            new ProductOrder(0L, "PDO-" + i, Collections.singletonList(productOrderSample),
                    "", null, new ResearchProject(0L, "", "", true, i == 0 ?
                    ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS :
                    ResearchProject.RegulatoryDesignation.RESEARCH_ONLY)
            );
            MercurySample mercurySample = new MercurySample(sampleKey[i], MercurySample.MetadataSource.BSP);
            productOrderSample.setMercurySample(mercurySample);
            mercurySample.getProductOrderSamples().add(productOrderSample);

            poolTube.addSample(mercurySample);
        }

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(barcode, poolTube);
        List<LibraryDataType> libraryDataTypes = limsQueries.fetchLibraryDetailsByTubeBarcode(mapBarcodeToVessel);
        assertThat(libraryDataTypes, Matchers.hasSize(1));
        LibraryDataType libraryDataType = libraryDataTypes.get(0);
        assertThat(libraryDataType.getLibraryName(), Matchers.equalTo(barcode));
        assertThat(libraryDataType.getTubeBarcode(), Matchers.equalTo(barcode));
        assertThat(libraryDataType.getSampleDetails(), Matchers.hasSize(1));
        SampleInfoType sampleInfoType = libraryDataType.getSampleDetails().get(0);
        assertThat(sampleInfoType.getSampleName(), Matchers.equalTo(sampleKey[0]));
        assertThat(libraryDataType.getRegulatoryDesignation(), Matchers.hasSize(2));
        assertThat(libraryDataType.getRegulatoryDesignation(), Matchers.hasItem("CLINICAL_DIAGNOSTICS"));
        assertThat(libraryDataType.getRegulatoryDesignation(), Matchers.hasItem("RESEARCH_ONLY"));
    }

    @Test(groups = DATABASE_FREE)
    public void testFindImmediatePlateParents() {
        List<String> parents = limsQueries.findImmediatePlateParents(plate3);
        assertThat(parents.size(), equalTo(2));
        assertThat(parents, hasItem("plate1"));
        assertThat(parents, hasItem("plate2"));
    }

    @Test(groups = DATABASE_FREE)
    public void testFindImmediatePlateParentsNotFound() {
        expect(staticPlateDao.findByBarcode("unknown_plate")).andReturn(null);
        replay(staticPlateDao);

        Exception caught = null;
        try {
            limsQueries.findImmediatePlateParents("unknown_plate");
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(RuntimeException.class));
        assertThat(caught.getMessage(), Matchers.equalTo("Plate not found for barcode: unknown_plate"));

        verify(staticPlateDao);
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchParentRackContentsForPlate() {
        BarcodedTube tube1 = new BarcodedTube("tube1");
        BarcodedTube tube2 = new BarcodedTube("tube2");
        StaticPlate plate = new StaticPlate("plate1", Eppendorf96);
        expect(staticPlateDao.findByBarcode("plate1")).andReturn(plate);
        doSectionTransfer(makeTubeFormation(tube1, tube2), plate);
        replay(staticPlateDao);

        Map<String, Boolean> result = limsQueries.fetchParentRackContentsForPlate("plate1");
        assertThat(result.size(), is(96));
        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            String well = entry.getKey();
            if (well.equals("A01") || well.equals("A02")) {
                assertThat("Wrong value for well: " + well, entry.getValue(), is(true));
            } else {
                assertThat("Wrong value for well: " + well, entry.getValue(), is(false));
            }
        }

        verify(staticPlateDao);
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchSourceTubesForPlate() {
        BarcodedTube tube1 = new BarcodedTube("tube1");
        BarcodedTube tube2 = new BarcodedTube("tube2");
        BarcodedTube tube3 = new BarcodedTube("tube3");
        StaticPlate plate = new StaticPlate("plate1", Eppendorf96);
        expect(staticPlateDao.findByBarcode("plate1")).andReturn(plate);
        doSectionTransfer(makeTubeFormation(tube1, tube2), plate);
        doSectionTransfer(makeTubeFormation(tube3), plate);
        replay(staticPlateDao);

        List<WellAndSourceTubeType> result = limsQueries.fetchSourceTubesForPlate("plate1");
        assertThat(result.size(), is(3));

        verify(staticPlateDao);
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchTransfersForPlate() {
        StaticPlate plate1 = new StaticPlate("plate1", Eppendorf96);
        StaticPlate plate2 = new StaticPlate("plate2", Eppendorf96);
        StaticPlate plate3 = new StaticPlate("plate3", Eppendorf96);
        StaticPlate plate4 = new StaticPlate("plate4", Eppendorf96);
        doSectionTransfer(plate1, plate2);
        doSectionTransfer(plate2, plate3);
        doSectionTransfer(plate3, plate4);

        List<PlateTransferType> result = limsQueries.fetchTransfersForPlate(plate4, 2);
        assertThat(result.size(), is(2));
        assertThat(result.get(0).getSourceBarcode(), equalTo("plate3"));
        assertThat(result.get(0).getSourceSection(), equalTo("ALL96"));
        assertThat(result.get(0).getDestinationBarcode(), equalTo("plate4"));
        assertThat(result.get(0).getDestinationSection(), equalTo("ALL96"));
        assertThat(result.get(1).getSourceBarcode(), equalTo("plate2"));
        assertThat(result.get(1).getSourceSection(), equalTo("ALL96"));
        assertThat(result.get(1).getDestinationBarcode(), equalTo("plate3"));
        assertThat(result.get(1).getDestinationSection(), equalTo("ALL96"));
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchTransfersForPlateNotFound() {
        expect(staticPlateDao.findByBarcode("unknown_plate")).andReturn(null);
        replay(staticPlateDao);

        Exception caught = null;
        try {
            limsQueries.fetchTransfersForPlate("unknown_plate", 2);
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(RuntimeException.class));
        assertThat(caught.getMessage(), Matchers.equalTo("Plate not found for barcode: unknown_plate"));

        verify(staticPlateDao);
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchQuantForTube() {
        BarcodedTube tube = new BarcodedTube("tube1");
        expect(labVesselDao.findByIdentifier("tube1")).andReturn(tube);
        replay(labVesselDao);

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        LabMetric quantMetric = new LabMetric(new BigDecimal("44.44"), LabMetric.MetricType.POND_PICO,
                LabMetric.LabUnit.UG_PER_ML, "D04", gregorianCalendar.getTime());
        tube.addMetric(quantMetric);

        gregorianCalendar.add(Calendar.HOUR, 1);
        quantMetric = new LabMetric(new BigDecimal("55.55"), LabMetric.MetricType.POND_PICO,
                LabMetric.LabUnit.UG_PER_ML, "D04", gregorianCalendar.getTime());
        tube.addMetric(quantMetric);

        Double quantValue = limsQueries.fetchNearestQuantForTube("tube1", "Pond Pico");
        assertThat(quantValue, equalTo(55.55));
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchQPCRForTube() {
        BarcodedTube tube = new BarcodedTube("tube1");
        expect(labVesselDao.findByIdentifier("tube1")).andReturn(tube);
        replay(labVesselDao);

        LabMetric quantMetric =
                new LabMetric(new BigDecimal("55.55"), LabMetric.MetricType.ECO_QPCR, LabMetric.LabUnit.UG_PER_ML,
                        "D04", new Date());
        tube.addMetric(quantMetric);

        Double quantValue = limsQueries.fetchNearestQuantForTube("tube1", LabMetric.MetricType.ECO_QPCR.getDisplayName());
        assertThat(quantValue, equalTo(55.55));
    }

    @Test(groups = DATABASE_FREE)
    public void testDoesLimsRecognizeAllTubes() {
        Map<String, BarcodedTube> mercuryTubes = new HashMap<>();
        String barcode = "mercury_barcode";
        mercuryTubes.put(barcode, new BarcodedTube(barcode));
        expect(barcodedTubeDao.findByBarcodes(Arrays.asList(barcode))).andReturn(mercuryTubes);
        Map<String, BarcodedTube> badTubes = new HashMap<>();
        String badBarcode = "bad_barcode";
        badTubes.put(badBarcode, null);
        expect(barcodedTubeDao.findByBarcodes(Arrays.asList(badBarcode))).andReturn(badTubes);
        replay(barcodedTubeDao);

        Assert.assertTrue(limsQueries.doesLimsRecognizeAllTubes(Arrays.asList(barcode)), "Wrong return");
        Assert.assertFalse(limsQueries.doesLimsRecognizeAllTubes(Arrays.asList(badBarcode)), "Wrong return");
        verify(barcodedTubeDao);
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchConcentrationAndVolumeAndWeightForTubeBarcodes() {
        String barcode = "tube1";
        Map<String, LabVessel> mercuryTubes = new HashMap<>();
        BarcodedTube tube = new BarcodedTube(barcode);
        tube.addSample(new MercurySample("SM-1234", MercurySample.MetadataSource.MERCURY));
        mercuryTubes.put(barcode, tube);

        //Should not find Final Library Size since its not a concentration
        LabMetric finalLibrarySizeMetric =
                new LabMetric(new BigDecimal(224), LabMetric.MetricType.FINAL_LIBRARY_SIZE, LabMetric.LabUnit.UG_PER_ML,
                        "A01", new Date());
        tube.addMetric(finalLibrarySizeMetric);
        Map<String, ConcentrationAndVolumeAndWeightType> concentrationAndVolumeTypeMap =
                limsQueries.fetchConcentrationAndVolumeAndWeightForTubeBarcodes(mercuryTubes,
                        Collections.<String, GetSampleDetails.SampleInfo>emptyMap(), true);
        assertThat(concentrationAndVolumeTypeMap.size(), equalTo(1));
        ConcentrationAndVolumeAndWeightType concentrationAndVolumeType = concentrationAndVolumeTypeMap.get(barcode);
        assertThat(concentrationAndVolumeType.isWasFound(), equalTo(true));
        assertThat(concentrationAndVolumeType.getConcentration(), equalTo(null));

        BigDecimal labMetricQuant = BigDecimal.valueOf(22.21);
        LabMetric quantMetric =
                new LabMetric(labMetricQuant, LabMetric.MetricType.INITIAL_PICO, LabMetric.LabUnit.UG_PER_ML,
                        "A02", new Date());
        tube.addMetric(quantMetric);
        BigDecimal volume = BigDecimal.valueOf(40.04);
        tube.setVolume(volume);
        BigDecimal receptacleWeight = BigDecimal.valueOf(.002);
        tube.setReceptacleWeight(receptacleWeight);

        concentrationAndVolumeTypeMap = limsQueries.fetchConcentrationAndVolumeAndWeightForTubeBarcodes(mercuryTubes,
                Collections.<String, GetSampleDetails.SampleInfo>emptyMap(), true);
        assertThat(concentrationAndVolumeTypeMap.size(), equalTo(1));
        concentrationAndVolumeType = concentrationAndVolumeTypeMap.get(barcode);
        assertThat(concentrationAndVolumeType.isWasFound(), equalTo(true));
        assertThat(concentrationAndVolumeType.getTubeBarcode(), equalTo(barcode));
        assertThat(concentrationAndVolumeType.getConcentration(), equalTo(labMetricQuant));
        assertThat(concentrationAndVolumeType.getVolume(), equalTo(volume));
        assertThat(concentrationAndVolumeType.getWeight(), equalTo(receptacleWeight));
        assertThat(concentrationAndVolumeType.getConcentrationUnits(),
                equalTo(LabMetric.LabUnit.UG_PER_ML.getDisplayName()));

        // If volume or concentration is null on labvessel and sample details have data for both, then trust
        // sample details as the source
        Map<String, LabVessel> clinicalGenomeTubes = new HashMap<>();
        tube = new BarcodedTube(barcode);
        tube.setVolume(volume);
        tube.addSample(new MercurySample("SM-1234", MercurySample.MetadataSource.BSP));
        clinicalGenomeTubes.put(barcode, tube);
        Map<String, GetSampleDetails.SampleInfo> mapBarcodeToInfo = new HashMap<>();
        GetSampleDetails.SampleInfo sampleInfo = new GetSampleDetails.SampleInfo();
        sampleInfo.setConcentration(5f);
        sampleInfo.setVolume(25f);
        mapBarcodeToInfo.put(barcode, sampleInfo);
        concentrationAndVolumeTypeMap =
                limsQueries.fetchConcentrationAndVolumeAndWeightForTubeBarcodes(clinicalGenomeTubes, mapBarcodeToInfo, true);
        ConcentrationAndVolumeAndWeightType concentrationAndVolumeAndWeightType =
                concentrationAndVolumeTypeMap.get(barcode);
        BigDecimal expectedConcentration = new BigDecimal("5.00");
        BigDecimal expectedVolume = new BigDecimal("25.00");
        assertThat(concentrationAndVolumeAndWeightType.getConcentration(), equalTo(expectedConcentration));
        assertThat(concentrationAndVolumeAndWeightType.getVolume(), equalTo(expectedVolume));
    }
}
