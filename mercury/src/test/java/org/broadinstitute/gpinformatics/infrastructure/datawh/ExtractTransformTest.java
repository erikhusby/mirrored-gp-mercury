package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Unit test of ExtractTransform.
 *
 * @author epolk
 */

@Test(enabled =  true, groups = TestGroups.EXTERNAL_INTEGRATION)
public class ExtractTransformTest extends Arquillian {
    private String datafileDir;
    private final Date now = new Date();
    private final String nowMsec = String.valueOf(now.getTime());
    private String badDataDir = datafileDir + nowMsec;

    @Inject
    private ExtractTransform extractTransform;
    @Inject
    private AuditReaderDao auditReaderDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @BeforeClass
    public void beforeClass() throws Exception {
        datafileDir = System.getProperty("java.io.tmpdir");
        badDataDir = datafileDir + System.getProperty("file.separator") + nowMsec;
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        extractTransform.setDatafileDir(datafileDir);
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        long etlMsec = extractTransform.getCurrentRunStartTime();
        String etlFullDate = ExtractTransform.fullDateFormat.format(new Date(etlMsec));
        deleteEtlFiles(etlFullDate);
    }

    /** Deletes all the files written by these tests including data, isReady, and lastEtlRun files. */
    private void deleteEtlFiles(final String etlFullDate) {
        File dir = new File (datafileDir);
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dirname, String filename) {
                return filename.startsWith(etlFullDate) || filename.equals(ExtractTransform.LAST_ETL_FILE);
            }
        };
        for (File file : dir.listFiles(filter)) {
            file.delete();
        }
    }

    /** Passes a blank and a non-existent directory for datafiles. */
    public void testInvalidDir() {
        extractTransform.setDatafileDir(null);
        Assert.assertEquals(-1, extractTransform.incrementalEtl());

        extractTransform.setDatafileDir("");
        Assert.assertEquals(-1, extractTransform.incrementalEtl());

        extractTransform.setDatafileDir(badDataDir);
        Assert.assertEquals(-1, extractTransform.incrementalEtl());
    }

    /**  Mathematically excludes changes by setting last ETL version impossibly high. */
    public void testNoChanges() {
        extractTransform.writeLastEtlRun(datafileDir, Long.MAX_VALUE - 1);
        Assert.assertEquals(0, extractTransform.incrementalEtl());
        Assert.assertTrue(ExtractTransform.getCurrentRunStartTime() >= 0);
    }

    /** Normal ETL.  Picks up the last 2000 (or fewer) audits. */
    public void testNormalEtl() throws Exception {
        long endRev = auditReaderDao.currentRevNumber(now);
        if (endRev == 0) return;
        long startRev = Math.max(0, endRev - 2000);
        extractTransform.writeLastEtlRun(datafileDir, startRev);
        int recordCount = extractTransform.incrementalEtl();
        Assert.assertTrue(recordCount > 0);
        Assert.assertEquals(endRev, extractTransform.readLastEtlRun(datafileDir));

        long etlMsec = extractTransform.getCurrentRunStartTime();
        String readyFilename = ExtractTransform.fullDateFormat.format(new Date(etlMsec)) + ExtractTransform.READY_FILE_SUFFIX;
        Assert.assertTrue((new File(datafileDir, readyFilename)).exists());
    }

    /** Passes a non-existent directory for the last run file. */
    public void testInvalidLastEtlBadDir() {
        Assert.assertEquals(0L, extractTransform.readLastEtlRun(badDataDir));
    }

    /** Writes an unparsable timestamp. */
    public void testReadLastEtlUnparsable() throws IOException {
        File file = new File (datafileDir, ExtractTransform.LAST_ETL_FILE);
        FileWriter fw = new FileWriter(file, false);
        fw.write("abcedfg");
        fw.close();

        Assert.assertEquals(0L, extractTransform.readLastEtlRun(datafileDir));
    }

    /** Takes the mutex, ETL cannot run. */
    public void testMutex() {
        Assert.assertTrue(ExtractTransform.getMutex().tryAcquire());
        try {
            int recordCount = extractTransform.incrementalEtl();
            Assert.assertEquals(-1, recordCount);
        } finally {
            ExtractTransform.getMutex().release();
        }
    }
}

