package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
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
 * dbfree unit test of ProductOrder etl.
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class LabBatchEtlDbFreeTest {
    String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    long entityId = 1122334455L;
    String batchName = "LCSET-1235";
    boolean isActive = true;
    Date createdOn = new Date(1350000000000L);
    Date dueDate = new Date(1354000000000L);

    AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    LabBatch obj = createMock(LabBatch.class);
    LabBatchDAO dao = createMock(LabBatchDAO.class);

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(auditReader, dao, obj);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getLabBatchId()).andReturn(entityId);
        replay(auditReader, dao, obj);

        LabBatchEtl tst = new LabBatchEtl();
        tst.setLabBatchDAO(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.getEntityClass(), LabBatch.class);

        assertEquals(tst.getBaseFilename(), "lab_batch");

        assertEquals(tst.entityId(obj), (Long) entityId);

        assertNull(tst.entityStatusRecord(etlDateStr, null, null, false));

        assertTrue(tst.isEntityEtl());

        verify(dao, auditReader, obj);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(LabBatch.class, -1L)).andReturn(null);

        replay(dao, auditReader, obj);

        LabBatchEtl tst = new LabBatchEtl();
        tst.setLabBatchDAO(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.entityRecord(etlDateStr, false, -1L).size(), 0);

        verify(dao, auditReader, obj);
    }

    public void testMakeEtlRecord() throws Exception {
        expect(dao.findById(LabBatch.class, entityId)).andReturn(obj);

        List<LabBatch> list = new ArrayList<LabBatch>();
        list.add(obj);
        expect(obj.getLabBatchId()).andReturn(entityId);
        expect(obj.getBatchName()).andReturn(batchName);
        expect(obj.getActive()).andReturn(isActive);
        expect(obj.getCreatedOn()).andReturn(createdOn);
        expect(obj.getDueDate()).andReturn(dueDate);

        replay(dao, auditReader, obj);

        LabBatchEtl tst = new LabBatchEtl();
        tst.setLabBatchDAO(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecord(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);

        String[] parts = records.iterator().next().split(",");
        assertEquals(parts[0], etlDateStr);
        assertEquals(parts[1], "F");
        assertEquals(parts[2], String.valueOf(entityId));
        assertEquals(parts[3], batchName);
        assertEquals(parts[4], isActive ? "T":"F");
        assertEquals(parts[5], ExtractTransform.secTimestampFormat.format(createdOn));
        assertEquals(parts[6], ExtractTransform.secTimestampFormat.format(dueDate));

        verify(dao, auditReader, obj);
    }

    public void testMakeEtlRecord2() throws Exception {
        List<LabBatch> list = new ArrayList<LabBatch>();
        list.add(obj);
        expect(dao.findAll(eq(LabBatch.class), (GenericDao.GenericDaoCallback<LabBatch>)anyObject())).andReturn(list);

        expect(obj.getLabBatchId()).andReturn(entityId);
        expect(obj.getBatchName()).andReturn(batchName);
        expect(obj.getActive()).andReturn(isActive);
        expect(obj.getCreatedOn()).andReturn(createdOn);
        expect(obj.getDueDate()).andReturn(dueDate);

        replay(dao, auditReader, obj);

        LabBatchEtl tst = new LabBatchEtl();
        tst.setLabBatchDAO(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecordsInRange(entityId, entityId, etlDateStr, false);
        assertEquals(records.size(), 1);
        String[] parts = records.iterator().next().split(",");
        assertEquals(parts[0], etlDateStr);
        assertEquals(parts[1], "F");
        assertEquals(parts[2], String.valueOf(entityId));
        assertEquals(parts[3], batchName);
        assertEquals(parts[4], isActive ? "T":"F");
        assertEquals(parts[5], ExtractTransform.secTimestampFormat.format(createdOn));
        assertEquals(parts[6], ExtractTransform.secTimestampFormat.format(dueDate));

        verify(dao, auditReader, obj);
    }

}

