package org.broadinstitute.gpinformatics.mercury.control.lims;

import edu.mit.broad.prodinfo.thrift.lims.*;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.*;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.broadinstitute.gpinformatics.mercury.control.lims.LimsQueryResourceResponseFactoryTest.WellAndSourceTubeMatcher.matchesWellAndSourceTube;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

/**
 * @author breilly
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class LimsQueryResourceResponseFactoryTest {

    // 2012/03/15 13:23

    private LimsQueryResourceResponseFactory factory = new LimsQueryResourceResponseFactory();

    private FlowcellDesignation flowcellDesignation;
    private Lane lane;
    private LibraryData libraryData;
    private Date libraryDateCreated;
    private SampleInfo sampleInfo;
    private WellAndSourceTube wellAndSourceTube;
    private PlateTransfer plateTransfer;
    private PoolGroup poolGroup;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {
        // TODO: better test data that includes lists of size > 1
        sampleInfo = new SampleInfo("SM-1234", "TestSample", (short) 6, "CATTAG", "TestReference", "lsid");
        GregorianCalendar calendar = new GregorianCalendar(2012, 3, 15, 13, 23);
        libraryDateCreated = calendar.getTime();
        libraryData = new LibraryData(true, "TestLibrary-1", "TestLibrary", "12345678", Arrays.asList(sampleInfo), new SimpleDateFormat("yyyy/MM/dd HH:mm").format(libraryDateCreated), true, true);
        lane = new Lane("1", Arrays.asList(libraryData), 1.23, Arrays.asList(libraryData));
        flowcellDesignation = new FlowcellDesignation(Arrays.asList(lane), "TestDesignation", (short) 101, true, true, (short) 3, true, "Single");

        wellAndSourceTube = new WellAndSourceTube("A01", "tube_barcode");

        plateTransfer = new PlateTransfer(
                "plate_barcode1", "section1", Arrays.asList(
                        new WellAndSourceTube("A01", "tube1"),
                        new WellAndSourceTube("A02", "tube2")),
                "plate_barcode2", "section2", Arrays.asList(
                        new WellAndSourceTube("B01", "tube3"),
                        new WellAndSourceTube("B02", "tube4")));

        poolGroup = new PoolGroup("group_name", Arrays.asList("tube_barcode1", "tube_barcode2"));
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testMakeFlowcellDesignation() {
        FlowcellDesignationType outFlowcellDesignation = factory.makeFlowcellDesignation(flowcellDesignation);
        assertFlowcellDesignation(outFlowcellDesignation, flowcellDesignation);
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testMakeLane() {
        LaneType outLane = factory.makeLane(lane);
        assertLane(outLane, lane);
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testMakeLibraryData() throws ParseException {
        LibraryDataType outLibraryData = factory.makeLibraryData(libraryData);
        assertLibraryData(outLibraryData, libraryData);
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testMakeSampleInfo() {
        SampleInfoType outSampleInfo = factory.makeSampleInfo(sampleInfo);
        assertSampleInfo(outSampleInfo, sampleInfo);
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testMakeWellAndSourceTube() {
        WellAndSourceTubeType outWellAndSourceTube = factory.makeWellAndSourceTube(wellAndSourceTube);
        assertThat(outWellAndSourceTube.getWellName(), equalTo(wellAndSourceTube.getWellName()));
        assertThat(outWellAndSourceTube.getTubeBarcode(), equalTo(wellAndSourceTube.getTubeBarcode()));
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testMakePlateTransfer() {
        PlateTransferType outPlateTransfer = factory.makePlateTransfer(plateTransfer);
        assertThat(outPlateTransfer.getSourceBarcode(), equalTo(plateTransfer.getSourceBarcode()));
        assertThat(outPlateTransfer.getSourceSection(), equalTo(plateTransfer.getSourceSection()));
        assertThat(outPlateTransfer.getSourcePositionMap().size(), equalTo(plateTransfer.getSourcePositionMap().size()));
        for (WellAndSourceTube tube : plateTransfer.getSourcePositionMap()) {
            assertThat(outPlateTransfer.getSourcePositionMap(), hasItem(matchesWellAndSourceTube(tube)));
        }
        assertThat(outPlateTransfer.getDestinationBarcode(), equalTo(plateTransfer.getDestinationBarcode()));
        assertThat(outPlateTransfer.getDestinationSection(), equalTo(plateTransfer.getDestinationSection()));
        assertThat(outPlateTransfer.getDestinationPositionMap().size(), equalTo(plateTransfer.getDestinationPositionMap().size()));
        for (WellAndSourceTube tube : plateTransfer.getDestinationPositionMap()) {
            assertThat(outPlateTransfer.getDestinationPositionMap(), hasItem(matchesWellAndSourceTube(tube)));
        }
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testMakePoolGroup() {
        PoolGroupType outPoolGroup = factory.makePoolGroup(poolGroup);
        assertThat(outPoolGroup.getName(), equalTo(poolGroup.getName()));
        assertThat(outPoolGroup.getTubeBarcodes(), equalTo(poolGroup.getTubeBarcodes()));
    }

    private void assertFlowcellDesignation(FlowcellDesignationType outFlowcellDesignation, FlowcellDesignation expected) {
        for (int i = 0; i < expected.getLanesSize(); i++) {
            assertLane(outFlowcellDesignation.getLanes().get(i), expected.getLanes().get(i));
        }
        assertThat(outFlowcellDesignation.getDesignationName(), equalTo(expected.getDesignationName()));
        assertThat(outFlowcellDesignation.getReadLength(), equalTo((int) expected.getReadLength()));
        assertThat(outFlowcellDesignation.isPairedEndRun(), equalTo(expected.isIsPairedEndRun()));
        assertThat(outFlowcellDesignation.isIndexedRun(), equalTo(expected.isIsIndexedRun()));
        assertThat(outFlowcellDesignation.getControlLane(), equalTo((int) expected.getControlLane()));
        assertThat(outFlowcellDesignation.isKeepIntensityFiles(), equalTo(expected.isKeepIntensityFiles()));
        assertThat(outFlowcellDesignation.getIndexingReadConfiguration(), equalTo(expected.getIndexingReadConfiguration()));
    }

    private void assertLane(LaneType outLane, Lane expected) {
        assertThat(outLane.getLaneName(), equalTo(expected.getLaneName()));
        for (int i = 0; i < expected.getLibraryDataSize(); i++) {
            assertLibraryData(outLane.getLibraryData().get(i), expected.getLibraryData().get(i));
        }
        assertThat(outLane.getLoadingConcentration(), equalTo(BigDecimal.valueOf(expected.getLoadingConcentration())));
        for (int i = 0; i < expected.getDerivedLibraryDataSize(); i++) {
            assertLibraryData(outLane.getDerivedLibraryData().get(i), expected.getDerivedLibraryData().get(i));
        }
    }

    private void assertLibraryData(LibraryDataType outLibraryData, LibraryData expected) {
        assertThat(outLibraryData.isWasFound(), equalTo(expected.isWasFound()));
        assertThat(outLibraryData.getLibraryName(), equalTo(expected.getLibraryName()));
        assertThat(outLibraryData.getLibraryType(), equalTo(expected.getLibraryType()));
        assertThat(outLibraryData.getTubeBarcode(), equalTo(expected.getTubeBarcode()));
        assertThat(outLibraryData.getSampleDetails().size(), equalTo(expected.getSampleDetails().size()));
        for (int i = 0; i < expected.getSampleDetailsSize(); i++) {
            assertSampleInfo(outLibraryData.getSampleDetails().get(i), expected.getSampleDetails().get(i));
        }
        assertThat(outLibraryData.getDateCreated(), equalTo(libraryDateCreated));
        assertThat(outLibraryData.isDiscarded(), equalTo(expected.isIsDiscarded()));
        assertThat(outLibraryData.isDestroyed(), equalTo(expected.isIsDestroyed()));
    }

    private void assertSampleInfo(SampleInfoType outSampleInfo, SampleInfo expected) {
        assertThat(outSampleInfo.getSampleName(), equalTo(expected.getSampleName()));
        assertThat(outSampleInfo.getSampleType(), equalTo(expected.getSampleType()));
        assertThat(outSampleInfo.getIndexLength(), equalTo((int) expected.getIndexLength()));
        assertThat(outSampleInfo.getIndexSequence(), equalTo(expected.getIndexSequence()));
        assertThat(outSampleInfo.getReferenceSequence(), equalTo(expected.getReferenceSequence()));
    }

    public static class WellAndSourceTubeMatcher extends BaseMatcher<WellAndSourceTubeType> {
        private WellAndSourceTube other;

        public static Matcher matchesWellAndSourceTube(WellAndSourceTube other) {
            return new WellAndSourceTubeMatcher(other);
        }

        private WellAndSourceTubeMatcher(WellAndSourceTube other) {
            this.other = other;
        }

        @Override
        public boolean matches(Object o) {
            WellAndSourceTubeType arg = (WellAndSourceTubeType) o;
            return arg.getWellName().equals(other.getWellName()) &&
                    arg.getTubeBarcode().equals(other.getTubeBarcode());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("WellAndSourceTube matching " + other.getWellName() + ":" + other.getTubeBarcode());
        }
    }
}
