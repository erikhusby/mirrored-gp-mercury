package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.MetricReworkDisposition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * dbfree unit test of entity etl.
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class LabMetricEtlDbFreeTest {
    private final String etlDateStr = ExtractTransform.formatTimestamp(new Date());
    private final long entityId = 1122334455L;
    private final String vesselBarcode = "BARCODE4TEST";
    private final Long vesselID = new Long(99999);
    private final LabMetric.MetricType type = LabMetric.MetricType.POND_PICO;
    private final LabMetric.LabUnit units = LabMetric.LabUnit.UG_PER_ML;
    private final BigDecimal value = new BigDecimal(3.14);
    private final String runName = "PicoGreen hydra";
    private final Date runDate = new Date(1373988504L);
    private final Set<LabVessel> vesselList = new HashSet<>();
    private final VesselPosition vesselPosition = VesselPosition.D04;
    private final LabMetricDecision.Decision decision = LabMetricDecision.Decision.PASS;
    private final Long userID = 87L;
    private final String deciderName = "Maxwell Smart";
    private final Date decisionDate = new Date(1373988504L + ( 1000 * 60 * 60 * 24 ) ) ;
    private final String overrideReason = "Missed it by that much.";
    private final MetricReworkDisposition reworkDisposition = MetricReworkDisposition.TUBE_SPLIT_ADJUSTED_DOWN;
    private final String decisionNote = reworkDisposition.getDescription();

    private LabMetricEtl tst;

    private final AuditReaderDao auditReader = EasyMock.createMock(AuditReaderDao.class);
    private final LabMetricRunDao dao = EasyMock.createMock(LabMetricRunDao.class);
    private final LabMetric obj = EasyMock.createMock(LabMetric.class);
    private final LabVessel labVessel = EasyMock.createMock(LabVessel.class);
    private final LabMetricRun run = EasyMock.createMock(LabMetricRun.class);
    private final LabVessel vessel = EasyMock.createMock(LabVessel.class);
    private final BSPUserList userList = EasyMock.createMock(BSPUserList.class);
    private final LabMetricDecision labMetricDecision = EasyMock.createMock(LabMetricDecision.class);
    private final Object[] mocks = new Object[]{auditReader, dao, obj, labVessel, run, vessel, userList, labMetricDecision };

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        EasyMock.reset(mocks);

        vesselList.clear();
        vesselList.add(vessel);

        tst = new LabMetricEtl(dao, userList);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        EasyMock.expect(obj.getLabMetricId()).andReturn(entityId);
        EasyMock.replay(mocks);

        Assert.assertEquals(tst.entityClass, LabMetric.class);
        Assert.assertEquals(tst.baseFilename, "lab_metric");
        Assert.assertEquals(tst.entityId(obj), (Long) entityId);

        EasyMock.verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        EasyMock.expect(dao.findById(LabMetric.class, -1L)).andReturn(null);
        EasyMock.replay(mocks);
        Assert.assertEquals(tst.dataRecords(etlDateStr, false, -1L).size(), 0);
        EasyMock.verify(mocks);
    }

    public void testCantMakeEtlRecord2() throws Exception {
        EasyMock.expect(dao.findById(LabMetric.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabVessel()).andReturn(null);
        EasyMock.expect(obj.getLabMetricId()).andReturn(entityId);
        EasyMock.replay(mocks);
        Assert.assertEquals(tst.dataRecords(etlDateStr, false, entityId).size(), 0);
        EasyMock.verify(mocks);
    }

    public void testVesselNotTube() throws Exception {
        EasyMock.expect(dao.findById(LabMetric.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabVessel()).andReturn(labVessel).times(2);
        EasyMock.expect(obj.getLabMetricRun()).andReturn(run);
        EasyMock.expect(obj.getLabMetricId()).andReturn(entityId);
        EasyMock.expect(labVessel.getLabel()).andReturn(vesselBarcode);
        EasyMock.expect(labVessel.getLabVesselId()).andReturn(vesselID);
        EasyMock.expect(obj.getName()).andReturn(type);
        EasyMock.expect(obj.getUnits()).andReturn(units);
        EasyMock.expect(obj.getValue()).andReturn(value);
        EasyMock.expect(obj.getVesselPosition()).andReturn(vesselPosition).times(2);
        EasyMock.expect(obj.getLabMetricDecision()).andReturn(null).anyTimes();
        EasyMock.expect(run.getRunName()).andReturn(runName);
        EasyMock.expect(run.getRunDate()).andReturn(runDate);
        EasyMock.replay(mocks);
        Assert.assertEquals(tst.dataRecords(etlDateStr, false, entityId).size(), 1);
        EasyMock.verify(mocks);
    }

    public void testWithoutLabMetricRun() throws Exception {
        EasyMock.expect(dao.findById(LabMetric.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabVessel()).andReturn(labVessel).times(2);
        EasyMock.expect(obj.getLabMetricRun()).andReturn(null);
        EasyMock.expect(obj.getLabMetricId()).andReturn(entityId);
        EasyMock.expect(labVessel.getLabel()).andReturn(vesselBarcode);
        EasyMock.expect(labVessel.getLabVesselId()).andReturn(vesselID);
        EasyMock.expect(obj.getName()).andReturn(type);
        EasyMock.expect(obj.getUnits()).andReturn(units);
        EasyMock.expect(obj.getValue()).andReturn(value);
        EasyMock.expect(obj.getCreatedDate()).andReturn(runDate);
        EasyMock.expect(obj.getVesselPosition()).andReturn(vesselPosition).times(2);
        EasyMock.expect(obj.getLabMetricDecision()).andReturn(null).anyTimes();

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), null, runDate, false);
    }

    public void testWithLabMetricRun() throws Exception {
        EasyMock.expect(dao.findById(LabMetric.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabVessel()).andReturn(labVessel).times(2);
        EasyMock.expect(obj.getLabMetricRun()).andReturn(run);

        EasyMock.expect(obj.getLabMetricId()).andReturn(entityId);
        EasyMock.expect(labVessel.getLabel()).andReturn(vesselBarcode);
        EasyMock.expect(labVessel.getLabVesselId()).andReturn(vesselID);
        EasyMock.expect(obj.getName()).andReturn(type);
        EasyMock.expect(obj.getUnits()).andReturn(units);
        EasyMock.expect(obj.getValue()).andReturn(value);
        EasyMock.expect(obj.getVesselPosition()).andReturn(vesselPosition).times(2);
        EasyMock.expect(obj.getLabMetricDecision()).andReturn(null).anyTimes();
        EasyMock.expect(run.getRunName()).andReturn(runName);
        EasyMock.expect(run.getRunDate()).andReturn(runDate);
        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), runName, runDate, false);
    }

    public void testWithLabMetricDecision() throws Exception {
        EasyMock.expect(dao.findById(LabMetric.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabVessel()).andReturn(labVessel).times(2);
        EasyMock.expect(obj.getLabMetricRun()).andReturn(run);

        EasyMock.expect(obj.getLabMetricId()).andReturn(entityId);
        EasyMock.expect(labVessel.getLabel()).andReturn(vesselBarcode);
        EasyMock.expect(labVessel.getLabVesselId()).andReturn(vesselID);
        EasyMock.expect(obj.getName()).andReturn(type);
        EasyMock.expect(obj.getUnits()).andReturn(units);
        EasyMock.expect(obj.getValue()).andReturn(value);
        EasyMock.expect(obj.getVesselPosition()).andReturn(vesselPosition).times(2);
        EasyMock.expect(obj.getLabMetricDecision()).andReturn(labMetricDecision).anyTimes();

        EasyMock.expect(labMetricDecision.getDecidedDate()).andReturn(decisionDate);
        EasyMock.expect(labMetricDecision.getDeciderUserId()).andReturn(userID);
        EasyMock.expect(labMetricDecision.getDecision()).andReturn(decision);
        EasyMock.expect(labMetricDecision.getOverrideReason()).andReturn(overrideReason);
        EasyMock.expect(userList.getUserFullName(userID)).andReturn(deciderName);
        EasyMock.expect(labMetricDecision.getReworkDisposition()).andReturn(reworkDisposition).times(2);
        EasyMock.expect(labMetricDecision.getNote()).andReturn(decisionNote);

        EasyMock.expect(run.getRunName()).andReturn(runName);
        EasyMock.expect(run.getRunDate()).andReturn(runDate);
        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), runName, runDate, true);
    }


    private void verifyRecord(String record, String metricRunName, Date metricRunDate, boolean withDecision) {
        int i = 0;
        String[] parts = record.split(",", 17);
        Assert.assertEquals(parts[i++], etlDateStr);
        Assert.assertEquals(parts[i++], "F");
        Assert.assertEquals(parts[i++], String.valueOf(entityId));
        Assert.assertEquals(parts[i++], String.valueOf(type));
        Assert.assertEquals(parts[i++], String.valueOf(units));
        Assert.assertEquals(parts[i++], String.valueOf(value));
        Assert.assertEquals(parts[i++], GenericEntityEtl.format(metricRunName));
        Assert.assertEquals(parts[i++], GenericEntityEtl.format(metricRunDate));
        Assert.assertEquals(parts[i++], String.valueOf(vesselID));
        Assert.assertEquals(parts[i++], String.valueOf(vesselBarcode));
        Assert.assertEquals(parts[i++], vesselPosition.name());
        Assert.assertEquals(parts[i++], withDecision?decision.toString():"");
        Assert.assertEquals(parts[i++], withDecision?GenericEntityEtl.format(decisionDate):"");
        Assert.assertEquals(parts[i++], withDecision?deciderName:"");
        Assert.assertEquals(parts[i++], withDecision?overrideReason:"");
        Assert.assertEquals(parts[i++], withDecision ? reworkDisposition.name() : "");
        Assert.assertEquals(parts[i++], withDecision ? decisionNote : "");
        Assert.assertEquals(parts.length, i);
    }

}
