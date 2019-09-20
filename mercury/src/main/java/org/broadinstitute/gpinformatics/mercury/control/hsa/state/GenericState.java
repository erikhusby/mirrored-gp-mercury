package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.Collections;

@Entity
@Audited
public class GenericState extends State {

    public GenericState() {
    }

    public GenericState(String name, FiniteStateMachine finiteStateMachine) {
        super(name, finiteStateMachine, Collections.emptySet(), Collections.emptySet());
    }
}
