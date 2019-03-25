package org.broadinstitute.gpinformatics.mercury.entity.queue;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;

/**
 * Connects a Queue Grouping to a LabVessel.  Used to track status and who completed and when.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "queue_entity")
public class QueueEntity {

    @Id
    @SequenceGenerator(name = "seq_queue_entity", schema = "mercury", sequenceName = "seq_queue_entity")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_queue_entity")
    private Long queueEntityId;

    @JoinColumn(name = "queue_grouping_id")
    @ManyToOne(targetEntity = QueueGrouping.class)
    private QueueGrouping queueGrouping;

    @Column(name = "queue_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private QueueStatus queueStatus;

    @JoinColumn(name = "lab_vessel_id")
    @ManyToOne(targetEntity = LabVessel.class)
    private LabVessel labVessel;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "completed_on")
    private Date completedOn;

    public QueueEntity() {
    }

    public QueueEntity(QueueGrouping queueGrouping, LabVessel labVessel) {
        this.queueGrouping = queueGrouping;
        this.queueStatus = QueueStatus.Active;
        this.labVessel = labVessel;
    }

    public Long getQueueEntityId() {
        return queueEntityId;
    }

    public void setQueueEntityId(Long queueEntityId) {
        this.queueEntityId = queueEntityId;
    }

    public QueueGrouping getQueueGrouping() {
        return queueGrouping;
    }

    public void setQueueGrouping(QueueGrouping queueGrouping) {
        this.queueGrouping = queueGrouping;
    }

    public QueueStatus getQueueStatus() {
        return queueStatus;
    }

    public void setQueueStatus(QueueStatus queueStatus) {
        this.queueStatus = queueStatus;
    }

    public LabVessel getLabVessel() {
        return labVessel;
    }

    public void setLabVessel(LabVessel labVessel) {
        this.labVessel = labVessel;
    }

    public Long getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(Long completedBy) {
        this.completedBy = completedBy;
    }

    public Date getCompletedOn() {
        return completedOn;
    }

    public void setCompletedOn(Date completedOn) {
        this.completedOn = completedOn;
    }
}
