package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import javax.persistence.*;


@Entity
@Audited
@Table(schema = "mercury", name = "sample_instance_sub_tasks")
@BatchSize(size = 50)
public class SampleInstanceSubTasks {


    @SequenceGenerator(name = "seq_sample_instance_sub_tasks", schema = "mercury",  sequenceName = "seq_sample_instance_sub_tasks")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_sample_instance_sub_tasks")
    @Id
    private Long sampleInstanceSubTasksId;

    @ManyToOne(fetch = FetchType.LAZY)
    private SampleInstance sampleInstance;

    private String subTask;

    public void setSubTask(String subTask) { this.subTask = subTask; }

    public String getSubTask() { return this.subTask; }

    public SampleInstance getSampleInstance() { return sampleInstance;  }

    public void setSampleInstance(SampleInstance sampleInstance) { this.sampleInstance = sampleInstance;  }
}
