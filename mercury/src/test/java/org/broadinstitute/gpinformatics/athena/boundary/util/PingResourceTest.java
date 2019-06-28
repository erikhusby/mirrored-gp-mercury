package org.broadinstitute.gpinformatics.athena.boundary.util;

import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.List;

/**
 *
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class PingResourceTest extends StubbyContainerTest {

    public PingResourceTest(){}

    @Inject
    PingResource pingResource;


    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {

    }

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDown() throws Exception {

    }

    @Test(groups = TestGroups.STUBBY)
    public void testPing() throws Exception {
        List<String> results = pingResource.ping();
        Assert.assertNotNull(results);
        Assert.assertTrue(results.size() > 0);
    }
}
