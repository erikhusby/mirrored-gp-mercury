package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.UnknownUserException;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class ClinicalResourceDbFreeTest {

    private ClinicalResource clinicalResource;
    private UserBean userBean;
    private BspUser testUser;

    @BeforeMethod
    public void setUp() throws Exception {
        testUser = new BspUser();
        userBean = Mockito.mock(UserBean.class);
        clinicalResource = new ClinicalResource(userBean, null); // TODO: provide a (mock?) ManifestSessionEjb
    }

    /**
     * Test that passing in <code>false</code> for isFromSampleKit throws an unsupported operation exception. This is
     * intended for future use for Buick-like projects. For now, we only support <code>true</code>, which is for the
     * workflow we're supporting at this time for clinical diagnostics.
     */
    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testCreateSessionNotFromSampleKit() {
        Mockito.when(userBean.getBspUser()).thenReturn(testUser);
        clinicalResource.createAccessioningSession("test_user", "test manifest", "RP-1", Boolean.FALSE);
    }

    /**
     * Test that passing in an unknown user throws an UnknownUserException. As tested elsewhere, this will result in a
     * 403 response.
     */
    @Test(expectedExceptions = UnknownUserException.class)
    public void testCreateSessionUnknownUser() {
        clinicalResource.createAccessioningSession("unknown_user", "test manifest", "RP-1", Boolean.TRUE);
    }
}
