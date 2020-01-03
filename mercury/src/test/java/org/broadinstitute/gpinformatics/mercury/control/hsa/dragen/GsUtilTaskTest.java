package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.codehaus.plexus.util.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class GsUtilTaskTest {

    @Test
    public void testCp() {
        File tmpCram = FileUtils.createTempFile("test", "cram", null);
        GsUtilTask task = GsUtilTask.cp(tmpCram, "gs://broad-gplims-dev");
        String expected = String.format(
                "gsutil -o GSUtil:parallel_process_count=1 -o GSUtil:parallel_thread_count=4 -o GSUtil:parallel_composite_upload_threshold=150M cp %s gs://broad-gplims-dev",
                tmpCram.getPath());
        Assert.assertEquals(task.getCommandLineArgument(), expected);
    }
}