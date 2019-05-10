package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

public enum Status {
    QUEUED("Queued"),
    RUNNING("Running"),
    COMPLETE("Complete"),
    STOPPED("Stopped"),
    FAILED("Failed");

    private final String statusName;

    Status(String statusName) {

        this.statusName = statusName;
    }

    public String getStatusName() {
        return statusName;
    }
}
