package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Audited
@Table(schema = "mercury", name = "batch_starting_vessels")
public class LabBatchStartingVessel {

    @Id
    @SequenceGenerator(name = "SEQ_BATCH_STARTING_VESSEL", schema = "mercury",
            sequenceName = "SEQ_BATCH_STARTING_VESSEL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BATCH_STARTING_VESSEL")
    private Long batchStartingVesselId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabBatch labBatch;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel labVessel;

    @Column
    private BigDecimal concentration;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel dilutionVessel;

    @Enumerated(EnumType.STRING)
    private VesselPosition vesselPosition;

    public LabBatchStartingVessel() {
    }

    public LabBatchStartingVessel(@Nonnull LabVessel labVessel, @Nonnull LabBatch labBatch) {
        this(labVessel, labBatch, null);
    }

    public LabBatchStartingVessel(@Nonnull LabVessel labVessel, @Nonnull LabBatch labBatch,
                                  @Nullable BigDecimal concentration) {
        this(labVessel, labBatch, concentration, null);
    }

    public LabBatchStartingVessel(@Nonnull LabVessel labVessel, @Nonnull LabBatch labBatch,
                                  @Nullable BigDecimal concentration, VesselPosition vesselPosition) {
        this.labVessel = labVessel;
        this.labBatch = labBatch;
        this.concentration = concentration;
        this.vesselPosition = vesselPosition;
    }

    public Long getBatchStartingVesselId() {
        return batchStartingVesselId;
    }

    public void setBatchStartingVesselId(Long labBatchStartingVesselId) {
        this.batchStartingVesselId = labBatchStartingVesselId;
    }

    public LabBatch getLabBatch() {
        return labBatch;
    }

    public void setLabBatch(LabBatch batch) {
        this.labBatch = batch;
    }

    public LabVessel getLabVessel() {
        return labVessel;
    }

    public void setLabVessel(LabVessel vessel) {
        this.labVessel = vessel;
    }

    public BigDecimal getConcentration() {
        return concentration;
    }

    public void setConcentration(BigDecimal concentration) {
        this.concentration = concentration;
    }

    public LabVessel getDilutionVessel() {
        return dilutionVessel;
    }

    public void setDilutionVessel(LabVessel dilutionVessel) {
        this.dilutionVessel = dilutionVessel;
        this.dilutionVessel.addDilutionReferences(this);
    }

    public VesselPosition getVesselPosition() {
        return vesselPosition;
    }

    public void setVesselPosition(VesselPosition vesselPosition) {
        this.vesselPosition = vesselPosition;
    }
}
