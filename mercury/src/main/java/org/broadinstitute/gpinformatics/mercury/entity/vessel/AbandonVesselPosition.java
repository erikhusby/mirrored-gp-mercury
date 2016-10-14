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
    private Long abandonVesselPositionId;

    @ManyToOne(fetch = FetchType.LAZY)
    private AbandonVessel abandonVessel;

    private String vesselPosition;

    private String reason;

    private Date abandonedOn;

    public Long getAbandonVesselPositionId() {
        return abandonVesselPositionId;
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
