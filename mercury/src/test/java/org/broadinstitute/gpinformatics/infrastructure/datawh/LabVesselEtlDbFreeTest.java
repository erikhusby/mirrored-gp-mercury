package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class LabVesselEtlDbFreeTest {
    private final String etlDateStr = ExtractTransform.formatTimestamp(new Date());
    private long entityId = 1122334455L;
    private String vesselLabel = "8917538.0";
    private LabVessel.ContainerType vesselType = LabVessel.ContainerType.TUBE;
    private String vesselName = "vessel_name";
    private Date createdOn = new Date();
    private LabVesselEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private LabVessel obj = createMock(LabVessel.class);
    private LabVesselDao dao = createMock(LabVesselDao.class);
    private Object[] mocks = new Object[]{auditReader, dao, obj};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);

        tst = new LabVesselEtl(dao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getLabVesselId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, LabVessel.class);
        assertEquals(tst.baseFilename, "lab_vessel");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(LabVessel.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(LabVessel.class, entityId)).andReturn(obj);

        expect(obj.getLabVesselId()).andReturn(entityId);
        expect(obj.getLabel()).andReturn(vesselLabel);
        expect(obj.getType()).andReturn(vesselType);
        expect(obj.getName()).andReturn(vesselName);
        expect(obj.getCreatedOn()).andReturn(createdOn);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
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
        assertEquals(parts[i++], vesselName);
        assertEquals(parts[i++], ExtractTransform.formatTimestamp(createdOn));
        assertEquals(parts.length, i);
    }
}

