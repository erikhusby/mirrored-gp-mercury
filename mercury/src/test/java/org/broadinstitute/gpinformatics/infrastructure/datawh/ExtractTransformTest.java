package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
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
import java.io.*;
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
    static final String PRODUCT_ORDER_SAMPLE_CLASSNAME =   ProductOrderSample.class.getName();
    static final String PRODUCT_ORDER_SAMPLE_FILENAME = "product_order_sample.dat";
    static final String RESEARCH_PROJECT_CLASSNAME = ResearchProject.class.getName();
    static final String RESEARCH_PROJECT_FILENAME = "research_project.dat";
    final String WORKFLOW_CONFIG_CLASSNAME = WorkflowConfig.class.getName();
    final String WORKFLOW_CONFIG_FILENAME = "workflow_config.dat";
    private Logger logger = Logger.getLogger(getClass());

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
        for (File file : new File(dir).listFiles(filter)) {
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
        extractTransform.writeLastEtlRun(Long.MAX_VALUE - 1);
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
    private static final long MSEC_IN_ONE_HOUR = 1000L * 60L * 60L;
    private static final long TEST_DURATION = 1000L * 5L; //5 seconds

    /** Normal ETL.  Picks up the last hour's audits. */
    public void testNormalEtl() throws Exception {
        long endEtl = System.currentTimeMillis();
        long startEtl = endEtl - MSEC_IN_ONE_HOUR;

        extractTransform.writeLastEtlRun(startEtl);
        int recordCount = extractTransform.incrementalEtl();
        Assert.assertTrue(extractTransform.readLastEtlRun() - endEtl < TEST_DURATION);
        if (recordCount > 0) {
            long etlMsec = extractTransform.getIncrementalRunStartTime();
            String readyFilename = ExtractTransform.secTimestampFormat.format(new Date(etlMsec))
                    + ExtractTransform.READY_FILE_SUFFIX;
            Assert.assertTrue((new File(datafileDir, readyFilename)).exists());
        } else {
            logger.warn("No recent changes found, so etl was not tested.");
        }
    }
/*
    public void testDevEntity() throws Exception {
        String dateString = "20130122161907";
        String endDateStr = "20130122174541";
        long startEtl = ExtractTransform.secTimestampFormat.parse(dateString).getTime();
        long endEtl =   ExtractTransform.secTimestampFormat.parse(endDateStr).getTime();
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl, dateString);
        Assert.assertEquals(recordCount, 90);

        File pdo  = new File(datafileDir, dateString + "_product_order.dat");
        File pdoSample  = new File(datafileDir, dateString + "_product_order_sample.dat");
        Assert.assertTrue(pdo.exists());
        Assert.assertTrue(pdoSample.exists());
        String pdoContent = FileUtils.readFileToString(pdo);
        String pdoSampleContent = FileUtils.readFileToString(pdoSample);

        final String CRLF = System.getProperty("line.separator");
        String pdoExpected = "1,20130122161907,F,8106,19029,39,Submitted,20130122161842,20130122161905," +
                "Plasmo_100Genomes_SNPs,DNA9LV,PDO-424_";

        Assert.assertEquals(pdoContent.replaceAll(CRLF,"_"), pdoExpected);

        String pdoSampleExpected =
                "1,20130122161907,F,9415,177,SM-35BEJ,,24_2,20130122161907,F,9414,177," +
                        "SM-35BD4,,23_3,20130122161907,F,9413,177,SM-35BEL,,22_4,20130122161907" +
                        ",F,9412,177,SM-35BER,,21_5,20130122161907,F,9411,177,SM-35BDG,,20_6,20" +
                        "130122161907,F,9410,177,SM-35BDF,,19_7,20130122161907,F,9409,177,SM-35" +
                        "BF4,,18_8,20130122161907,F,9408,177,SM-35BD6,,17_9,20130122161907,F,94" +
                        "23,177,SM-35BD9,,32_10,20130122161907,F,9422,177,SM-35BF1,,31_11,20130" +
                        "122161907,F,9421,177,SM-35BD8,,30_12,20130122161907,F,9420,177,SM-35BD" +
                        "H,,29_13,20130122161907,F,9419,177,SM-35BF3,,28_14,20130122161907,F,94" +
                        "18,177,SM-35BEM,,27_15,20130122161907,F,9417,177,SM-35BDB,,26_16,20130" +
                        "122161907,F,9416,177,SM-35BCZ,,25_17,20130122161907,F,9430,177,SM-35BD" +
                        "7,,39_18,20130122161907,F,9431,177,SM-35BEY,,40_19,20130122161907,F,94" +
                        "28,177,SM-35BET,,37_20,20130122161907,F,9429,177,SM-35BDE,,38_21,20130" +
                        "122161907,F,9426,177,SM-35BEV,,35_22,20130122161907,F,9427,177,SM-35BE" +
                        "O,,36_23,20130122161907,F,9424,177,SM-35BDD,,33_24,20130122161907,F,94" +
                        "25,177,SM-35BD5,,34_25,20130122161907,F,9434,177,SM-35BEP,,43_26,20130" +
                        "122161907,F,9432,177,SM-35BD3,,41_27,20130122161907,F,9433,177,SM-35BE" +
                        "K,,42_28,20130122161907,F,9391,177,SM-35BDI,,0_29,20130122161907,F,939" +
                        "2,177,SM-35BEU,,1_30,20130122161907,F,9393,177,SM-35BDA,,2_31,20130122" +
                        "161907,F,9394,177,SM-35BEZ,,3_32,20130122161907,F,9395,177,SM-35BEQ,,4" +
                        "_33,20130122161907,F,9396,177,SM-35BES,,5_34,20130122161907,F,9397,177" +
                        ",SM-35BDC,,6_35,20130122161907,F,9398,177,SM-35BDJ,,7_36,2013012216190" +
                        "7,F,9399,177,SM-35BEN,,8_37,20130122161907,F,9400,177,SM-35BCY,,9_38,2" +
                        "0130122161907,F,9401,177,SM-35BEI,,10_39,20130122161907,F,9402,177,SM-" +
                        "35BEX,,11_40,20130122161907,F,9403,177,SM-35BEW,,12_41,20130122161907," +
                        "F,9404,177,SM-35BD2,,13_42,20130122161907,F,9405,177,SM-35BF2,,14_43,2" +
                        "0130122161907,F,9406,177,SM-35BD1,,15_44,20130122161907,F,9407,177,SM-" +
                        "35BDK,,16_";

        Assert.assertEquals(pdoSampleContent.replaceAll(CRLF,"_"), pdoSampleExpected);

        //Assert.assertTrue(content.contains("," + entity.getProductOrderSampleId() + ","));
    }
*/

    /** Backfill ETL for a non-default range. */
    public void testBackfillEtlRange() throws Exception {
        // Selects an entity to ETL, or skips the test if there's none available.
        List<ProductOrderSample> list = auditReaderDao.findAll(ProductOrderSample.class, 1, 3);
        if (list.isEmpty()) {
            Logger.getLogger(this.getClass()).warn("No Product Order Samples found, so etl was not tested.");
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

    /** Backfill ETL for WorkflowConfig. */
    public void testBackfillEtlWorkflowConfig() throws Exception {
        final long msecStart = System.currentTimeMillis();

        Assert.assertEquals(Response.Status.NO_CONTENT, extractTransform.backfillEtl(WORKFLOW_CONFIG_CLASSNAME, 0, 0));

        final long msecEnd = System.currentTimeMillis() + 1000;

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
                Assert.assertTrue(filename.endsWith(WORKFLOW_CONFIG_FILENAME));
                foundDataFile = true;

                // verifies non-empty .dat file
                Reader reader = new FileReader(file);
                String content = IOUtils.toString(reader);
                IOUtils.closeQuietly(reader);
                Assert.assertTrue(content.contains(","));
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
    private File[] getDirFiles(String directoryName, long msecStart, long msecEnd) {
        final long yyyymmddHHMMSSstart = Long.parseLong(ExtractTransform.secTimestampFormat.format(new Date(msecStart)));
        final long yyyymmddHHMMSSend = Long.parseLong(ExtractTransform.secTimestampFormat.format(new Date(msecEnd)));
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
        extractTransform.setDatafileDir(badDataDir);
        Assert.assertEquals(0L, extractTransform.readLastEtlRun());
    }

    /** Writes an unparsable timestamp. */
    public void testReadLastEtlUnparsable() throws IOException {
        File file = new File(datafileDir, ExtractTransform.LAST_ETL_FILE);
        FileUtils.write(file, "abcedfg");

        Assert.assertEquals(0L, extractTransform.readLastEtlRun());
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

