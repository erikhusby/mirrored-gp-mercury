package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TopOffStateMachineDecorator {
    public static String NAME = "TOPOFF";
    private final FiniteStateMachine finiteStateMachine;

    public enum StateNames {
        HoldForTopOff("Hold For Top Off", false),
        HiSeqX("Illumina HiSeq X 10"),
        Nova("Illumina NovaSeq 6000"),
        SentToRework("Sent To Rework"),
        PoolGroups("Pool Groups", false);

        private final String displayName;
        private final boolean createState;

        private static final Map<String, StateNames> mapNameToState = new HashMap<>();

        static {
            for (StateNames stateNames: StateNames.values()) {
                mapNameToState.put(stateNames.getDisplayName(), stateNames);
            }
        }

        StateNames(String displayName) {
            this(displayName, true);
        }

        StateNames(String displayName, boolean createState) {
            this.displayName = displayName;
            this.createState = createState;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static StateNames getStateByName(String displayname) {
            return mapNameToState.get(displayname);
        }

        public boolean isCreateState() {
            return createState;
        }
    }

    public TopOffStateMachineDecorator(FiniteStateMachine finiteStateMachine) {
        this.finiteStateMachine = finiteStateMachine;
    }

    public State getStateByName(StateNames stateNames) {
        for (State state: finiteStateMachine.getStates()) {
            if (state.getStateName().equals(stateNames.name())) {
                return state;
            }
        }

        return null;
    }

    public List<State> getPoolGroups() {
        return getFiniteStateMachine().getStates().stream()
                .filter(state -> OrmUtil.proxySafeIsInstance(state, PoolGroupState.class))
                .collect(Collectors.toList());
    }

    public FiniteStateMachine getFiniteStateMachine() {
        return finiteStateMachine;
    }
}
