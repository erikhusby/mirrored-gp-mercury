package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.statehandler;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;

import java.io.IOException;

public class StateHandler {

    public boolean onEnter(State state) throws IOException {
        return true;
    }

    public boolean onExit(State state) {
        return true;
    }

}
