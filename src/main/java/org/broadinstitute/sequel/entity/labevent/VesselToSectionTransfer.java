package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.OrmUtil;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.SBSSection;
import org.broadinstitute.sequel.entity.vessel.VesselContainer;
import org.broadinstitute.sequel.entity.vessel.VesselContainerEmbedder;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

/**
 * Represents a transfer from a tube to all positions (wells) in a (plate) section
 */
@Entity
@Audited
public class VesselToSectionTransfer {
    @Id
    @SequenceGenerator(name = "SEQ_VESSEL_TO_SECTION_TRANSFER", sequenceName = "SEQ_VESSEL_TO_SECTION_TRANSFER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VESSEL_TO_SECTION_TRANSFER")
    private Long vesselToSectionTransferId;

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
        return OrmUtil.proxySafeCast(targetVessel, VesselContainerEmbedder.class).getVesselContainer();
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }
}
