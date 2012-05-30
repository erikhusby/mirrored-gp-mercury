package org.broadinstitute.sequel.entity.run;

import org.broadinstitute.sequel.entity.person.Person;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import java.util.HashSet;
import java.util.Set;

@Entity
public class SequencingRun {

    @SequenceGenerator(name = "SEQ_SEQUENCING_RUN", sequenceName = "SEQ_SEQUENCING_RUN")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SEQUENCING_RUN")
    @Id
    private Long sequencingRunId;

    private String runName;

    private String runBarcode;

    private String machineName;

    @ManyToOne(fetch = FetchType.LAZY)
    private Person operator;

    private Boolean testRun;

    @OneToMany(cascade = CascadeType.PERSIST)
    private Set<RunCartridge> runCartridges = new HashSet<RunCartridge>();

    public SequencingRun(String runName, String runBarcode, String machineName, Person operator, Boolean testRun,
            Set<RunCartridge> runCartridges) {
        this.runName = runName;
        this.runBarcode = runBarcode;
        this.machineName = machineName;
        this.operator = operator;
        this.testRun = testRun;
        this.runCartridges = runCartridges;
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

    public Person getOperator() {
        return operator;
    }

    public void setOperator(Person operator) {
        this.operator = operator;
    }

    /**
     * Critical attribute for bass, submissions,
     * and billing.  We know which runs are test
     * runs, and we wish to exclude them from
     * activities like billing or aggregation.
     * @return
     */
    public boolean isTestRun() {
        return testRun;
    }

    /**
     * Flowcell, PTP, Ion chip, 384 well plate for pacbio/sanger,etc.
     * @return
     */
    public Iterable<RunCartridge> getSampleCartridge() {
        return runCartridges;
    }
}
