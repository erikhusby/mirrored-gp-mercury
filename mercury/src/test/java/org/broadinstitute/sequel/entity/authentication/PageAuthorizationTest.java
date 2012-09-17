package org.broadinstitute.sequel.entity.authentication;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 5/7/12
 *         Time: 1:25 PM
 */
public class PageAuthorizationTest {

    final String testPath ="/testPath/";

    final String allRoleName ="All_test";
    final String devRoleName ="Sequel-Developers_test";
    final String pmRoleName ="Sequel-ProjectManagers_test";
    final String luRoleName ="Sequel-LabUsers_test";
    final String lmRoleName ="Sequel-LabManagers_test";

    PageAuthorization testPage ;

    AuthorizedRole roleAll;
    AuthorizedRole roleDev;
    AuthorizedRole rolePM;
    AuthorizedRole roleLabUser;
    AuthorizedRole roleLabManager;

    @BeforeMethod
    public void setUp() throws Exception {
        testPage = new PageAuthorization(testPath);

        roleAll = new AuthorizedRole(allRoleName);
        roleDev = new AuthorizedRole(devRoleName);
        rolePM = new AuthorizedRole(pmRoleName);
        roleLabUser = new AuthorizedRole(luRoleName);
        roleLabManager = new AuthorizedRole(lmRoleName);
    }

    @Test
    public void test_basic_path_definition() throws Exception {

        Assert.assertNotNull(testPage.getRoleList(), "Even though empty, the role list should NOT be null");
        Assert.assertNull(testPage.getAuthorizationId(), "At this point, the Authorization ID should be null");

        Assert.assertEquals(testPage.getPagePath(), testPath,
                            "Created page should have the same path as the one it was initialized with");

        testPage.setAuthorizationId(27L);

        Assert.assertEquals(testPage.getAuthorizationId(), new Long(27), "The authorization ID should Match");
    }

    @Test
    public void test_basic_authorization_creation() throws Exception{

        testPage.addRoleAccess(roleDev);

        Assert.assertNotNull(testPage.getRoleList(), "The role list should not be null");
        Assert.assertEquals(testPage.getRoleList().size(), 1, "There should be one role in the list of roles");


        List<String> testList = new LinkedList<String>();
        testList.add(roleDev.getRoleName());

        Assert.assertEquals(testPage.getRoleList(), testList);
    }

    @Test
    public void test_manual_path_authorization() throws Exception{

        List<AuthorizedRole> roles = new LinkedList<AuthorizedRole>();
        roles.add(roleDev);

        testPage.setRoleAccess(roles);

        Assert.assertNotNull(testPage.getRoleList(), "The role list should not be null");
        Assert.assertEquals(testPage.getRoleList().size(), 1, "There should be one role in the list of roles");


        List<String> testList = new LinkedList<String>();
        testList.add(roleDev.getRoleName());

        Assert.assertEquals(testPage.getRoleList(), testList);
    }

}
