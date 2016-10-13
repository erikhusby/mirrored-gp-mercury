package org.broadinstitute.gpinformatics.mercury.entity.vessel;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * This entity represents a piece of plastic that has been marked as abandoned. If it has multiple positions that
 * can be individually abandoned that data is stored in AbandonVesselPosition.
 */

@Entity
@Audited
@Table(schema = "mercury", name = "abandon_vessel")
@BatchSize(size = 50)
public class AbandonVessel {




    @SequenceGenerator(name = "seq_abandon_vessel", schema = "mercury",  sequenceName = "seq_abandon_vessel")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_abandon_vessel")
    @Id
    @Column(name = "abandon_vessel_id")
    private Long abandonedVesselsId;

    @Column(name = "lab_vessel")
    private Long labVessel;

    @Column(name = "reason")
    private String reason;

    @Column(name = "abandoned_on")
    private Date abandonedOn;

    @OneToMany(mappedBy = "abandonVessel", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @BatchSize(size = 100)
    private Set<AbandonVesselPosition> abandonVesselPosition = new HashSet<>();

    public Long getAbandonedVesselsId()
    {
        return this.abandonedVesselsId;
    }

    public Set<AbandonVesselPosition> getAbandonedVesselPosition() {
        return this.abandonVesselPosition;
    }

    public void removeAbandonedWells(AbandonVesselPosition abandonVesselPosition) { this.abandonVesselPosition.remove(abandonVesselPosition); }

    public void getAbandonedVesselPosition(AbandonVesselPosition abandonVesselPosition)  { this.abandonVesselPosition.add(abandonVesselPosition); }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public AbandonVessel getAbandonVessel() { return this; }

    public long getLabVessel() {
        return labVessel;
    }

    public void setAbandonedVessel(LabVessel labVessel) {
        this.labVessel = labVessel.getLabVesselId();
    }

    public void addAbandonVesselPosition(AbandonVesselPosition abandonedWells) {
        abandonVesselPosition.add(abandonedWells);
        abandonedWells.setAbandonVessel(this);
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
