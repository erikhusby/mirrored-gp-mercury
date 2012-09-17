package org.broadinstitute.sequel.control.lims;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.Lane;
import edu.mit.broad.prodinfo.thrift.lims.LibraryData;
import edu.mit.broad.prodinfo.thrift.lims.SampleInfo;
import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.limsquery.generated.FlowcellDesignationType;
import org.broadinstitute.sequel.limsquery.generated.LaneType;
import org.broadinstitute.sequel.limsquery.generated.LibraryDataType;
import org.broadinstitute.sequel.limsquery.generated.SampleInfoType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author breilly
 */
public class LimsQueryResourceResponseFactoryTest {

    // 2012/03/15 13:23

    private LimsQueryResourceResponseFactory factory = new LimsQueryResourceResponseFactory();

    private FlowcellDesignation flowcellDesignation;
    private Lane lane;
    private LibraryData libraryData;
    private Date libraryDateCreated;
    private SampleInfo sampleInfo;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {
        // TODO: better test data that includes lists of size > 1
        sampleInfo = new SampleInfo("SM-1234", "TestSample", (short) 6, "CATTAG", "TestReference");
        GregorianCalendar calendar = new GregorianCalendar(2012, 3, 15, 13, 23);
        libraryDateCreated = calendar.getTime();
        libraryData = new LibraryData(true, "TestLibrary-1", "TestLibrary", "12345678", Arrays.asList(sampleInfo), new SimpleDateFormat("yyyy/MM/dd HH:mm").format(libraryDateCreated), true, true);
        lane = new Lane("1", Arrays.asList(libraryData), 1.23, Arrays.asList(libraryData));
        flowcellDesignation = new FlowcellDesignation(Arrays.asList(lane), "TestDesignation", (short) 101, true, true, (short) 3, true);
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

    private void assertFlowcellDesignation(FlowcellDesignationType outFlowcellDesignation, FlowcellDesignation expected) {
        for (int i = 0; i < expected.getLanesSize(); i++) {
            assertLane(outFlowcellDesignation.getLanes().get(i), expected.getLanes().get(i));
        }
        assertThat(outFlowcellDesignation.getDesignationName(), equalTo(expected.getDesignationName()));
        assertThat(outFlowcellDesignation.getReadLength(), equalTo(Integer.valueOf(expected.getReadLength())));
        assertThat(outFlowcellDesignation.isPairedEndRun(), equalTo(expected.isIsPairedEndRun()));
        assertThat(outFlowcellDesignation.getControlLane(), equalTo(Integer.valueOf(expected.getControlLane())));
        assertThat(outFlowcellDesignation.isKeepIntensityFiles(), equalTo(expected.isKeepIntensityFiles()));
    }

    private void assertLane(LaneType outLane, Lane expected) {
        assertThat(outLane.getLaneName(), equalTo(expected.getLaneName()));
        for (int i = 0; i < expected.getLibraryDataSize(); i++) {
            assertLibraryData(outLane.getLibraryData().get(i), expected.getLibraryData().get(i));
        }
        assertThat(outLane.getLoadingConcentration(), equalTo(expected.getLoadingConcentration()));
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
        assertThat(outLibraryData.getDateCreated().toGregorianCalendar().getTime(), equalTo(libraryDateCreated));
        assertThat(outLibraryData.isDiscarded(), equalTo(expected.isIsDiscarded()));
        assertThat(outLibraryData.isDestroyed(), equalTo(expected.isIsDestroyed()));
    }

    private void assertSampleInfo(SampleInfoType outSampleInfo, SampleInfo expected) {
        assertThat(outSampleInfo.getSampleName(), equalTo(expected.getSampleName()));
        assertThat(outSampleInfo.getSampleType(), equalTo(expected.getSampleType()));
        assertThat(outSampleInfo.getIndexLength(), equalTo(Integer.valueOf(expected.getIndexLength())));
        assertThat(outSampleInfo.getIndexSequence(), equalTo(expected.getIndexSequence()));
        assertThat(outSampleInfo.getReferenceSequence(), equalTo(expected.getReferenceSequence()));
    }
}
