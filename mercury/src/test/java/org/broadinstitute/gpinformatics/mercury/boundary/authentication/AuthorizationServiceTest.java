package org.broadinstitute.gpinformatics.mercury.boundary.authentication;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.mercury.entity.authentication.AuthorizedRole;
import org.broadinstitute.gpinformatics.mercury.entity.authentication.PageAuthorization;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * @author Scott Matthews
 */
//@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class AuthorizationServiceTest extends ContainerTest {
    static final String testPath ="/testPath/" + UUID.randomUUID();

    static final String allRoleName = "All_test";
    static final String devRoleName = "Developers_test";
    static final String pmRoleName = "ProjectManagers_test";
    static final String luRoleName = "LabUsers_test";
    static final String lmRoleName = "LabManagers_test";

    PageAuthorization testPage;

    AuthorizedRole roleAll;
    AuthorizedRole roleDev;
    AuthorizedRole rolePM;
    AuthorizedRole roleLabUser;
    AuthorizedRole roleLabManager;
    List<String> predefinedRoleList;
    int numAuthPages;
    int numAuthRoles;

    @Inject AuthorizationService authSvc;

    @BeforeMethod
    public void setUp() throws Exception {

        predefinedRoleList = new LinkedList<String>();

        testPage = new PageAuthorization(testPath);

        roleAll = new AuthorizedRole(allRoleName);
        roleDev = new AuthorizedRole(devRoleName);
        rolePM = new AuthorizedRole(pmRoleName);
        roleLabUser = new AuthorizedRole(luRoleName);
        roleLabManager = new AuthorizedRole(lmRoleName);

        if (authSvc != null) {

            // Record the initial state settings so the tests can check for deltas.
            numAuthPages = authSvc.getAllAuthorizedPages().size();
            numAuthRoles = authSvc.retrieveAllRolesNames().size();

            authSvc.addNewRole(allRoleName);
            authSvc.addNewRole(devRoleName);
            authSvc.addNewRole(pmRoleName);
            authSvc.addNewRole(lmRoleName);
            authSvc.addNewRole(luRoleName);

            predefinedRoleList.add(allRoleName);
            predefinedRoleList.add(lmRoleName);

            authSvc.addNewPageAuthorization(testPath,predefinedRoleList);
        }
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (authSvc != null) {
            authSvc.removeRole(roleAll);
            authSvc.removeRole(roleDev);
            authSvc.removeRole(rolePM);
            authSvc.removeRole(roleLabManager);
            authSvc.removeRole(roleLabUser);
            authSvc.removePageAuthorization(testPath);
        }
    }

    @Test
    public void testRetrieveAuthorizedRoles() throws Exception {
        Collection<String> roleList = authSvc.retrieveAuthorizedRoles(testPage.getPagePath());

        Assert.assertTrue(roleList.contains(allRoleName));
        Assert.assertTrue(roleList.contains(lmRoleName));
        Assert.assertFalse(roleList.contains(pmRoleName));
        Assert.assertFalse(roleList.contains(luRoleName));
        Assert.assertFalse(roleList.contains(devRoleName));
    }

    @Test
    public void testAddNewAuthorization() throws Exception {
        List<String> roleList = new LinkedList<String>();
        roleList.add(pmRoleName);
        roleList.add(luRoleName);
        authSvc.addNewPageAuthorization("/testpath2/",roleList);

        Assert.assertNotNull(authSvc.findByPage("/testpath2/"));

        roleList.clear();
        roleList.addAll(authSvc.retrieveAuthorizedRoles("/testpath2/"));
        Assert.assertFalse(roleList.isEmpty());

        Assert.assertTrue(roleList.contains(pmRoleName));
        Assert.assertTrue(roleList.contains(luRoleName));

        authSvc.removePageAuthorization("/testpath2/");
    }

    @Test
    public void testIsPageProtected() throws Exception {
        Assert.assertTrue(authSvc.isPageProtected(testPath));

        Assert.assertFalse(authSvc.isPageProtected("/testpath2/"));
    }

    @Test
    public void testGetAllAuthorizedPages() throws Exception {
        Assert.assertFalse(authSvc.getAllAuthorizedPages().isEmpty());
        Assert.assertEquals(authSvc.getAllAuthorizedPages().size(), numAuthPages + 1);

        List<String> roleList = new LinkedList<String>();
        roleList.add(pmRoleName);
        roleList.add(luRoleName);

        authSvc.addNewPageAuthorization("/testpath2/", roleList);
        Assert.assertFalse(authSvc.getAllAuthorizedPages().isEmpty());
        Assert.assertEquals(authSvc.getAllAuthorizedPages().size(), numAuthPages + 2);
        authSvc.removePageAuthorization("/testpath2/");
    }

    @Test
    public void testFindByPageName() throws Exception {
        PageAuthorization foundPage = authSvc.findByPage(testPath);

        Assert.assertNotNull(foundPage);
        Assert.assertEquals(foundPage.getRoleList(), predefinedRoleList);
    }

    @Test
    public void testAddRolesToPage() throws Exception {
        PageAuthorization foundPage = authSvc.findByPage(testPath);

        Assert.assertEquals(foundPage.getRoleAccess().size(), 2);

        List<String> roleList = new LinkedList<String>();
        roleList.add(pmRoleName);
        roleList.add(luRoleName);

        authSvc.addRolesToPage(testPath,roleList);
        foundPage = authSvc.findByPage(testPath);

        Assert.assertEquals(foundPage.getRoleAccess().size(), 4);

        Assert.assertTrue(foundPage.getRoleAccess().contains(rolePM));
        Assert.assertTrue(foundPage.getRoleAccess().contains(roleLabUser));
        Assert.assertTrue(foundPage.getRoleAccess().contains(roleAll));
        Assert.assertTrue(foundPage.getRoleAccess().contains(roleLabManager));
        Assert.assertFalse(foundPage.getRoleAccess().contains(roleDev));
    }

    @Test
    public void testRetrieveAllRolesNames() throws Exception {
        Collection<String> registeredRoleNames = authSvc.retrieveAllRolesNames();

        Assert.assertEquals(registeredRoleNames.size(), numAuthRoles + 5);

        Assert.assertTrue(registeredRoleNames.contains(devRoleName));
        Assert.assertTrue(registeredRoleNames.contains(allRoleName));
        Assert.assertTrue(registeredRoleNames.contains(pmRoleName));
        Assert.assertTrue(registeredRoleNames.contains(lmRoleName));
        Assert.assertTrue(registeredRoleNames.contains(luRoleName));
    }

    @Test
    public void testRetrieveAllRoles() throws Exception {
        Collection<AuthorizedRole> registeredRoleNames = authSvc.retrieveAllRoles();

        Assert.assertEquals(registeredRoleNames.size(), numAuthRoles + 5);

        Assert.assertTrue(registeredRoleNames.contains(roleDev));
        Assert.assertTrue(registeredRoleNames.contains(roleAll));
        Assert.assertTrue(registeredRoleNames.contains(rolePM));
        Assert.assertTrue(registeredRoleNames.contains(roleLabManager));
        Assert.assertTrue(registeredRoleNames.contains(roleLabUser));
    }
}
