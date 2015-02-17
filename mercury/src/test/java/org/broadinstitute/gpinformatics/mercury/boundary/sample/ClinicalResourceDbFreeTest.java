package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.UnknownUserException;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.DATABASE_FREE)
public class ClinicalResourceDbFreeTest {

    private ClinicalResource clinicalResource;
    private UserBean userBean;
    private BspUser testUser;
    private ManifestSessionEjb manifestSessionEjb;

    @BeforeMethod
    public void setUp() throws Exception {
        testUser = new BspUser();
        userBean = Mockito.mock(UserBean.class);
        manifestSessionEjb = Mockito.mock(ManifestSessionEjb.class);
        clinicalResource = new ClinicalResource(userBean, manifestSessionEjb);
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

    /**
     * Test that the call fails if isFromSampleKit is not passed in.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateManifestWithIsFromSampleKitNull() {
        Mockito.when(userBean.isValidBspUser()).thenReturn(true);
        clinicalResource.createAccessioningSession("test_user", "test manifest", "RP-1", null);
    }

    /**
     * Test that the call fails if a null manifest name is provided.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateManifestNullManifestName() {
        Mockito.when(userBean.isValidBspUser()).thenReturn(true);
        clinicalResource.createAccessioningSession("test user", null, "RP-1", Boolean.TRUE);
    }

    /**
     * Test that the call fails if an empty manifest name is provided.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateManifestEmptyManifestName() {
        Mockito.when(userBean.isValidBspUser()).thenReturn(true);
        clinicalResource.createAccessioningSession("test user", "", "RP-1", Boolean.TRUE);
    }

    /**
     * Test that a valid request returns the ID for a new manifest manifest.
     */
    public void testValidRequest() {
        String username = "test_user";
        BspUser user = new BspUser();
        user.setUserId(1L);
        user.setUsername(username);
        Mockito.when(userBean.getBspUser()).thenReturn(user);
        Mockito.when(userBean.isValidBspUser()).thenReturn(true);
        long expectedManifestId = 1L;
        ManifestSession manifestSession = new ManifestSession(expectedManifestId);
        String researchProjectKey = "RP-1";
        String manifestName = "test manifest";

        Mockito.when(manifestSessionEjb.createManifestSession(researchProjectKey, manifestName, user)).thenReturn(
                manifestSession);

        long manifestId =
                clinicalResource.createAccessioningSession(username, manifestName, researchProjectKey, Boolean.TRUE);

        Mockito.verify(userBean).login(username);
        assertThat(manifestId, equalTo(expectedManifestId));
    }
}
