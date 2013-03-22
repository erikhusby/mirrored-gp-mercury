package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class LabBatchEtlDbFreeTest {
    private String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    private long entityId = 1122334455L;
    private String batchName = "LCSET-1235";
    private LabBatchEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private LabBatch obj = createMock(LabBatch.class);
    private LabBatchDAO dao = createMock(LabBatchDAO.class);
    private Object[] mocks = new Object[]{auditReader, dao, obj};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);

        tst = new LabBatchEtl();
        tst.setLabBatchDAO(dao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getLabBatchId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, LabBatch.class);
        assertEquals(tst.baseFilename, "lab_batch");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(LabBatch.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(LabBatch.class, entityId)).andReturn(obj);

        expect(obj.getLabBatchId()).andReturn(entityId);
        expect(obj.getBatchName()).andReturn(batchName);

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
        assertEquals(parts[i++], batchName);
        assertEquals(parts.length, i);
    }

}

