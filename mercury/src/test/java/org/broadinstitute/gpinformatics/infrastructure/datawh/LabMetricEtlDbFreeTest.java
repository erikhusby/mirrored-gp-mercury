package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;

/**
 * dbfree unit test of entity etl.
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class LabMetricEtlDbFreeTest {
    private final String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    private final long entityId = 1122334455L;
    private final long labVesselId = 2233445566L;
    private final LabMetric.MetricType type = LabMetric.MetricType.POND_PICO;
    private final LabMetric.LabUnit units = LabMetric.LabUnit.UG_PER_ML;
    private final BigDecimal value = new BigDecimal(3.14);
    private final String runName = "PicoGreen hydra";
    private final Date runDate = new Date(1373988504L);
    private LabMetricEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private LabMetricRunDao dao = createMock(LabMetricRunDao.class);
    private LabMetric obj = createMock(LabMetric.class);
    private LabVessel labVessel = createMock(LabVessel.class);
    private LabMetricRun run = createMock(LabMetricRun.class);
    private Object[] mocks = new Object[]{auditReader, dao, obj, labVessel, run};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);

        tst = new LabMetricEtl(dao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getLabMetricId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, LabMetric.class);
        assertEquals(tst.baseFilename, "lab_metric");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(LabMetric.class, -1L)).andReturn(null);
        replay(mocks);
        assertEquals(tst.dataRecords(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testWithoutLabMetricRun() throws Exception {
        expect(dao.findById(LabMetric.class, entityId)).andReturn(obj);
        expect(obj.getLabMetricId()).andReturn(entityId);
        expect(obj.getLabVessel()).andReturn(labVessel);
        expect(obj.getName()).andReturn(type);
        expect(obj.getUnits()).andReturn(units);
        expect(obj.getValue()).andReturn(value);
        expect(obj.getLabMetricRun()).andReturn(null);
        expect(labVessel.getLabVesselId()).andReturn(labVesselId);
        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), null, null);

        verify(mocks);
    }

    public void testWithLabMetricRun() throws Exception {
        expect(dao.findById(LabMetric.class, entityId)).andReturn(obj);
        expect(obj.getLabMetricId()).andReturn(entityId);
        expect(obj.getLabVessel()).andReturn(labVessel);
        expect(obj.getName()).andReturn(type);
        expect(obj.getUnits()).andReturn(units);
        expect(obj.getValue()).andReturn(value);
        expect(obj.getLabMetricRun()).andReturn(run);
        expect(labVessel.getLabVesselId()).andReturn(labVesselId);
        expect(run.getRunName()).andReturn(runName);
        expect(run.getRunDate()).andReturn(runDate);
        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), runName, runDate);

        verify(mocks);
    }

    private void verifyRecord(String record, String metricRunName, Date metricRunDate) {
        int i = 0;
        String[] parts = record.split(",");
        assertEquals(parts[i++], etlDateStr);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], String.valueOf(labVesselId));
        assertEquals(parts[i++], String.valueOf(type));
        assertEquals(parts[i++], String.valueOf(units));
        assertEquals(parts[i++], String.valueOf(value));
        assertEquals(parts[i++], GenericEntityEtl.format(metricRunName));
        assertEquals(parts[i++], GenericEntityEtl.format(metricRunDate));
        assertEquals(parts.length, i);
    }
}

