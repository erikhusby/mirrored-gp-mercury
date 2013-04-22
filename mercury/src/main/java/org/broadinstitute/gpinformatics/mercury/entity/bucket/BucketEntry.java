package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Comparator;
import java.util.Date;

/**
 * An entry into a lab {@link Bucket}.  An entry is
 * defined by the {@link LabVessel} that should be worked
 * on and the {@link String product order} for which
 * the work is related.
 */
@Entity
@Audited
@Table (schema = "mercury",name = "bucket_entry")
public class BucketEntry  {

    public static final Comparator<BucketEntry> byDate = new Comparator<BucketEntry>() {
        @Override
        public int compare ( BucketEntry bucketEntryPrime, BucketEntry bucketEntrySecond ) {
            int result = bucketEntryPrime.getCreatedDate().compareTo(bucketEntrySecond.getCreatedDate());

            if(result == 0) {
                result = bucketEntryPrime.getProductOrderRanking().compareTo(bucketEntrySecond.getProductOrderRanking());
            }

            return result;
        }
    };

    public static final Comparator<BucketEntry> byPdo = new Comparator<BucketEntry>() {
        @Override
        public int compare ( BucketEntry bucketEntryPrime, BucketEntry bucketEntrySecond ) {
            int result = bucketEntryPrime.getPoBusinessKey().compareTo(bucketEntrySecond.getPoBusinessKey());

            if(result == 0) {
                result = bucketEntryPrime.getLabVessel().compareTo(bucketEntrySecond.getLabVessel());
            }

            return result;
        }
    };

    @SequenceGenerator (name = "SEQ_BUCKET_ENTRY", schema = "mercury",  sequenceName = "SEQ_BUCKET_ENTRY")
    @GeneratedValue (strategy = GenerationType.SEQUENCE, generator = "SEQ_BUCKET_ENTRY")
    @Id
    @Column(name = "bucket_entry_id")
    private Long bucketEntryId;

    @ManyToOne (cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn (name = "lab_vessel_id")
    private LabVessel labVessel;

    @Column(name = "po_business_key")
    private String poBusinessKey;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn (name = "bucket_existence_id")
    private Bucket bucket;

    /*
        TODO SGM:  Implement this as a separate join table to have the ranking associated directly with the Product
        order, and not duplicated across bucket entries
     */
    @Column(name = "product_order_ranking")
    private Integer productOrderRanking = 1;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    /** The batch into which the bucket was drained. */
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private LabBatch labBatch;

    protected BucketEntry () {
    }

    public BucketEntry ( @Nonnull LabVessel labVesselIn, @Nonnull String poBusinessKey, @Nonnull Bucket bucket ) {

        this(labVesselIn, poBusinessKey);
        this.bucket = bucket;

    }

    public BucketEntry ( @Nonnull LabVessel labVesselIn, @Nonnull String poBusinessKey) {
        labVessel = labVesselIn;
        this.poBusinessKey = poBusinessKey;

        createdDate = new Date();

    }

    /**
     * accessor for the Lab vessel associated with this entry into a bucket
     * @return an instance of a lab vessel waiting to be processed
     */
    public LabVessel getLabVessel() {
        return labVessel;
    }

    /**
     * accessor for the Business key of the product order associated with this entry in a bucket
     * @return a representation of a product order associated with an item in a bucket waiting to be processed
     */
    public String getPoBusinessKey() {
        return poBusinessKey;
    }

    /**
     * accessor for the bucket to which this entry is associated with
     * @return a specific instance of a Bucket
     */
    public Bucket getBucket() {
        return bucket;
    }

    /**
     * allows a user to associate a specific bucket instance with this bucket entry
     *
     * @param bucketExistenceIn a specific instance of a Bucket to associate with this entry
     */
    public void setBucket(Bucket bucketExistenceIn) {
        bucket = bucketExistenceIn;
    }

    /**
     * accessor to retrieve the date of creation for this bucket entry
     * @return date representing the date this entry was added to the bucket
     */
    public Date getCreatedDate () {
        return createdDate;
    }

    public Integer getProductOrderRanking () {
        return productOrderRanking;
    }

    public void setProductOrderRanking ( Integer productOrderRanking ) {
        this.productOrderRanking = productOrderRanking;
    }

    public Long getBucketEntryId () {
        return bucketEntryId;
    }

    public LabBatch getLabBatch() {
        return labBatch;
    }

    public void setLabBatch(LabBatch labBatch) {
        this.labBatch = labBatch;
    }

    @Override
    public boolean equals ( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof BucketEntry ) ) {
            return false;
        }

        BucketEntry that = ( BucketEntry ) o;

        if ( labVessel != null ? !labVessel.equals ( that.getLabVessel() ) : that.getLabVessel() != null ) {
            return false;
        }
        if ( poBusinessKey != null ? !poBusinessKey.equals ( that.getPoBusinessKey() ) : that.getPoBusinessKey() != null ) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode () {
        int result = labVessel != null ? labVessel.hashCode () : 0;
        result = 31 * result + ( poBusinessKey != null ? poBusinessKey.hashCode () : 0 );
        return result;
    }

    public int compareTo (BucketEntry other) {
        CompareToBuilder builder = new CompareToBuilder ();
        builder.append(labVessel, other.getLabVessel());
        builder.append(poBusinessKey, other.getPoBusinessKey());

        return builder.toComparison();
    }
}
