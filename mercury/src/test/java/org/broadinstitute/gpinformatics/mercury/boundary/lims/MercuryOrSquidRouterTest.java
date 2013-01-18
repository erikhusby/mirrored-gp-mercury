package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
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
 * The current logic is that vessels or batches of vessels containing any references to samples that are part of an
 * Exome Express product orders should be processed by Mercury. Everything else should go to Squid.
 *
 * Uses Mockito instead of EasyMock because Mockito has better support for stubbing behavior. Specifically, setUp() can
 * configure mock DAOs to return the various test entities without also setting the expectation that each test will
 * fetch every test entity.
 *
 * @author breilly
 */
public class MercuryOrSquidRouterTest {

    private static final String MERCURY_TUBE_1 = "mercuryTube1";
    private static final String MERCURY_TUBE_2 = "mercuryTube2";
    private static final String MERCURY_TUBE_3 = "mercuryTube3";

    private MercuryOrSquidRouter mercuryOrSquidRouter;

    private TwoDBarcodedTubeDAO mockTwoDBarcodedTubeDAO;
    private ProductOrderDao mockProductOrderDao;

    private TwoDBarcodedTube tube1;
    private TwoDBarcodedTube tube2;
    private TwoDBarcodedTube tube3;
    private ResearchProject testProject;
    private Product testProduct;
    private Product exomeExpress;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() throws Exception {
        mockTwoDBarcodedTubeDAO = mock(TwoDBarcodedTubeDAO.class);
        mockProductOrderDao = mock(ProductOrderDao.class);
        mercuryOrSquidRouter = new MercuryOrSquidRouter(mockTwoDBarcodedTubeDAO, mockProductOrderDao);

//        when(mockTwoDBarcodedTubeDAO.findByBarcode(anyString())).thenReturn(null); // TODO: Make this explicit and required? Currently this is the default behavior even without this call

        tube1 = new TwoDBarcodedTube(MERCURY_TUBE_1);
        when(mockTwoDBarcodedTubeDAO.findByBarcode(MERCURY_TUBE_1)).thenReturn(tube1);

        tube2 = new TwoDBarcodedTube(MERCURY_TUBE_2);
        when(mockTwoDBarcodedTubeDAO.findByBarcode(MERCURY_TUBE_2)).thenReturn(tube2);

        tube3 = new TwoDBarcodedTube(MERCURY_TUBE_3);
        when(mockTwoDBarcodedTubeDAO.findByBarcode(MERCURY_TUBE_3)).thenReturn(tube3);

        testProject = new ResearchProject(101L, "Test Project", "Test project", true);

        ProductFamily family = new ProductFamily("Test Product Family");
        testProduct = new Product("Test Product", family, "Test product", "P-TEST-1", new Date(), new Date(),
                0, 0, 0, 0, "Test samples only", "None", true, "Test Workflow", false);
        exomeExpress = new Product("Exome Express", family, "Exome express", "P-EX-1", new Date(), new Date(),
                0, 0, 0, 0, "Test exome express samples only", "None", true, "Test Exome Express Workflow", false);
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
        assertThat(mercuryOrSquidRouter.routeForTube("squidTube"), is(SQUID));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode("squidTube");
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithoutOrder() {
        assertThat(mercuryOrSquidRouter.routeForTube(MERCURY_TUBE_1), is(SQUID));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_1);
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithExomeExpressOrder() {
        ProductOrder order = placeOrderForTube(tube1, exomeExpress);
        assertThat(mercuryOrSquidRouter.routeForTube(MERCURY_TUBE_1), is(MERCURY));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_1);
        verify(mockProductOrderDao).findByBusinessKey(order.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithNonExomeExpressOrder() {
        ProductOrder order = placeOrderForTube(tube1, testProduct);
        assertThat(mercuryOrSquidRouter.routeForTube(MERCURY_TUBE_1), is(SQUID));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_1);
        verify(mockProductOrderDao).findByBusinessKey(order.getBusinessKey());
    }

    /*
     * Test fixture utilities
     */

    private ProductOrder placeOrderForTube(TwoDBarcodedTube tube, Product product) {
        ProductOrder order = new ProductOrder(101L, "Test Order",
                Collections.singletonList(new ProductOrderSample("SM-1")), "Quote-1", product, testProject);
        order.setJiraTicketKey("PDO-1");
        when(mockProductOrderDao.findByBusinessKey("PDO-1")).thenReturn(order);
        tube.addSample(new MercurySample("PDO-1", "SM-1"));
        return order;
    }
}
