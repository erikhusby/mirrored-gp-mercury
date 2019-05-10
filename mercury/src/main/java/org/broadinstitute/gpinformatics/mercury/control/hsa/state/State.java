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
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST}, optional = false)
    @JoinColumn(name = "FINITE_STATE_MACHINE")
    private FiniteStateMachine finiteStateMachine;

    private String stateName;

    private boolean alive;

    private boolean startState;

    private Date startTime;

    private Date endTime;

    public State() {
    }

    public State(String stateName, FiniteStateMachine finiteStateMachine) {
        this.stateName = stateName;
        this.finiteStateMachine = finiteStateMachine;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String name) {
        this.stateName = name;
    }

    public boolean isStartState() {
        return startState;
    }

    public void setStartState(boolean start) {
        this.startState = start;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean active) {
        this.alive = active;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public FiniteStateMachine getFiniteStateMachine() {
        return finiteStateMachine;
    }

    public void setFiniteStateMachine(
            FiniteStateMachine finiteStateMachine) {
        this.finiteStateMachine = finiteStateMachine;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("stateId", stateId)
                .append("stateName", stateName)
                .append("finiteStateMachine", finiteStateMachine)
                .append("alive", alive)
                .append("task", task)
                .append("startState", startState)
                .append("startTime", startTime)
                .append("end", endTime)
                .toString();
    }
}
