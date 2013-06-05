package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LibraryDataType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.PlateTransferType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SampleInfoType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.WellAndSourceTubeType;
import org.hamcrest.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Tests for LimsQueries boundary interface.
 *
 * @author breilly
 */
public class LimsQueriesTest {

    private StaticPlateDAO staticPlateDAO;
    private LabVesselDao labVesselDao;
    private LimsQueries limsQueries;

    private StaticPlate plate3;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setup() {
        // todo jmt mocks could be removed by small refactoring into @DaoFree methods
        staticPlateDAO = createMock(StaticPlateDAO.class);
        labVesselDao = createMock(LabVesselDao.class);

        plate3 = new StaticPlate("plate3", Eppendorf96);

        doSectionTransfer(makeTubeFormation(new TwoDBarcodedTube("tube")), plate3);
        doSectionTransfer(new StaticPlate("plate1", Eppendorf96), plate3);
        doSectionTransfer(new StaticPlate("plate2", Eppendorf96), plate3);
        limsQueries = new LimsQueries(staticPlateDAO, labVesselDao);
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchLibraryDetailsByTubeBarcode() {
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        String twoDBarcode = "1234";
        TwoDBarcodedTube twoDBarcodedTube = new TwoDBarcodedTube(twoDBarcode);
        String sampleKey = "SM-1234";
        twoDBarcodedTube.addSample(new MercurySample(sampleKey));
        mapBarcodeToVessel.put(twoDBarcode, twoDBarcodedTube);
        List<LibraryDataType> libraryDataTypes = limsQueries.fetchLibraryDetailsByTubeBarcode(mapBarcodeToVessel);
        assertThat(libraryDataTypes.size(), equalTo(1));
        LibraryDataType libraryDataType = libraryDataTypes.get(0);
        assertThat(libraryDataType.getLibraryName(), Matchers.equalTo(twoDBarcode));
        assertThat(libraryDataType.getTubeBarcode(), Matchers.equalTo(twoDBarcode));
        assertThat(libraryDataType.getSampleDetails().size(), equalTo(1));
        SampleInfoType sampleInfoType = libraryDataType.getSampleDetails().get(0);
        assertThat(sampleInfoType.getSampleName(), Matchers.equalTo(sampleKey));
    }

    @Test(groups = DATABASE_FREE)
    public void testFindImmediatePlateParents() {
        expect(staticPlateDAO.findByBarcode("plate3")).andReturn(plate3);
        replay(staticPlateDAO);

        List<String> parents = limsQueries.findImmediatePlateParents("plate3");
        assertThat(parents.size(), equalTo(2));
        assertThat(parents, hasItem("plate1"));
        assertThat(parents, hasItem("plate2"));

        verify(staticPlateDAO);
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchParentRackContentsForPlate() {
        TwoDBarcodedTube tube1 = new TwoDBarcodedTube("tube1");
        TwoDBarcodedTube tube2 = new TwoDBarcodedTube("tube2");
        StaticPlate plate = new StaticPlate("plate1", Eppendorf96);
        expect(staticPlateDAO.findByBarcode("plate1")).andReturn(plate);
        doSectionTransfer(makeTubeFormation(tube1, tube2), plate);
        replay(staticPlateDAO);

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

        verify(staticPlateDAO);
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchSourceTubesForPlate() {
        TwoDBarcodedTube tube1 = new TwoDBarcodedTube("tube1");
        TwoDBarcodedTube tube2 = new TwoDBarcodedTube("tube2");
        TwoDBarcodedTube tube3 = new TwoDBarcodedTube("tube3");
        StaticPlate plate = new StaticPlate("plate1", Eppendorf96);
        expect(staticPlateDAO.findByBarcode("plate1")).andReturn(plate);
        doSectionTransfer(makeTubeFormation(tube1, tube2), plate);
        doSectionTransfer(makeTubeFormation(tube3), plate);
        replay(staticPlateDAO);

        List<WellAndSourceTubeType> result = limsQueries.fetchSourceTubesForPlate("plate1");
        assertThat(result.size(), is(3));

        verify(staticPlateDAO);
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
        expect(staticPlateDAO.findByBarcode("unknown_plate")).andReturn(null);
        replay(staticPlateDAO);

        Exception caught = null;
        try {
            limsQueries.fetchTransfersForPlate("unknown_plate", 2);
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(RuntimeException.class));
        assertThat(caught.getMessage(), Matchers.equalTo("Plate not found for barcode: unknown_plate"));

        verify(staticPlateDAO);
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchQuantForTube() {
        TwoDBarcodedTube tube = new TwoDBarcodedTube("tube1");
        expect(labVesselDao.findByIdentifier("tube1")).andReturn(tube);
        replay(labVesselDao);

        LabMetric quantMetric =
                new LabMetric(new BigDecimal("55.55"), LabMetric.MetricType.POND_PICO, LabMetric.LabUnit.UG_PER_ML);
        tube.addMetric(quantMetric);

        Double quantValue = limsQueries.fetchQuantForTube("tube1", "Pond Pico");
        assertThat(quantValue, equalTo(55.55));
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchQPCRForTube() {
        TwoDBarcodedTube tube = new TwoDBarcodedTube("tube1");
        expect(labVesselDao.findByIdentifier("tube1")).andReturn(tube);
        replay(labVesselDao);

        LabMetric quantMetric =
                new LabMetric(new BigDecimal("55.55"), LabMetric.MetricType.ECO_QPCR, LabMetric.LabUnit.UG_PER_ML);
        tube.addMetric(quantMetric);

        Double quantValue = limsQueries.fetchQuantForTube("tube1", LabMetric.MetricType.ECO_QPCR.getDisplayName());
        assertThat(quantValue, equalTo(55.55));
    }
}
