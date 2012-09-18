package org.broadinstitute.gpinformatics.mercury.entity.authentication;

import junit.framework.Assert;
import org.testng.annotations.Test;

/**
 * @author Scott Matthews
 *         Date: 5/7/12
 *         Time: 4:21 PM
 */
public class AuthorizedRoleTest {

    @Test
    public void basic_role_definition() {
        final String test_role_name = "Test_Role";
        AuthorizedRole testRole = new AuthorizedRole(test_role_name);

        Assert.assertNotNull(testRole);
        Assert.assertNull(testRole.getRoleId());
        Assert.assertNull(testRole.getRoleId());

        Assert.assertEquals(testRole.getRoleName(), test_role_name);

        testRole.setRoleId(54L);

        Assert.assertEquals(testRole.getRoleId(), new Long(54));

    }

    @Test
    public void bad_role_test() {

        try {
            AuthorizedRole badRole = new AuthorizedRole(null);
            Assert.fail();
        } catch (NullPointerException npe) {

        }


        try {
            AuthorizedRole badRole = new AuthorizedRole("");
            Assert.fail();
        } catch (IllegalArgumentException npe) {

        }



    }

}
