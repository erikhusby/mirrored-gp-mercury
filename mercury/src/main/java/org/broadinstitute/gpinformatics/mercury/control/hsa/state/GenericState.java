package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.Collections;
import java.util.Set;

@Entity
@Audited
public class GenericState extends State {

    public GenericState() {
    }

    public GenericState(String name, FiniteStateMachine finiteStateMachine) {
        super(name, finiteStateMachine, Collections.emptySet(), Collections.emptySet());
    }

    public GenericState(String name, FiniteStateMachine finiteStateMachine, Set<MercurySample> mercurySamples) {
        super(name, finiteStateMachine, mercurySamples, Collections.emptySet());
    }
}
