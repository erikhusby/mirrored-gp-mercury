/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.bass;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class BassConfigTest {
    private BassConfig bassConfig;

    @BeforeTest
    public void setUp() {
        bassConfig = new BassConfig(Deployment.DEV);
    }

    public void testHost() {
        Assert.assertEquals(bassConfig.getHost(), "bass.broadinstitute.org");
    }

    public void getPort() {
        Assert.assertEquals(bassConfig.getPort(), 443);
    }

    public void getLogin() {
        Assert.assertEquals(bassConfig.getLogin(), "mercury-bass");
    }

    public void getHttpScheme() {
        Assert.assertEquals(BassConfig.getHttpScheme(), "https://");
    }

    public void getWSUrl() {
        Assert.assertEquals(bassConfig.getWSUrl("foo"), "https://bass.broadinstitute.org:443/foo");
    }
}
