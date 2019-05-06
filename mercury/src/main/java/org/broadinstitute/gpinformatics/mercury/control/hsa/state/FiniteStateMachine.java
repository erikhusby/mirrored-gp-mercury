package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Audited
@Table(schema = "mercury")
public class FiniteStateMachine {

    @Id
    @SequenceGenerator(name = "SEQ_FINITE_STATE_MACHINE", schema = "mercury", sequenceName = "SEQ_FINITE_STATE_MACHINE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FINITE_STATE_MACHINE")
    @Column(name = "FINITE_STATE_MACHINE_ID")
    private Long finiteStateMachineId;

    @OneToMany(cascade = CascadeType.PERSIST)
    private List<State> states;

    @OneToMany(cascade = CascadeType.PERSIST)
    private List<Transition> transitions;

    @Enumerated(EnumType.STRING)
    private Status status;

    public FiniteStateMachine() {
    }

    public List<State> getStates() {
        return states;
    }

    public void setStates(List<State> states) {
        this.states = states;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
    }

    public State getStartState() {
        return states.stream().filter(State::isStart).findFirst().get();
    }

    public List<Transition> getTransitionsFromState(State state) {
        return transitions.stream()
                .filter(t -> t.getFromState().equals(state))
                .collect(Collectors.toList());
    }

    public Long getFiniteStateMachineId() {
        return finiteStateMachineId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isComplete() {
        return states.stream().noneMatch(State::isActive);
    }

    public List<State> getActiveStates() {
        return states.stream().filter(State::isActive).collect(Collectors.toList());
    }
}
