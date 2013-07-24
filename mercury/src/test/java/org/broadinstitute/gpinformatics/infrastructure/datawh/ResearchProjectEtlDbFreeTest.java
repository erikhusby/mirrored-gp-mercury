package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
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
public class ResearchProjectEtlDbFreeTest {
    private final String etlDateString = ExtractTransform.formatTimestamp(new Date());
    private long entityId = 1122334455L;
    private String title = "Test ResearchProject";
    private Date createdDate = new Date(1350000000000L);
    private ResearchProject.Status status = ResearchProject.Status.Open;
    private boolean irbNotEngaged = false;
    private String jiraTicketKey = "PDO-0000";
    private Long parentResearchProjectId = 12345000L;
    private Long rootResearchProjectId = 12345600L;
    private ResearchProjectEtl tst;

    private ResearchProject parentObj = createMock(ResearchProject.class);
    private ResearchProject rootObj = createMock(ResearchProject.class);

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private ResearchProject obj = createMock(ResearchProject.class);
    private ResearchProjectDao dao = createMock(ResearchProjectDao.class);
    private Object[] mocks = new Object[]{auditReader, dao, obj, parentObj, rootObj};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);

        tst = new ResearchProjectEtl(dao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getResearchProjectId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, ResearchProject.class);
        assertEquals(tst.baseFilename, "research_project");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ResearchProject.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateString, false, -1L).size(), 0);

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

        expect(obj.getParentResearchProject()).andReturn(parentObj).times(2);
        expect(obj.getRootResearchProject()).andReturn(rootObj).times(2);

        expect(parentObj.getResearchProjectId()).andReturn(parentResearchProjectId);
        expect(rootObj.getResearchProjectId()).andReturn(rootResearchProjectId);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private String formatLong(Long value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    private void verifyRecord(String record) {
        String[] parts = record.split(",");
        int i = 0;
        assertEquals(parts[i++], etlDateString);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], status.getDisplayName());
        assertEquals(parts[i++], EtlTestUtilities.format(createdDate));
        assertEquals(parts[i++], title);
        assertEquals(parts[i++], EtlTestUtilities.format(irbNotEngaged));
        assertEquals(parts[i++], jiraTicketKey);
        assertEquals(parts[i++], formatLong(parentResearchProjectId));
        assertEquals(parts[i++], formatLong(rootResearchProjectId));
        assertEquals(parts.length, i);
    }
}

