package org.broadinstitute.gpinformatics.mercury.boundary.hsa.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.dragen.DragenAppContext;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.StateMachine;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.Transition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FiniteStateMachineEngine implements StateMachineEngine {
    private static final Log log = LogFactory.getLog(FiniteStateMachineEngine.class);

    private final DragenAppContext context;

    public FiniteStateMachineEngine(DragenAppContext context) {
        this.context = context;
    }

    @Override
    public void executeProcess(StateMachine stateMachine) {
        State startState = stateMachine.getStartState();
        startState.getOnEnter().ifPresent(task -> task.fireEvent(context));
        stateMachine.addActiveState(startState);

        while (true) {
            Set<State> inactivateStates = new HashSet<>();
            for (State state : stateMachine.getActiveStates()) {
                log.debug("Checking transitions from " + state);
                List<Transition> transitionsFromState = stateMachine.getTransitionsFromState(state);

                for (Transition transition : transitionsFromState) {
                    log.info("Processing Transition " + transition);
                    transition.getTask().ifPresent(t -> t.fireEvent(context));
                    state.getOnExit().ifPresent(t -> t.fireEvent(context));
                    State toState = transition.getToState();
                    stateMachine.addActiveState(toState);
                    toState.getOnEnter().ifPresent(t -> t.fireEvent(context));
                }
                inactivateStates.add(state);
            }

            inactivateStates.forEach(stateMachine::inactivateState);

            if (stateMachine.isComplete()) {
                log.info("Finite State Machine complete");
                return;
            }
        }
    }
}
