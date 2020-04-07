package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.Set;

@Entity
@Audited
public class AlignmentState extends State {

    public AlignmentState(String name, FiniteStateMachine finiteStateMachine, Set<MercurySample> mercurySamples,
                          Set<IlluminaSequencingRunChamber> sequencingRunChambers) {
        super(name, finiteStateMachine, mercurySamples, sequencingRunChambers);
    }

    public AlignmentState() {
    }
}
