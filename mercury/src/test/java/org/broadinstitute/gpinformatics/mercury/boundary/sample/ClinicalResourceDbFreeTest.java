package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.UnknownUserException;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.DATABASE_FREE)
public class ClinicalResourceDbFreeTest {

    private ClinicalResource clinicalResource;
    private UserBean userBean;
    private ManifestSessionEjb manifestSessionEjb;

    @BeforeMethod
    public void setUp() throws Exception {
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
        String username = "test_user";
        setupTestUser(username);

        clinicalResource.createAccessioningSession(username, "test manifest", "RP-1", Boolean.FALSE);
    }

    /**
     * Test that passing in <code>false</code> for isFromSampleKit throws an unsupported operation exception. This is
     * intended for future use for Buick-like projects. For now, we only support <code>true</code>, which is for the
     * workflow we're supporting at this time for clinical diagnostics.
     */
    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testManifestWithSamplesNotFromSampleKit() {
        String username = "test_user";
        setupTestUser(username);

        clinicalResource.createManifestWithSamples(username, "test manifest", "RP-1", Boolean.FALSE,
                Collections.<Sample>emptySet());
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
     * Test that passing in an unknown user throws an UnknownUserException. As tested elsewhere, this will result in a
     * 403 response.
     */
    @Test(expectedExceptions = UnknownUserException.class)
    public void testCreateManifestWithSamplesUnknownUser() {
        clinicalResource.createManifestWithSamples("unknown_user", "test manifest", "RP-1", Boolean.TRUE,
                Collections.<Sample>emptySet());
    }

    /**
     * Test that the call fails if isFromSampleKit is not passed in.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateManifestNullIsFromSampleKit() {
        String username = "test_user";
        setupTestUser(username);

        clinicalResource.createAccessioningSession(username, "test manifest", "RP-1", null);
    }

    /**
     * Test that the call fails if isFromSampleKit is not passed in.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateManifestWithSamplesNullIsFromSampleKit() {
        String username = "test_user";
        setupTestUser(username);

        clinicalResource.createManifestWithSamples(username, "test manifest", "RP-1", null,
                Collections.<Sample>emptySet());
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
     * Test that the call fails if a null manifest name is provided.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateManifestWithSamplesNullManifestName() {
        Mockito.when(userBean.isValidBspUser()).thenReturn(true);
        clinicalResource.createManifestWithSamples("test user", null, "RP-1", Boolean.TRUE,
                Collections.<Sample>emptySet());
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
     * Test that the call fails if an empty manifest name is provided.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateManifestWithSamplesEmptyManifestName() {
        Mockito.when(userBean.isValidBspUser()).thenReturn(true);
        clinicalResource.createManifestWithSamples("test user", "", "RP-1", Boolean.TRUE,
                Collections.<Sample>emptySet());
    }

    /**
     * Test that a valid request returns the ID for a new manifest manifest.
     */
    public void testValidCreateAccessioningSession() {
        String username = "test_user";
        BspUser user = setupTestUser(username);

        long expectedManifestId = 1L;
        String manifestName = "test manifest";
        String researchProjectKey = "RP-1";
        setupTestManifest(expectedManifestId, manifestName, researchProjectKey, user);

        long manifestId =
                clinicalResource.createAccessioningSession(username, manifestName, researchProjectKey, Boolean.TRUE);

        verifyUserLogin(username);
        Mockito.verify(manifestSessionEjb).createManifestSession(researchProjectKey, manifestName);
        assertThat(manifestId, equalTo(expectedManifestId));
    }

    /**
     * Test service that creates a manifest with all sample information.
     */
    public void testValidCreateManifestWithSamples() {
        String username = "test_user";
        BspUser user = setupTestUser(username);

        long expectedManifestId = 1L;
        String manifestName = "test manifest";
        String researchProjectKey = "RP-1";
        setupTestManifest(expectedManifestId, manifestName, researchProjectKey, user);

        Collection<Sample> samples = new ArrayList<>();
        clinicalResource.createManifestWithSamples(username, manifestName, researchProjectKey, Boolean.TRUE, samples);

        verifyUserLogin(username);
        Mockito.verify(manifestSessionEjb).createManifestSession(researchProjectKey, manifestName);
        Mockito.verify(manifestSessionEjb).addSamplesToManifest(expectedManifestId, samples);
    }

    private BspUser setupTestUser(String username) {
        BspUser user = new BspUser();
        user.setUserId(1L);
        user.setUsername(username);
        Mockito.when(userBean.getBspUser()).thenReturn(user);
        Mockito.when(userBean.isValidBspUser()).thenReturn(true);
        return user;
    }

    private void verifyUserLogin(String username) {
        Mockito.verify(userBean).login(username);
    }

    private void setupTestManifest(long manifestId, String manifestName, String researchProjectKey, BspUser user) {
        ManifestSession manifestSession = new ManifestSession(manifestId);
        Mockito.when(manifestSessionEjb.createManifestSession(researchProjectKey, manifestName)).thenReturn(
                manifestSession);
    }
}
