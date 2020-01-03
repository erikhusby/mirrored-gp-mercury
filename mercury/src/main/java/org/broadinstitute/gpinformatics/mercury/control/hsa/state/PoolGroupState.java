package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.Collections;
import java.util.Set;

@Entity
@Audited
public class PoolGroupState extends State {

    public PoolGroupState() {
    }

    public PoolGroupState(String stateName, FiniteStateMachine stateMachine, Set<MercurySample> mercurySamples) {
        super(stateName, stateMachine, mercurySamples, Collections.emptySet());
    }
}
