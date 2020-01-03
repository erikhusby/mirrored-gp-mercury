package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class GsUtilLogReaderTest {

    File slurmLogFile = new File("src/test/resources/testdata/dragen/slurm-2975.out");

    @Test
    public void testParseTransferStatus() throws IOException {
        GsUtilLogReader.Result result = GsUtilLogReader.parseTransferStatus(slurmLogFile);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getNumFiles(), "0 files");
        Assert.assertEquals(result.getFileSize(), 18.3, 0.1);
        Assert.assertEquals(result.getFileSizeScale(), "GiB");
        Assert.assertEquals(result.getUploaded(), 17.2, 0.1);
        Assert.assertEquals(result.getUploadedScale(), "GiB");
        Assert.assertEquals(result.getRate(), "382.4 KiB/s");
    }
}