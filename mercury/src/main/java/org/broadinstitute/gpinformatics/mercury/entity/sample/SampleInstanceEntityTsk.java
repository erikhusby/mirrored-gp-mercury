package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * This class represents the DEV ticket subtasks associated with a SampleInstanceEntity
 * that is made from a PooledTube upload.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "sample_instance_entity_tsk")
@BatchSize(size = 50)
public class SampleInstanceEntityTsk {


    @SequenceGenerator(name = "seq_sample_instance_entity_tsk", schema = "mercury",  sequenceName = "seq_sample_instance_entity_tsk")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_sample_instance_entity_tsk")
    @Id
    private Long sampleInstanceEntityTskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SAMPLE_INSTANCE_ENTITY")
    private SampleInstanceEntity sampleInstanceEntity;

    private int orderOfCreation;

    private String subTask;

    public int getOrderOfCreation() {
        return orderOfCreation;
    }

    public void setOrderOfCreation(int orderOfCreation) {
        this.orderOfCreation = orderOfCreation;
    }

    public void setSubTask(String subTask) { this.subTask = subTask; }

    public String getSubTask() { return this.subTask; }

    public SampleInstanceEntity getSampleInstanceEntity() { return sampleInstanceEntity;  }

    public void setSampleInstanceEntity(SampleInstanceEntity sampleInstanceEntity) { this.sampleInstanceEntity = sampleInstanceEntity;  }
}
