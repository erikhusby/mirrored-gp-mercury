package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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

    @ManyToOne(fetch = FetchType.LAZY)
    private Person operator;

    private Boolean testRun;

    private Date runDate;

    @OneToMany(cascade = CascadeType.PERSIST) // todo jmt should this have mappedBy?
    // have to specify name, generated aud name is too long for Oracle
    @JoinTable(schema = "mercury", name = "seq_run_run_cartridges")
    private Set<RunCartridge> runCartridges = new HashSet<RunCartridge>();

    public SequencingRun(String runName, String runBarcode, String machineName, Person operator, Boolean testRun,
            Date runDate, Set<RunCartridge> runCartridges) {
        this.runName = runName;
        this.runBarcode = runBarcode;
        this.machineName = machineName;
        this.operator = operator;
        this.testRun = testRun;
        this.runDate = runDate;
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

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }
}
