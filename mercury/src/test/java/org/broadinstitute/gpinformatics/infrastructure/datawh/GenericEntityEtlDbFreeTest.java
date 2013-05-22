package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.hibernate.envers.RevisionType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;

/**
 * dbfree unit test of GenericEntityEtl as implemented by LabBatch etl.
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class GenericEntityEtlDbFreeTest {
    String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    long entityId = 1122334455L;
    String batchName = "LCSET-1235";

    String datafileDir = System.getProperty("java.io.tmpdir");

    AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    LabBatch obj = createMock(LabBatch.class);
    LabBatchDAO dao = createMock(LabBatchDAO.class);
    Object[] mocks = new Object[]{auditReader, dao, obj};
    
    LabBatchEtl tst = new LabBatchEtl(dao);
    RevInfo[] revInfo = new RevInfo[] {new RevInfo(), new RevInfo(), new RevInfo()};

    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    public void beforeClass() {
        for (int i = 0; i < revInfo.length; ++i) {
            revInfo[i].setRevInfoId(i + 1L);
            revInfo[i].setRevDate(new Date(1355000000000L + i * 1000000000L));
        }
    }

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        reset(mocks);
    }

    @AfterMethod(groups = TestGroups.DATABASE_FREE)
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }


    public void testEtl() throws Exception {
        Collection<Long> revIds = new ArrayList<Long>();
        revIds.add(entityId);

        List<Object[]> dataChanges = new ArrayList<Object[]>();
        dataChanges.add(new Object[]{obj, revInfo[0], RevisionType.ADD});

        expect(auditReader.fetchDataChanges(revIds, tst.entityClass, true)).andReturn(dataChanges);
        expect(dao.findById(LabBatch.class, entityId)).andReturn(obj);

        expect(obj.getLabBatchId()).andReturn(entityId).times(2);
        expect(obj.getBatchName()).andReturn(batchName);

        replay(mocks);

        tst.setAuditReaderDao(auditReader);

        int recordCount = tst.doEtl(revIds, etlDateStr);
        assertEquals(recordCount, 1);

        String dataFilename = etlDateStr + "_" + tst.baseFilename + ".dat";
        File datafile = new File(datafileDir, dataFilename);
        Assert.assertTrue(datafile.exists());

        verify(mocks);
    }

    public void testDeletionEtl() throws Exception {
        Collection<Long> revIds = new ArrayList<Long>();
        revIds.add(entityId);

        // Three changes to one entity result in one deletion record.
        List<Object[]> dataChanges = new ArrayList<Object[]>();
        dataChanges.add(new Object[]{obj, revInfo[0], RevisionType.ADD});
        dataChanges.add(new Object[]{obj, revInfo[1], RevisionType.MOD});
        dataChanges.add(new Object[]{obj, revInfo[2], RevisionType.DEL});

        expect(auditReader.fetchDataChanges(eq(revIds), (Class)anyObject(), eq(true))).andReturn(dataChanges);
        expect(obj.getLabBatchId()).andReturn(entityId).times(3);

        replay(mocks);

        tst.setAuditReaderDao(auditReader);

        int recordCount = tst.doEtl(revIds, etlDateStr);
        assertEquals(recordCount, 1);

        String dataFilename = etlDateStr + "_" + tst.baseFilename + ".dat";
        File datafile = new File(datafileDir, dataFilename);
        Assert.assertTrue(datafile.exists());

        verify(mocks);
    }

}
