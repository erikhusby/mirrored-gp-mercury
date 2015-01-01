package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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

    @SequenceGenerator(name = "SEQ_REWORK_DETAIL", schema = "mercury", sequenceName = "SEQ_REWORK_DETAIL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REWORK_DETAIL")
    @Id
    @Column(name = "rework_detail_id")
    private Long reworkDetailId;

    @OneToMany(mappedBy = "reworkDetail")
    private Set<BucketEntry> bucketEntries = new HashSet<>();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_defined_reason")
    private ReworkReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReworkLevel reworkLevel;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LabEventType reworkStep;

    @Column(name = "rework_comment")
    private String comment;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "add_to_rework_bucket_event_id")
    private LabEvent addToReworkBucketEvent;

    /**
     * For JPA.
     */
    protected ReworkDetail() {
    }

    public ReworkDetail(ReworkReason reason, ReworkLevel reworkLevel,
                        LabEventType reworkStep, String comment, LabEvent addToReworkBucketEvent) {
        this.reason = reason;
        this.reworkLevel = reworkLevel;
        this.reworkStep = reworkStep;
        this.comment = comment;
        this.addToReworkBucketEvent = addToReworkBucketEvent;
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

    public ReworkLevel getReworkLevel() {
        return reworkLevel;
    }

    public LabEventType getReworkStep() {
        return reworkStep;
    }

    public String getComment() {
        return comment;
    }

    public LabEvent getAddToReworkBucketEvent() {
        return addToReworkBucketEvent;
    }

    public ReworkReason getReason() {
        return reason;
    }

    public void setReason(ReworkReason reason) {
        this.reason = reason;
    }
}
