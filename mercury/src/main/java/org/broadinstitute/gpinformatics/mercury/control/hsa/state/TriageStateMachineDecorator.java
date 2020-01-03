package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

public class TriageStateMachineDecorator {
    public static String NAME = "TRIAGE";

    private final FiniteStateMachine finiteStateMachine;

    public TriageStateMachineDecorator(FiniteStateMachine finiteStateMachine) {
        this.finiteStateMachine = finiteStateMachine;
    }

    public State getOverrideInSpecState() {
        return finiteStateMachine.getStates().iterator().next();
    }

    public FiniteStateMachine getFiniteStateMachine() {
        return finiteStateMachine;
    }
}
