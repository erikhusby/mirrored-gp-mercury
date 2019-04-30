package org.broadinstitute.gpinformatics.mercury.boundary.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.TaskResult;

public class ProcessTask implements Task {

    private String commandLineArgument;

    @Override
    public TaskResult fireEvent(DragenAppContext context) {
        return context.getInstance().fireProcess(commandLineArgument, this);
    }

    public String getCommandLineArgument() {
        return commandLineArgument;
    }

    public void setCommandLineArgument(String commandLineArgument) {
        this.commandLineArgument = commandLineArgument;
    }

}
