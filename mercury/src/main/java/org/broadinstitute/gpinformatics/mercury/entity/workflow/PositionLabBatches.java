package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Audited
@Table(schema = "mercury")
public class PositionLabBatches {
    @Id
    @SequenceGenerator(name = "SEQ_POSITION_LAB_BATCHES", schema = "mercury", sequenceName = "SEQ_POSITION_LAB_BATCHES")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_POSITION_LAB_BATCHES")
    private Long positionLabBatchesId;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable(schema = "mercury", name = "PLB_lab_batch"
            , joinColumns = {@JoinColumn(name = "POSITION_LAB_BATCH")}
            , inverseJoinColumns = {@JoinColumn(name = "LAB_BATCH")})
    private Set<LabBatch> labBatchSet = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "LAB_EVENT")
    private LabEvent labEvent;

    @Enumerated(EnumType.STRING)
    private VesselPosition vesselPosition;

    public Set<LabBatch> getLabBatchSet() {
        return labBatchSet;
    }
}
