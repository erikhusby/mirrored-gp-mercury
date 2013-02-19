package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
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
public class ResearchProjectEtlDbFreeTest {
    String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    long entityId = 1122334455L;
    String title = "Test ResearchProject";
    Date createdDate = new Date(1350000000000L);
    ResearchProject.Status status = ResearchProject.Status.Open;
    boolean irbNotEngaged = false;
    String jiraTicketKey = "PDO-0000";

    AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    ResearchProject obj = createMock(ResearchProject.class);
    ResearchProjectDao dao = createMock(ResearchProjectDao.class);
    Object[] mocks = new Object[]{auditReader, dao, obj};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getResearchProjectId()).andReturn(entityId);
        replay(mocks);

        ResearchProjectEtl tst = new ResearchProjectEtl();
        tst.setResearchProjectDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.getEntityClass(), ResearchProject.class);

        assertEquals(tst.getBaseFilename(), "research_project");

        assertEquals(tst.entityId(obj), (Long) entityId);

        assertNull(tst.entityStatusRecord(etlDateStr, null, null, false));

        assertTrue(tst.isEntityEtl());

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ResearchProject.class, -1L)).andReturn(null);

        replay(mocks);

        ResearchProjectEtl tst = new ResearchProjectEtl();
        tst.setResearchProjectDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.entityRecord(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(ResearchProject.class, entityId)).andReturn(obj);

        expect(obj.getResearchProjectId()).andReturn(entityId);
        expect(obj.getStatus()).andReturn(status).times(2);
        expect(obj.getCreatedDate()).andReturn(createdDate);
        expect(obj.getTitle()).andReturn(title);
        expect(obj.getIrbNotEngaged()).andReturn(irbNotEngaged);
        expect(obj.getJiraTicketKey()).andReturn(jiraTicketKey);

        replay(mocks);

        ResearchProjectEtl tst = new ResearchProjectEtl();
        tst.setResearchProjectDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecord(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    public void testBackfillEtl() throws Exception {
        List<ResearchProject> list = new ArrayList<ResearchProject>();
        list.add(obj);
        expect(dao.findAll(eq(ResearchProject.class), (GenericDao.GenericDaoCallback<ResearchProject>) anyObject())).andReturn(list);

        expect(obj.getResearchProjectId()).andReturn(entityId);
        expect(obj.getStatus()).andReturn(status).times(2);
        expect(obj.getCreatedDate()).andReturn(createdDate);
        expect(obj.getTitle()).andReturn(title);
        expect(obj.getIrbNotEngaged()).andReturn(irbNotEngaged);
        expect(obj.getJiraTicketKey()).andReturn(jiraTicketKey);

        replay(mocks);

        ResearchProjectEtl tst = new ResearchProjectEtl();
        tst.setResearchProjectDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecordsInRange(entityId, entityId, etlDateStr, false);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
        String[] parts = record.split(",");
        int i = 0;
        assertEquals(parts[i++], etlDateStr);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], status.getDisplayName());
        assertEquals(parts[i++], EtlTestUtilities.format(createdDate));
        assertEquals(parts[i++], title);
        assertEquals(parts[i++], EtlTestUtilities.format(irbNotEngaged));
        assertEquals(parts[i++], jiraTicketKey);
        assertEquals(parts.length, i);
    }

}

