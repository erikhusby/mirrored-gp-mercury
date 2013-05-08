package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.doSectionTransfer;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.makeTubeFormation;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.BOTH;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.SQUID;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Mockito.*;

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
    private static final String CONTROL_TUBE = "controlTube";
    public static final String CONTROL_SAMPLE_ID = "SM-CONTROL1";
    public static final String NA12878 = "NA12878";
    public static final String MERCURY_PLATE = "mercuryPlate";

    private MercuryOrSquidRouter mercuryOrSquidRouter;

    private LabVesselDao mockLabVesselDao;
    private ControlDao mockControlDao;
    private AthenaClientService mockAthenaClientService;
    private BSPSampleDataFetcher mockBspSampleDataFetcher;
    private int productOrderSequence = 1;

    private TwoDBarcodedTube tube1;
    private TwoDBarcodedTube tube2;
    private TwoDBarcodedTube tube3;
    private TwoDBarcodedTube controlTube;
    private StaticPlate plate;
    private ResearchProject testProject;
    private Product testProduct;
    private Product exomeExpress;
    private Bucket picoBucket;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() throws Exception {
        mockLabVesselDao = mock(LabVesselDao.class);
        mockControlDao = mock(ControlDao.class);
        mockAthenaClientService = mock(AthenaClientService.class);
        mockBspSampleDataFetcher = mock(BSPSampleDataFetcher.class);
        mercuryOrSquidRouter = new MercuryOrSquidRouter(mockLabVesselDao, mockControlDao, mockAthenaClientService,
                new WorkflowLoader(), mockBspSampleDataFetcher);

//        when(mockTwoDBarcodedTubeDAO.findByBarcode(anyString())).thenReturn(null); // TODO: Make this explicit and required? Currently this is the default behavior even without this call

        tube1 = new TwoDBarcodedTube(MERCURY_TUBE_1);
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>(){{add(MERCURY_TUBE_1);}})).thenReturn(
                new HashMap<String, LabVessel>(){{put(MERCURY_TUBE_1, tube1);}});
        when(mockBspSampleDataFetcher.fetchSamplesFromBSP(Arrays.asList("SM-1")))
                .thenReturn(Collections.singletonMap("SM-1", makeBspSampleDTO("Sample1")));

        tube2 = new TwoDBarcodedTube(MERCURY_TUBE_2);
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>(){{add(MERCURY_TUBE_2);}})).thenReturn(
                new HashMap<String, LabVessel>(){{put(MERCURY_TUBE_2, tube2);}});

        tube3 = new TwoDBarcodedTube(MERCURY_TUBE_3);
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>(){{add(MERCURY_TUBE_3);}})).thenReturn(
                new HashMap<String, LabVessel>(){{put(MERCURY_TUBE_3, tube3);}});

        controlTube = new TwoDBarcodedTube(CONTROL_TUBE);
        controlTube.addSample(new MercurySample(CONTROL_SAMPLE_ID));
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>(){{add(CONTROL_TUBE);}})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(CONTROL_TUBE, controlTube);
                }});

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>(){{add(MERCURY_TUBE_1); add(MERCURY_TUBE_2);}})).thenReturn(
                new HashMap<String, LabVessel>(){{put(MERCURY_TUBE_1, tube1); put(MERCURY_TUBE_2, tube2);}});

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>(){{add("squidTube"); add(MERCURY_TUBE_1);}})).thenReturn(
                new HashMap<String, LabVessel>(){{put("squidTube", null); put(MERCURY_TUBE_1, tube1);}});

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>(){{add(MERCURY_TUBE_1); add(MERCURY_TUBE_2); add(CONTROL_TUBE);}})).thenReturn(
                new HashMap<String, LabVessel>(){{put(MERCURY_TUBE_1, tube1); put(MERCURY_TUBE_2, tube2); put(CONTROL_TUBE, controlTube);}});

        when(mockControlDao.findAllActive())
                .thenReturn(Arrays.asList(new Control(NA12878, Control.ControlType.POSITIVE)));
        when(mockBspSampleDataFetcher.fetchSamplesFromBSP(Arrays.asList(CONTROL_SAMPLE_ID)))
                .thenReturn(Collections.singletonMap(CONTROL_SAMPLE_ID, makeBspSampleDTO(NA12878)));

        plate = new StaticPlate(MERCURY_PLATE, Eppendorf96);
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>(){{add(MERCURY_PLATE);}})).thenReturn(
                new HashMap<String, LabVessel>(){{put(MERCURY_PLATE, plate);}});

        testProject = new ResearchProject(101L, "Test Project", "Test project", true);

        ProductFamily family = new ProductFamily("Test Product Family");
        testProduct = new Product("Test Product", family, "Test product", "P-TEST-1", new Date(), new Date(),
                0, 0, 0, 0, "Test samples only", "None", true, "Test Workflow", false, "agg type");

        //todo SGM:  Revisit. This probably meant to set the Workflow to ExEx
        exomeExpress = new Product("Exome Express", family, "Exome express", "P-EX-1", new Date(), new Date(),
                0, 0, 0, 0, "Test exome express samples only", "None", true, WorkflowName.EXOME_EXPRESS.getWorkflowName(), false, "agg type");

        picoBucket = new Bucket("Pico/Plating Bucket");
    }

    private BSPSampleDTO makeBspSampleDTO(String collaboratorSampleId) {
        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>();
        dataMap.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, collaboratorSampleId);
        return new BSPSampleDTO(dataMap);
    }

    /*
     * Tests for routeForTubes()
     */

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesNoneInMercury() {
        assertThat(mercuryOrSquidRouter.routeForVesselBarcodes(Arrays.asList("squidTube1", "squidTube2")), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add("squidTube1"); add("squidTube2");}});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithoutOrders() {
        assertThat(mercuryOrSquidRouter.routeForVesselBarcodes(Arrays.asList("squidTube", MERCURY_TUBE_1)), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add("squidTube"); add(MERCURY_TUBE_1);}});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithNonExomeExpressOrders() {
        placeOrderForTube(tube1, testProduct);
        assertThat(mercuryOrSquidRouter.routeForVesselBarcodes(Arrays.asList("squidTube", MERCURY_TUBE_1)), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add("squidTube"); add(MERCURY_TUBE_1);}});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithoutExomeExpressOrders() {
        placeOrderForTube(tube2, testProduct);
        assertThat(mercuryOrSquidRouter.routeForVesselBarcodes(Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2)), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add(MERCURY_TUBE_1); add(MERCURY_TUBE_2);}});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithExomeExpressOrders() {
        ProductOrder order = placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        assertThat(mercuryOrSquidRouter.routeForVesselBarcodes(Arrays.asList("squidTube", MERCURY_TUBE_1)), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add("squidTube"); add(MERCURY_TUBE_1);}});
        verify(mockAthenaClientService).retrieveProductOrderDetails(order.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithOrdersSomeExomeExpress() {
        placeOrderForTube(tube1, testProduct);
        ProductOrder order = placeOrderForTubeAndBucket(tube2, exomeExpress, picoBucket);
        assertThat(mercuryOrSquidRouter.routeForVesselBarcodes(Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2)), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add(MERCURY_TUBE_1); add(MERCURY_TUBE_2);}});
        verify(mockAthenaClientService).retrieveProductOrderDetails(order.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithExomeExpressOrders() {
        ProductOrder order1 = placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        ProductOrder order2 = placeOrderForTubeAndBucket(tube2, exomeExpress, picoBucket);
        assertThat(mercuryOrSquidRouter.routeForVesselBarcodes(Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2)), is(BOTH));
        // only verify for one tube because current implementation short-circuits once one qualifying order is found
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add(MERCURY_TUBE_1); add(MERCURY_TUBE_2);}});
        verify(mockAthenaClientService).retrieveProductOrderDetails(order1.getBusinessKey());
        verify(mockAthenaClientService).retrieveProductOrderDetails(order2.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithExomeExpressOrdersWithControls() {
        ProductOrder order1 = placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        ProductOrder order2 = placeOrderForTubeAndBucket(tube2, exomeExpress, picoBucket);
        assertThat(mercuryOrSquidRouter.routeForVesselBarcodes(
                Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2, CONTROL_TUBE)),
                is(BOTH));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add(MERCURY_TUBE_1); add(MERCURY_TUBE_2); add(CONTROL_TUBE);}});
        verify(mockAthenaClientService).retrieveProductOrderDetails(order1.getBusinessKey());
        verify(mockAthenaClientService).retrieveProductOrderDetails(order2.getBusinessKey());
        verify(mockControlDao).findAllActive();
        verify(mockBspSampleDataFetcher).fetchSamplesFromBSP(Arrays.asList(CONTROL_SAMPLE_ID));
    }

    /*
     * Tests for routeForPlate()
     */

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateNotInMercury() {
        assertThat(mercuryOrSquidRouter.routeForVessel("squidPlate"), equalTo(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add("squidPlate");}});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithoutOrder() {
        assertThat(mercuryOrSquidRouter.routeForVessel(MERCURY_PLATE), equalTo(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add(MERCURY_PLATE);}});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithNonExomeExpressOrder() {
        placeOrderForTube(tube1, testProduct);
        doSectionTransfer(makeTubeFormation(tube1), plate);
        assertThat(mercuryOrSquidRouter.routeForVessel(MERCURY_PLATE), equalTo(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add(MERCURY_PLATE);}});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithOrdersSomeExomeExpress() {
        placeOrderForTube(tube1, testProduct);
        ProductOrder order = placeOrderForTubeAndBucket(tube2, exomeExpress, picoBucket);
        doSectionTransfer(makeTubeFormation(tube1, tube2), plate);
        assertThat(mercuryOrSquidRouter.routeForVessel(MERCURY_PLATE), equalTo(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add(MERCURY_PLATE);}});
        verify(mockAthenaClientService).retrieveProductOrderDetails(order.getBusinessKey());
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithExomeExpressOrders() {
        ProductOrder order1 = placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        ProductOrder order2 = placeOrderForTubeAndBucket(tube2, exomeExpress, picoBucket);
        doSectionTransfer(makeTubeFormation(tube1, tube2), plate);
        assertThat(mercuryOrSquidRouter.routeForVessel(MERCURY_PLATE), equalTo(BOTH));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add(MERCURY_PLATE);}});
        // must look up one order or the other, but not both since they're both Exome Express
        verify(mockAthenaClientService, atMost(2)).retrieveProductOrderDetails(or(eq(order1.getBusinessKey()),
                                                                               eq(order2.getBusinessKey())));
    }

    /*
     * Tests for routeForTube()
     */

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeNotInMercury() {
        assertThat(mercuryOrSquidRouter.routeForVessel("squidTube"), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add("squidTube");}});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithoutOrder() {
        // This would go to squid because the tube, at this point in time, is not associated with a product
        assertThat(mercuryOrSquidRouter.routeForVessel(MERCURY_TUBE_1), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add(MERCURY_TUBE_1);}});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithNonExomeExpressOrder() {
        placeOrderForTube(tube1, testProduct);
        assertThat(mercuryOrSquidRouter.routeForVessel(MERCURY_TUBE_1), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add(MERCURY_TUBE_1);}});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithExomeExpressOrder() {
        ProductOrder order = placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        assertThat(mercuryOrSquidRouter.routeForVessel(MERCURY_TUBE_1), is(BOTH));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>(){{add(MERCURY_TUBE_1);}});
        verify(mockAthenaClientService).retrieveProductOrderDetails(order.getBusinessKey());
    }

    /*
     * Test fixture utilities
     */

    private ProductOrder placeOrderForTube(TwoDBarcodedTube tube, Product product) {
        return placeOrderForTubeAndBucket(tube, product, null);
    }

    private ProductOrder placeOrderForTubeAndBucket(TwoDBarcodedTube tube, Product product, Bucket bucket) {
        ProductOrder order = new ProductOrder(101L, "Test Order",
                Collections.singletonList(new ProductOrderSample("SM-1")), "Quote-1", product, testProject);
        String jiraTicketKey = "PDO-" + productOrderSequence++;
        order.setJiraTicketKey(jiraTicketKey);
        when(mockAthenaClientService.retrieveProductOrderDetails(jiraTicketKey)).thenReturn(order);
        tube.addSample(new MercurySample("SM-1"));
        if (bucket != null) {
            bucket.addEntry(jiraTicketKey, tube);
        }
        return order;
    }
}
