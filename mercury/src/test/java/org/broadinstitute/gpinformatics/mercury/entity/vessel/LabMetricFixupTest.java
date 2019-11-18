package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;

/**
 * Fixup production Lab Metric entities
 */
@Test(groups = TestGroups.FIXUP)
public class LabMetricFixupTest extends Arquillian {
    public static final Pattern TAB_PATTERN = Pattern.compile("\\t");

    @Inject
    private LabMetricRunDao dao;

    @Inject
    private LabMetricDao labMetricDao;

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction utx;

    @Inject
    private LabVesselDao labVesselDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(PROD, "prod");
    }

    @Test(enabled = false)
    public void fixupGplim1874() throws Exception {
        // These dates were obtained from lab_metric_aud and rev_info on prod db.
        // I.e. they are the dates that the lab metric entities were created in the db.
        updateLabMetricRunDate(1, 93, "2013-07-02-19:39:30");
        updateLabMetricRunDate(951, 1043, "2013-07-11-13:42:29");
        updateLabMetricRunDate(1051, 1051, "2013-07-12-07:58:42");
        updateLabMetricRunDate(1951, 1999, "2013-07-26-11:01:58");
        updateLabMetricRunDate(2000, 2000, "2013-07-26-13:11:47");
        updateLabMetricRunDate(2001, 2012, "2013-07-29-14:02:52");
        updateLabMetricRunDate(2013, 2017, "2013-07-31-11:33:06");
        updateLabMetricRunDate(2018, 2029, "2013-08-01-12:26:24");
        updateLabMetricRunDate(2951, 2951, "2013-08-02-09:49:20");
        updateLabMetricRunDate(2952, 2960, "2013-08-05-13:38:34");
        updateLabMetricRunDate(2961, 2965, "2013-08-05-13:44:20");
        updateLabMetricRunDate(2966, 2970, "2013-08-05-14:04:25");
        updateLabMetricRunDate(2971, 2971, "2013-08-06-12:23:07");
        updateLabMetricRunDate(3951, 3959, "2013-08-08-13:24:39");
        updateLabMetricRunDate(3960, 3960, "2013-08-08-15:31:23");
        updateLabMetricRunDate(4951, 4991, "2013-08-12-14:25:42");
        updateLabMetricRunDate(4992, 5000, "2013-08-15-14:44:48");
        updateLabMetricRunDate(5951, 5982, "2013-08-15-14:44:48");
        updateLabMetricRunDate(5983, 5986, "2013-08-15-15:32:21");
        updateLabMetricRunDate(6001, 6001, "2013-08-16-09:49:01");
        updateLabMetricRunDate(6951, 6954, "2013-08-19-13:39:12");
        updateLabMetricRunDate(6955, 6961, "2013-08-19-16:04:15");
        updateLabMetricRunDate(7001, 7001, "2013-08-20-09:43:41");
        updateLabMetricRunDate(7002, 7094, "2013-08-21-16:01:50");
        dao.flush();
    }

    private void updateLabMetricRunDate(long startEntityId, long endEntityId, String timestamp) throws Exception {
        Date createdDate = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").parse(timestamp);
        for (long entityId = startEntityId; entityId <= endEntityId; ++entityId) {
            LabMetric labMetric = dao.findById(LabMetric.class, entityId);
            if (labMetric != null) {
                labMetric.setCreatedDate(createdDate);
            }
        }
    }

    @Test(enabled = false)
    public void gplim3119FixupQuant() {
        userBean.loginOSUser();
        LabMetric labMetric = dao.findById(LabMetric.class, 67522L);
        Assert.assertTrue(labMetric != null &&
                          labMetric.getName() == LabMetric.MetricType.ECO_QPCR &&
                          labMetric.getValue().compareTo(new BigDecimal("3.69")) == 0);
        labMetric.setValue(new BigDecimal("25.93380293"));
        Logger.getLogger(this.getClass().getName()).info("updated metric=" + labMetric.getLabMetricId() +
                                                         " value=" + labMetric.getValue());
        dao.flush();
    }

    @Test(enabled = false)
    public void gplim3233ChangeInitialToPond() {
        userBean.loginOSUser();
        LabMetricRun labMetricRun = dao.findById(LabMetricRun.class, 1072L);
        Assert.assertEquals(labMetricRun.getRunName(), "LCSET-6493_Pond Pico_11262014_");
        System.out.println("Updating run " + labMetricRun.getRunName());
        labMetricRun.setMetricType(LabMetric.MetricType.POND_PICO);
        for (LabMetric labMetric : labMetricRun.getLabMetrics()) {
            System.out.println("Updating metric " + labMetric.getLabMetricId());
            labMetric.setMetricType(LabMetric.MetricType.POND_PICO);
        }
        dao.persist(new FixupCommentary("GPLIM-3233, LCSET-6493, change Initial Pico to Pond Pico"));
        dao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim3508() {
        try {
            utx.begin();
            userBean.loginOSUser();
            // This is too time sensitive to hotfix the bug in fetchQuantForTube, so remove the duplicate run.
            LabMetricRun labMetricRun = dao.findByName("LCSET-7039_Pond Pico3_RM_040315");
            for (LabMetric labMetric : labMetricRun.getLabMetrics()) {
                labMetric.getLabVessel().getMetrics().remove(labMetric);
                for (Metadata metadata : labMetric.getMetadataSet()) {
                    dao.remove(metadata);
                }
                dao.remove(labMetric);
            }
            for (Metadata metadata : labMetricRun.getMetadata()) {
                dao.remove(metadata);
            }

            System.out.println("Deleting " + labMetricRun.getRunName());
            dao.remove(labMetricRun);
            dao.persist(new FixupCommentary("GPLIM-3508 remove duplicate Pico run"));
            dao.flush();
            utx.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException |
                HeuristicRollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupGplim3514() {
        Map<String, String> barcodeToPondQuants = new HashMap<String, String>() {{
            // tested on dev:  put("0180654866", "21.66");
            //                 put("0180654901", "21.01");
            put("0180654876","25.87226483");
            put("0175073819","29.3801513");
            put("0175073767","28.4380814");
            put("0180654826","30.51237225");
            put("0175073776","28.55423798");
        }};

        userBean.loginOSUser();
        Map<String, LabVessel> vessels = labVesselDao.findByBarcodes (new ArrayList<>(barcodeToPondQuants.keySet()));
        Assert.assertEquals(vessels.size(), barcodeToPondQuants.size());
        for (Map.Entry<String, LabVessel> entry : vessels.entrySet()) {
            Assert.assertNotNull(entry.getValue(), "barcode " + entry.getKey());

            LabMetric metric = entry.getValue().findMostRecentLabMetric(LabMetric.MetricType.POND_PICO);
            Assert.assertNotNull(metric,  "barcode " + entry.getKey());

            String newValue = barcodeToPondQuants.get(entry.getKey());
            Assert.assertNotNull(newValue, "barcode " + entry.getKey());

            System.out.println(
                    "Updating lab metric " + metric.getLabMetricId() + " pond quant from " + metric.getValue() + " to "
                    + newValue);
            metric.setValue(new BigDecimal(newValue));
        }
        dao.persist(new FixupCommentary("GPLIM-3514 update generic pico due to rerun of the pond quant"));
        dao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim3518() {
        // Adds a synthetic generic quant to represent the lab having done a manual dilution to the
        // stock tube and the subsequent skipping of the pico in order to conserve the sample mass.

        LabMetric.MetricType quantType = LabMetric.MetricType.INITIAL_PICO;
        //For dev testing use barcode "0175331205" and existingValue 20.77
        String tubeBarcode = "1109293898";
        BigDecimal existingValue = new BigDecimal("3.14");
        BigDecimal newValue = new BigDecimal("2.52");

        userBean.loginOSUser();
        LabVessel vessel = labVesselDao.findByIdentifier(tubeBarcode);
        Assert.assertNotNull(vessel);

        LabMetric metric = vessel.findMostRecentLabMetric(quantType);
        Assert.assertTrue(MathUtils.isSame(existingValue.doubleValue(), metric.getValue().doubleValue()));
        Assert.assertNotNull(metric.getLabMetricRun());

        // Verifies that the two lims queries are as expected before the change.
        // LimsQueries limsQueries = new LimsQueries(staticPlateDao, labVesselDao, barcodedTubeDao);
        // Assert.assertTrue(MathUtils.isSame(limsQueries.fetchQuantForTube(tubeBarcode, quantType.getDisplayName()),
        //         existingValue.doubleValue()));
        // Map<String,ConcentrationAndVolumeAndWeightType> map =
        //         limsQueries.fetchConcentrationAndVolumeAndWeightForTubeBarcodes(Collections.singletonList(tubeBarcode));
        // Assert.assertEquals(map.size(), 1);
        // Assert.assertEquals(map.values().size(), 1);
        // Assert.assertTrue(MathUtils.isSame(map.values().iterator().next().getConcentration().doubleValue(),
        //         existingValue.doubleValue()));

        // All INITIAL_PICO quants have lab metric runs.
        // Adds a new lab metric and lab metric run, having unique run name and dated "now".
        Date newDate = new Date();
        final LabMetric newLabMetric = new LabMetric(newValue, quantType, LabMetric.LabUnit.NG_PER_UL,
                metric.getVesselPosition(), newDate);
        String newRunName = metric.getLabMetricRun().getRunName() + "_1";
        Assert.assertNull(dao.findByName(newRunName));

        final LabMetricRun newLabMetricRun = new LabMetricRun(newRunName, newDate, quantType);
        newLabMetricRun.addMetric(newLabMetric);

        vessel.addMetric(newLabMetric);

        System.out.println("Adding lab metric " + newLabMetric.getLabMetricId() +
                           " and lab metric run " + newLabMetricRun.getLabMetricRunId());
        dao.persistAll(new ArrayList<Object>() {{
            add(new FixupCommentary("GPLIM-3518 add synthetic initial pico quant"));
            add(newLabMetricRun);
            add(newLabMetric);
        }});
        dao.flush();

        // Verifies that the two lims queries in fact pick the latest generic quant.
        // Assert.assertTrue(MathUtils.isSame(limsQueries.fetchQuantForTube(tubeBarcode, quantType.getDisplayName()),
        //         newValue.doubleValue()));
        // map = limsQueries.fetchConcentrationAndVolumeAndWeightForTubeBarcodes(Collections.singletonList(tubeBarcode));
        // Assert.assertEquals(map.size(), 1);
        // Assert.assertEquals(map.values().size(), 1);
        // Assert.assertTrue(MathUtils.isSame(map.values().iterator().next().getConcentration().doubleValue(),
        //         newValue.doubleValue()));

    }

    @Test(enabled = false)
    public void fixupGplim3903() {
        try {
            utx.begin();
            userBean.loginOSUser();
            LabMetricRun labMetricRun = dao.findByName("113015_XTRval_InitialRiboNorm2_MC");
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(labMetricRun.getRunDate());
            calendar.set(Calendar.DAY_OF_MONTH, 24);
            // avoid clash with 112415_XTRval_InitialRiboNorm_MC
            calendar.set(Calendar.MINUTE, 47);
            System.out.println("Setting date to " + calendar.getTime() + " on " + labMetricRun.getRunName());
            labMetricRun.setRunDate(calendar.getTime());
            for (LabMetric labMetric : labMetricRun.getLabMetrics()) {
                System.out.println("Setting date to " + calendar.getTime() + " on " + labMetric.getLabMetricId());
                labMetric.setCreatedDate(calendar.getTime());
            }

            dao.persist(new FixupCommentary("GPLIM-3903 fix date on Initial Ribo run"));
            dao.flush();
            utx.commit();
        } catch (NotSupportedException | SystemException | HeuristicMixedException | RollbackException |
                HeuristicRollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupSupport1289() {
        try {
            utx.begin();
            userBean.loginOSUser();
            deleteRun("CRSP_ribotest_Nov19_pico", "SUPPORT-1289 remove Pico run, leaving Ribo run only");
            deleteRun("CRSP_ribotest_Nov16", "SUPPORT-1289 remove Pico run, leaving Ribo run only");
            utx.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException |
                HeuristicRollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupSupport1417() {
        try {
            utx.begin();
            userBean.loginOSUser();
            deleteRun("LCSET-8516_Shearing_Pico", "SUPPORT-1417 remove Varioskan run, so lab can do generic upload");
            deleteRun("LCSET-8516_Shearing_Pico_3", "SUPPORT-1417 remove Varioskan run, so lab can do generic upload");
            utx.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException |
                HeuristicRollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupGplim4028() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        LabMetric labMetric = labVesselDao.findById(LabMetric.class, 167003L);
        Assert.assertEquals(labMetric.getName(), LabMetric.MetricType.FINGERPRINT_PICO);
        Assert.assertEquals(labMetric.getLabMetricRun().getRunName(), "LCSET-8751 FP and Initial");
        System.out.println("Deleting LabMetric " + labMetric.getLabMetricId());
        labVesselDao.remove(labMetric);
        dao.persist(new FixupCommentary("GPLIM-4028 delete fingerpinting metric"));
        dao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim4105() {
        try {
            utx.begin();
            userBean.loginOSUser();
            deleteRun("9059 FP pico", "GPLIM-4105 remove FP run which was uploaded as Initial");
            utx.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException |
                HeuristicRollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void gplim4084ChangePondToShearingPico() {
        try {
            utx.begin();
            userBean.loginOSUser();
            deleteRun("8937_Pond_", "GPLIM-4084 remove Pico run, so lab can upload as Shearing with correct run name");
            utx.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException |
                HeuristicRollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupSupport1734() {
        try {
            utx.begin();
            userBean.loginOSUser();
            deleteRun("4-20-2016 IgFit Stock re-ribo",
                    "SUPPORT-1734 remove Plating Ribo run, so lab can do initial ribo upload");
            utx.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException |
                HeuristicRollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupGplim4444() {
        try {
            utx.begin();
            userBean.loginOSUser();
            deleteRun("LCSET-10117 FP Daughter Pico", "GPLIM-4444 remove FP run associated with wrong tubes");
            utx.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException |
                HeuristicRollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupGplim4803() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        List<LabMetric.MetricType> metricTypes = Arrays.asList(LabMetric.MetricType.VIIA_QPCR,
                LabMetric.MetricType.ECO_QPCR);
        for (LabMetric.MetricType metricType: metricTypes) {
            for (LabMetric labMetric : labMetricDao.findByMetricType(metricType)) {
                if (labMetric.getUnits() != LabMetric.LabUnit.NM) {
                    labMetric.setLabUnit(LabMetric.LabUnit.NM);
                    System.out.println("Lab metric " + labMetric.getLabMetricId() + " set lab unit to " +
                                       LabMetric.LabUnit.NM.getDisplayName());
                }
            }
        }
        labMetricDao.persist(new FixupCommentary("GPLIM-4803 change viia and eco lab units to nM"));
        labMetricDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim4854() {
        try {
            utx.begin();
            userBean.loginOSUser();
            deleteRun("05.19.17 Viia7 Set 1 Custom EL", "GPLIM-4854 incorrect uploads");
            utx.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException |
                HeuristicRollbackException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/DeleteLabMetricRuns.txt, so it can
     * be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * SUPPORT-3624 User uploaded the wrong pico type
     * 11_30-02:57 LCSET-12440
     */
    @Test(enabled = false)
    public void fixupSupport3624() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("DeleteLabMetricRuns.txt"));
        String reason = lines.get(0);

        for (String runName : lines.subList(1, lines.size())) {
            deleteRun(runName, reason);
        }

        utx.commit();
    }

    @Test(enabled = false)
    public void fixupSupport3463() throws Exception {
        long[] wrongLabMetricIds = new long[]{
                335907L,
                335908L,
                335909L,
                335910L,
                335911L,
                335912L,
                335913L,
                335914L,
                335915L,
                335916L,
                335917L,
                335918L,
                335919L,
                335920L,
                335921L,
                335922L,
                335923L,
                335924L,
                335925L,
                335926L,
                335927L,
                335928L,
                335929L,
                335930L,
                335931L,
                335932L,
                335933L,
                335934L,
                335935L,
                335936L,
                335937L,
                335938L,
                335939L,
                335940L,
                335941L,
                335942L,
                335943L,
                335944L,
                335945L,
                335946L,
                335947L,
                335948L,
                335949L,
                335950L,
                335951L,
                335952L,
                335953L,
                335954L,
                335955L,
                335956L,
                335957L,
                335958L,
                335959L,
                335960L,
                335961L,
                335962L,
                335963L,
                335964L,
                335965L,
                335966L,
                335967L,
                335968L,
                335969L,
                335970L,
                335971L,
                335972L,
                335973L,
                335974L,
                335975L,
                335976L,
                335977L,
                335978L,
                335979L,
                335980L,
                335981L,
                335982L,
                335983L,
                335984L,
                335985L,
                335986L,
                335987L,
                335988L,
                335989L,
                335990L,
                335991L,
                335992L,
                335993L,
                335994L,
                335995L,
                335996L,
                335997L,
                335998L,
                335999L
        };
        userBean.loginOSUser();
        utx.begin();
        for (long metricId : wrongLabMetricIds) {
            LabMetric labMetric = labVesselDao.findById(LabMetric.class, metricId);
            Assert.assertEquals(labMetric.getName(), LabMetric.MetricType.SHEARING_PICO);
            Assert.assertEquals(labMetric.getCreatedDate().toString(), "2017-07-26 15:05:00.245");
            System.out.println("Deleting LabMetric " + labMetric.getLabMetricId());
            labVesselDao.remove(labMetric);
        }
        dao.persist(new FixupCommentary("SUPPORT-3463 delete shearing metric"));
        dao.flush();
        utx.commit();
    }

    private void deleteRun(String runName, String reason) {
        LabMetricRun labMetricRun = dao.findByName(runName);
        for (LabMetric labMetric : labMetricRun.getLabMetrics()) {
            labMetric.getLabVessel().getMetrics().remove(labMetric);
            for (Metadata metadata : labMetric.getMetadataSet()) {
                dao.remove(metadata);
            }
            dao.remove(labMetric);
        }
        for (Metadata metadata : labMetricRun.getMetadata()) {
            dao.remove(metadata);
        }

        System.out.println("Deleting " + labMetricRun.getRunName());
        dao.remove(labMetricRun);
        dao.persist(new FixupCommentary(reason));
        dao.flush();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/ChangeMetricRunType.txt, so it
     * can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * SUPPORT-3483
     * 10x NCI inters low input\tPlating Pico\tTrue
     * 12128 inters\tPlating Pico\tTrue
     */
    @Test(enabled = false)
    public void fixupSupport3483() throws IOException {
        userBean.loginOSUser();
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("ChangeMetricRunType.txt"));
        String jiraTicket = lines.get(0);
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = TAB_PATTERN.split(lines.get(i));
            if (fields.length != 3) {
                throw new RuntimeException("Expected three tab separated fields in " + lines.get(i));
            }
            String runName = fields[0];
            LabMetricRun labMetricRun = dao.findByName(runName);
            Assert.assertNotNull(labMetricRun, runName + " not found");
            System.out.println("Updating run " + labMetricRun.getRunName());
            String metricTypeName = fields[1];
            LabMetric.MetricType metricType = LabMetric.MetricType.getByDisplayName(metricTypeName);
            Assert.assertNotNull(metricType, metricTypeName + " not found");
            labMetricRun.setMetricType(metricType);
            for (LabMetric labMetric : labMetricRun.getLabMetrics()) {
                System.out.println("Updating metric " + labMetric.getLabMetricId());
                labMetric.setMetricType(metricType);
            }
            if (Boolean.valueOf(fields[2])) {
                updateRisk(labMetricRun.getLabMetrics());
            }
        }
        dao.persist(new FixupCommentary(jiraTicket));
        dao.flush();
    }

    /**
     * This test reads its parameters from a file, testdata/DeleteGenericMetric.txt, so it can be used for other similar fixups,
     * without writing a new test.  Example contents of the file are:
     * GPLIM-6452
     * Plating Pico
     * 0185769113
     * 0185769029
     */
    @Test(enabled = false)
    public void fixupGplim6452() throws Exception {
        utx.begin();
        userBean.loginOSUser();
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("DeleteGenericMetric.txt"));
        String jiraTicket = lines.get(0);
        LabMetric.MetricType metricType = LabMetric.MetricType.getByDisplayName(lines.get(1));
        for (int i = 2; i < lines.size(); i++) {
            String barcode = lines.get(i).trim();
            LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
            if (labVessel == null) {
                throw new RuntimeException("Failed to find lab vessel " + barcode);
            }

            Set<LabMetric> deleteMetrics = new HashSet<>();
            for (LabMetric labMetric: labVessel.getMetrics()) {
                if (labMetric.getName() == metricType) {
                    deleteMetrics.add(labMetric);
                    System.out.println("delete lab metric " + labMetric.getLabMetricId() + " from " +
                                       labVessel.getLabel());
                }
            }
            labVessel.getMetrics().removeAll(deleteMetrics);
            for (LabMetric labMetric: deleteMetrics) {
                labMetricDao.remove(labMetric);
            }
        }
        labVesselDao.persist(new FixupCommentary(jiraTicket + " deleted lab metrics of type " + lines.get(1)));
        labVesselDao.flush();
        utx.commit();
    }

    private void updateRisk(Set<LabMetric> labMetrics) {
        Map<ProductOrder, List<ProductOrderSample>> mapPdoToListPdoSamples = new HashMap<>();
        Multimap<ProductOrderSample, LabMetric> mapPdoSampleToMetrics = HashMultimap.create();
        // This code could be factored out of QuantificationEJB.updateRisk, but it seems risky to change
        // production code for a fixup.
        //noinspection Duplicates
        for (LabMetric localLabMetric : labMetrics) {
            if (localLabMetric.getLabMetricDecision() != null) {
                for (SampleInstanceV2 sampleInstanceV2 : localLabMetric.getLabVessel().getSampleInstancesV2()) {
                    ProductOrderSample singleProductOrderSample = sampleInstanceV2.getSingleProductOrderSample();
                    if (singleProductOrderSample != null) {
                        ProductOrder productOrder = singleProductOrderSample.getProductOrder();
                        List<ProductOrderSample> productOrderSamples =
                                mapPdoToListPdoSamples.get(productOrder);
                        if (productOrderSamples == null) {
                            productOrderSamples = new ArrayList<>();
                            mapPdoToListPdoSamples.put(productOrder, productOrderSamples);
                        }
                        productOrderSamples.add(singleProductOrderSample);
                        mapPdoSampleToMetrics.put(singleProductOrderSample, localLabMetric);
                    }
                }
            }
        }
        for (Map.Entry<ProductOrder, List<ProductOrderSample>> pdoListPdoSamplesEntry :
                mapPdoToListPdoSamples.entrySet()) {
            ProductOrder productOrder = pdoListPdoSamplesEntry.getKey();
            System.out.print("Calculating risk for " + productOrder.getBusinessKey() + " ");
            List<ProductOrderSample> productOrderSamples = pdoListPdoSamplesEntry.getValue();
            for (ProductOrderSample productOrderSample : productOrderSamples) {
                System.out.print(productOrderSample.getSampleKey() + " ");
            }
            System.out.println();

            int i = productOrder.calculateRisk(productOrderSamples);
            System.out.println("Risk count " + i);
        }
    }
}
