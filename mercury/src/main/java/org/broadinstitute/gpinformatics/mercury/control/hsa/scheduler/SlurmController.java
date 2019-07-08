package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.ProcessTask;
import org.zeroturnaround.exec.ProcessResult;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

@Dependent
public class SlurmController implements SchedulerController {
    private static final Log log = LogFactory.getLog(SlurmController.class);

    @Inject
    private ShellUtils shellUtils;

    @Override
    public List<PartitionInfo> listPartitions() {
        return runProcesssParseList(Arrays.asList("sinfo", "-o", "\"%all\""), PartitionInfo.class, 1);
    }

    @Override
    public List<QueueInfo> listQueue() {
        return runProcesssParseList(Arrays.asList("squeue", "-o", "\"%all\""), QueueInfo.class, 1);
    }

    @Override
    public String batchJob(String partition, ProcessTask processTask) {
        List<String> cmd;
        if (partition == null) {
            cmd = Arrays.asList( "sbatch", "-J", processTask.getTaskName(), "-wrap", processTask.getCommandLineArgument());
        } else {
            cmd = Arrays.asList( "sbatch", "-J", processTask.getTaskName(), "-p", partition, "-wrap", processTask.getCommandLineArgument());
        }
        ProcessResult processResult = runProcess(cmd);
        if (processResult.getExitValue() != 0 || !processResult.hasOutput()) {
            throw new RuntimeException("Failed to batch job with exit code");
        }
        String str = processResult.getOutput().getString();
        return str.replaceAll("[^0-9]", "");
    }

    @Override
    public boolean cancelJob(String jobId) {
        List<String> cmd = Arrays.asList("scancel", jobId);
        ProcessResult processResult = runProcess(cmd);
        return false;
    }

    @Override
    public boolean isJobComplete(String jobName, long jobId) {
        return listQueue().stream().noneMatch(queueInfo ->
                queueInfo.getName().equalsIgnoreCase(jobName) && queueInfo.getJobId() == jobId);
    }

    @Override
    public JobInfo fetchJobInfo(int jobId) {
        List<JobInfo> jobInfoList =
                runProcesssParseList(Arrays.asList("sacct", "-j", String.valueOf(jobId), "-p"), JobInfo.class);
        if (jobInfoList.size() > 0) {
            return jobInfoList.get(0);
        }

        return null;
    }

    private ProcessResult runProcess(List<String> cmd) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            return shellUtils.runSyncProcess(cmd, os);
        } catch (Exception e) {
            log.error("Error attempting to run sync process", e);
            throw new RuntimeException(e);
        }
    }

    private <T> List<T> runProcesssParseList(List<String> cmd, Class<T> beanClass) {
        return runProcesssParseList(cmd, beanClass, 0);
    }

    private <T> List<T> runProcesssParseList(List<String> cmd, Class<T> beanClass, int skipLines) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ProcessResult processResult = shellUtils.runSyncProcess(cmd, os);
            if (processResult.getExitValue() != -1 && processResult.hasOutput()) {
                String output = processResult.getOutput().getString();
                CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(new StringReader(output)).
                        withSeparator('|').
                        withType(beanClass).
                        withSkipLines(skipLines).
                        build();
                return csvToBean.parse();
            } else {
                throw new RuntimeException("Process failed with exit value " + processResult.getExitValue());
            }
        }  catch (Exception e) {
            log.error("Error attempting to run process", e);
            throw new RuntimeException(e);
        }
    }

    // For Testing
    public void setShellUtils(ShellUtils shellUtils) {
        this.shellUtils = shellUtils;
    }
}
