package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;

/**
 * FlowcellDesignation represents a flowcell loading tube (norm, denature, or pooled norm) that is staged
 * for putting on a flowcell. Staging can happen in steps: tube selection, parameter editing, queueing,
 * and allocating to flowcell lanes. Probably not all in one session by one lab user.
 */

@Audited
@Entity
@Table(schema = "mercury")
public class FlowcellDesignation {

    @SequenceGenerator(name = "seq_flowcell_designation", schema = "mercury", sequenceName = "seq_flowcell_designation")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_flowcell_designation")
    @Id
    private Long designationId;

    @Nonnull
    @ManyToOne
    @JoinColumn(name = "LOADING_TUBE" )
    private LabVessel loadingTube;

    /**
     * The designation's lcset chosen by the user, i.e. there was no single
     * LCSET returned after walking the chain of custody.
     */
    @ManyToOne
    @JoinColumn(name = "CHOSEN_LCSET" )
    private LabBatch chosenLcset;

    @Enumerated(EnumType.STRING)
    private IndexType indexType;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private IlluminaFlowcell.FlowcellType sequencerModel;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private Date createdOn;
    private Integer numberLanes;
    private Integer readLength;
    private BigDecimal loadingConc;
    private boolean poolTest;
    private boolean pairedEndRead;

    public enum Priority {
        LOW, NORMAL, HIGH;
    }

    public enum Status {
        UNSAVED("unsaved", true, false),
        QUEUED("Queued", true, true),
        ABANDONED("Abandoned", true, true),
        IN_FCT("In Fct", false, false);

        private final String displayName;
        /** Indicates if the designation fields may be modified. */
        private final boolean modifiable;
        /** Indicates if the designation status may be changed to this value. */
        private final boolean targetable;

        Status(String displayName, boolean modifiable, boolean targetable) {
            this.displayName = displayName;
            this.modifiable = modifiable;
            this.targetable = targetable;
        }

        @Nonnull
        public String getDisplayName() {
            return displayName;
        }

        public boolean isModifiable() {
            return modifiable;
        }

        public boolean isTargetable() {
            return targetable;
        }
    }

    public enum IndexType {
        SINGLE("Single", 8),
        DUAL("Dual", 16),
        NONE("None", 0);

        private final String displayName;
        private final int indexSize;

        IndexType(String displayName, int indexSize) {
            this.displayName = displayName;
            this.indexSize = indexSize;
        }

        @Nonnull
        public String getDisplayName() {
            return displayName;
        }

        public int getIndexSize() {
            return indexSize;
        }

        public static IndexType findByDisplayName(String displayName) {
            for (IndexType indexType : IndexType.values()) {
                if (indexType.getDisplayName().equals(displayName)) {
                    return indexType;
                }
            }
            return null;
        }
    }


    public FlowcellDesignation() {
    }

    public FlowcellDesignation(@Nonnull LabVessel loadingTube, LabBatch chosenLcset,
            IndexType indexType, boolean poolTest, IlluminaFlowcell.FlowcellType sequencerModel,
            Integer numberLanes, Integer readLength, BigDecimal loadingConc, boolean pairedEndRead,
            Status status, Priority priority) {
        this.loadingTube = loadingTube;
        this.chosenLcset = chosenLcset;
        this.createdOn = new Date();
        this.indexType = indexType;
        this.poolTest = poolTest;
        this.sequencerModel = sequencerModel;
        this.numberLanes = numberLanes;
        this.readLength = readLength;
        this.loadingConc = loadingConc;
        this.pairedEndRead = pairedEndRead;
        this.status = status;
        this.priority = priority;
    }

    @Nonnull
    public LabVessel getLoadingTube() {
        return loadingTube;
    }

    public void setLoadingTube(@Nonnull LabVessel loadingTube) {
        this.loadingTube = loadingTube;
    }

    @Nonnull
    public LabBatch getChosenLcset() {
        return chosenLcset;
    }

    public void setChosenLcset(LabBatch lcset) {
        this.chosenLcset = lcset;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public IndexType getIndexType() {
        return indexType;
    }

    public void setIndexType(IndexType indexType) {
        this.indexType = indexType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Integer getNumberLanes() {
        return numberLanes;
    }

    public void setNumberLanes(Integer numberLanes) {
        this.numberLanes = numberLanes;
    }

    public Integer getReadLength() {
        return readLength;
    }

    public void setReadLength(Integer readLength) {
        this.readLength = readLength;
    }

    public BigDecimal getLoadingConc() {
        return loadingConc;
    }

    public void setLoadingConc(BigDecimal loadingConc) {
        this.loadingConc = loadingConc;
    }

    public boolean isPoolTest() {
        return poolTest;
    }

    public void setPoolTest(boolean poolTest) {
        this.poolTest = poolTest;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public IlluminaFlowcell.FlowcellType getSequencerModel() {
        return sequencerModel;
    }

    public void setSequencerModel(IlluminaFlowcell.FlowcellType sequencerModel) {
        this.sequencerModel = sequencerModel;
    }

    public Long getDesignationId() {
        return designationId;
    }

    public boolean isPairedEndRead() {
        return pairedEndRead;
    }

    public void setPairedEndRead(boolean pairedEndRead) {
        this.pairedEndRead = pairedEndRead;
    }

    public static final Comparator<? super FlowcellDesignation> BY_DATE_DESC = new Comparator<FlowcellDesignation>() {
        @Override
        public int compare(FlowcellDesignation o1, FlowcellDesignation o2) {
            return o2.getCreatedOn().compareTo(o1.getCreatedOn());
        }
    };

}
