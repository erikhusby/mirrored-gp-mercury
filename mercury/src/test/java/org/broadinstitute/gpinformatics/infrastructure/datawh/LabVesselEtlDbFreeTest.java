package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.CONTAINER_TYPE;
import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * dbfree unit test of entity etl.
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class LabVesselEtlDbFreeTest {
    String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    long entityId = 1122334455L;
    String vesselLabel = "8917538.0";
    CONTAINER_TYPE vesselType = CONTAINER_TYPE.TUBE;

    AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    LabVessel obj = createMock(LabVessel.class);
    LabVesselDao dao = createMock(LabVesselDao.class);
    Object[] mocks = new Object[]{auditReader, dao, obj};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getLabVesselId()).andReturn(entityId);
        replay(mocks);

        LabVesselEtl tst = new LabVesselEtl();
        tst.setLabVesselDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.getEntityClass(), LabVessel.class);

        assertEquals(tst.getBaseFilename(), "lab_vessel");

        assertEquals(tst.entityId(obj), (Long) entityId);

        assertNull(tst.entityStatusRecord(etlDateStr, null, null, false));

        assertTrue(tst.isEntityEtl());

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(LabVessel.class, -1L)).andReturn(null);

        replay(mocks);

        LabVesselEtl tst = new LabVesselEtl();
        tst.setLabVesselDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.entityRecord(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(LabVessel.class, entityId)).andReturn(obj);

        expect(obj.getLabVesselId()).andReturn(entityId);
        expect(obj.getLabel()).andReturn(vesselLabel);
        expect(obj.getType()).andReturn(vesselType);

        replay(mocks);

        LabVesselEtl tst = new LabVesselEtl();
        tst.setLabVesselDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecord(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    public void testBackfillEtl() throws Exception {
        List<LabVessel> list = new ArrayList<LabVessel>();
        list.add(obj);
        expect(dao.findAll(eq(LabVessel.class), (GenericDao.GenericDaoCallback<LabVessel>)anyObject())).andReturn(list);

        expect(obj.getLabVesselId()).andReturn(entityId);
        expect(obj.getLabel()).andReturn(vesselLabel);
        expect(obj.getType()).andReturn(vesselType);

        replay(mocks);

        LabVesselEtl tst = new LabVesselEtl();
        tst.setLabVesselDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecordsInRange(entityId, entityId, etlDateStr, false);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
	int i = 0;
        String[] parts = record.split(",");
        assertEquals(parts[i++], etlDateStr);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], vesselLabel);
        assertEquals(parts[i++], vesselType.getName());
    }
}

