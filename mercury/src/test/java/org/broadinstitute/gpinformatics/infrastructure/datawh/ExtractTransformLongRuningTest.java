package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtilityKeepScope;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.hibernate.SQLQuery;
import org.hibernate.type.LongType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.ws.rs.core.Response;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Container test of ExtractTransform for the long running tests excluded from continuous integration.
 */

@Test(groups = TestGroups.LONG_RUNNING, singleThreaded = true)
public class ExtractTransformLongRuningTest extends Arquillian {
    private static final Log logger = LogFactory.getLog(ExtractTransform.class);
    private String datafileDir;
    public final long MSEC_IN_SEC = 1000L;
    private Long[] longLabEventIds = null;
    private Long[] longPdoSampleIds = null;
    private Long[] longBackfillPdoSampleIds = null;

    @Inject
    private ExtractTransform extractTransform;
    @Inject
    private LabVesselDao labVesselDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, "dev", SessionContextUtilityKeepScope.class);
    }

    @BeforeClass
    public void beforeClass() throws Exception {
        datafileDir = System.getProperty("java.io.tmpdir");
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);
        // One time setup.
        if (labVesselDao != null && longLabEventIds == null) {
            // The join with revchanges is needed to make audits visible to ETL.  Currently the audits before
            // 8/6/14 (rev 330051) do not have joins with revchanges, so this test must not pick them up.
            longLabEventIds = getJoinedIds("select id1, id2, rev from "
                    + "(select count(*), min(lab_event_id) id1, max(lab_event_id) id2, rev from lab_event_aud "
                    + "where lab_event_type = 'INFINIUM_AUTOCALL_ALL_STARTED' and revtype != 2 "
                    + "and exists (select 1 from revchanges rc where rc.rev = lab_event_aud.rev) "
                    + "group by rev having count(*) > 1000 "
                    + "order by 1) where rownum = 1");
            Assert.assertFalse(longLabEventIds == null || longLabEventIds.length < 3);

            longPdoSampleIds = getJoinedIds("select id1, id2, rev from "
                    + "(select count(*), min(product_order_sample_id) id1, max(product_order_sample_id) id2, rev "
                    + "from athena.product_order_sample_aud "
                    + "where revtype = 1 and delivery_status != 'ABANDONED' "
                    + "and exists (select 1 from revchanges rc where rc.rev = athena.product_order_sample_aud.rev) "
                    + "group by rev having count(*) > 8250 "
                    + "order by 1, 4 desc) where rownum = 1");
            Assert.assertFalse(longPdoSampleIds == null || longPdoSampleIds.length < 3);

            longBackfillPdoSampleIds = getJoinedIds(
                    "select min(product_order_sample_id) id1, max(product_order_sample_id) id2, min(rev) rev from "
                            + "(select product_order_sample_id, rev "
                            + "from athena.product_order_sample_aud where rev = 570528 "
                            + "and rownum < 6500 order by 1)");
            Assert.assertFalse(longPdoSampleIds == null || longPdoSampleIds.length < 3);

        }
    }

    private Long[] getJoinedIds(String queryString) {
        Query query = labVesselDao.getEntityManager().createNativeQuery(queryString);
        query.unwrap(SQLQuery.class)
                .addScalar("id1", LongType.INSTANCE)
                .addScalar("id2", LongType.INSTANCE)
                .addScalar("rev", LongType.INSTANCE);
        Object[] obj = (Object[])query.getSingleResult();
        return new Long[]{(Long)obj[0], (Long)obj[1], (Long)obj[2]};
    }

    @Test
    public void testLongRunningLabEventBackfillEtl() throws Exception {
        // Tests lab event etl which has more complex processing.
        long startRun = System.currentTimeMillis();
        Response response1 = extractTransform.backfillEtl(LabEvent.class.getName(), longLabEventIds[0],
                longLabEventIds[1]);
        logger.info("LabEvent backfill etl ran " + (System.currentTimeMillis() - startRun) / 60000 + " minutes");
        Assert.assertEquals(response1.getStatus(), Response.Status.OK.getStatusCode());
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, "_event_fact.dat", "F", longLabEventIds[0]));
    }

    @Test
    public void testLongRunningLabEventIncrementalEtl() throws Exception {
        // Tests lab event etl which has more complex processing.
        RevInfo revInfo = labVesselDao.findById(RevInfo.class, longLabEventIds[2]);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.formatTimestamp(revInfo.getRevDate());
        String endEtl = ExtractTransform.formatTimestamp(
                new Date(ExtractTransform.parseTimestamp(startEtl).getTime() + MSEC_IN_SEC));
        long startRun = System.currentTimeMillis();
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        logger.info("LabEvent incremental etl ran " + (System.currentTimeMillis() - startRun) / 60000 + " minutes");
        Assert.assertTrue(recordCount > 0);
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, "_event_fact.dat", "F", longLabEventIds[0]));
    }

    @Test
    public void testLongRunningPdoSampleBackfillEtl() throws Exception {
        long startRun = System.currentTimeMillis();
        Response response = extractTransform.backfillEtl(ProductOrderSample.class.getName(),
                longBackfillPdoSampleIds[0], longBackfillPdoSampleIds[1]);
        logger.info("PdoSample backfill etl ran " + (System.currentTimeMillis() - startRun) / 60000 + " minutes");
        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, "_product_order_sample.dat", "F",
                longBackfillPdoSampleIds[1]));
    }

    @Test
    public void testLongRunningPdoSampleIncrementalEtl() throws Exception {
        RevInfo revInfo = labVesselDao.findById(RevInfo.class, longPdoSampleIds[2]);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.formatTimestamp(revInfo.getRevDate());
        String endEtl = ExtractTransform.formatTimestamp(
                new Date(ExtractTransform.parseTimestamp(startEtl).getTime() + MSEC_IN_SEC));
        long startRun = System.currentTimeMillis();
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        logger.info("PdoSample incremental etl ran " + (System.currentTimeMillis() - startRun) / 60000 + " minutes");
        Assert.assertTrue(recordCount > 0);
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, "_product_order_sample.dat", "F",
                longPdoSampleIds[0]));
    }

}
