package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * This entity represents individual positions within a vessel that have been marked as abandoned
 */

@Entity
@Audited
@Table(schema = "mercury", name = "abandon_vessel_position")
@BatchSize(size = 50)
public class AbandonVesselPosition {


    @SequenceGenerator(name = "seq_abandon_vessel_position", schema = "mercury",  sequenceName = "seq_abandon_vessel_position")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_abandon_vessel_position")
    @Id
    @Column(name = "abandon_vessel_position_id")
    private Long abandonVesselPositionId;

    @ManyToOne(fetch = FetchType.LAZY)
    private AbandonVessel abandonVessel;

    @Column(name = "abandon_vessel", insertable = false, updatable = false, nullable = false)
    private Long abandonVesselId;

    @Column(name = "vessel_position")
    private String vesselPosition;

    @Column(name = "reason")
    private String reason;

    @Column(name = "abandoned_on")
    private Date abandonedOn;

    public Long getAbandonVesselPositionId() {
        return abandonVesselPositionId;
    }

    public Long getAbandonVessel() {
        return abandonVesselId;
    }

    public void setAbandonVessel(Long abandonVessel) {
        this.abandonVesselId = abandonVessel;
    }

    public AbandonVessel getLabVessel() {
        return abandonVessel;
    }

    public void setAbandonVessel(AbandonVessel abandonVessel) {
        this.abandonVessel = abandonVessel;
    }

    public String getPosition() {
        return vesselPosition;
    }

    public void setPosition(String vesselPosition) {
        this.vesselPosition = vesselPosition;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getAbandonedOn() {
        return this.abandonedOn;
    }

    public void setAbandonedOn(boolean toggle) {
        if (toggle) {
            this.abandonedOn = new Date();
        } else {
            this.abandonedOn = null;
        }
    }
}
