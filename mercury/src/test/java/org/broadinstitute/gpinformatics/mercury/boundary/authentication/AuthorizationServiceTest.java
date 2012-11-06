package org.broadinstitute.gpinformatics.mercury.boundary.authentication;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.authentication.AuthorizedRole;
import org.broadinstitute.gpinformatics.mercury.entity.authentication.PageAuthorization;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Scott Matthews
 */
//@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class AuthorizationServiceTest extends ContainerTest {
    final String testPath ="/testPath/";

    final String allRoleName ="All_test";
    final String devRoleName ="Mercury-Developers_test";
    final String pmRoleName ="Mercury-ProjectManagers_test";
    final String luRoleName ="Mercury-LabUsers_test";
    final String lmRoleName ="Mercury-LabManagers_test";

    PageAuthorization testPage ;

    AuthorizedRole roleAll;
    AuthorizedRole roleDev;
    AuthorizedRole rolePM;
    AuthorizedRole roleLabUser;
    AuthorizedRole roleLabManager;
    List<String> predefinedRoleList;

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

        if(null != authSvc) {
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
        if(null != authSvc) {
            authSvc.removeRole(roleAll);
            authSvc.removeRole(roleDev);
            authSvc.removeRole(rolePM);
            authSvc.removeRole(roleLabManager);
            authSvc.removeRole(roleLabUser);

            authSvc.removePageAuthorization(testPath);
        }
    }

    @Test
    public void test_retrieve_authorized_roles() throws Exception {
        Collection<String> roleList =authSvc.retrieveAuthorizedRoles(testPage.getPagePath());

        Assert.assertTrue(roleList.contains(allRoleName));
        Assert.assertTrue(roleList.contains(lmRoleName));
        Assert.assertFalse(roleList.contains(pmRoleName));
        Assert.assertFalse(roleList.contains(luRoleName));
        Assert.assertFalse(roleList.contains(devRoleName));
    }

    @Test
    public void test_add_new_authorization() throws Exception {
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
    public void test_is_page_protected() throws Exception {
        Assert.assertTrue(authSvc.isPageProtected(testPath));

        Assert.assertFalse(authSvc.isPageProtected("/testpath2/"));
    }

    @Test
    public void test_get_all_authorized_pages() throws Exception {
        Assert.assertFalse(authSvc.getAllAuthorizedPages().isEmpty());
        Assert.assertEquals(authSvc.getAllAuthorizedPages().size(), 1);

        List<String> roleList = new LinkedList<String>();
        roleList.add(pmRoleName);
        roleList.add(luRoleName);

        authSvc.addNewPageAuthorization("/testpath2/", roleList);
        Assert.assertFalse(authSvc.getAllAuthorizedPages().isEmpty());
        Assert.assertEquals(authSvc.getAllAuthorizedPages().size(), 2);
        authSvc.removePageAuthorization("/testpath2/");

    }

    @Test
    public void test_find_by_page_name() throws Exception {
        PageAuthorization foundPage = authSvc.findByPage(testPath);

        Assert.assertNotNull(foundPage);
        Assert.assertEquals(foundPage.getRoleList(), predefinedRoleList);
    }

    @Test
    public void test_add_roles_to_page() throws Exception {
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
    public void test_retrieve_all_roles_names() throws Exception {
        Collection<String> registeredRoleNames = authSvc.retrieveAllRolesNames();

        Assert.assertEquals(registeredRoleNames.size(), 10);

        Assert.assertTrue(registeredRoleNames.contains(devRoleName));
        Assert.assertTrue(registeredRoleNames.contains(allRoleName));
        Assert.assertTrue(registeredRoleNames.contains(pmRoleName));
        Assert.assertTrue(registeredRoleNames.contains(lmRoleName));
        Assert.assertTrue(registeredRoleNames.contains(luRoleName));
    }

    @Test
    public void test_retrieve_all_roles() throws Exception {
        Collection<AuthorizedRole> registeredRoleNames = authSvc.retrieveAllRoles();

        Assert.assertEquals(registeredRoleNames.size(), 10);

        Assert.assertTrue(registeredRoleNames.contains(roleDev));
        Assert.assertTrue(registeredRoleNames.contains(roleAll));
        Assert.assertTrue(registeredRoleNames.contains(rolePM));
        Assert.assertTrue(registeredRoleNames.contains(roleLabManager));
        Assert.assertTrue(registeredRoleNames.contains(roleLabUser));
    }

}
