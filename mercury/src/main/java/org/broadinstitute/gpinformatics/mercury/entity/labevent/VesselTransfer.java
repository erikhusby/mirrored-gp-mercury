package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * Super class for all transfers, allows them to share the same table, since they have many columns in common.
 */
@Entity
@Audited
@Table(schema = "mercury")
@DiscriminatorOptions(force = true)
public abstract class VesselTransfer {
    @Id
    @SequenceGenerator(name = "SEQ_VESSEL_TRANSFER", schema = "mercury", sequenceName = "SEQ_VESSEL_TRANSFER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VESSEL_TRANSFER")
    private Long vesselTransferId;

    @ManyToMany(cascade = {CascadeType.PERSIST})
    Set<MessageMetaData> sourceMetaData = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST})
    Set<MessageMetaData> targetMetaData = new HashSet<>();

    public Set<MessageMetaData> getSourceMetaData() {
        return sourceMetaData;
    }

    public void setSourceMetaData(Set<MessageMetaData> sourceMetaData) {
        this.sourceMetaData = sourceMetaData;
    }

    public Set<MessageMetaData> getTargetMetaData() {
        return targetMetaData;
    }

    public void setTargetMetaData(Set<MessageMetaData> targetMetaData) {
        this.targetMetaData = targetMetaData;
    }
}
