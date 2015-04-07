package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
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
import java.util.Date;
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

    @Test(enabled = true)
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
}
