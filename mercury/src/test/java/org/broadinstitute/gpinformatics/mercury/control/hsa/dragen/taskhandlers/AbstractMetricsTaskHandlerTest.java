package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.ShellUtils;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.zeroturnaround.exec.ProcessResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.testng.Assert.*;

public class AbstractMetricsTaskHandlerTest {

    @Test
    public void testUploadMetric() throws IOException {
        String ctlFile = "src/main/db/datawh/control/mapping_run_metrics.ctl";
        File dat = new File("/Users/jowalsh/Desktop/jowalsh/Documents/Dragen/DATS/MAPPING_RUN_METRICS.csv");
        parse(dat, ctlFile);
    }

    @Test
    public void seqDemuxSample() throws IOException {
        String ctlFile = "src/main/db/datawh/control/demultiplex_metric.ctl";
        File dat = new File("/Users/jowalsh/Desktop/jowalsh/Documents/Dragen/DATS/SEQ_DEMULTIPLEX_SAMPLE_METRIC.csv");
        parse(dat, ctlFile);
    }

    @Test
    public void vcMetrics() throws IOException {
        String ctlFile = "src/main/db/datawh/control/variant_call_run_metrics.ctl";
        File dat = new File("/Users/jowalsh/Desktop/jowalsh/Documents/Dragen/DATS/VC.csv");
        parse(dat, ctlFile);
    }

    @Test
    public void seqDemuxLane() throws IOException {
        String ctlFile = "src/main/db/datawh/control/demultiplex_lane_metric.ctl";
        File dat = new File("/Users/jowalsh/Desktop/jowalsh/Documents/Dragen/DATS/SEQ_DEMULTIPLEX_LANE_METRIC.csv");
        parse(dat, ctlFile);
    }

    private void parse(File inputFile, String ctlFile) throws IOException {
        AbstractMetricsTaskHandler handler = new AbstractMetricsTaskHandler() {
            @Override
            public void handleTask(Task task, SchedulerContext schedulerContext) {

            }
        };

        handler.setShellUtils(new ShellUtils());
        BufferedReader csvReader = new BufferedReader(new FileReader(inputFile));
        String row = null;
        int rowCtr = 1;
        while ((row = csvReader.readLine()) != null) {
            System.out.println("Printing Row: " + rowCtr);
            File dat = new File(inputFile.getParentFile() + inputFile.getName() + "_partial.csv");
            FileUtils.writeStringToFile(dat, row);
            try {
                ProcessResult processResult = handler.uploadMetric(ctlFile, dat);
                Assert.assertEquals(0, processResult.getExitValue(), "Failed on " + rowCtr);
            } catch (Exception e) {
                throw new RuntimeException("Failed on row " + rowCtr);
            }
            rowCtr++;
        }
    }
}