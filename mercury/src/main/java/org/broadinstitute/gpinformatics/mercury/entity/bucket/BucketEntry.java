package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.broadinstitute.gpinformatics.mercury.entity.ProductOrderId;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Comparator;

/**
 * An entry into a lab {@link Bucket}.  An entry is
 * defined by the {@link LabVessel} that should be worked
 * on and the {@link ProductOrderId product order} for which
 * the work is related.
 */
@Entity
@Audited
@Table (schema = "mercury",name = "bucket_entry")
public class BucketEntry {

    @SequenceGenerator (name = "SEQ_BUCKET_ENTRY", schema = "mercury",  sequenceName = "SEQ_BUCKET_ENTRY")
    @GeneratedValue (strategy = GenerationType.SEQUENCE, generator = "SEQ_BUCKET_ENTRY")
    @Id
    @Column(name = "bucket_entry_id")
    private Long bucketEntryId;

    @ManyToOne (cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private LabVessel labVessel;

    @Column(name = "po_business_key")
    private String poBusinessKey;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private Bucket bucketExistence;

    @Column(name = "product_order_ranking")
    private Integer productOrderRanking;

    public BucketEntry ( LabVessel labVesselIn, String poBusinessKey) {
        this(labVesselIn, poBusinessKey, null);
    }

    public BucketEntry ( LabVessel labVesselIn, String poBusinessKey, Integer productOrderRankingIn ) {
        labVessel = labVesselIn;
        this.poBusinessKey = poBusinessKey;
        productOrderRanking = productOrderRankingIn;
    }

    public LabVessel getLabVessel() {
        return this.labVessel;
    }

    public String getPoBusinessKey() {
        return this.poBusinessKey;
    }

    public Bucket getBucketExistence () {
        return bucketExistence;
    }

    public Integer getProductOrderRanking () {
        return productOrderRanking;
    }

    @Override
    public boolean equals ( Object o ) {
        if ( this == o )
            return true;
        if ( !( o instanceof BucketEntry ) )
            return false;

        BucketEntry that = ( BucketEntry ) o;

        if ( labVessel != null ? !labVessel.equals ( that.labVessel ) : that.labVessel != null )
            return false;
        if ( poBusinessKey != null ? !poBusinessKey.equals ( that.poBusinessKey ) : that.poBusinessKey != null )
            return false;

        return true;
    }

    @Override
    public int hashCode () {
        int result = labVessel != null ? labVessel.hashCode () : 0;
        result = 31 * result + ( poBusinessKey != null ? poBusinessKey.hashCode () : 0 );
        return result;
    }
}
