package org.broadinstitute.gpinformatics.mercury.boundary.hsa.engine;

import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.StateMachine;

public interface StateMachineEngine {
    void executeProcess(StateMachine stateMachine);
}
