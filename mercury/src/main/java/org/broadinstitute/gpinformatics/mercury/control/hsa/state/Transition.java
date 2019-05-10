package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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

/**
 * Defines a transition between states in the finite state machine.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class Transition {

    @Id
    @SequenceGenerator(name = "SEQ_TRANSITION", schema = "mercury", sequenceName = "SEQ_TRANSITION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TRANSITION")
    @Column(name = "TRANSITION_ID")
    private Long transitionId;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST}, optional = false)
    @JoinColumn(name = "FROM_STATE")
    private State fromState;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST}, optional = false)
    @JoinColumn(name = "TO_STATE")
    private State toState;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST}, optional = false)
    @JoinColumn(name = "FINITE_STATE_MACHINE")
    private FiniteStateMachine finiteStateMachine;

    @Column(name = "TRANSITION_NAME")
    private String transitionName;

    public Transition() {
    }

    public Transition(String transitionName, FiniteStateMachine finiteStateMachine) {
        this.transitionName = transitionName;
        this.finiteStateMachine = finiteStateMachine;
    }

    public Long getTransitionId() {
        return transitionId;
    }

    public State getFromState() {
        return fromState;
    }

    public void setFromState(State fromState) {
        this.fromState = fromState;
    }

    public State getToState() {
        return toState;
    }

    public void setToState(State toState) {
        this.toState = toState;
    }

    public String getTransitionName() {
        return transitionName;
    }

    public void setTransitionName(String name) {
        this.transitionName = name;
    }

    public FiniteStateMachine getFiniteStateMachine() {
        return finiteStateMachine;
    }

    public void setFiniteStateMachine(
            FiniteStateMachine finiteStateMachine) {
        this.finiteStateMachine = finiteStateMachine;
    }

    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( !(other instanceof Transition) ) return false;
        Transition castOther = (Transition) other;
        return new EqualsBuilder().append(fromState, castOther.getFromState())
                .append(finiteStateMachine, castOther.getFiniteStateMachine())
                .append(transitionId, castOther.getTransitionId())
                .append(toState, castOther.getToState())
                .append(transitionName, castOther.getTransitionName()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getTransitionId()).append(getFromState()).append(getToState())
                .append(getTransitionName()).append(getFiniteStateMachine()).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("finiteStateMachine", finiteStateMachine)
                .append("transitionId", transitionId)
                .append("transitionName", transitionName)
                .append("fromState", fromState)
                .append("toState", toState)
                .toString();
    }
}
