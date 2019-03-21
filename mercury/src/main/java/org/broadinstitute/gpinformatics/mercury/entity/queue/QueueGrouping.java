package org.broadinstitute.gpinformatics.mercury.entity.queue;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Formula;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

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
import javax.persistence.Version;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Audited
@Table(schema = "mercury", name = "queue_grouping")
public class QueueGrouping {

    @Id
    @SequenceGenerator(name = "seq_queue_grouping", schema = "mercury", sequenceName = "seq_queue_grouping")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_queue_grouping")
    private Long queueGroupingId;

    @Column(name = "queue_group_text")
    private String queueGroupingText;

    @JoinColumn(name = "queue_id")
    @ManyToOne(targetEntity = GenericQueue.class)
    private GenericQueue associatedQueue;

    @OneToMany(mappedBy = "queueGrouping", cascade = CascadeType.PERSIST)
    @BatchSize(size = 100)
    private List<QueueEntity> queuedEntities;

    @Column(name = "queue_origin")
    @Enumerated(EnumType.STRING)
    private QueueOrigin queueOrigin;

    @Column(name = "sort_order")
    private Long sortOrder;

    @Version
    @Column(name = "version")
    private long version;

    @Column(name = "queue_priority_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private QueuePriority queuePriority;

    @Column(name = "queue_specialization")
    @Enumerated(EnumType.STRING)
    private QueueSpecialization queueSpecialization;

    public QueueGrouping() {
    }

    public QueueGrouping(String readableText, GenericQueue genericQueue, QueueSpecialization queueSpecialization) {
        this.queueGroupingText = readableText;
        this.sortOrder = Long.MAX_VALUE;
        this.associatedQueue = genericQueue;
        this.queueSpecialization = queueSpecialization;

        this.queuedEntities = new ArrayList<>();
    }

    public Long getQueueGroupingId() {
        return queueGroupingId;
    }

    public void setQueueGroupingId(Long queueGroupingId) {
        this.queueGroupingId = queueGroupingId;
    }

    public String getQueueGroupingText() {
        return queueGroupingText;
    }

    public void setQueueGroupingText(String queueGroupingText) {
        this.queueGroupingText = queueGroupingText;
    }

    public GenericQueue getAssociatedQueue() {
        return associatedQueue;
    }

    public void setAssociatedQueue(GenericQueue associatedQueue) {
        this.associatedQueue = associatedQueue;
    }

    public List<QueueEntity> getQueuedEntities() {
        return queuedEntities;
    }

    public void setQueuedEntities(List<QueueEntity> queuedEntities) {
        this.queuedEntities = queuedEntities;
    }

    public Long getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Long sortOrder) {
        this.sortOrder = sortOrder;
    }

    protected long getVersion() {
        return version;
    }

    protected void setVersion(long version) {
        this.version = version;
    }

    public static final BySortOrder BY_SORT_ORDER = new BySortOrder();

    public boolean shouldSkipPriorityCheck() {
        boolean isRepeat = false;
        for (QueueEntity queueEntity : getQueuedEntities()) {
            if (queueEntity.getQueueStatus() == QueueStatus.Repeat) {
                isRepeat = true;
                break;
            }
        }

        return getQueuePriority().shouldSkipPriorityCheck() || isRepeat;
    }

    public static class BySortOrder implements Comparator<QueueGrouping> {
        @Override
        public int compare(QueueGrouping o1, QueueGrouping o2) {
            return o1.getSortOrder().compareTo(o2.getSortOrder());
        }
    };

    public QueuePriority getQueuePriority() {
        return queuePriority;
    }

    public void setQueuePriority(QueuePriority queuePriority) {
        this.queuePriority = queuePriority;
    }

    public QueueOrigin getQueueOrigin() {
        return queueOrigin;
    }

    public void setQueueOrigin(QueueOrigin queueOrigin) {
        this.queueOrigin = queueOrigin;
    }

    public QueueSpecialization getQueueSpecialization() {
        return queueSpecialization;
    }

    public void setQueueSpecialization(QueueSpecialization queueSpecialization) {
        this.queueSpecialization = queueSpecialization;
    }
}
