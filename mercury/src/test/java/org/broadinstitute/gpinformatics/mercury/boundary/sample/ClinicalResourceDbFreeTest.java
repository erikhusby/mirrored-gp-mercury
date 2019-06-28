package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.UnknownUserException;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.ClinicalResourceBean;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class ClinicalResourceDbFreeTest {

    private static final String RP_1 = "RP-1";
    private static final String MANIFEST_NAME = "test manifest";
    private static final String TEST_USER = "test_user";
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
    public void testManifestWithSamplesNotFromSampleKit() {
        stubValidLogin();

        ClinicalResourceBean clinicalResourceBean =
                ClinicalSampleTestFactory.createClinicalResourceBean(TEST_USER, MANIFEST_NAME, RP_1, Boolean.FALSE, 2);
        clinicalResource.createManifestWithSamples(clinicalResourceBean);
    }

    /**
     * Test that passing in an unknown user throws an UnknownUserException. As tested elsewhere, this will result in a
     * 403 response.
     */
    @Test(expectedExceptions = UnknownUserException.class)
    public void testCreateManifestWithSamplesUnknownUser() {
        ClinicalResourceBean clinicalResourceBean = ClinicalSampleTestFactory
                .createClinicalResourceBean("unknown_user", MANIFEST_NAME, RP_1, Boolean.TRUE,
                        Collections.<Sample>emptySet());
        clinicalResource.createManifestWithSamples(clinicalResourceBean);
    }

    /**
     * Test that the call fails if a null manifest name is provided.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateManifestWithSamplesNullManifestName() {
        Mockito.when(userBean.isValidBspUser()).thenReturn(true);

        ClinicalResourceBean clinicalResourceBean = ClinicalSampleTestFactory
                .createClinicalResourceBean("test user", null, RP_1, Boolean.TRUE, Collections.<Sample>emptySet());
        clinicalResource.createManifestWithSamples(clinicalResourceBean);
    }

    /**
     * Test that the call fails if an empty manifest name is provided.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateManifestWithSamplesEmptyManifestName() {
        Mockito.when(userBean.isValidBspUser()).thenReturn(true);
        ClinicalResourceBean clinicalResourceBean = ClinicalSampleTestFactory
                .createClinicalResourceBean(TEST_USER, "", RP_1, Boolean.TRUE, Collections.<Sample>emptySet());
        clinicalResource.createManifestWithSamples(clinicalResourceBean);
    }

    /**
     * Test service that creates a manifest with all sample information.
     */
    public void testValidCreateManifestWithSamples() {
        stubValidLogin();

        long expectedManifestId = 1L;
        Collection<Sample> samples = ClinicalSampleTestFactory.getRandomTestSamples(5);
        stubManifestCreation(expectedManifestId, MANIFEST_NAME, RP_1, samples);

        Boolean isFromSampleKit = Boolean.TRUE;

        ClinicalResourceBean clinicalResourceBean = ClinicalSampleTestFactory
                .createClinicalResourceBean(TEST_USER, MANIFEST_NAME, RP_1, isFromSampleKit, samples);
        clinicalResource.createManifestWithSamples(clinicalResourceBean);

        verifyUserLogin(TEST_USER);
        Mockito.verify(manifestSessionEjb)
                .createManifestSession(RP_1, MANIFEST_NAME, isFromSampleKit, samples);
    }

    /**
     * Stubs {@link UserBean} to report that the user has successfully logged in. There is no need for an actual
     * {@link BspUser} at this level because {@link ClinicalResource} doesn't actually need to access one. It simply
     * performs login and validates that login was successful. ClinicalResource then is able to get the BspUser from its
     * own injected reference to the session-scoped UserBean.
     */
    private void stubValidLogin() {
        Mockito.when(userBean.isValidBspUser()).thenReturn(true);
    }

    /**
     * Verifies that a login operation was performed on the session-scoped {@link UserBean}.
     *
     * @param username    the username that should have been logged in
     */
    private void verifyUserLogin(String username) {
        Mockito.verify(userBean).login(username);
    }

    /**
     * Stubs the behavior of {@link ManifestSessionEjb} for creating a manifest.
     *
     * @param manifestId         the ID for the created manifest
     * @param manifestName       the name for the created manifest
     * @param researchProjectKey the business key of the research project that the manifest is associated with
     * @param samples            the Samples to add to the new manifest.
     */
    private void stubManifestCreation( final long manifestId, String manifestName, String researchProjectKey,
                                      Collection<Sample> samples) {
        Mockito.when(manifestSessionEjb.createManifestSession(researchProjectKey, manifestName, true,
                samples)).then(new Answer<ManifestSession>() {

            @Override
            public ManifestSession answer(final InvocationOnMock invocation) throws Throwable {
                return new ManifestSession() {
                    @Override
                    public Long getManifestSessionId() {
                        return manifestId;
                    }
                };
            }
        });
    }


    public void testManifestWithEmptySamples() {
        stubValidLogin();
        try {
            ClinicalResourceBean clinicalResourceBean = ClinicalSampleTestFactory
                    .createClinicalResourceBean(TEST_USER, MANIFEST_NAME, RP_1, true, Collections.<Sample>emptyList());
            clinicalResource.createManifestWithSamples(clinicalResourceBean);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), endsWith(ClinicalResource.EMPTY_LIST_OF_SAMPLES_NOT_ALLOWED));
        }
    }

    public void testAddSampleToManifestSessionEmptySample()  {
        stubValidLogin();
        try {
            ClinicalResourceBean clinicalResourceBean = ClinicalSampleTestFactory
                    .createClinicalResourceBean(TEST_USER, MANIFEST_NAME, RP_1, true, Collections.singleton(
                            new Sample()));
            clinicalResource.createManifestWithSamples(clinicalResourceBean);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), endsWith(ClinicalResource.SAMPLE_CONTAINS_NO_METADATA));
        }
    }

    public void testAddSampleToManifestNullSamples() throws Exception {
        stubValidLogin();
        try {
            ClinicalResourceBean clinicalResourceBean = ClinicalSampleTestFactory
                    .createClinicalResourceBean(TEST_USER, MANIFEST_NAME, RP_1, true, Collections.<Sample>singleton(null));
            clinicalResource.createManifestWithSamples(clinicalResourceBean);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), endsWith(ClinicalResource.SAMPLE_IS_NULL));
        }
    }

    public void testAddSampleToManifestNullMetadataValue() throws Exception {
        stubValidLogin();

        try {
            Map<Metadata.Key, String> metadata = new HashMap<Metadata.Key, String>() {{
                    put(Metadata.Key.PERCENT_TUMOR, null);
            }};
            Sample sample = ClinicalSampleTestFactory.createSample(metadata);
            ClinicalResourceBean clinicalResourceBean = ClinicalSampleTestFactory
                    .createClinicalResourceBean(TEST_USER, MANIFEST_NAME, RP_1, true, Collections.singletonList(sample));
            clinicalResource.createManifestWithSamples(clinicalResourceBean);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), endsWith(ClinicalResource.SAMPLE_CONTAINS_NO_METADATA));
        }
    }

    public void testAddSampleToManifestRequiredMaterialTypeMissing() throws Exception {
        stubValidLogin();

        try {
            Map<Metadata.Key, String> metadata = new HashMap<Metadata.Key, String>() {{
                    put(Metadata.Key.SAMPLE_ID, "SM-NOTGOODENOUGH");
            }};
            Sample sample = ClinicalSampleTestFactory.createSample(metadata);
            ClinicalResourceBean clinicalResourceBean = ClinicalSampleTestFactory
                    .createClinicalResourceBean(TEST_USER, MANIFEST_NAME, RP_1, true, Collections.singletonList(sample));
            clinicalResource.createManifestWithSamples(clinicalResourceBean);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), startsWith(ClinicalResource.REQUIRED_FIELD_MISSING));
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFromSampleKitNull() throws JAXBException {
        ClinicalResourceBean clinicalResourceBean =
                ClinicalSampleTestFactory.createClinicalResourceBean(TEST_USER, MANIFEST_NAME, RP_1, true, 5);
        clinicalResourceBean.setFromSampleKit(null);
        assertThat(clinicalResourceBean.isFromSampleKit(), nullValue());
        clinicalResource.createManifestWithSamples(clinicalResourceBean);
    }
}
