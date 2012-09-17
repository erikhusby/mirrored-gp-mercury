package org.broadinstitute.sequel.entity.labevent;

import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

/**
 * Super class for all transfers, allows them to share the same table, since they have many columns in common.
 */
@Entity
@Audited
@DiscriminatorOptions(force = true)
public abstract class VesselTransfer {
    @Id
    @SequenceGenerator(name = "SEQ_VESSEL_TRANSFER", sequenceName = "SEQ_VESSEL_TRANSFER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VESSEL_TRANSFER")
    private Long vesselTransferId;
}
