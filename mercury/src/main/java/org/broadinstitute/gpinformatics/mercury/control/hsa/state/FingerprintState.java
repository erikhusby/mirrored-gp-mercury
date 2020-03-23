package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.Collections;
import java.util.Set;

@Entity
@Audited
public class FingerprintState extends State {

    public FingerprintState(String name, MercurySample mercurySample,
                            FiniteStateMachine finiteStateMachine, IlluminaSequencingRunChamber runChamber) {
        super(name, finiteStateMachine, Collections.singleton(mercurySample), Collections.singleton(runChamber));
    }

    public FingerprintState() {
    }

    public FingerprintState(String name, MercurySample mercurySample, FiniteStateMachine finiteStateMachine,
                            Set<IlluminaSequencingRunChamber> aggregationChambers) {
        super(name, finiteStateMachine, Collections.singleton(mercurySample), aggregationChambers);
    }

    public MercurySample getMercurySample() {
        return getMercurySamples().iterator().next();
    }
}
