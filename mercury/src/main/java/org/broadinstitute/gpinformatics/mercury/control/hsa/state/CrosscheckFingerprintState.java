package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.Collections;
import java.util.Set;

@Entity
@Audited
public class CrosscheckFingerprintState extends State {

    public CrosscheckFingerprintState() {
    }

    public CrosscheckFingerprintState(String name, MercurySample mercurySample,
                                      FiniteStateMachine finiteStateMachine, Set<IlluminaSequencingRunChamber> runChambers) {
        super(name, finiteStateMachine, Collections.singleton(mercurySample), runChambers);
    }

    public MercurySample getMercurySample() {
        return getMercurySamples().iterator().next();
    }
}
