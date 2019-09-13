package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.control.hsa.FastQListBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentTask;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Audited
public class AlignmentState extends State {

    @ManyToMany
    @JoinTable(schema = "mercury", name = "sample_alignment_state"
            , joinColumns = {@JoinColumn(name = "ALIGNMENT_STATE")}
            , inverseJoinColumns = {@JoinColumn(name = "MERCURY_SAMPLE")})
    private Set<MercurySample> mercurySamples = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST})
    @JoinTable(schema = "mercury", name = "src_demultiplix_state"
            , joinColumns = {@JoinColumn(name = "DEMULTIPLEX_STATE")}
            , inverseJoinColumns = {@JoinColumn(name = "SEQUENCING_RUN_CHAMBER")})
    private Set<IlluminaSequencingRunChamber> sequencingRunChambers = new HashSet<>();

    public AlignmentState() {
    }

    public AlignmentState(String name, FiniteStateMachine finiteStateMachine, Set<MercurySample> mercurySamples,
                          Set<IlluminaSequencingRunChamber> sequencingRunChambers) {
        super(name, finiteStateMachine);

        this.mercurySamples = mercurySamples;
        this.sequencingRunChambers = sequencingRunChambers;
    }

    @Override
    public void onEnter() {
        for (Task t: getTasks()) {
            if (OrmUtil.proxySafeIsInstance(t, AlignmentTask.class)) {
                AlignmentTask alignmentTask = OrmUtil.proxySafeCast(t, AlignmentTask.class);
                if (!alignmentTask.getOutputDir().exists()) {
                    alignmentTask.getOutputDir().mkdir();
                }
                if (!alignmentTask.getFastQList().exists()) {
                    FastQListBuilder fastQListBuilder = new FastQListBuilder();
                }
            }
        }
    }

    public Set<MercurySample> getMercurySamples() {
        return mercurySamples;
    }

    public Set<IlluminaSequencingRunChamber> getSequencingRunChambers() {
        return sequencingRunChambers;
    }
}
