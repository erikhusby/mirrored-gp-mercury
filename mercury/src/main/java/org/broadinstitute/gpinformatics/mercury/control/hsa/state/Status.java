package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

public enum Status {
    QUEUED("Queued"),
    RUNNING("Running"),
    COMPLETE("Complete"),
    CANCELLED("Cancelled"),
    SUSPENDED("Suspended"),
    FAILED("Failed"),
    RETRY("Retry"),
    UNKNOWN("Unknown");

    private final String statusName;

    Status(String statusName) {

        this.statusName = statusName;
    }

    public String getStatusName() {
        return statusName;
    }
}
