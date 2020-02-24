package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.statehandler;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;

import java.io.IOException;

public abstract class StateHandler<T extends State> {

    public boolean onEnter(T state) throws IOException {
        return true;
    }

    public boolean onExit(T state) {
        return true;
    }

}
