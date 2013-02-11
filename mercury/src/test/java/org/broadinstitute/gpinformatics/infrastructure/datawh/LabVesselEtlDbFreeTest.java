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
 * dbfree unit test of ProductOrder etl.
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

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(auditReader, dao, obj);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getLabVesselId()).andReturn(entityId);
        replay(auditReader, dao, obj);

        LabVesselEtl tst = new LabVesselEtl();
        tst.setLabVesselDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.getEntityClass(), LabVessel.class);

        assertEquals(tst.getBaseFilename(), "lab_vessel");

        assertEquals(tst.entityId(obj), (Long) entityId);

        assertNull(tst.entityStatusRecord(etlDateStr, null, null, false));

        assertTrue(tst.isEntityEtl());

        verify(dao, auditReader, obj);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(LabVessel.class, -1L)).andReturn(null);

        replay(dao, auditReader, obj);

        LabVesselEtl tst = new LabVesselEtl();
        tst.setLabVesselDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.entityRecord(etlDateStr, false, -1L).size(), 0);

        verify(dao, auditReader, obj);
    }

    public void testMakeEtlRecord() throws Exception {
        expect(dao.findById(LabVessel.class, entityId)).andReturn(obj);

        List<LabVessel> list = new ArrayList<LabVessel>();
        list.add(obj);
        expect(obj.getLabVesselId()).andReturn(entityId);
        expect(obj.getLabel()).andReturn(vesselLabel);
        expect(obj.getType()).andReturn(vesselType);

        replay(dao, auditReader, obj);

        LabVesselEtl tst = new LabVesselEtl();
        tst.setLabVesselDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecord(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);

        String[] parts = records.iterator().next().split(",");
        assertEquals(parts[0], etlDateStr);
        assertEquals(parts[1], "F");
        assertEquals(parts[2], String.valueOf(entityId));
        assertEquals(parts[3], vesselLabel);
        assertEquals(parts[4], vesselType.getName());

        verify(dao, auditReader, obj);
    }

    public void testMakeEtlRecord2() throws Exception {
        List<LabVessel> list = new ArrayList<LabVessel>();
        list.add(obj);
        expect(dao.findAll(eq(LabVessel.class), (GenericDao.GenericDaoCallback<LabVessel>)anyObject())).andReturn(list);

        expect(obj.getLabVesselId()).andReturn(entityId);
        expect(obj.getLabel()).andReturn(vesselLabel);
        expect(obj.getType()).andReturn(vesselType);

        replay(dao, auditReader, obj);

        LabVesselEtl tst = new LabVesselEtl();
        tst.setLabVesselDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecordsInRange(entityId, entityId, etlDateStr, false);
        assertEquals(records.size(), 1);
        String[] parts = records.iterator().next().split(",");
        assertEquals(parts[0], etlDateStr);
        assertEquals(parts[1], "F");
        assertEquals(parts[2], String.valueOf(entityId));
        assertEquals(parts[3], vesselLabel);
        assertEquals(parts[4], vesselType.getName());

        verify(dao, auditReader, obj);
    }

}

