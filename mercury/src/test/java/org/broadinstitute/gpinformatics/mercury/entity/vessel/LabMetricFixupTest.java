package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
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
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixup production Lab Metric entities
 */
@Test(groups = TestGroups.FIXUP)
public class LabMetricFixupTest extends Arquillian {

    @Inject
    private LabMetricRunDao dao;

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction utx;

    @Inject
    private LabVesselDao labVesselDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
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
}
