package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

public enum Status {
    WAITING("Waiting"),
    RUNNING("Running"),
    COMPLETE("Complete"),
    STOPPED("Complete"),
    FAILED("Failed");

    private final String statusName;

    Status(String statusName) {

        this.statusName = statusName;
    }

    public String getStatusName() {
        return statusName;
    }
}
