package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Date;

/**
 * A control that is not significant enough to be registered as a sample.  An example is the controls used to fill a
 * fingerprinting plate to 48 or 96 wells.
 */
@Entity
@Audited
public class ControlReagent  extends Reagent {

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "CONTROL")
    private Control control;

    public ControlReagent(@Nullable String reagentName, @Nullable String lot, @Nullable Date expiration,
            Control control) {
        super(reagentName, lot, expiration);
        this.control = control;
    }

    /** For JPA. */
    ControlReagent() {
    }

    public Control getControl() {
        return control;
    }
}
