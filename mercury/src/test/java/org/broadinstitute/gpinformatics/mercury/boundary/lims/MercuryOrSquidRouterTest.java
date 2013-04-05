package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.doSectionTransfer;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.makeTubeFormation;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.MERCURY;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.SQUID;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Mockito.*;

//import static org.easymock.EasyMock.*;

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
    public static final String SQUID_TUBE_1 = "squidTube1";
    public static final String SQUID_TUBE_2 = "squidTube2";

    private MercuryOrSquidRouter mercuryOrSquidRouter;

    private AthenaClientService mockAthenaClientService;
    private int productOrderSequence = 1;

    private TwoDBarcodedTube tube1;
    private TwoDBarcodedTube tube2;

    private TwoDBarcodedTube squidTube1;
    private TwoDBarcodedTube squidTube2;

    private StaticPlate plate;
    private StaticPlate squidPlate;

    private ResearchProject testProject;
    private Product testProduct;
    private Product exomeExpress;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() throws Exception {
        mockAthenaClientService = mock(AthenaClientService.class);
        mercuryOrSquidRouter = new MercuryOrSquidRouter( mockAthenaClientService);

//        when(mockTwoDBarcodedTubeDAO.findByBarcode(anyString())).thenReturn(null); // TODO: Make this explicit and required? Currently this is the default behavior even without this call
        squidTube1 = new TwoDBarcodedTube(SQUID_TUBE_1);
        squidTube2 = new TwoDBarcodedTube(SQUID_TUBE_2);

        tube1 = new TwoDBarcodedTube(MERCURY_TUBE_1);
        tube2 = new TwoDBarcodedTube(MERCURY_TUBE_2);

        squidPlate = new StaticPlate("squidPlate", Eppendorf96);
        plate = new StaticPlate("mercuryPlate", Eppendorf96);


        testProject = new ResearchProject(101L, "Test Project", "Test project", true);

        ProductFamily family = new ProductFamily("Test Product Family");
        testProduct = new Product("Test Product", family, "Test product", "P-TEST-1", new Date(), new Date(),
                0, 0, 0, 0, "Test samples only", "None", true, "Test Workflow", false);

        //todo SGM:  Revisit. This probably meant to set the Workflow to ExEx
        exomeExpress = new Product("Exome Express", family, "Exome express", "P-EX-1", new Date(), new Date(),
                0, 0, 0, 0, "Test exome express samples only", "None", true, WorkflowName.EXOME_EXPRESS.getWorkflowName(), false);
    }

    /*
     * Tests for routeForTubes()
     */

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesNoneInMercury() {
        assertThat(mercuryOrSquidRouter.routeForVessels(Arrays.<LabVessel>asList(squidTube1, squidTube2)), is(SQUID));
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithoutOrders() {

        assertThat(mercuryOrSquidRouter.routeForVessels(Arrays.<LabVessel>asList(squidTube1, tube1)), is(SQUID));
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithNonExomeExpressOrders() {
        ProductOrder order = placeOrderForTube(tube1, testProduct);
        assertThat(mercuryOrSquidRouter.routeForVessels(Arrays.<LabVessel>asList(squidTube1, tube1)), is(SQUID));
        verify(mockAthenaClientService).retrieveProductOrderDetails(order.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithoutExomeExpressOrders() {
        ProductOrder order = placeOrderForTube(tube2, testProduct);
        assertThat(mercuryOrSquidRouter.routeForVessels(Arrays.<LabVessel>asList(tube1, tube2)), is(SQUID));
        verify(mockAthenaClientService).retrieveProductOrderDetails(order.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithExomeExpressOrders() {
        ProductOrder order;
        order = placeOrderForTube(tube1, exomeExpress);
        assertThat(mercuryOrSquidRouter.routeForVessels(Arrays.<LabVessel>asList(squidTube1, tube1)), is(MERCURY));
        verify(mockAthenaClientService).retrieveProductOrderDetails(order.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithOrdersSomeExomeExpress() {
        ProductOrder order1 = placeOrderForTube(tube1, testProduct);
        ProductOrder order2 = placeOrderForTube(tube2, exomeExpress);
        assertThat(mercuryOrSquidRouter.routeForVessels(Arrays.<LabVessel>asList(tube1, tube2)), is(MERCURY));
        verify(mockAthenaClientService).retrieveProductOrderDetails(order1.getBusinessKey());
        verify(mockAthenaClientService).retrieveProductOrderDetails(order2.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithExomeExpressOrders() {
        ProductOrder order1 = placeOrderForTube(tube1, exomeExpress);
        placeOrderForTube(tube2, exomeExpress);
        assertThat(mercuryOrSquidRouter.routeForVessels(Arrays.<LabVessel>asList(tube1, tube2)), is(MERCURY));
        verify(mockAthenaClientService).retrieveProductOrderDetails(order1.getBusinessKey());
    }

    /*
     * Tests for routeForPlate()
     */

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateNotInMercury() {
        assertThat(mercuryOrSquidRouter.routeForVessel(squidPlate), equalTo(SQUID));
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithoutOrder() {
        assertThat(mercuryOrSquidRouter.routeForVessel(plate), equalTo(SQUID));
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithNonExomeExpressOrder() {
        ProductOrder order = placeOrderForTube(tube1, testProduct);
        doSectionTransfer(makeTubeFormation(tube1), plate);
        assertThat(mercuryOrSquidRouter.routeForVessel(plate), equalTo(SQUID));
        verify(mockAthenaClientService).retrieveProductOrderDetails(order.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithOrdersSomeExomeExpress() {
        ProductOrder order1 = placeOrderForTube(tube1, testProduct);
        ProductOrder order2 = placeOrderForTube(tube2, exomeExpress);
        doSectionTransfer(makeTubeFormation(tube1, tube2), plate);
        assertThat(mercuryOrSquidRouter.routeForVessel(plate), equalTo(MERCURY));
        // only verify for the Exome Express order because the other may be skipped if this one is inspected first
        verify(mockAthenaClientService).retrieveProductOrderDetails(order2.getBusinessKey());
        // looking up the non-Exome Express order is allowed
        verify(mockAthenaClientService, atMost(1)).retrieveProductOrderDetails(order1.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithExomeExpressOrders() {
        ProductOrder order1 = placeOrderForTube((TwoDBarcodedTube) tube1, exomeExpress);
        ProductOrder order2 = placeOrderForTube((TwoDBarcodedTube) tube2, exomeExpress);
        doSectionTransfer(makeTubeFormation(tube1, tube2), plate);
        assertThat(mercuryOrSquidRouter.routeForVessel(plate), equalTo(MERCURY));
        // must look up one order or the other, but not both since they're both Exome Express
        verify(mockAthenaClientService).retrieveProductOrderDetails(or(eq(order1.getBusinessKey()),
                eq(order2.getBusinessKey())));
    }

    /*
     * Tests for routeForTube()
     */

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeNotInMercury() {
        assertThat(mercuryOrSquidRouter.routeForVessel(squidTube1), is(SQUID));
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithoutOrder() {
        assertThat(mercuryOrSquidRouter.routeForVessel(tube1), is(SQUID));
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithNonExomeExpressOrder() {
        ProductOrder order = placeOrderForTube(tube1, testProduct);
        assertThat(mercuryOrSquidRouter.routeForVessel(tube1), is(SQUID));
        verify(mockAthenaClientService).retrieveProductOrderDetails(order.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithExomeExpressOrder() {
        ProductOrder order = placeOrderForTube(tube1, exomeExpress);
        assertThat(mercuryOrSquidRouter.routeForVessel(tube1), is(MERCURY));
        verify(mockAthenaClientService).retrieveProductOrderDetails(order.getBusinessKey());
    }

    /*
     * Test fixture utilities
     */

    private ProductOrder placeOrderForTube(TwoDBarcodedTube tube, Product product) {
        ProductOrder order = new ProductOrder(101L, "Test Order",
                Collections.singletonList(new ProductOrderSample("SM-1")), "Quote-1", product, testProject);
        String jiraTicketKey = "PDO-" + productOrderSequence++;
        order.setJiraTicketKey(jiraTicketKey);
        when(mockAthenaClientService.retrieveProductOrderDetails(jiraTicketKey)).thenReturn(order);
        tube.addSample(new MercurySample(jiraTicketKey, "SM-1"));
        return order;
    }
}
