package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
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
public class ResearchProjectIrbEtlDbFreeTest {
    private final String etlDateString = ExtractTransform.formatTimestamp(new Date());
    private long entityId = 1122334455L;
    private long researchProjectId = 2233445511L;
    private String irb = "NHGRI-1234-00-02042012-7";
    private ResearchProjectIRB.IrbType irbType = ResearchProjectIRB.IrbType.FARBER;
    private ResearchProjectIrbEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private ResearchProjectDao dao = createMock(ResearchProjectDao.class);
    private ResearchProjectIRB obj = createMock(ResearchProjectIRB.class);
    private ResearchProject project = createMock(ResearchProject.class);

    private Object[] mocks = new Object[]{auditReader, dao, obj, project};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);

        tst = new ResearchProjectIrbEtl(dao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getResearchProjectIRBId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, ResearchProjectIRB.class);
        assertEquals(tst.baseFilename, "research_project_irb");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ResearchProjectIRB.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateString, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(ResearchProjectIRB.class, entityId)).andReturn(obj);
        expect(obj.getResearchProjectIRBId()).andReturn(entityId);
        expect(obj.getResearchProject()).andReturn(project).times(2);
        expect(project.getResearchProjectId()).andReturn(researchProjectId);
        expect(obj.getIrb()).andReturn(irb);
        expect(obj.getIrbType()).andReturn(irbType).times(2);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    public void testIncrementalEtlWithNulls() throws Exception {
        expect(dao.findById(ResearchProjectIRB.class, entityId)).andReturn(obj);
        expect(obj.getResearchProjectIRBId()).andReturn(entityId);
        expect(obj.getResearchProject()).andReturn(null);
        expect(obj.getIrb()).andReturn(irb);
        expect(obj.getIrbType()).andReturn(null);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        String[] parts = records.iterator().next().split(",", 6);
        assertEquals(parts[3], "");
        assertEquals(parts[5], "");

        verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;
        String[] parts = record.split(",", 6);
        assertEquals(parts[i++], etlDateString);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], String.valueOf(researchProjectId));
        assertEquals(parts[i++], irb);
        assertEquals(parts[i++], irbType.getDisplayName());
        assertEquals(parts.length, i);
    }

}

