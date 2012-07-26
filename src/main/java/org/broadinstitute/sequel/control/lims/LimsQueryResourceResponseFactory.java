package org.broadinstitute.sequel.control.lims;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.Lane;
import edu.mit.broad.prodinfo.thrift.lims.LibraryData;
import edu.mit.broad.prodinfo.thrift.lims.SampleInfo;
import org.broadinstitute.sequel.limsquery.generated.FlowcellDesignationType;
import org.broadinstitute.sequel.limsquery.generated.LaneType;
import org.broadinstitute.sequel.limsquery.generated.LibraryDataType;
import org.broadinstitute.sequel.limsquery.generated.SampleInfoType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author breilly
 */
public class LimsQueryResourceResponseFactory {

    public FlowcellDesignationType makeFlowcellDesignation(FlowcellDesignation designation) {
        FlowcellDesignationType outDesignation = new FlowcellDesignationType();
        for (Lane lane : designation.getLanes()) {
            LaneType outLane = makeLane(lane);
            outDesignation.getLanes().add(outLane);
        }
        outDesignation.setDesignationName(designation.getDesignationName());
        outDesignation.setReadLength(designation.getReadLength());
        outDesignation.setPairedEndRun(designation.isIsPairedEndRun());
        outDesignation.setControlLane(designation.getControlLane());
        outDesignation.setKeepIntensityFiles(designation.isKeepIntensityFiles());
        return outDesignation;
    }

    public LaneType makeLane(Lane lane) {
        LaneType outLane = new LaneType();
        outLane.setLaneName(lane.getLaneName());
        for (LibraryData libraryData : lane.getLibraryData()) {
            outLane.getLibraryData().add(makeLibraryData(libraryData));
        }
        outLane.setLoadingConcentration(lane.getLoadingConcentration());
        for (LibraryData libraryData : lane.getDerivedLibraryData()) {
            outLane.getDerivedLibraryData().add(makeLibraryData(libraryData));
        }
        return outLane;
    }

    public LibraryDataType makeLibraryData(LibraryData libraryData) {
        LibraryDataType outLibraryData = new LibraryDataType();
        outLibraryData.setWasFound(libraryData.isWasFound());
        outLibraryData.setLibraryName(libraryData.getLibraryName());
        outLibraryData.setLibraryType(libraryData.getLibraryType());
        outLibraryData.setTubeBarcode(libraryData.getTubeBarcode());
        for (SampleInfo sampleInfo : libraryData.getSampleDetails()) {
            outLibraryData.getSampleDetails().add(makeSampleInfo(sampleInfo));
        }

        Date createDateTime;
        try {
            createDateTime = new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(libraryData.getDateCreated());
        } catch (ParseException e) {
            throw new RuntimeException("Unexpected date format. Wanted: yyyy-MM-ddTHH:mm:ss.SSSZ. Got: " + libraryData.getDateCreated(), e);
        }
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(createDateTime);
        outLibraryData.setDateCreated(new XMLGregorianCalendarImpl(calendar));

        outLibraryData.setDiscarded(libraryData.isIsDiscarded());
        outLibraryData.setDestroyed(libraryData.isIsDestroyed());

        return outLibraryData;
    }

    public SampleInfoType makeSampleInfo(SampleInfo sampleInfo) {
        SampleInfoType outSampleInfo = new SampleInfoType();
        outSampleInfo.setSampleName(sampleInfo.getSampleName());
        outSampleInfo.setSampleType(sampleInfo.getSampleType());
        outSampleInfo.setIndexLength(sampleInfo.getIndexLength());
        outSampleInfo.setIndexSequence(sampleInfo.getIndexSequence());
        outSampleInfo.setReferenceSequence(sampleInfo.getReferenceSequence());
        return outSampleInfo;
    }
}
