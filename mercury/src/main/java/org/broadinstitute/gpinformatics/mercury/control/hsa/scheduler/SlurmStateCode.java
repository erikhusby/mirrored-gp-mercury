package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import java.util.HashMap;
import java.util.Map;

public enum SlurmStateCode {
    BOOT_FAIL,
    CANCELLED,
    COMPLETED,
    COMPLETING,
    DEADLINE,
    FAILED,
    NODE_FAIL,
    OUT_OF_MEMORY,
    PENDING,
    PREEMPTED,
    RUNNING,
    RESV_DEL_HOLD,
    REQUEUE_FED,
    REQUEUE_HOLD,
    REQUEUED,
    RESIZING,
    REVOKED,
    SIGNALING,
    SPECIAL_EXIT,
    STAGE_OUT,
    STOPPED,
    SUSPENDED,
    TIMEOUT;

    private static final Map<String, SlurmStateCode> MAP_NAME_TO_STATE_CODE =
            new HashMap<>(SlurmStateCode.values().length);

    static {
        for (SlurmStateCode stateCode : SlurmStateCode.values()) {
            MAP_NAME_TO_STATE_CODE.put(stateCode.name(), stateCode);
        }
    }

    public static SlurmStateCode getByName(String name) {
        return MAP_NAME_TO_STATE_CODE.get(name);
    }
}
