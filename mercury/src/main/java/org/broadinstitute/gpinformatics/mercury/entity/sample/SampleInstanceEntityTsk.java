package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import javax.persistence.*;


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

    private String subTask;

    public void setSubTask(String subTask) { this.subTask = subTask; }

    public String getSubTask() { return this.subTask; }

    public SampleInstanceEntity getSampleInstanceEntity() { return sampleInstanceEntity;  }

    public void setSampleInstanceEntity(SampleInstanceEntity sampleInstanceEntity) { this.sampleInstanceEntity = sampleInstanceEntity;  }
}
