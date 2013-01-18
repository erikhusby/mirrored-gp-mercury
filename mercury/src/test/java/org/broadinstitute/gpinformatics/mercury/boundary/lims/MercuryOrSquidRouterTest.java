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

import java.util.*;

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
 * <p>
 * The current logic is that vessels or batches of vessels containing any references to samples that are part of an
 * Exome Express product orders should be processed by Mercury. Everything else should go to Squid. (This definition
 * should match the documentation for {@link MercuryOrSquidRouter}.)
 * <p>
 * There are a multitude of scenarios tested here. While many of them may seem redundant due to the relative simplicity
 * of the implementation, we know that we are going to have to tweak the implementation over time (at least once, to
 * remove the Exome Express restriction). The test cases will need to be tweaked along with changes to the business
 * rules but should remain complete enough to fully test any changes to the implementation.
 * <p>
 * Tests are grouped by method under test and expected routing result.
 * <p>
 * Mockito is used instead of EasyMock because Mockito has better support for stubbing behavior. Specifically, setUp()
 * can configure mock DAOs to return the various test entities without also setting the expectation that each test will
 * fetch every test entity.
 */
public class MercuryOrSquidRouterTest {

    private static final String MERCURY_TUBE_1 = "mercuryTube1";
    private static final String MERCURY_TUBE_2 = "mercuryTube2";
    private static final String MERCURY_TUBE_3 = "mercuryTube3";

    private MercuryOrSquidRouter mercuryOrSquidRouter;

    private TwoDBarcodedTubeDAO mockTwoDBarcodedTubeDAO;
    private ProductOrderDao mockProductOrderDao;
    private int productOrderSequence = 1;

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

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesNoneInMercury() {
        assertThat(mercuryOrSquidRouter.routeForTubes(Arrays.asList("squidTube1", "squidTube2")), is(SQUID));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode("squidTube1");
        verify(mockTwoDBarcodedTubeDAO).findByBarcode("squidTube2");
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithoutOrders() {
        assertThat(mercuryOrSquidRouter.routeForTubes(Arrays.asList("squidTube", MERCURY_TUBE_1)), is(SQUID));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode("squidTube");
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_1);
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithNonExomeExpressOrders() {
        ProductOrder order = placeOrderForTube(tube1, testProduct);
        assertThat(mercuryOrSquidRouter.routeForTubes(Arrays.asList("squidTube", MERCURY_TUBE_1)), is(SQUID));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode("squidTube");
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_1);
        verify(mockProductOrderDao).findByBusinessKey(order.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithoutExomeExpressOrders() {
        ProductOrder order = placeOrderForTube(tube2, testProduct);
        assertThat(mercuryOrSquidRouter.routeForTubes(Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2)), is(SQUID));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_1);
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_2);
        verify(mockProductOrderDao).findByBusinessKey(order.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithExomeExpressOrders() {
        ProductOrder order = placeOrderForTube(tube1, exomeExpress);
        assertThat(mercuryOrSquidRouter.routeForTubes(Arrays.asList("squidTube", MERCURY_TUBE_1)), is(MERCURY));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode("squidTube");
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_1);
        verify(mockProductOrderDao).findByBusinessKey(order.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithOrdersSomeExomeExpress() {
        ProductOrder order1 = placeOrderForTube(tube1, testProduct);
        ProductOrder order2 = placeOrderForTube(tube2, exomeExpress);
        assertThat(mercuryOrSquidRouter.routeForTubes(Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2)), is(MERCURY));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_1);
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_2);
        verify(mockProductOrderDao).findByBusinessKey(order1.getBusinessKey());
        verify(mockProductOrderDao).findByBusinessKey(order2.getBusinessKey());
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
    public void testRouteForTubeInMercuryWithNonExomeExpressOrder() {
        ProductOrder order = placeOrderForTube(tube1, testProduct);
        assertThat(mercuryOrSquidRouter.routeForTube(MERCURY_TUBE_1), is(SQUID));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_1);
        verify(mockProductOrderDao).findByBusinessKey(order.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithExomeExpressOrder() {
        ProductOrder order = placeOrderForTube(tube1, exomeExpress);
        assertThat(mercuryOrSquidRouter.routeForTube(MERCURY_TUBE_1), is(MERCURY));
        verify(mockTwoDBarcodedTubeDAO).findByBarcode(MERCURY_TUBE_1);
        verify(mockProductOrderDao).findByBusinessKey(order.getBusinessKey());
    }

    /*
     * Test fixture utilities
     */

    private ProductOrder placeOrderForTube(TwoDBarcodedTube tube, Product product) {
        ProductOrder order = new ProductOrder(101L, "Test Order",
                Collections.singletonList(new ProductOrderSample("SM-1")), "Quote-1", product, testProject);
        String jiraTicketKey = "PDO-" + productOrderSequence++;
        order.setJiraTicketKey(jiraTicketKey);
        when(mockProductOrderDao.findByBusinessKey(jiraTicketKey)).thenReturn(order);
        tube.addSample(new MercurySample(jiraTicketKey, "SM-1"));
        return order;
    }
}
