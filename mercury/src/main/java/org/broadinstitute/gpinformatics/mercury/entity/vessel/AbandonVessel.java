package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;

/**
 * This entity represents a piece of plastic that has been marked as abandoned. If it has multiple positions that
 * can be individually abandoned, vesselPosition will not be null.
 */

@Entity
@Audited
@Table(schema = "mercury", name = "abandon_vessel")
@BatchSize(size = 50)
public class AbandonVessel {

    @SequenceGenerator(name = "seq_abandon_vessel", schema = "mercury",  sequenceName = "seq_abandon_vessel")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_abandon_vessel")
    @Id
    private Long abandonVesselId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LAB_VESSEL")
    private LabVessel labVessel;

    @Enumerated(EnumType.STRING)
    private Reason reason;

    private Date abandonedOn;

    private Long abandonedBy;

    @Enumerated(EnumType.STRING)
    private VesselPosition vesselPosition;

    public Long getAbandonVesselId()
    {
        return this.abandonVesselId;
    }

    public Reason getReason() { return reason;  }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    public LabVessel getLabVessel() {
        return labVessel;
    }

    public void setAbandonedVessel(LabVessel labVessel) { this.labVessel = labVessel; }

    public void setVesselPosition(VesselPosition vesselPosition) {
        this.vesselPosition = vesselPosition;
    }

    public VesselPosition getVesselPosition() {
        return this.vesselPosition;
    }

    public Date getAbandonedOn() {
        return this.abandonedOn;
    }

    public void setAbandonedOn(Date abandonedOn) {
        this.abandonedOn = abandonedOn;
    }

    public Long getAbandonedBy() {
        return abandonedBy;
    }

    public void setAbandonedBy(Long abandonedBy) {
        this.abandonedBy = abandonedBy;
    }

    public enum Reason implements Displayable {
        FAILED_QC("Failed QC"),
        LAB_INCIDENT("Lab incident"),
        EQUIPMENT_FAILURE("Equipment failure"),
        DEPLETED("Depleted"),
        UNUSED_TUBES_RETURNED_BY_COLLABORATOR("Unused tubes returned by collaborator"),
        ORDER_COMPLETED_DISCARDED_PER_SOP("Order completed - Discarded per SOP");
        private final String value;

        Reason(String value) {
            this.value = value;
        }

        @Override
        public String getDisplayName() {
            return value;
        }
}

    public  Reason[] getReasonList() {  return  Reason.values();   }
}
