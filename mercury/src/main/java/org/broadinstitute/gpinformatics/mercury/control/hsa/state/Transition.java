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
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Optional;

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
    @Column(name = "FROM_STATE")
    private State fromState;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST}, optional = false)
    @Column(name = "TO_STATE")
    private State toState;

    private String name;

    public Transition() {
    }

    public Transition(String name) {
        this.name = name;
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
                .append(transitionId, castOther.getTransitionId())
                .append(toState, castOther.getToState())
                .append(name, castOther.getName()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getTransitionId()).append(getFromState()).append(getToState())
                .append(getName()).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("transitionId", transitionId)
                .append("name", name)
                .append("fromState", fromState)
                .append("toState", toState)
                .toString();
    }
}
