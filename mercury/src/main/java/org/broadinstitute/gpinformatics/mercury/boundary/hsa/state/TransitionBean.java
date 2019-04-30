package org.broadinstitute.gpinformatics.mercury.boundary.hsa.state;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Optional;

public class TransitionBean implements Transition {

    private String name;
    private State fromState;
    private State toState;
    private Task task;

    public TransitionBean(String name) {
        this.name = name;
    }

    @Override
    public State getFromState() {
        return fromState;
    }

    @Override
    public void setFromState(State fromState) {
        this.fromState = fromState;
    }

    @Override
    public State getToState() {
        return toState;
    }

    @Override
    public void setToState(State toState) {
        this.toState = toState;
    }

    @Override
    public void setTask(Task task) {
        this.task = task;
    }

    @Override
    public Optional<Task> getTask() {
        return Optional.ofNullable(task);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( !(other instanceof Transition) ) return false;
        Transition castOther = (Transition) other;
        return new EqualsBuilder().append(fromState, castOther.getFromState())
                .append(toState, castOther.getToState())
                .append(name, castOther.getName())
                .append(task, castOther.getTask()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getFromState()).append(getToState()).append(getTask())
                .append(getName()).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("fromState", fromState)
                .append("toState", toState)
                .append("task", task)
                .toString();
    }
}
