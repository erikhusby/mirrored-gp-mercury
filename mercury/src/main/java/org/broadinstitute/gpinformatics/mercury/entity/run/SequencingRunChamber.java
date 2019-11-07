package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds run chamber level information about a run.
 */
@Entity
@Audited
@Table(schema = "mercury")
public abstract class SequencingRunChamber {

    @SequenceGenerator(name = "SEQ_SEQUENCING_RUN_CHAMBER", schema = "mercury", sequenceName = "SEQ_SEQUENCING_RUN_CHAMBER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SEQUENCING_RUN_CHAMBER")
    @Id
    private Long sequencingRunChamberId;

    /** For JPA. */
    protected SequencingRunChamber() {
    }
}
