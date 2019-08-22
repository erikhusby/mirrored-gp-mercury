package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRunChamber;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Entity
@Audited
public class DemultiplexState extends State {

    @ManyToMany(cascade = {CascadeType.PERSIST})
    @JoinTable(schema = "mercury", name = "src_demultiplix_state"
            , joinColumns = {@JoinColumn(name = "DEMULTIPLEX_STATE")}
            , inverseJoinColumns = {@JoinColumn(name = "SEQUENCING_RUN_CHAMBER")})
    private Set<IlluminaSequencingRunChamber> sequencingRunChambers = new HashSet<>();

    protected DemultiplexState() {
    }

    // TODO Should take a list of samples as well. Say I just want to do 2 samples from 1 lane.
    public DemultiplexState(String name, Set<IlluminaSequencingRunChamber> sequencingRunChambers, FiniteStateMachine finiteStateMachine) {
        super(name, finiteStateMachine);

        for (IlluminaSequencingRunChamber sequencingRunChamber: sequencingRunChambers) {
            addSequencingRunChamber(sequencingRunChamber);
        }
    }

    /**
     * If no sample sheet was supplied
     */
    @Override
    public void OnEnter() {

    }

    @Override
    public Optional<Task> getExitTask() {
        return super.getExitTask();
    }

    public IlluminaSequencingRun getRun() {
        if (sequencingRunChambers != null && !sequencingRunChambers.isEmpty()) {
            SequencingRunChamber runChamber = sequencingRunChambers.iterator().next();
            if (OrmUtil.proxySafeIsInstance(runChamber, IlluminaSequencingRunChamber.class)) {
                IlluminaSequencingRunChamber illuminaSequencingRunChamber =
                        OrmUtil.proxySafeCast(runChamber, IlluminaSequencingRunChamber.class);
                return illuminaSequencingRunChamber.getIlluminaSequencingRun();
            }

        }
        return null;
    }

    public String getAnalysisKey() {
        return null;
    }

    public void addSequencingRunChamber(IlluminaSequencingRunChamber sequencingRunChamber) {
        sequencingRunChambers.add(sequencingRunChamber);
    }

    public Set<IlluminaSequencingRunChamber> getSequencingRunChambers() {
        return sequencingRunChambers;
    }

    public void setSequencingRunChambers(
            Set<IlluminaSequencingRunChamber> sequencingRunChamberList) {
        this.sequencingRunChambers = sequencingRunChamberList;
    }
}
