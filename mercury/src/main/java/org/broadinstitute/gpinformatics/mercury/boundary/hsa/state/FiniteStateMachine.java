package org.broadinstitute.gpinformatics.mercury.boundary.hsa.state;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class FiniteStateMachine implements StateMachine {

    private static final Log log = LogFactory.getLog(FiniteStateMachine.class);

    private List<State> states;
    private List<Transition> transitions;
    private Set<State> activeStates;

    public FiniteStateMachine() {
        activeStates = new LinkedHashSet<>();
    }

    @Override
    public List<State> getStates() {
        return states;
    }

    @Override
    public void setStates(List<State> states) {
        this.states = states;
    }

    @Override
    public List<Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
    }

    @Override
    public State getStartState() {
        return states.stream().filter(State::isStart).findFirst().get();
    }

    @Override
    public List<Transition> getTransitionsFromState(State state) {
        return transitions.stream()
                .filter(t -> t.getFromState().equals(state))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isComplete() {
        return activeStates.isEmpty();
    }

    @Override
    public void addActiveState(State state) {
        if (!activeStates.add(state)) {
            log.info("State already added to active records " + state);
        }
    }

    @Override
    public Set<State> getActiveStates() {
        return activeStates;
    }

    @Override
    public void inactivateState(State state) {
        activeStates.remove(state);
    }
}
