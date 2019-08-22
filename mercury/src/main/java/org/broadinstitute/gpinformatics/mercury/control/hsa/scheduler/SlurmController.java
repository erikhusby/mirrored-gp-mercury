package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.ProcessTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
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

    @Inject
    private DragenConfig dragenConfig;

    @Override
    public List<PartitionInfo> listPartitions() {
        return runProcesssParseList(Arrays.asList("ssh", dragenConfig.getSlurmHost(), "sinfo", "-o", "\"%all\""),
                PartitionInfo.class, 1);
    }

    @Override
    public List<QueueInfo> listQueue() {
        return runProcesssParseList(Arrays.asList("ssh", dragenConfig.getSlurmHost(), "squeue", "-o", "\"%all\""), QueueInfo.class, 1);
    }

    /**
     * Schedule a job through slurm with exclusive flag - meaning job allocation can not share nodes with other running jobs.
     * @param partition - Request a specific partition to be used
     * @param processTask - Task object defining the command line argument to be supplied
     * @return Process ID of the task being started
     */
    @Override
    public String batchJob(String partition, ProcessTask processTask) {
        List<String> cmd;
        String dragenCmd = String.format("--wrap=\"%s%s\"", dragenConfig.getDragenPath(), processTask.getCommandLineArgument());
        if (partition == null) {
            cmd = Arrays.asList( "ssh", dragenConfig.getSlurmHost(), "sbatch", "--exclusive",  "-J", processTask.getTaskName(), dragenCmd);
        } else {
            cmd = Arrays.asList( "ssh", dragenConfig.getSlurmHost(), "sbatch", "--exclusive", "-J", processTask.getTaskName(), "-p", partition, dragenCmd);
        }
        ProcessResult processResult = runProcess(cmd);
        if (processResult.getExitValue() != 0) {
            String output = processResult.hasOutput() ? processResult.getOutput().getString() : "";
            throw new RuntimeException("Failed to batch job with exit code: " + processResult.getExitValue() + " " + output);
        }

        // Outputs: Submitted batch job {process ID} - just grab the job ID portion
        String str = processResult.getOutput().getString();
        return str.replaceAll("[^0-9]", "");
    }

    @Override
    public boolean cancelJob(String jobId) {
        List<String> cmd = Arrays.asList("ssh", dragenConfig.getSlurmHost(), "scancel", jobId);
        ProcessResult processResult = runProcess(cmd);
        return processResult.getExitValue() == 0;
    }

    @Override
    public Status fetchJobStatus(long jobId) {
        JobInfo jobInfo = fetchJobInfo(jobId);
        SlurmStateCode slurmStateCode = SlurmStateCode.getByName(jobInfo.getState());
        switch (slurmStateCode) {
            case BOOT_FAIL:
            case FAILED:
            case OUT_OF_MEMORY:
            case DEADLINE:
            case NODE_FAIL:
            case PREEMPTED:
            case SPECIAL_EXIT:
            case STOPPED:
            case TIMEOUT:
            case REVOKED:
                return Status.FAILED;
            case CANCELLED:
                return Status.CANCELLED;
            case COMPLETED:
                return Status.COMPLETE;
            case PENDING:
            case REQUEUE_HOLD:
            case REQUEUE_FED:
            case REQUEUED:
            case STAGE_OUT:
            case SIGNALING:
                return Status.QUEUED;
            case RUNNING:
            case COMPLETING:
            case RESIZING:
                return Status.RUNNING;
            case RESV_DEL_HOLD:
            case SUSPENDED:
                return Status.SUSPENDED;
            default:
                return Status.QUEUED;
        }
    }

    @Override
    public JobInfo fetchJobInfo(long jobId) {
        List<JobInfo> jobInfoList =
                runProcesssParseList(Arrays.asList("ssh", dragenConfig.getSlurmHost(), "sacct", "-j", String.valueOf(jobId), "-p"), JobInfo.class);
        if (jobInfoList.size() > 0) {
            return jobInfoList.get(0);
        }

        return null;
    }

    private ProcessResult runProcess(List<String> cmd) {
        try {
            return shellUtils.runSyncProcess(cmd);
        } catch (Exception e) {
            log.error("Error attempting to run sync process", e);
            throw new RuntimeException(e);
        }
    }

    private <T> List<T> runProcesssParseList(List<String> cmd, Class<T> beanClass) {
        return runProcesssParseList(cmd, beanClass, 0);
    }

    private <T> List<T> runProcesssParseList(List<String> cmd, Class<T> beanClass, int skipLines) {
        try {
            ProcessResult processResult = shellUtils.runSyncProcess(cmd);
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

    public void setDragenConfig(DragenConfig dragenConfig) {
        this.dragenConfig = dragenConfig;
    }
}
