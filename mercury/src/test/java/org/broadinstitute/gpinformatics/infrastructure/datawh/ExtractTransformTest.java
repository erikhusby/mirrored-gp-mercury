package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
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
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
    static final String PRODUCT_ORDER_SAMPLE_CLASSNAME = "org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample";
    static final String PRODUCT_ORDER_SAMPLE_FILENAME = "product_order_sample.dat";
    static final String RESEARCH_PROJECT_CLASSNAME = "org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject";
    static final String RESEARCH_PROJECT_FILENAME = "research_project.dat";

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
        ExtractTransform.setDatafileDir(datafileDir);
        deleteEtlFiles(datafileDir);
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        deleteEtlFiles(datafileDir);
    }

    /** Deletes all the files written by these tests including .dat, isReady, and lastEtlRun files. */
    private static void deleteEtlFiles(String dir) {
        // Uses current year month day to determine whether to delete a file.
        final String yyyymmdd = (new SimpleDateFormat("yyyyMMdd")).format(new Date());
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dirname, String filename) {
                return (filename.startsWith(yyyymmdd) && filename.endsWith(".dat")
                        || filename.startsWith(yyyymmdd) && filename.endsWith(ExtractTransform.READY_FILE_SUFFIX))
                        || filename.equals(ExtractTransform.LAST_ETL_FILE);
            }
        };
        for (File file : new File (dir).listFiles(filter)) {
            FileUtils.deleteQuietly(file);
        }
    }


    /** Passes a blank and a non-existent directory for datafiles. */
    public void testInvalidDir() {
        ExtractTransform.setDatafileDir(null);
        Assert.assertEquals(-1, extractTransform.incrementalEtl());
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR,
                extractTransform.backfillEtl(PRODUCT_ORDER_SAMPLE_CLASSNAME, 0, Long.MAX_VALUE));

        ExtractTransform.setDatafileDir("");
        Assert.assertEquals(-1, extractTransform.incrementalEtl());
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR,
                extractTransform.backfillEtl(PRODUCT_ORDER_SAMPLE_CLASSNAME, 0, Long.MAX_VALUE));

        ExtractTransform.setDatafileDir(badDataDir);
        Assert.assertEquals(-1, extractTransform.incrementalEtl());
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR,
                extractTransform.backfillEtl(PRODUCT_ORDER_SAMPLE_CLASSNAME, 0, Long.MAX_VALUE));
    }

    /**  Mathematically excludes changes by setting last ETL version impossibly high. */
    public void testNoChanges() {
        extractTransform.writeLastEtlRun(datafileDir, Long.MAX_VALUE - 1);
        Assert.assertEquals(0, extractTransform.incrementalEtl());
        Assert.assertTrue(ExtractTransform.getIncrementalRunStartTime() >= 0);
    }

    public void testBadRange() {
        Assert.assertEquals(Response.Status.BAD_REQUEST,
                extractTransform.backfillEtl(PRODUCT_ORDER_SAMPLE_CLASSNAME, -1, Long.MAX_VALUE));

        Assert.assertEquals(Response.Status.BAD_REQUEST,
                extractTransform.backfillEtl(PRODUCT_ORDER_SAMPLE_CLASSNAME, 1000, 999));
    }

    /** Must supply fully qualified class name */
    public void testInvalidClassName() {
        Assert.assertEquals(Response.Status.NOT_FOUND,
                extractTransform.backfillEtl("ProductOrderSample", 0, Long.MAX_VALUE));
    }

    /** Normal ETL.  Picks up the last 2000 (or fewer) audits. */
    public void testNormalEtl() throws Exception {
        long endRev = auditReaderDao.currentRevNumber(now);
        if (endRev == 0) {
            return;
        }
        long startRev = Math.max(0, endRev - 2000);
        extractTransform.writeLastEtlRun(datafileDir, startRev);
        int recordCount = extractTransform.incrementalEtl();
        Assert.assertTrue(recordCount > 0);
        Assert.assertEquals(endRev, extractTransform.readLastEtlRun(datafileDir));

        long etlMsec = ExtractTransform.getIncrementalRunStartTime();
        String readyFilename = ExtractTransform.fullDateFormat.format(new Date(etlMsec)) + ExtractTransform.READY_FILE_SUFFIX;
        Assert.assertTrue((new File(datafileDir, readyFilename)).exists());
    }

    /** Backfill ETL for a non-default range. */
    public void testBackfillEtlRange() throws Exception {
        // Selects an entity to ETL, or skips the test if there's none available.
        List<ProductOrderSample> list = auditReaderDao.findAll(ProductOrderSample.class, 1, 3);
        if (list.isEmpty()) {
            return;
        }
        ProductOrderSample entity = null;
        for (ProductOrderSample pos : list) {
            if (pos.getProductOrderSampleId() > 1) {
                entity = pos;
                break;
            }
        }
        Assert.assertNotNull(entity);
        // ETLs a range of entity ids that includes the known entity id.
        long startId = entity.getProductOrderSampleId() -  1;
        long endId = entity.getProductOrderSampleId() +  1;

        long msecStart = System.currentTimeMillis();

        Assert.assertEquals(Response.Status.NO_CONTENT,
                extractTransform.backfillEtl(PRODUCT_ORDER_SAMPLE_CLASSNAME, startId, endId));

        long msecEnd = System.currentTimeMillis() + 1000;

        // Verifies there is only one .dat file and one _is_ready file, both having
        // a datetime in the expected range.
        File[] filelist = getDirFiles(datafileDir, msecStart, msecEnd);
        boolean foundDataFile = false;
        boolean foundIsReadyFile = false;
        for (File file : filelist) {
            String filename = file.getName();
            if (filename.endsWith(".dat")) {
                //should only have found one dat file
                Assert.assertFalse(foundDataFile);
                Assert.assertTrue(filename.endsWith(PRODUCT_ORDER_SAMPLE_FILENAME));
                foundDataFile = true;

                // verifies that the entity id is present in the .dat file
                String content = FileUtils.readFileToString(file);
                Assert.assertTrue(content.contains("," + entity.getProductOrderSampleId() + ","));
            }
            if (filename.endsWith("_is_ready")) {
                //should only have found one _is_ready file
                Assert.assertFalse(foundIsReadyFile);
                foundIsReadyFile = true;
            }
        }
        Assert.assertTrue(foundDataFile);
        Assert.assertTrue(foundIsReadyFile);
    }

    /** Backfill ETL for default range. */
    public void testBackfillEtl() throws Exception {
        // Skips the test if there's no entities available.
        if (auditReaderDao.findAll(ResearchProject.class, 1, 1).isEmpty()) {
            return;
        }

        long msecStart = System.currentTimeMillis();

        Assert.assertEquals(Response.Status.NO_CONTENT,
                extractTransform.backfillEtl(RESEARCH_PROJECT_CLASSNAME, 0, Long.MAX_VALUE));

        long msecEnd = System.currentTimeMillis() + 1000;

        // Verifies there is only one .dat file and one _is_ready file, both having
        // a datetime in the expected range.
        File[] list = getDirFiles(datafileDir, msecStart, msecEnd);
        boolean foundDataFile = false;
        boolean foundIsReadyFile = false;
        for (File file : list) {
            String filename = file.getName();
            if (filename.endsWith(".dat")) {
                //should only have found one dat file
                Assert.assertFalse(foundDataFile);
                Assert.assertTrue(filename.endsWith(RESEARCH_PROJECT_FILENAME));
                foundDataFile = true;

                // data file should have at least one record in it
                List<String> content = FileUtils.readLines(file);
                Assert.assertTrue(!content.isEmpty());
            }
            if (filename.endsWith("_is_ready")) {
                //should only have found one _is_ready file
                Assert.assertFalse(foundIsReadyFile);
                foundIsReadyFile = true;
            }
        }
        Assert.assertTrue(foundDataFile);
        Assert.assertTrue(foundIsReadyFile);
    }

    /** Returns all files in the given directory, having filename timestamp in the given range. */
    private static File[] getDirFiles(String directoryName, long msecStart, long msecEnd) {
        final long yyyymmddHHMMSSstart = Long.parseLong(ExtractTransform.fullDateFormat.format(new Date(msecStart)));
        final long yyyymmddHHMMSSend = Long.parseLong(ExtractTransform.fullDateFormat.format(new Date(msecEnd)));
        File dir = new File (directoryName);
        File[] list = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dirname, String filename) {
                try {
                    // Only cares about files named <dateTime>_<*>
                    String s = filename.split("_")[0];
                    long timestamp = Long.parseLong(s);
                    return (timestamp >= yyyymmddHHMMSSstart && timestamp <= yyyymmddHHMMSSend);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });
        return list;
    }


    /** Passes a non-existent directory for the last run file. */
    public void testInvalidLastEtlBadDir() {
        Assert.assertEquals(0L, extractTransform.readLastEtlRun(badDataDir));
    }

    /** Writes an unparsable timestamp. */
    public void testReadLastEtlUnparsable() throws IOException {
        File file = new File(datafileDir, ExtractTransform.LAST_ETL_FILE);
        FileUtils.write(file, "abcedfg");

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

