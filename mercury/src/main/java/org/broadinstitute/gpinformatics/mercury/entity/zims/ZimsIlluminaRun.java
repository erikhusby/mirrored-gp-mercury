package org.broadinstitute.gpinformatics.mercury.entity.zims;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRead;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;

import javax.xml.bind.annotation.XmlElement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Note that we use {@link XmlElement} here for fields
 * so that the resulting json isn't choc-full of "@"'s.
 * See http://stackoverflow.com/questions/9576460/json-objects-returning-with-field-name-is-this-a-bug-or-a-feature
 * for more details.
 */
public class ZimsIlluminaRun {
    public static final String DATE_FORMAT = "MM/dd/yyyy HH:mm";

    @JsonProperty("name")
    private String runName;
    
    @JsonProperty("barcode")
    private String runBarcode;

    @JsonProperty("flowcellBarcode")
    private String flowcellBarcode;

    @JsonProperty("sequencer")
    private String sequencer;

    @JsonProperty("sequencerModel")
    private String sequencerModel;

    @JsonProperty("systemOfRecord")
    private SystemRouter.System systemOfRecord;

    @JsonIgnore
    private Date runDate;

    @JsonIgnore
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

    @JsonProperty("lanes")
    private List<ZimsIlluminaChamber> chambers = new ArrayList<>();

    @JsonProperty("pairedRun")
    private Boolean isPaired;

    @JsonProperty("reads")
    private List<ZamboniRead> reads = new ArrayList<>();

    // if something blows up, we put the error message here
    // to keep clients happy
    @JsonProperty
    private String error;

    @JsonProperty("actualReadStructure")
    private String actualReadStructure;

    @JsonProperty("imagedAreaPerLaneMM2")
    private Double imagedAreaPerLaneMM2;

    @JsonProperty("setupReadStructure")
    private String setupReadStructure;

    @JsonProperty("lanesSequenced")
    private String lanesSequenced;

    @JsonProperty("runFolder")
    private String runFolder;

    public ZimsIlluminaRun() {
    }

    public ZimsIlluminaRun(String runName,
                           String runBarcode,
                           String flowcellBarcode,
                           String sequencer,
                           String sequencerModel,
                           String runDate,
                           Boolean isPaired,
                           String actualReadStructure,
                           double imagedAreaPerLaneMM2,
                           String lanesSequenced,
                           SystemRouter.System systemOfRecord) {
        this.runName = runName;
        this.runBarcode = runBarcode;
        this.flowcellBarcode = flowcellBarcode;
        this.sequencer = sequencer;
        this.sequencerModel = sequencerModel;
        this.systemOfRecord = systemOfRecord;
        try {
            this.runDate = dateFormat.parse(runDate);
        }
        catch(ParseException e) {
            throw new RuntimeException("Cannot parse run date " + runDate + " for " + runName);
        }
        this.isPaired = isPaired;
        this.actualReadStructure = actualReadStructure;
        this.imagedAreaPerLaneMM2 = ThriftConversionUtil.zeroAsNull(imagedAreaPerLaneMM2);
        this.lanesSequenced = lanesSequenced;
    }

    public ZimsIlluminaRun(String runName,
                           String runBarcode,
                           String flowcellBarcode,
                           String sequencer,
                           String sequencerModel,
                           String runDate,
                           Boolean isPaired,
                           String actualReadStructure,
                           double imagedAreaPerLaneMM2,
                           SystemRouter.System systemOfRecord) {
        this(runName, runBarcode, flowcellBarcode, sequencer, sequencerModel, runDate, isPaired, actualReadStructure,
                imagedAreaPerLaneMM2, null, systemOfRecord);
    }

    public ZimsIlluminaRun(String runName, String runBarcode, String flowcellBarcode, String sequencer,
                           String sequencerModel, String runDate, Boolean paired, String actualReadStructure,
                           double imagedAreaPerLaneMM2, String setupReadStructure, String lanesSequenced,
                           String runFolder, SystemRouter.System systemOfRecord) {
        this(runName, runBarcode, flowcellBarcode, sequencer, sequencerModel, runDate, paired, actualReadStructure,
                imagedAreaPerLaneMM2, lanesSequenced, systemOfRecord);
        this.setupReadStructure = setupReadStructure;
        this.runFolder = runFolder;
    }

    /**
     * Creates a ZimsIlluminaRun from a TZamboniRun.
     *
     * TODO: Make this method fill in more data from the thrift run.
     * It would be good if this method could handle the rest of what is currently done in
     * {@link org.broadinstitute.gpinformatics.mercury.boundary.zims.IlluminaRunResource#getRun(edu.mit.broad.prodinfo.thrift.lims.TZamboniRun, java.util.Map, org.broadinstitute.gpinformatics.mercury.control.zims.ThriftLibraryConverter, org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao)}.
     * However, that method currently relies on some external data sources that are not available to ZimsIlluminaRun and
     * it's not immediately obvious that passing those into this method is the right way to go about it.
     *
     * @param zamboniRun    the thrift run to build the ZimsIlluminaRun from
     * @return a new ZimsIlluminaRun
     */
    public static ZimsIlluminaRun makeZimsIlluminaRun(TZamboniRun zamboniRun) {
        return new ZimsIlluminaRun(
                zamboniRun.getRunName(),
                zamboniRun.getRunBarcode(),
                zamboniRun.getFlowcellBarcode(),
                zamboniRun.getSequencer(),
                zamboniRun.getSequencerModel(),
                zamboniRun.getRunDate(),
                zamboniRun.isPairedRun(),
                zamboniRun.getActualReadStructure(),
                zamboniRun.getImagedAreaPerLaneMM2(),
                zamboniRun.getSetupReadStructure(),
                zamboniRun.getLanesSequenced(),
                zamboniRun.getRunFolder(),
                SystemRouter.System.SQUID);
    }

    /**
     * Converts the thrift bean {@link TZamboniRead} to mercury's {@link ZamboniRead}.
     * In thrift, zero is a dual use value that might mean zero or might
     * mean null.  In the context of {@link TZamboniRead}, 0 is really null,
     * so we do a zero-to-null conversion here.
     *
     * @param thriftRead The thrift read to use
     */
    public void addRead(TZamboniRead thriftRead) {
        Integer firstCycle = ThriftConversionUtil.zeroAsNull(thriftRead.getFirstCycle());
        Integer length = ThriftConversionUtil.zeroAsNull(thriftRead.getLength());
        reads.add(new ZamboniRead(firstCycle,length,thriftRead.getReadType().toString()));
    }

    public List<ZamboniRead> getReads() {
        return reads;
    }

    public Boolean getPairedRun() {
        return isPaired;
    }


    /**
     * Format is 01/03/2010 24:19
     *
     * @return The run date formatted as a string.
     */
    @JsonProperty("runDateString")
    public String getRunDateString() {
        String date = null;
        if (runDate != null) {
            date = dateFormat.format(runDate);
        }
        return date;
    }

    @SuppressWarnings("unused")
    public void setRunDateString(String runDate) throws ParseException {
        if (runDate != null) {
            this.runDate = dateFormat.parse(runDate);
        }
        else {
            this.runDate = null;
        }
    }

    public String getSequencerModel() {
        return sequencerModel;
    }

    public String getSequencer() {
        return sequencer;
    }

    public String getFlowcellBarcode() {
        return flowcellBarcode;
    }

    public String getName() {
        return runName;
    }
    
    public String getBarcode() {
        return runBarcode;
    }

    public void addLane(ZimsIlluminaChamber chamber) {
        chambers.add(chamber);
    }
    
    public Collection<ZimsIlluminaChamber> getLanes() {
        return chambers;
    }

    /**
     * Should only be used in the REST resource itself.
     *
     * @param error The error string
     */
    public void setError(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }


    public String getActualReadStructure() {
        return actualReadStructure;
    }

    public Double getImagedAreaPerLaneMM2() {
        return imagedAreaPerLaneMM2;
    }

    public String getSetupReadStructure() {
        return setupReadStructure;
    }

    public String getLanesSequenced() {
        return lanesSequenced;
    }

    public String getRunFolder() {
        return runFolder;
    }

    public SystemRouter.System getSystemOfRecord() {
        return systemOfRecord;
    }
}


