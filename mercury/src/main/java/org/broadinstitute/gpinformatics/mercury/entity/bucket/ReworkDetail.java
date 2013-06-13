package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * @author breilly
 */
@Entity
@Audited
@Table(schema = "mercury", name = "rework_detail")
public class ReworkDetail {

    @SequenceGenerator(name = "SEQ_REWORK_DETAIL", schema = "mercury",  sequenceName = "SEQ_REWORK_DETAIL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REWORK_DETAIL")
    @Id
    @Column(name = "rework_detail_id")
    private Long reworkDetailId;

    @OneToMany(mappedBy = "reworkDetail")
    private Set<BucketEntry> bucketEntries = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReworkEntry.ReworkReason reworkReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReworkEntry.ReworkLevel reworkLevel;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LabEventType reworkStep;

    @Column
    private String comment;

    /** For JPA. */
    protected ReworkDetail() {}

    public ReworkDetail(ReworkEntry.ReworkReason reworkReason,
                        ReworkEntry.ReworkLevel reworkLevel,
                        LabEventType reworkStep, String comment) {
        this.reworkReason = reworkReason;
        this.reworkLevel = reworkLevel;
        this.reworkStep = reworkStep;
        this.comment = comment;
    }

    public Set<BucketEntry> getBucketEntries() {
        return bucketEntries;
    }

    public void addBucketEntry(BucketEntry bucketEntry) {
        bucketEntries.add(bucketEntry);
    }

    public void removeBucketEntry(BucketEntry bucketEntry) {
        bucketEntries.remove(bucketEntry);
    }

    public ReworkEntry.ReworkReason getReworkReason() {
        return reworkReason;
    }

    public ReworkEntry.ReworkLevel getReworkLevel() {
        return reworkLevel;
    }

    public LabEventType getReworkStep() {
        return reworkStep;
    }

    public String getComment() {
        return comment;
    }
}
