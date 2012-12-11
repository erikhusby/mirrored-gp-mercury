package org.broadinstitute.gpinformatics.mercury.control.lims;

import edu.mit.broad.prodinfo.thrift.lims.*;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
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
        if (lane.getDerivedLibraryData() != null) {
            for (LibraryData libraryData : lane.getDerivedLibraryData()) {
                outLane.getDerivedLibraryData().add(makeLibraryData(libraryData));
            }
        }
        return outLane;
    }

    public LibraryDataType makeLibraryData(LibraryData libraryData) {
        LibraryDataType outLibraryData = new LibraryDataType();
        outLibraryData.setWasFound(libraryData.isWasFound());
        outLibraryData.setLibraryName(libraryData.getLibraryName());
        outLibraryData.setLibraryType(libraryData.getLibraryType());
        outLibraryData.setTubeBarcode(libraryData.getTubeBarcode());
        if (libraryData.getSampleDetails() != null) {
            for (SampleInfo sampleInfo : libraryData.getSampleDetails()) {
                outLibraryData.getSampleDetails().add(makeSampleInfo(sampleInfo));
            }
        }

        Date createDateTime;
        String dateCreated = libraryData.getDateCreated();
        if (dateCreated != null) {
            try {
                createDateTime = new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(dateCreated);
            } catch (ParseException e) {
                throw new RuntimeException("Unexpected date format. Wanted: yyyy-MM-ddTHH:mm:ss.SSSZ. Got: " + dateCreated, e);
            }
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(createDateTime);
            try {
                outLibraryData.setDateCreated(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar));
            } catch (DatatypeConfigurationException e) {
                throw new RuntimeException(e);
            }
        }

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

    public WellAndSourceTubeType makeWellAndSourceTube(WellAndSourceTube wellAndSourceTube) {
        WellAndSourceTubeType outWellAndSourceTube = new WellAndSourceTubeType();
        outWellAndSourceTube.setWellName(wellAndSourceTube.getWellName());
        outWellAndSourceTube.setTubeBarcode(wellAndSourceTube.getTubeBarcode());
        return outWellAndSourceTube;
    }

    public PlateTransferType makePlateTransfer(PlateTransfer plateTransfer) {
        PlateTransferType plateTransferType = new PlateTransferType();
        plateTransferType.setSourceBarcode(plateTransfer.getSourceBarcode());
        plateTransferType.setSourceSection(plateTransfer.getSourceSection());
        for (WellAndSourceTube wellAndSourceTube : plateTransfer.getSourcePositionMap()) {
            plateTransferType.getSourcePositionMap().add(makeWellAndSourceTube(wellAndSourceTube));
        }
        plateTransferType.setDestinationBarcode(plateTransfer.getDestinationBarcode());
        plateTransferType.setDestinationSection(plateTransfer.getDestinationSection());
        for (WellAndSourceTube wellAndSourceTube : plateTransfer.getDestinationPositionMap()) {
            plateTransferType.getDestinationPositionMap().add(makeWellAndSourceTube(wellAndSourceTube));
        }
        return plateTransferType;
    }

    public PoolGroupType makePoolGroup(PoolGroup poolGroup) {
        PoolGroupType poolGroupType = new PoolGroupType();
        poolGroupType.setName(poolGroup.getName());
        for (String tubeBarcode : poolGroup.getTubeBarcodes()) {
            poolGroupType.getTubeBarcodes().add(tubeBarcode);
        }
        return poolGroupType;
    }
}
