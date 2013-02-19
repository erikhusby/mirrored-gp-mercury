package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectCohort;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
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
public class ResearchProjectCohortEtlDbFreeTest {
    String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    long entityId = 1122334455L;
    long researchProjectId = 2233445511L;

    AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    ResearchProjectDao dao = createMock(ResearchProjectDao.class);
    ResearchProjectCohort obj = createMock(ResearchProjectCohort.class);
    ResearchProject project = createMock(ResearchProject.class);

    Object[] mocks = new Object[]{auditReader, dao, obj, project};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getResearchProjectCohortId()).andReturn(entityId);
        replay(mocks);

        ResearchProjectCohortEtl tst = new ResearchProjectCohortEtl();
        tst.setResearchProjectDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.getEntityClass(), ResearchProjectCohort.class);

        assertEquals(tst.getBaseFilename(), "research_project_cohort");

        assertEquals(tst.entityId(obj), (Long) entityId);

        assertNull(tst.entityStatusRecord(etlDateStr, null, null, false));

        assertTrue(tst.isEntityEtl());

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ResearchProjectCohort.class, -1L)).andReturn(null);

        replay(mocks);

        ResearchProjectCohortEtl tst = new ResearchProjectCohortEtl();
        tst.setResearchProjectDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.entityRecord(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(ResearchProjectCohort.class, entityId)).andReturn(obj);
        expect(obj.getResearchProjectCohortId()).andReturn(entityId);
        expect(obj.getResearchProject()).andReturn(project).times(2);
        expect(project.getResearchProjectId()).andReturn(researchProjectId);

        replay(mocks);

        ResearchProjectCohortEtl tst = new ResearchProjectCohortEtl();
        tst.setResearchProjectDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecord(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    public void testIncrementalEtlWithNulls() throws Exception {
        expect(dao.findById(ResearchProjectCohort.class, entityId)).andReturn(obj);
        expect(obj.getResearchProjectCohortId()).andReturn(entityId);
        expect(obj.getResearchProject()).andReturn(null);

        replay(mocks);

        ResearchProjectCohortEtl tst = new ResearchProjectCohortEtl();
        tst.setResearchProjectDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecord(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);

        String[] parts = records.iterator().next().split(",");
        assertEquals(parts[3], "\"\"");

        verify(mocks);
    }

    public void testBackfillEtl() throws Exception {
        List<ResearchProjectCohort> list = new ArrayList<ResearchProjectCohort>();
        list.add(obj);
        expect(dao.findAll(eq(ResearchProjectCohort.class), (GenericDao.GenericDaoCallback<ResearchProjectCohort>) anyObject())).andReturn(list);
        expect(obj.getResearchProjectCohortId()).andReturn(entityId);
        expect(obj.getResearchProject()).andReturn(project).times(2);
        expect(project.getResearchProjectId()).andReturn(researchProjectId);

        replay(mocks);

        ResearchProjectCohortEtl tst = new ResearchProjectCohortEtl();
        tst.setResearchProjectDao(dao);
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
        assertEquals(parts[i++], String.valueOf(researchProjectId));
        assertEquals(parts.length, i);
    }

}

