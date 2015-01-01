package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.presentation.ActionBeanBaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class SecurityActionBeanTest extends ActionBeanBaseTest<SecurityActionBean> {

    private static final String TEST_USERNAME = "QADudeTest";
    private static final String TEST_PASSWORD = "QA";

    public SecurityActionBeanTest() {
        super(new SecurityActionBean());
    }

    /**
     * Test login to the web application.
     *
     * @throws Exception
     */
    @Test(enabled = false, groups = {TestGroups.EXTERNAL_INTEGRATION})
    public void testSignIn() throws Exception {
        Assert.assertNotNull(getBean());

        getBean().setUsername(TEST_USERNAME);
        getBean().setPassword(TEST_PASSWORD);
        getBean().signIn();
        Assert.assertTrue(!getBean().hasErrors(), "Should not have any errors! " + getBean().getContext().getValidationErrors().toString());
    }
}
