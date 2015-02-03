package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.UnknownUserException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class ClinicalResourceTest {

    private ClinicalResource clinicalResource;

    @BeforeMethod
    public void setUp() throws Exception {
        clinicalResource = new ClinicalResource();
    }

    /**
     * Test that passing <code>false</code> for isFromSampleKit throws an unsupported operation exception. This is
     * intended for future use for Buick-like projects. For now, we only support <code>true</code>, which is for the
     * workflow we're supporting at this time for clinical diagnostics.
     */
    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testCreateSessionNotFromSampleKit() {
        clinicalResource.createAccessioningSession("test_user", "test manifest", "RP-1", Boolean.FALSE);
    }

    @Test(expectedExceptions = UnknownUserException.class)
    public void testCreateSessionUnknownUser() {
        clinicalResource.createAccessioningSession("unknown_user", "test manifest", "RP-1", Boolean.TRUE);
    }
}