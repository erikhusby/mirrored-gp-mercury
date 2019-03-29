package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.hibernate.annotations.BatchSize;
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
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An entry into a lab {@link Bucket}.  An entry is
 * defined by the {@link LabVessel} that should be worked
 * on and the {@link String product order} for which
 * the work is related.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "bucket_entry")
public class BucketEntry {

    public static final Comparator<BucketEntry> byDate = new Comparator<BucketEntry>() {
        @Override
        public int compare(BucketEntry bucketEntryPrime, BucketEntry bucketEntrySecond) {
            int result = bucketEntryPrime.getCreatedDate().compareTo(bucketEntrySecond.getCreatedDate());

            if (result == 0) {
                result =
                        bucketEntryPrime.getProductOrderRanking().compareTo(bucketEntrySecond.getProductOrderRanking());
            }

            return result;
        }
    };

    public static final Comparator<BucketEntry> byPdo = new Comparator<BucketEntry>() {
        @Override
        public int compare(BucketEntry bucketEntryPrime, BucketEntry bucketEntrySecond) {
            int result = bucketEntryPrime.getProductOrder().getBusinessKey().compareTo(bucketEntrySecond.getProductOrder().getBusinessKey());

            if (result == 0) {
                result = bucketEntryPrime.getLabVessel().compareTo(bucketEntrySecond.getLabVessel());
            }

            return result;
        }
    };

    public enum Status {
        Active, Archived
    }

    @SequenceGenerator(name = "SEQ_BUCKET_ENTRY", schema = "mercury", sequenceName = "SEQ_BUCKET_ENTRY")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BUCKET_ENTRY")
    @Id
    @Column(name = "bucket_entry_id")
    private Long bucketEntryId;

    // FetchType.EAGER is a temporary fix for org.hibernate.PropertyNotFoundException: field [tubeType] not found on
    // ... LabVessel_$$_javassist_33 in LabBatchEjb.updateLabBatch
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "lab_vessel_id")
    private LabVessel labVessel;

    @Column(name = "po_business_key")
    private String poBusinessKey;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_order_id")
    private ProductOrder productOrder;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "bucket_existence_id")
    private Bucket bucket;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.Active;

    /*
        TODO Implement this as a separate join table to have the ranking associated directly with the Product
        order, and not duplicated across bucket entries
        todo jmt can this be removed?
     */
    @Column(name = "product_order_ranking")
    private Integer productOrderRanking = 1;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    /**
     * The batch into which the bucket was drained.
     */
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "LAB_BATCH")
    @BatchSize(size = 500)
    private LabBatch labBatch;

    @Column(name = "ENTRY_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private BucketEntryType entryType = BucketEntryType.PDO_ENTRY;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "rework_detail_id")
    private ReworkDetail reworkDetail;

    /**
     * Workflows for this bucket. A null value indicates it has not been initialized. Initialization occurs in
     * getWorkflows()
     */
    @Transient
    private Collection<String> workflows = null;

    protected BucketEntry() {
    }

    public BucketEntry(@Nonnull LabVessel vessel, @Nonnull ProductOrder productOrder, @Nonnull Bucket bucket,
                       @Nonnull BucketEntryType entryType, int productOrderRanking) {
        this.labVessel = vessel;
        vessel.addBucketEntry(this); // todo jmt
        this.bucket = bucket;
        this.entryType = entryType;
        this.productOrderRanking = productOrderRanking;
        this.createdDate = new Date();
        setProductOrder(productOrder);
    }

    public BucketEntry(@Nonnull LabVessel vessel, @Nonnull ProductOrder productOrder, @Nonnull Bucket bucket,
                       @Nonnull BucketEntryType entryType, int productOrderRanking, @Nonnull Date date) {
        this(vessel, productOrder, bucket, entryType, productOrderRanking);
        createdDate = date;
    }

    /**
     * This Constructor is used in a fix-up Test, therefore it can't be removed.
     */
    @Deprecated
    public BucketEntry(@Nonnull LabVessel labVesselIn, @Nonnull ProductOrder productOrder,
                       @Nonnull BucketEntryType entryType) {
        this(labVesselIn, productOrder, null, entryType);
    }


    /**
     * TODO: since this is currently only used in tests it should be moved, or the tests should use a different constructor.
     * This Constructor is only called by tests and another deprecated constructor
     */
    @Deprecated
    public BucketEntry(@Nonnull LabVessel vessel, @Nonnull ProductOrder productOrder, Bucket bucket,
                       @Nonnull BucketEntryType entryType) {
        this(vessel, productOrder, bucket, entryType, 1);
    }

    /**
     * accessor for the Lab vessel associated with this entry into a bucket
     *
     * @return an instance of a lab vessel waiting to be processed
     */
    public LabVessel getLabVessel() {
        return labVessel;
    }

    /** For fixups only */
    public void setLabVessel(LabVessel labVessel) {
        this.labVessel = labVessel;
    }

    /**
     * accessor for the Business key of the product order associated with this entry in a bucket
     *
     * @return a representation of a product order associated with an item in a bucket waiting to be processed
     */
    @Deprecated
    public String getPoBusinessKey() {
        return poBusinessKey;
    }

    public ProductOrder getProductOrder() {
        return productOrder;
    }

    public void setProductOrder(@Nonnull ProductOrder productOrder) {
        this.productOrder = checkNotNull(productOrder);

        //TODO Temporary add until GPLIM-2710 is implemented:  This should be able to be removed now.
        this.poBusinessKey = productOrder.getBusinessKey();
    }

    /**
     * accessor for the bucket to which this entry is associated with
     *
     * @return a specific instance of a Bucket
     */
    public Bucket getBucket() {
        return bucket;
    }

    /**
     * accessor to retrieve the date of creation for this bucket entry
     *
     * @return date representing the date this entry was added to the bucket
     */
    public Date getCreatedDate() {
        return createdDate;
    }

    public Integer getProductOrderRanking() {
        return productOrderRanking;
    }

    public void setProductOrderRanking(Integer productOrderRanking) {
        this.productOrderRanking = productOrderRanking;
    }

    public Long getBucketEntryId() {
        return bucketEntryId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Nullable
    public LabBatch getLabBatch() {
        return labBatch;
    }

    public void setLabBatch(LabBatch labBatch) {
        this.labBatch = labBatch;
        if (labVessel != null) {
            labVessel.clearCaches();
        }
    }

    public BucketEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(BucketEntryType entryType) {
        this.entryType = entryType;
    }

    public ReworkDetail getReworkDetail() {
        return reworkDetail;
    }

    public void setReworkDetail(ReworkDetail reworkDetail) {
        if (this.reworkDetail != null) {
            this.reworkDetail.removeBucketEntry(this);
        }
        this.reworkDetail = reworkDetail;
        if (reworkDetail != null) {
            reworkDetail.addBucketEntry(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !OrmUtil.proxySafeIsInstance(o, BucketEntry.class)) {
            return false;
        }

        BucketEntry that = OrmUtil.proxySafeCast(o, BucketEntry.class);

        return new EqualsBuilder()
                .append(getLabVessel(), that.getLabVessel())
                .append(getBucket(), that.getBucket())
                .append(getCreatedDate(), that.getCreatedDate())
                .isEquals();
    }

    @Override
    public int hashCode () {
        return new HashCodeBuilder()
                .append(getLabVessel())
                .append(getBucket())
                .append(getCreatedDate())
                .toHashCode();
    }

    @Override
    public String toString() {
        String workflowString=null;
        if (workflows == null) {
            workflowString="(not initialized)";
        } else if (workflows.isEmpty()) {
            workflowString = "(no workflows)";
        }

        return String.format("Bucket: %s, %s, Vessel %s, Batch %s, Workflow: %s",
                bucket != null ? bucket.getBucketDefinitionName() : "(no bucket)",
                productOrder != null?productOrder.getBusinessKey():"(no product order)",
                labVessel != null ? labVessel.getLabel() : "(no vessel)",
                workflowString == null ? this.workflows : workflowString,
                labBatch != null ? labBatch.getBatchName() : "(not batched)");
    }

    public int compareTo(BucketEntry other) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(getStatus(), other.getStatus());
        builder.append(getLabVessel(), other.getLabVessel());
        builder.append(getProductOrder(), other.getProductOrder());
        builder.append(getEntryType(), other.getEntryType());

        return builder.toComparison();
    }

    @Nonnull
    private Collection<String> loadWorkflows(WorkflowConfig workflowConfig) {
        Collection<String> workflows = new HashSet<>();
        for (String workflow : getProductOrder().getProductWorkflows()) {
            ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflowByName(workflow);
            for (WorkflowBucketDef workflowBucketDef : productWorkflowDef.getEffectiveVersion().getBuckets()) {
                if (workflowBucketDef.meetsBucketCriteria(labVessel, productOrder)) {
                    workflows.add(workflow);
                }
            }
        }
        return workflows;
    }

    /**
     * Initialize workflows for this batch
     * @return
     */
    @Nonnull
    public Collection<String> getWorkflows(WorkflowConfig workflowConfig) {
        if (workflows == null) {
            workflows = loadWorkflows(workflowConfig);
        }
        return workflows;
    }

    public enum BucketEntryType {
        PDO_ENTRY("PDO Entry"), REWORK_ENTRY("Rework Entry");

        private String name;

        BucketEntryType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
