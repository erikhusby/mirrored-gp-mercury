package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Audited
@Table(schema = "mercury")
public class SequencingRun {

    @SequenceGenerator(name = "SEQ_SEQUENCING_RUN", schema = "mercury", sequenceName = "SEQ_SEQUENCING_RUN")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SEQUENCING_RUN")
    @Id
    private Long sequencingRunId;

    private String runName;

    private String runBarcode;

    private String machineName;

    private Long operator;

    private Boolean testRun;

    private Date runDate;

    @ManyToOne(cascade = CascadeType.PERSIST)
    // have to specify name, generated aud name is too long for Oracle
    @JoinTable(schema = "mercury", name = "seq_run_run_cartridges",joinColumns = @JoinColumn(name="sequencing_run"),
                      inverseJoinColumns = @JoinColumn(name="run_cartridge"))
    private RunCartridge runCartridge;

    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "run_location_id")
    private OutputDataLocation runLocation;

    public SequencingRun(String runName, String runBarcode, String machineName, Long operator, Boolean testRun,
                         Date runDate, RunCartridge runCartridge, OutputDataLocation runLocation) {
        this.runName = runName;
        this.runBarcode = runBarcode;
        this.machineName = machineName;
        this.operator = operator;
        this.testRun = testRun;
        this.runDate = runDate;
        this.runLocation = runLocation;
        setRunCartridge(runCartridge);
    }

    protected SequencingRun() {
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public String getRunBarcode() {
        return runBarcode;
    }

    public void setRunBarcode(String runBarcode) {
        this.runBarcode = runBarcode;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public Long getOperator() {
        return operator;
    }

    public void setOperator(Long operator) {
        this.operator = operator;
    }

    /**
     * Critical attribute for bass, submissions,
     * and billing.  We know which runs are test
     * runs, and we wish to exclude them from
     * activities like billing or aggregation.
     *
     * @return
     */
    public boolean isTestRun() {
        return testRun;
    }

    /**
     * Flowcell, PTP, Ion chip, 384 well plate for pacbio/sanger,etc.
     *
     * @return
     */
    public RunCartridge getSampleCartridge() {
        return runCartridge;
    }

    public void setRunCartridge(RunCartridge runCartridge) {
        this.runCartridge = runCartridge;
        runCartridge.addSequencingRun(this);
    }

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    public OutputDataLocation getRunLocation() {
        return runLocation;
    }

    public void setRunLocation(OutputDataLocation runLocation) {
        this.runLocation = runLocation;
    }
}
