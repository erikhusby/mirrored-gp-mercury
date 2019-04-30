package org.broadinstitute.gpinformatics.mercury.boundary.hsa.state;

import java.util.List;
import java.util.Set;

public interface StateMachine {
    public List<State> getStates();

    public void setStates(List<State> states);

    public State getStartState();

    public List<Transition> getTransitions();

    public void setTransitions(List<Transition> transitions);

    public List<Transition> getTransitionsFromState(State state);

    public boolean isComplete();

    void addActiveState(State currentState);

    Set<State> getActiveStates();

    void inactivateState(State state);
}
