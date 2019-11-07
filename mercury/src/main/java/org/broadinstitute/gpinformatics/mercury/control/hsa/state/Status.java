package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import java.util.HashMap;
import java.util.Map;

public enum Status {
    QUEUED("Queued"),
    RUNNING("Running"),
    TRIAGE("Triage"),
    COMPLETE("Complete"),
    CANCELLED("Cancelled"),
    SUSPENDED("Suspended"),
    FAILED("Failed"),
    RETRY("Retry"),
    UNKNOWN("Unknown");

    private final String statusName;
    private static Map<String, Status> mapNameToStatus = new HashMap<>();

    static {
        for (Status status: Status.values()) {
            mapNameToStatus.put(status.getStatusName(), status);
        }
    }

    Status(String statusName) {

        this.statusName = statusName;
    }

    public String getStatusName() {
        return statusName;
    }

    public static Status getStatusByName(String name) {
        return mapNameToStatus.get(name);
    }
}
