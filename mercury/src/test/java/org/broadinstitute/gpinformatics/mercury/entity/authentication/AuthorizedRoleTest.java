package org.broadinstitute.gpinformatics.mercury.entity.authentication;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for roles.
 *
 * @author Scott Matthews
 */
public class AuthorizedRoleTest {
    @Test
    public void basicRoleDefinition() {
        final String test_role_name = "Test_Role";
        AuthorizedRole testRole = new AuthorizedRole(test_role_name);

        Assert.assertNotNull(testRole);
        Assert.assertNull(testRole.getRoleId());
        Assert.assertEquals(testRole.getRoleName(), test_role_name);

        testRole.setRoleId(54L);

        Assert.assertEquals(testRole.getRoleId(), new Long(54));
    }

    @Test
    public void badRoleTest() throws Exception {
        try {
            AuthorizedRole badRole = new AuthorizedRole(null);
            Assert.fail("Can't make a role with null passed in constructor!");
        } catch (NullPointerException npe) {
            // should reach here!
        }

        try {
            AuthorizedRole badRole = new AuthorizedRole("");
            Assert.fail("Can't make a role with empty string passed in constructor!");
        } catch (IllegalArgumentException npe) {
            // should reach here!
        }
    }
}
