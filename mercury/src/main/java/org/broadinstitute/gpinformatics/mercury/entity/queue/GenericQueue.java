package org.broadinstitute.gpinformatics.mercury.entity.queue;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.hibernate.annotations.BatchSize;
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
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.List;
import java.util.SortedSet;

@Entity
@Audited
@Table(schema = "mercury", name = "bucket")
public class GenericQueue {

    @Id
    @SequenceGenerator(name = "seq_queue", schema = "mercury", sequenceName = "seq_queue")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_queue")
    private Long queueId;

    @Column(name = "queue_name")
    private String queueName;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private QueueType queueType;

    @Column(name = "queue_description")
    private String queueDescription;

    @OneToMany(mappedBy = "associatedQueue", cascade = CascadeType.ALL)
    @BatchSize(size = 100)
    private SortedSet<QueueGrouping> queueGroupings;

    @JoinTable(name = "product_queue",
            joinColumns = @JoinColumn(referencedColumnName = "queue_id", columnDefinition = "queue_id"),
            inverseJoinColumns = @JoinColumn(referencedColumnName = "product_id", columnDefinition = "product_id"))
    private List<Product> associatedProducts;

}
