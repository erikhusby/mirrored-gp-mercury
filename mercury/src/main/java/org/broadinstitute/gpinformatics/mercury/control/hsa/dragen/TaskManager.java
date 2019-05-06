package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.TaskResult;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.enterprise.context.Dependent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Dependent
public class TaskManager {

    public void fireEvent(Task task, DragenAppContext dragenAppContext) {
        task.setStatus(Status.RUNNING);
        if (OrmUtil.proxySafeIsInstance(task, ProcessTask.class)) {
            handleStartProcess(task, dragenAppContext);
        }
    }

    private void handleStartProcess(Task task, DragenAppContext dragenAppContext) {
        ProcessTask processTask = OrmUtil.proxySafeCast(task, ProcessTask.class);
        TaskResult taskResult = dragenAppContext.getInstance().fireProcess(
                processTask.getCommandLineArgument(), processTask);
        processTask.setProcessId(taskResult.getProcessId());
    }

    public boolean isTaskComplete(Task task) {
        if (OrmUtil.proxySafeIsInstance(task, ProcessTask.class)) {
            try {
                return isProcessComplete(OrmUtil.proxySafeCast(task, ProcessTask.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (OrmUtil.proxySafeIsInstance(task, WaitForFileTask.class)) {
            WaitForFileTask waitForFileTask = OrmUtil.proxySafeCast(task, WaitForFileTask.class);
            File file = new File(waitForFileTask.getFilePath());
            return file.exists();
        }

        return false;
    }

    private boolean isProcessComplete(ProcessTask processTask) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", "ps -ef | grep dragen"); //TODO from config
        Process p = processBuilder.start();
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

        // TODO probably need to compare the command as well to uniquely verify
        List<Long> processIds = new ArrayList<>();
        String line;
        while ((line = input.readLine()) != null) {
            String[] data = line.split("\\s+");
            if (data.length > 2) {
                long pid = Long.parseLong(data[1]);
                processIds.add(pid);
            }
        }

        return !processIds.contains(processTask.getProcessId());
    }
}
