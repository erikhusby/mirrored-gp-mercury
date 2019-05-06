package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Optional;

@Entity
@Audited
@Table(schema = "mercury")
public abstract class State {

    @Id
    @SequenceGenerator(name = "SEQ_STATE", schema = "mercury", sequenceName = "SEQ_STATE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_STATE")
    @Column(name = "STATE_ID")
    private Long stateId;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST}, optional = false)
    @JoinColumn(name = "task")
    private Task task;

    private String name;

    private boolean active;

    private boolean start;

    public State() {
    }

    public State(String name) {
        this.name = name;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("stateId", stateId)
                .append("name", name)
                .append("active", active)
                .append("task", task)
                .append("start", start)
                .toString();
    }
}
