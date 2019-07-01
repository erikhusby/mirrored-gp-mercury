package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zeroturnaround.exec.ProcessResult;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.List;

public class SlurmController implements SchedulerController {
    private static final Log log = LogFactory.getLog(SlurmController.class);

    @Override
    public List<PartitionInfo> listPartitions() {
        return runProcesssParseList(new String[]{ "sinfo" }, PartitionInfo.class);
    }

    @Override
    public List<QueueInfo> listQueue() {
        return runProcesssParseList(new String[]{ "squeue" }, QueueInfo.class);
    }

    @Override
    public String batchJob(String jobName, String partition, String script) {
        String[] cmd = {"sbatch", "-J", jobName, "-p", partition};
        ProcessResult processResult = runProcess(cmd);
        if (processResult.getExitValue() != 0 || !processResult.hasOutput()) {
            throw new RuntimeException("Failed to batch job with exit code");
        }
        String str = processResult.getOutput().getString();
        return str.replaceAll("[^0-9]", "");
    }

    private ProcessResult runProcess(String[] cmd) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            return ShellUtils.runSyncProcess(cmd, os);
        } catch (Exception e) {
            log.error("Error attempting to run sync process", e);
            throw new RuntimeException(e);
        }
    }

    private <T> List<T> runProcesssParseList(String[] cmd, Class<T> beanClass) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ProcessResult processResult = ShellUtils.runSyncProcess(cmd, os);
            if (processResult.getExitValue() != -1 && processResult.hasOutput()) {
                String output = processResult.getOutput().getString();
                CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(new StringReader(output)).
                        withSeparator('\t').
                        withQuoteChar('\'').
                        withType(beanClass).
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
}
