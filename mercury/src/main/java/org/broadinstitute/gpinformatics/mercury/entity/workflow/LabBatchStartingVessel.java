package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
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
    @JoinColumn(name = "LAB_BATCH")
    private LabBatch labBatch;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "LAB_VESSEL")
    private LabVessel labVessel;

    @Column
    private BigDecimal concentration;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "DILUTION_VESSEL")
    private LabVessel dilutionVessel;

    @Enumerated(EnumType.STRING)
    private VesselPosition vesselPosition;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "FLOWCELL_DESIGNATION")
    private FlowcellDesignation flowcellDesignation;

    @Transient
    private String linkedLcset;

    @Transient
    private String productNames;

    public LabBatchStartingVessel() {
    }

    public LabBatchStartingVessel(@Nonnull LabVessel labVessel, @Nonnull LabBatch labBatch) {
        this.labVessel = labVessel;
        this.labBatch = labBatch;
    }

    public LabBatchStartingVessel(@Nonnull LabVessel labVessel, @Nonnull LabBatch labBatch,
                                  @Nullable BigDecimal concentration) {
        this(labVessel, labBatch);
        this.concentration = concentration;
    }

    public LabBatchStartingVessel(@Nonnull LabVessel labVessel, @Nonnull LabBatch labBatch,
                                  @Nullable BigDecimal concentration, VesselPosition vesselPosition) {
        this(labVessel, labBatch, concentration);
        this.vesselPosition = vesselPosition;
    }

    public LabBatchStartingVessel(@Nonnull LabVessel labVessel, @Nonnull LabBatch labBatch,
            @Nullable BigDecimal concentration, VesselPosition vesselPosition, @Nullable String linkedLcset,
            @Nullable String productNames, @Nullable FlowcellDesignation designation) {
        this(labVessel, labBatch, concentration, vesselPosition);
        this.linkedLcset = linkedLcset;
        this.productNames = productNames;
        this.flowcellDesignation = designation;
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

    public String getLinkedLcset() {
        return linkedLcset;
    }

    @Transient
    public void setLinkedLcset(String linkedLcset) {
        this.linkedLcset = linkedLcset;
    }

    public String getProductNames() {
        return productNames;
    }

    @Transient
    public void setProductNames(String productNames) {
        this.productNames = productNames;
    }

    public FlowcellDesignation getFlowcellDesignation(){
        return flowcellDesignation;
    }

    public void setFlowcellDesignation( FlowcellDesignation flowcellDesignation){
        this.flowcellDesignation = flowcellDesignation;
    }
}
