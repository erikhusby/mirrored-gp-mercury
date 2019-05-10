package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Audited
@Table(schema = "mercury")
public abstract class Task {

    @Id
    @SequenceGenerator(name = "SEQ_TASK", schema = "mercury", sequenceName = "SEQ_TASK")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TASK")
    @Column(name = "TASK_ID")
    private Long taskId;

    @Enumerated(EnumType.STRING)
    private Status status;

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, mappedBy = "task")
    private Set<State> states = new HashSet<>();

    public Task() {
        status = Status.QUEUED;
    }

    public Task(Status status) {
        this.status = status;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Set<State> getStates() {
        return states;
    }

    public void setStates(Set<State> states) {
        this.states = states;
    }
}
