package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Audited
public class GenericState extends State {

    public GenericState() {
    }

    public GenericState(String name, FiniteStateMachine finiteStateMachine) {
        super(name, finiteStateMachine);
    }
}
