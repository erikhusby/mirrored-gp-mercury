package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private final long labVesselId = 2233445566L;
    private final LabMetric.MetricType type = LabMetric.MetricType.POND_PICO;
    private final LabMetric.LabUnit units = LabMetric.LabUnit.UG_PER_ML;
    private final BigDecimal value = new BigDecimal(3.14);
    private final String runName = "PicoGreen hydra";
    private final Date runDate = new Date(1373988504L);
    private final Set<LabVessel> vesselList = new HashSet<>();
    private final String vesselPosition = "D4";
    private LabMetricEtl tst;

    private final AuditReaderDao auditReader = EasyMock.createMock(AuditReaderDao.class);
    private final LabMetricRunDao dao = EasyMock.createMock(LabMetricRunDao.class);
    private final LabMetric obj = EasyMock.createMock(LabMetric.class);
    private final LabVessel labVessel = EasyMock.createMock(LabVessel.class);
    private final LabMetricRun run = EasyMock.createMock(LabMetricRun.class);
    private final LabVessel vessel = EasyMock.createMock(LabVessel.class);
    private final Object[] mocks = new Object[]{auditReader, dao, obj, labVessel, run, vessel};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        EasyMock.reset(mocks);

        vesselList.clear();
        vesselList.add(vessel);

        tst = new LabMetricEtl(dao);
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
        EasyMock.expect(obj.getLabMetricId()).andReturn(entityId);
        EasyMock.expect(labVessel.getType()).andReturn(LabVessel.ContainerType.PLATE_WELL);
        EasyMock.replay(mocks);
        Assert.assertEquals(tst.dataRecords(etlDateStr, false, entityId).size(), 0);
        EasyMock.verify(mocks);
    }

    public void testWithoutLabMetricRun() throws Exception {
        EasyMock.expect(dao.findById(LabMetric.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabVessel()).andReturn(labVessel).times(3);
        EasyMock.expect(obj.getLabMetricRun()).andReturn(null);
        EasyMock.expect(obj.getLabMetricId()).andReturn(entityId);
        EasyMock.expect(labVessel.getLabVesselId()).andReturn(labVesselId);
        EasyMock.expect(labVessel.getType()).andReturn(LabVessel.ContainerType.TUBE);
        EasyMock.expect(obj.getName()).andReturn(type);
        EasyMock.expect(obj.getUnits()).andReturn(units);
        EasyMock.expect(obj.getValue()).andReturn(value);
        EasyMock.expect(obj.getCreatedDate()).andReturn(runDate);
        EasyMock.expect(obj.getVesselPosition()).andReturn(vesselPosition);
        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), null, runDate);
    }

    public void testWithLabMetricRun() throws Exception {
        EasyMock.expect(dao.findById(LabMetric.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabVessel()).andReturn(labVessel).times(3);
        EasyMock.expect(obj.getLabMetricRun()).andReturn(run);

        EasyMock.expect(obj.getLabMetricId()).andReturn(entityId);
        EasyMock.expect(labVessel.getLabVesselId()).andReturn(labVesselId);
        EasyMock.expect(labVessel.getType()).andReturn(LabVessel.ContainerType.TUBE);
        EasyMock.expect(obj.getName()).andReturn(type);
        EasyMock.expect(obj.getUnits()).andReturn(units);
        EasyMock.expect(obj.getValue()).andReturn(value);
        EasyMock.expect(obj.getVesselPosition()).andReturn(vesselPosition);
        EasyMock.expect(run.getRunName()).andReturn(runName);
        EasyMock.expect(run.getRunDate()).andReturn(runDate);
        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), runName, runDate);
    }

    private void verifyRecord(String record, String metricRunName, Date metricRunDate) {
        int i = 0;
        String[] parts = record.split(",");
        Assert.assertEquals(parts[i++], etlDateStr);
        Assert.assertEquals(parts[i++], "F");
        Assert.assertEquals(parts[i++], String.valueOf(entityId));
        Assert.assertEquals(parts[i++], String.valueOf(type));
        Assert.assertEquals(parts[i++], String.valueOf(units));
        Assert.assertEquals(parts[i++], String.valueOf(value));
        Assert.assertEquals(parts[i++], GenericEntityEtl.format(metricRunName));
        Assert.assertEquals(parts[i++], GenericEntityEtl.format(metricRunDate));
        Assert.assertEquals(parts[i++], String.valueOf(labVesselId));
        Assert.assertEquals(parts[i++], vesselPosition);
        Assert.assertEquals(parts.length, i);
    }
}

