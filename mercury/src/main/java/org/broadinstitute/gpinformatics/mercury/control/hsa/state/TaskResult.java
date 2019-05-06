package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

public class TaskResult {

    private final long processId;
    private final String output;
    private final int exitCode;

    public TaskResult(long processId, String output, int exitCode) {
        this.processId = processId;
        this.output = output;
        this.exitCode = exitCode;
    }

    public long getProcessId() {
        return processId;
    }

    public String getOutput() {
        return output;
    }

    public int getExitCode() {
        return exitCode;
    }
}
