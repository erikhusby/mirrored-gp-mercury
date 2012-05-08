package org.broadinstitute.sequel.boundary.authentication;

import org.broadinstitute.sequel.entity.authentication.AuthorizedRole;
import org.broadinstitute.sequel.entity.authentication.PageAuthorization;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * @author Scott Matthews
 *         Date: 5/7/12
 *         Time: 4:37 PM
 */
public class AuthenticationServiceTest {

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

    @Inject AuthenticationService authSvc;


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
    public void test_retrieve_authorized_roles() throws Exception {

    }

    @Test
    public void test_add_new_authorization() throws Exception {

    }

    @Test
    public void test_is_page_protected() throws Exception {

    }

    @Test
    public void test_get_all_authorized_pages() throws Exception {

    }

    @Test
    public void test_find_by_page_name() throws Exception {

    }

    @Test
    public void test_add_roles_to_page() throws Exception {

    }

    @Test
    public void test_retrieve_all_roles() throws Exception {

    }

}
