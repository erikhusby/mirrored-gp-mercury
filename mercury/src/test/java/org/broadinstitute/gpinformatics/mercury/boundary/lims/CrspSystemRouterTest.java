package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class CrspSystemRouterTest extends SystemRouterTest {

    @BeforeMethod
    @Override
    public void setUp() {
        super.setUp();
        Deployment.isCRSP = true;
    }

    @AfterMethod
    public void tearDown() {

        Deployment.isCRSP = false;
    }

    @Override
    public void testGetSystemOfRecordForControlOnly() {
        try {
            super.testGetSystemOfRecordForControlOnly();
            Assert.fail("CRSP Deployment should Never route to Squid");
        } catch (Exception e) {

        }
    }

    @Override
    public void testMercuryAndSquidRouting() {
        try {
            super.testMercuryAndSquidRouting();
            Assert.fail("CRSP Deployment should Never route to Squid");
        } catch (Exception e) {
        }
    }

    @Override
    public void testMercuryOnlyRouting() {
        try {
            super.testMercuryOnlyRouting();
            Assert.fail("CRSP Deployment should Never route to Squid");
        } catch (Exception e) {
        }
    }

    @Override
    public void testRouteForPlateInMercuryWithNonExomeExpressOrder() {
        try {
            super.testRouteForPlateInMercuryWithNonExomeExpressOrder();
            Assert.fail("CRSP Deployment should Never route to Squid");
        } catch (Exception e) {
        }
    }

    @Override
    public void testRouteForPlateNotInMercury() {
        try {
            super.testRouteForPlateNotInMercury();
            Assert.fail("CRSP Deployment should Never route to Squid");
        } catch (Exception e) {
        }
    }

    @Override
    public void testRouteForTubeInMercuryWithNonExomeExpressOrder() {
        try {
            super.testRouteForTubeInMercuryWithNonExomeExpressOrder();
            Assert.fail("CRSP Deployment should Never route to Squid");
        } catch (Exception e) {
        }
    }

    @Override
    public void testRouteForTubeNotInMercury() {
        try {
            super.testRouteForTubeNotInMercury();
            Assert.fail("CRSP Deployment should Never route to Squid");
        } catch (Exception e) {
        }
    }

    @Override
    public void testRouteForTubesAllInMercuryWithoutExomeExpressOrders() {
        try {
            super.testRouteForTubesAllInMercuryWithoutExomeExpressOrders();
            Assert.fail("CRSP Deployment should Never route to Squid");
        } catch (Exception e) {
        }
    }

    @Override
    public void testRouteForTubesNoneInMercury() {
        try {
            super.testRouteForTubesNoneInMercury();
            Assert.fail("CRSP Deployment should Never route to Squid");
        } catch (Exception e) {
        }
    }

    @Override
    public void testRouteForTubesSomeInMercuryWithNonExomeExpressOrders() {
        try {
            super.testRouteForTubesSomeInMercuryWithNonExomeExpressOrders();
            Assert.fail("CRSP Deployment should Never route to Squid");
        } catch (Exception e) {
        }
    }
}
