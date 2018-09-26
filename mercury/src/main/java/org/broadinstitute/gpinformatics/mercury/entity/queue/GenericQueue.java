package org.broadinstitute.gpinformatics.mercury.entity.queue;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.annotations.Where;
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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.List;
import java.util.SortedSet;

@Entity
@Audited
@Table(schema = "mercury", name = "queue")
public class GenericQueue {

    @Id
    @SequenceGenerator(name = "seq_queue", schema = "mercury", sequenceName = "seq_queue")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_queue")
    private Long queueId;

    @Column(name = "queue_name")
    private String queueName;

    @Column(name = "queue_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private QueueType queueType;

    @Column(name = "queue_description")
    private String queueDescription;

    @OneToMany(mappedBy = "associatedQueue", cascade = CascadeType.ALL)
    @BatchSize(size = 100)
    @Sort(comparator = QueueGrouping.BySortOrder.class, type = SortType.COMPARATOR)
    private SortedSet<QueueGrouping> queueGroupings;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "product_queue", schema = "mercury",
            joinColumns = @JoinColumn(referencedColumnName = "queue_id", columnDefinition = "queue_id"),
            inverseJoinColumns = @JoinColumn(referencedColumnName = "product_id", columnDefinition = "product_id"))
    private List<Product> associatedProducts;

    public Long getQueueId() {
        return queueId;
    }

    public void setQueueId(Long queueId) {
        this.queueId = queueId;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public QueueType getQueueType() {
        return queueType;
    }

    public void setQueueType(QueueType queueType) {
        this.queueType = queueType;
    }

    public String getQueueDescription() {
        return queueDescription;
    }

    public void setQueueDescription(String queueDescription) {
        this.queueDescription = queueDescription;
    }

    public SortedSet<QueueGrouping> getQueueGroupings() {
        return queueGroupings;
    }

    public void setQueueGroupings(SortedSet<QueueGrouping> queueGroupings) {
        this.queueGroupings = queueGroupings;
    }

    public List<Product> getAssociatedProducts() {
        return associatedProducts;
    }

    public void setAssociatedProducts(List<Product> associatedProducts) {
        this.associatedProducts = associatedProducts;
    }

    public boolean isQueueEmpty() {
        if (getQueueGroupings() != null) {
            for (QueueGrouping queueGrouping : getQueueGroupings()) {
                if (queueGrouping.getQueuedEntities() != null) {
                    for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
                        if (queueEntity.getQueueStatus() == QueueStatus.Active) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }
}
