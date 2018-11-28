package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 * JAX-RS DTO for Solexa sequencing run
 */
@XmlRootElement
public class SolexaRunBean implements Serializable {
    private String flowcellBarcode;
    private String runBarcode;
    private Date runDate;
    private String machineName;
    private String runDirectory;
    private String reagentBlockBarcode;

    public SolexaRunBean() {
    }

    public SolexaRunBean(String flowcellBarcode, String runBarcode, Date runDate, String machineName,
            String runDirectory, String reagentBlockBarcode) {
        this.flowcellBarcode = flowcellBarcode;
        this.runBarcode = runBarcode;
        this.runDate = runDate;
        this.machineName = machineName;
        this.runDirectory = runDirectory;
        this.reagentBlockBarcode = reagentBlockBarcode;
    }

    public String getFlowcellBarcode() {
        return flowcellBarcode;
    }

    public void setFlowcellBarcode(String flowcellBarcode) {
        this.flowcellBarcode = flowcellBarcode;
    }

    public String getRunBarcode() {
        return runBarcode;
    }

    public void setRunBarcode(String runBarcode) {
        this.runBarcode = runBarcode;
    }

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public String getRunDirectory() {
        return runDirectory;
    }

    public void setRunDirectory(String runDirectory) {
        this.runDirectory = runDirectory;
    }

    public String getReagentBlockBarcode() {
        return reagentBlockBarcode;
    }

    public void setReagentBlockBarcode(String reagentBlockBarcode) {
        this.reagentBlockBarcode = reagentBlockBarcode;
    }


    public SolexaRunBean(IlluminaSequencingRun illuminaSequencingRun) {
        flowcellBarcode = illuminaSequencingRun.getSampleCartridge().getCartridgeBarcode();
        runBarcode = illuminaSequencingRun.getRunBarcode();
        runDate = illuminaSequencingRun.getRunDate();
        if (illuminaSequencingRun.getMachineName() != null) {
            machineName = illuminaSequencingRun.getMachineName();
        }
        runDirectory = illuminaSequencingRun.getRunDirectory();
    }
}
