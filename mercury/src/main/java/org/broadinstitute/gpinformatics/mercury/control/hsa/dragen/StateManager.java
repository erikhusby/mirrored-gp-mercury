package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.statehandler.AlignmentStateHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.IOException;

@Dependent
public class StateManager {

    private static final Log log = LogFactory.getLog(StateManager.class);

    @Inject
    private AlignmentStateHandler alignmentStateHandler;

    public boolean handleOnEnter(State state) {
        if (OrmUtil.proxySafeIsInstance(state, AlignmentState.class)) {
            try {
                return alignmentStateHandler.onEnter(state);
            } catch (IOException e) {
                log.error("I/O Error handling alignment on error", e);
                return false;
            }
        }

        return true;
    }
}
