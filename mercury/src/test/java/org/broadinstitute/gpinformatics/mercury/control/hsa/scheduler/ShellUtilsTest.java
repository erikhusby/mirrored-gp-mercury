package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class ShellUtilsTest {

    @Test(enabled = true)
    public void testRunSyncProcess() {
        String ldruid = "mercury/guest@\"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=192.168.194.136)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SID=gpgold)))\"";
        System.out.println(ldruid);

        File outputRecord = new File("/var/folders/wc/8tc31w915kg659tf0gyzmcjsjj39hc/T/SL-NVA/1563287861893_SL-NVA_A1563287860863HSAFCDMXX/dragen/2019-07-16--10-37-42/fastq/Reports/demultiplex_metrics.dat");

        // TODO etl folder locaiton to ctl
        List<String> cmds = Arrays.asList("/Users/jowalsh/opt/oracle/sqlldr",
                "control=/seq/lims/datawh/dev/new/demultiplex_metric.ctl",
                "log=load.log",
                "bad=load.bad",
                String.format("data=%s", outputRecord.getPath()),
                "discard=load.dsc",
                "direct=false",
                String.format("userId=%s", ldruid));

        ShellUtils shellUtils = new ShellUtils();
        try {
            ProcessResult processResult = shellUtils.runSyncProcess(cmds);
            System.out.println(processResult.getOutput().getString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}