package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.broadinstitute.gpinformatics.mercury.entity.ProductOrderId;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * An entry into a lab {@link Bucket}.  An entry is
 * defined by the {@link LabVessel} that should be worked
 * on and the {@link ProductOrderId product order} for which
 * the work is related.
 */
@Entity
@Audited
@Table (schema = "mercury")
public class BucketEntry {

    @SequenceGenerator (name = "SEQ_BUCKET_ENTRY", schema = "mercury",  sequenceName = "SEQ_BUCKET_ENTRY")
    @GeneratedValue (strategy = GenerationType.SEQUENCE, generator = "SEQ_BUCKET_ENTRY")
    @Id
    private Long bucketEntryId;

    @ManyToOne (cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private LabVessel labVessel;

    private ProductOrderId productOrder;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private Bucket bucketExistence;

    private Integer productOrderRanking;

    public BucketEntry ( LabVessel labVesselIn, ProductOrderId productOrderIn ) {
        this(labVesselIn, productOrderIn, null);
    }

    public BucketEntry ( LabVessel labVesselIn, ProductOrderId productOrderIn, Integer productOrderRankingIn ) {
        labVessel = labVesselIn;
        productOrder = productOrderIn;
        productOrderRanking = productOrderRankingIn;
    }

    public LabVessel getLabVessel() {
        throw new RuntimeException("not implemented yet");
    }

    public ProductOrderId getProductOrderId() {
        throw new RuntimeException("not implemented yet");
    }

}
