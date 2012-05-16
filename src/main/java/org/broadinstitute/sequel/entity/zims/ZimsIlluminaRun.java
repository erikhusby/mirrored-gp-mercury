package org.broadinstitute.sequel.entity.zims;

import edu.mit.broad.prodinfo.thrift.lims.TZamboniRead;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Note that we use {@link XmlElement} here for fields
 * so that the resulting json isn't choc-full of "@"'s.
 * See http://stackoverflow.com/questions/9576460/json-objects-returning-with-field-name-is-this-a-bug-or-a-feature
 * for more details.
 */
@XmlRootElement(name = "IlluminaRun")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class ZimsIlluminaRun {
    
    @XmlElement(name = "name")
    private String runName;
    
    @XmlElement(name = "barcode")
    private String runBarcode;

    @XmlElement(name = "flowcellBarcode")
    private String flowcellBarcode;

    @XmlElement(name = "sequencer")
    private String sequencer;

    @XmlElement(name = "sequencerModel")
    private String sequencerModel;

    private Date runDate;

    @XmlElement(name = "firstCycle")
    private Integer firstCycle;

    @XmlElement(name = "firstCycleReadLength")
    private Integer firstCycleReadLength;

    @XmlElement(name = "lastCycle")
    private Integer lastCycle;

    final SimpleDateFormat dateFormat =  new SimpleDateFormat("MM/dd/yyyy HH:mm");

    @XmlElement(name = "lane")
    private List<ZimsIlluminaChamber> chambers = new ArrayList<ZimsIlluminaChamber>();

    @XmlElement(name = "molBarcodeCycle")
    private Integer molecularBarcodeCycle;

    @XmlElement(name = "molBarcodeLength")
    private Integer molecularBarcodeLength;

    @XmlElement(name = "pairedRun")
    private Boolean isPaired;

    @XmlElement(name = "Read")
    private List<ZamboniRead> reads = new ArrayList<ZamboniRead>();

    public ZimsIlluminaRun() {}

    public ZimsIlluminaRun(String runName,
                           String runBarcode,
                           String flowcellBarcode,
                           String sequencer,
                           String sequencerModel,
                           String runDate,
                           short firstCycle,
                           short firstCycleReadLength,
                           short lastCycle,
                           short molecularBarcodeCycle,
                           short molecularBarcodeLength,
                           boolean isPaired) {
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
        this.firstCycle = ThriftConversionUtil.zeroAsNull(firstCycle);
        this.firstCycleReadLength = ThriftConversionUtil.zeroAsNull(firstCycleReadLength);
        this.lastCycle = ThriftConversionUtil.zeroAsNull(lastCycle);
        this.molecularBarcodeCycle = ThriftConversionUtil.zeroAsNull(molecularBarcodeCycle);
        this.molecularBarcodeLength = ThriftConversionUtil.zeroAsNull(molecularBarcodeLength);
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

    public boolean getIsPaired() {
        return isPaired;
    }

    public Integer getMolecularBarcodeLength() {
        return molecularBarcodeLength;
    }

    public Integer getMolecularBarcodeCycle() {
        return molecularBarcodeCycle;
    }

    public Integer getLastCycle() {
        return lastCycle;
    }

    public Integer getFirstCycleReadLength() {
        return firstCycleReadLength;
    }

    public Integer getFirstCycle() {
        return firstCycle;
    }

    /**
     * Format is 01/03/2010 24:19
     * @return
     */
    @XmlElement(name = "runDate")
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

    public String getRunName() {
        return runName;
    }
    
    public String getRunBarcode() {
        return runBarcode;
    }

    public void addChamber(ZimsIlluminaChamber chamber) {
        chambers.add(chamber);
    }
    
    public Collection<ZimsIlluminaChamber> getChambers() {
        return chambers;
    }


}
