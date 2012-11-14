package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents a transfer from a tube to all positions (wells) in a (plate) section
 */
@Entity
@Audited
@Table(schema = "mercury")
public class VesselToSectionTransfer extends VesselTransfer {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel sourceVessel;

    @Enumerated(EnumType.STRING)
    private SBSSection targetSection;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel targetVessel;

    @Index(name = "ix_vtst_lab_event")
    @ManyToOne
    private LabEvent labEvent;

    public VesselToSectionTransfer(LabVessel sourceVessel, SBSSection targetSection, VesselContainer<?> targetVesselContainer, LabEvent labEvent) {
        this.sourceVessel = sourceVessel;
        this.targetSection = targetSection;
        this.labEvent = labEvent;
        this.targetVessel = targetVesselContainer.getEmbedder();
    }

    protected VesselToSectionTransfer() {
    }

    public LabVessel getSourceVessel() {
        return sourceVessel;
    }

    public SBSSection getTargetSection() {
        return targetSection;
    }

    public VesselContainer<?> getTargetVesselContainer() {
        return targetVessel.getContainerRole();
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }
}
