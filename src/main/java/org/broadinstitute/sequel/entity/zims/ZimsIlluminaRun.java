package org.broadinstitute.sequel.entity.zims;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

@XmlRootElement(name = "IlluminaRun")
public class ZimsIlluminaRun {
    
    @XmlAttribute(name = "name")
    private String runName;
    
    @XmlAttribute(name = "barcode")
    private String runBarcode;

    @XmlAttribute(name = "flowcellBarcode")
    private String flowcellBarcode;

    @XmlAttribute(name = "sequencer")
    private String sequencer;

    @XmlAttribute(name = "sequencerModel")
    private String sequencerModel;

    private Date runDate;

    @XmlAttribute(name = "firstCycle")
    private Integer firstCycle;

    @XmlAttribute(name = "firstCycleReadLength")
    private Integer firstCycleReadLength;

    @XmlAttribute(name = "lastCycle")
    private Integer lastCycle;

    final SimpleDateFormat dateFormat =  new SimpleDateFormat("MM/dd/yyyy HH:mm");

    @XmlElement(name = "lane")
    private final Collection<ZimsIlluminaChamber> chambers = new HashSet<ZimsIlluminaChamber>();

    public ZimsIlluminaRun() {}

    public ZimsIlluminaRun(String runName,
                           String runBarcode,
                           String flowcellBarcode,
                           String sequencer,
                           String sequencerModel,
                           String runDate,
                           short firstCycle,
                           short firstCycleReadLength,
                           short lastCycle) {
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
        this.firstCycle = zeroAsNull(firstCycle);
        this.firstCycleReadLength = zeroAsNull(firstCycleReadLength);
        this.lastCycle = zeroAsNull(lastCycle);
    }

    private Integer zeroAsNull(int number) {
        if (number == 0) {
            return null;
        }
        else {
            return new Integer(number);
        }
    }

    private Integer getLastCycle() {
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
    @XmlAttribute(name = "runDate")
    public String getRunDateString() {
        return dateFormat.format(runDate);
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
