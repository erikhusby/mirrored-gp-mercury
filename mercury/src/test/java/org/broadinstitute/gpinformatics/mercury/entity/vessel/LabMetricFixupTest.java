package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixup production Lab Metric entities
 */
public class LabMetricFixupTest extends Arquillian {

    @Inject
    private LabMetricRunDao dao;

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
}
