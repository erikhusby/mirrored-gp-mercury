package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ProjectPerson;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
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
public class ProjectPersonEtlDbFreeTest {
    String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    long entityId = 1122334455L;
    long researchProjectId = 2233445511L;
    long personId = 3344551122L;
    RoleType role = RoleType.PM;
    String firstName = "Zinthia";
    String lastName = "Zither";
    String userName = "zz";

    AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    ResearchProjectDao dao = createMock(ResearchProjectDao.class);
    ProjectPerson obj = createMock(ProjectPerson.class);
    ResearchProject project = createMock(ResearchProject.class);
    BSPUserList userList = createMock(BSPUserList.class);
    BspUser user = createMock(BspUser.class);

    Object[] mocks = new Object[]{auditReader, dao, obj, project, userList, user};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getProjectPersonId()).andReturn(entityId);
        replay(mocks);

        ProjectPersonEtl tst = new ProjectPersonEtl();
        tst.setResearchProjectDao(dao);
        tst.setAuditReaderDao(auditReader);
        tst.setBSPUserList(userList);

        assertEquals(tst.getEntityClass(), ProjectPerson.class);

        assertEquals(tst.getBaseFilename(), "research_project_person");

        assertEquals(tst.entityId(obj), (Long) entityId);

        assertNull(tst.entityStatusRecord(etlDateStr, null, null, false));

        assertTrue(tst.isEntityEtl());

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ProjectPerson.class, -1L)).andReturn(null);

        replay(mocks);

        ProjectPersonEtl tst = new ProjectPersonEtl();
        tst.setResearchProjectDao(dao);
        tst.setAuditReaderDao(auditReader);
        tst.setBSPUserList(userList);

        assertEquals(tst.entityRecord(etlDateStr, false, -1L).size(), 0);

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

        ProjectPersonEtl tst = new ProjectPersonEtl();
        tst.setResearchProjectDao(dao);
        tst.setAuditReaderDao(auditReader);
        tst.setBSPUserList(userList);

        Collection<String> records = tst.entityRecord(etlDateStr, false, entityId);
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

        ProjectPersonEtl tst = new ProjectPersonEtl();
        tst.setResearchProjectDao(dao);
        tst.setAuditReaderDao(auditReader);
        tst.setBSPUserList(userList);

        Collection<String> records = tst.entityRecord(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);

        String[] parts = records.iterator().next().split(",");
        assertEquals(parts[3], "\"\"");
        assertEquals(parts[4], "\"\"");

        verify(mocks);
    }

    public void testBackfillEtl() throws Exception {
        List<ProjectPerson> list = new ArrayList<ProjectPerson>();
        list.add(obj);
        expect(dao.findAll(eq(ProjectPerson.class), (GenericDao.GenericDaoCallback<ProjectPerson>) anyObject())).andReturn(list);
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

        ProjectPersonEtl tst = new ProjectPersonEtl();
        tst.setResearchProjectDao(dao);
        tst.setAuditReaderDao(auditReader);
        tst.setBSPUserList(userList);

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
        assertEquals(parts[i++], role.toString());
        assertEquals(parts[i++], String.valueOf(personId));
        assertEquals(parts[i++], firstName);
        assertEquals(parts[i++], lastName);
        assertEquals(parts[i++], userName);
        assertEquals(parts.length, i);
    }

}

