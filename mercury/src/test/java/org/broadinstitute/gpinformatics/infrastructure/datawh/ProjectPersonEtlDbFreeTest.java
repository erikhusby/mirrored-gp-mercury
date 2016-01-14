package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ProjectPerson;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
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
public class ProjectPersonEtlDbFreeTest {
    private final String etlDateString = ExtractTransform.formatTimestamp(new Date());
    private long entityId = 1122334455L;
    private long researchProjectId = 2233445511L;
    private long personId = 3344551122L;
    private RoleType role = RoleType.PM;
    private String firstName = "Zinthia";
    private String lastName = "Zither";
    private String userName = "zz";
    private ProjectPersonEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private ResearchProjectDao dao = createMock(ResearchProjectDao.class);
    private ProjectPerson obj = createMock(ProjectPerson.class);
    private ResearchProject project = createMock(ResearchProject.class);
    private BSPUserList userList = createMock(BSPUserList.class);
    private BspUser user = createMock(BspUser.class);

    private Object[] mocks = new Object[]{auditReader, dao, obj, project, userList, user};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);

        tst = new ProjectPersonEtl(dao, userList);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getProjectPersonId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, ProjectPerson.class);
        assertEquals(tst.baseFilename, "research_project_person");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ProjectPerson.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateString, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(ProjectPerson.class, entityId)).andReturn(obj);
        expect(obj.getPersonId()).andReturn(personId).times(2);
        expect(userList.getById(personId)).andReturn(user);
        expect(obj.getProjectPersonId()).andReturn(entityId);
        expect(obj.getResearchProject()).andReturn(project).times(2);
        expect(project.getResearchProjectId()).andReturn(researchProjectId);
        expect(obj.getRole()).andReturn(role).times(2);
        expect(user.getFirstName()).andReturn(firstName);
        expect(user.getLastName()).andReturn(lastName);
        expect(user.getUsername()).andReturn(userName);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    public void testIncrementalEtlWithNulls() throws Exception {
        expect(dao.findById(ProjectPerson.class, entityId)).andReturn(obj);
        expect(obj.getPersonId()).andReturn(personId).times(2);
        expect(userList.getById(personId)).andReturn(user);
        expect(obj.getProjectPersonId()).andReturn(entityId);
        expect(obj.getResearchProject()).andReturn(null);
        expect(obj.getRole()).andReturn(null);
        expect(user.getFirstName()).andReturn(firstName);
        expect(user.getLastName()).andReturn(lastName);
        expect(user.getUsername()).andReturn(userName);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        String[] parts = records.iterator().next().split(",");
        assertEquals(parts[3], "");
        assertEquals(parts[4], "");

        verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;
        String[] parts = record.split(",");
        assertEquals(parts[i++], etlDateString);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], String.valueOf(researchProjectId));
        assertEquals(parts[i++], role.toString());
        assertEquals(parts[i++], String.valueOf(personId));
        assertEquals(parts[i++], firstName);
        assertEquals(parts[i++], lastName);
        assertEquals(parts[i++], userName);
        assertEquals(parts.length, i);
    }

}

