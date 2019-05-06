package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Audited
@Table(schema = "mercury")
public class GenericState extends State {

    public GenericState(String name) {
        super(name);
    }
}
