package org.broadinstitute.gpinformatics.mercury.entity.queue;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.SortedSet;

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

    @JoinColumn(name = "lab_vessel_id")
    @ManyToOne(targetEntity = LabVessel.class)
    private LabVessel containerVessel;

    @OneToMany(mappedBy = "bucket", cascade = CascadeType.PERSIST)
    @Where(clause = "queue_status = 'Active'")
    @BatchSize(size = 100)
    private SortedSet<QueueEntity> queuedEntities;

    @Column(name = "sort_order")
    private Long sortOrder;
}
