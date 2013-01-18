package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.easymock.EasyMockSupport;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.MERCURY;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.SQUID;
//import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of logic to route messages and queries to Mercury or Squid as appropriate.
 *
 * Uses Mockito instead of EasyMock because Mockito has better support for stubbing behavior. Specifically, setUp() can
 * configure mock DAOs to return the various test entities without also setting the expectation that each test will
 * fetch every test entity.
 *
 * @author breilly
 */
public class MercuryOrSquidRouterTest {

    public static final String MERCURY_TUBE_1 = "mercuryTube1";
    public static final String MERCURY_TUBE_2 = "mercuryTube2";

    private MercuryOrSquidRouter mercuryOrSquidRouter;

    //    private TwoDBarcodedTubeDAO mockTwoDBarcodedTubeDAO = createMock(TwoDBarcodedTubeDAO.class); // EasyMock
    private TwoDBarcodedTubeDAO mockTwoDBarcodedTubeDAO = mock(TwoDBarcodedTubeDAO.class); // Mockito

    private TwoDBarcodedTube tube1;
    private TwoDBarcodedTube tube2;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() throws Exception {
        mercuryOrSquidRouter = new MercuryOrSquidRouter(mockTwoDBarcodedTubeDAO);

//        when(mockTwoDBarcodedTubeDAO.findByBarcode(anyString())).thenReturn(null); // Mockito
        tube1 = new TwoDBarcodedTube(MERCURY_TUBE_1);
//        expect(mockTwoDBarcodedTubeDAO.findByBarcode("mercuryTube1")).andReturn(tube1); // EasyMock
        when(mockTwoDBarcodedTubeDAO.findByBarcode(MERCURY_TUBE_1)).thenReturn(tube1); // Mockito

        tube2 = new TwoDBarcodedTube(MERCURY_TUBE_2);
        when(mockTwoDBarcodedTubeDAO.findByBarcode(MERCURY_TUBE_2)).thenReturn(tube2); // Mockito

        tube2.addSample(new MercurySample("PDO-1", "SM-1"));
        ProductFamily productFamily = new ProductFamily("Test Product Family");
        Product product = new Product("Test Product", productFamily, "Test product", "PDO-1",
                new Date(), new Date(), 0, 0, 0, 0, "Test samples only", "None", true, "Test Workflow", false);
        ResearchProject project = new ResearchProject(101L, "Test Project", "Test project", true);
        ProductOrder order = new ProductOrder(101L, "Test Order",
                Collections.singletonList(new ProductOrderSample("SM-1")), "Quote-1", product, project);
    }

    /*
     * Tests for routeForTubes()
     */

    //    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesNoneInMercury() {
    }

    //    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithoutOrders() {
    }

    //    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithOrders() {
    }

    //    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithoutOrders() {
    }

    //    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithOrders() {
    }

    /*
     * Tests for routeForPlate()
     */

    //    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateNotInMercury() {
    }

    //    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithoutOrder() {
    }

    //    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithOrder() {
    }

    /*
     * Tests for routeForTube()
     */

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeNotInMercury() {
//        expect(mockTwoDBarcodedTubeDAO.findByBarcode("squidTube")).andReturn(null); // EasyMock
//        replayAll(); // EasyMock

        assertThat(mercuryOrSquidRouter.routeForTube("squidTube"), is(SQUID));

//        verifyAll(); // EasyMock
        verify(mockTwoDBarcodedTubeDAO).findByBarcode("squidTube"); // Mockito
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithoutOrder() {
        assertThat(mercuryOrSquidRouter.routeForTube(MERCURY_TUBE_1), is(SQUID));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_1);
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithOrder() {
        assertThat(mercuryOrSquidRouter.routeForTube(MERCURY_TUBE_2), is(MERCURY));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_2);
    }
}
