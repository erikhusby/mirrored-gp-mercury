package org.broadinstitute.gpinformatics.mercury.boundary.hsa.state;

public class TaskResult {

    private String output;
    private int exitCode;

    public TaskResult(String output, int exitCode) {
        this.output = output;
        this.exitCode = exitCode;
    }

    public String getOutput() {
        return output;
    }

    public int getExitCode() {
        return exitCode;
    }
}
