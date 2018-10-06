package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Audited
@Table(schema = "mercury")
public class LabBatchSet {
    @Id
    @SequenceGenerator(name = "SEQ_LAB_BATCH_SET", schema = "mercury", sequenceName = "SEQ_LAB_BATCH_SET")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_BATCH_SET")
    private Long labBatchId;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name = "lab_batch", nullable = false)
    private Set<LabBatch> labBatchSet = new HashSet<>();

    public Set<LabBatch> getLabBatchSet() {
        return labBatchSet;
    }
}
