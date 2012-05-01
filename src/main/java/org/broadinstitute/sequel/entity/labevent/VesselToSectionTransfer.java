package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.VesselContainer;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

/**
 * Represents a transfer from a tube to all positions (wells) in a (plate) section
 */
@Entity
public class VesselToSectionTransfer {
    @Id
    @SequenceGenerator(name = "SEQ_VESSEL_TO_SECTION_TRANSFER", sequenceName = "SEQ_VESSEL_TO_SECTION_TRANSFER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VESSEL_TO_SECTION_TRANSFER")
    private Long vesselToSectionTransferId;

    @ManyToOne
    private LabVessel sourceVessel;
    private String targetSection;
    private VesselContainer targetVesselContainer;

    public VesselToSectionTransfer(LabVessel sourceVessel, String targetSection, VesselContainer targetVesselContainer) {
        this.sourceVessel = sourceVessel;
        this.targetSection = targetSection;
        this.targetVesselContainer = targetVesselContainer;
    }

    protected VesselToSectionTransfer() {
    }

    public LabVessel getSourceVessel() {
        return sourceVessel;
    }

    public String getTargetSection() {
        return targetSection;
    }

    public VesselContainer getTargetVesselContainer() {
        return targetVesselContainer;
    }
}
