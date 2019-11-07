package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Collections;

@Entity
@Audited
public class FingerprintState extends State {

    public FingerprintState(String name, MercurySample mercurySample,
                            FiniteStateMachine finiteStateMachine, IlluminaSequencingRunChamber runChamber) {
        super(name, finiteStateMachine, Collections.singleton(mercurySample), Collections.singleton(runChamber));
    }

    public FingerprintState() {
    }

    public MercurySample getMercurySample() {
        return getMercurySamples().iterator().next();
    }
}
