package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.statehandler.AggregationStateHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.statehandler.AlignmentStateHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
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

    @Inject
    private AggregationStateHandler aggregationStateHandler;

    public boolean handleOnEnter(State state) {
        try {
            if (OrmUtil.proxySafeIsInstance(state, AlignmentState.class)) {
                try {
                    return alignmentStateHandler.onEnter(OrmUtil.proxySafeCast(state, AlignmentState.class));
                } catch (IOException e) {
                    log.error("I/O Error handling alignment on enter", e);
                    return false;
                }
            }

            if (OrmUtil.proxySafeIsInstance(state, AggregationState.class)) {
                return aggregationStateHandler.onEnter(OrmUtil.proxySafeCast(state, AggregationState.class));
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to enter state " + state.getStateName(), e);
            return false;
        }

    }

    public boolean handleOnExit(State state) {
        return true;
    }
}