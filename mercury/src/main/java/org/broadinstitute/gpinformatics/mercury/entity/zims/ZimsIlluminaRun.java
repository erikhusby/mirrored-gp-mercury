package org.broadinstitute.gpinformatics.mercury.entity.zims;

import edu.mit.broad.prodinfo.thrift.lims.TZamboniRead;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.XmlElement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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

    @JsonIgnore
    private Date runDate;

    @JsonIgnore
    private final SimpleDateFormat dateFormat =  new SimpleDateFormat(DATE_FORMAT);

    @JsonProperty("lanes")
    private List<ZimsIlluminaChamber> chambers = new ArrayList<ZimsIlluminaChamber>();

    @JsonProperty("pairedRun")
    private Boolean isPaired;

    @JsonProperty("reads")
    private List<ZamboniRead> reads = new ArrayList<ZamboniRead>();

    // if something blows up, we put the error message here
    // to keep clients happy
    @JsonProperty
    private String error;

    public ZimsIlluminaRun() {}

    public ZimsIlluminaRun(String runName,
                           String runBarcode,
                           String flowcellBarcode,
                           String sequencer,
                           String sequencerModel,
                           String runDate,
                           Boolean isPaired) {
        this.runName = runName;
        this.runBarcode = runBarcode;
        this.flowcellBarcode = flowcellBarcode;
        this.sequencer = sequencer;
        this.sequencerModel = sequencerModel;
        try {
            this.runDate = dateFormat.parse(runDate);
        }
        catch(ParseException e) {
            throw new RuntimeException("Cannot parse run date " + runDate + " for " + runName);
        }
        this.isPaired = isPaired;
    }

    public void addRead(TZamboniRead thriftRead) {
        reads.add(new ZamboniRead(ThriftConversionUtil.zeroAsNull(thriftRead.getFirstCycle()),
                ThriftConversionUtil.zeroAsNull(thriftRead.getLength()),
                thriftRead.getReadType().toString()));
    }

    public List<ZamboniRead> getReads() {
        return reads;
    }

    public Boolean getPairedRun() {
        return isPaired;
    }


    /**
     * Format is 01/03/2010 24:19
     * @return
     */
    @JsonProperty("runDateString")
    public String getRunDateString() {
        String date = null;
        if (runDate != null) {
            date = dateFormat.format(runDate);
        }
        return date;
    }

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
     * @param error
     */
    public void setError(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }


}
