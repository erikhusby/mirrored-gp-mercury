package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.BSPExportsService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.security.ApplicationInstance;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.doSectionTransfer;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.makeTubeFormation;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter.System.MERCURY;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter.System.SQUID;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of logic to route messages and queries to Mercury or Squid as appropriate.
 * <p/>
 * The current logic is that vessels or batches of vessels containing any references to samples that are part of a
 * Mercury-supported workflow should be processed by Mercury. Everything else should go to Squid. (This definition
 * should match the documentation for {@link SystemRouter}.)
 * <p/>
 * There are a multitude of scenarios tested here. While many of them may seem redundant due to the relative simplicity
 * of the implementation, we know that we are going to have to tweak the implementation over time.  The test cases will
 * need to be tweaked along with changes to the business rules but should remain complete enough to fully test any
 * changes to the implementation.
 * <p/>
 * Tests are grouped by method under test and expected routing result.
 * <p/>
 * Mockito is used instead of EasyMock because Mockito has better support for stubbing behavior. Specifically, setUp()
 * can configure mock DAOs to return the various test entities without also setting the expectation that each test will
 * fetch every test entity.
 */
public class SystemRouterTest extends BaseEventTest {

    private static final String MERCURY_TUBE_1 = "mercuryTube1";
    private static final String MERCURY_TUBE_2 = "mercuryTube2";
    private static final String MERCURY_TUBE_3 = "mercuryTube3";
    private static final String CONTROL_TUBE = "controlTube";
    public static final String CONTROL_SAMPLE_ID = "SM-CONTROL1";
    public static final String NA12878 = "NA12878";
    public static final String MERCURY_PLATE = "mercuryPlate";
    public static final String BARCODE_SUFFIX = "mosRte";
    public static final String FLOWCELL_2500_TICKET = "FCT-3mosrte";


    private SystemRouter systemRouter;

    private LabVesselDao mockLabVesselDao;
    private ControlDao mockControlDao;
    private AthenaClientService mockAthenaClientService;
    private BSPSampleDataFetcher mockBspSampleDataFetcher;
    private BSPExportsService mockBspExportService;
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

    @Override
    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() {

        super.setUp();

        // Some of this mocking could be replaced by testing the @DaoFree routeForVessel method, but the mocks
        // existed before that method was factored out.
        mockLabVesselDao = mock(LabVesselDao.class);
        mockControlDao = mock(ControlDao.class);
        mockAthenaClientService = mock(AthenaClientService.class);
        mockBspSampleDataFetcher = mock(BSPSampleDataFetcher.class);
        mockBspExportService = mock(BSPExportsService.class);
        systemRouter = new SystemRouter(mockLabVesselDao, mockControlDao,
                new WorkflowLoader(), mockBspSampleDataFetcher, mockBspExportService);

        // By default, make BSP answer that it knows about all vessels and returns that they have not been exported.
        when(mockBspExportService.findExportDestinations(Matchers.<Collection<LabVessel>>anyObject())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                EnumSet<IsExported.ExternalSystem> noExportDestinations =
                        EnumSet.noneOf(IsExported.ExternalSystem.class);
                Set<IsExported.ExportResult> exportResultSet = new HashSet<>();
                Collection<LabVessel> labVessels = (Collection<LabVessel>) invocation.getArguments()[0];
                for (LabVessel labVessel : labVessels) {
                    IsExported.ExportResult exportResult = new IsExported.ExportResult();
                    exportResult.setBarcode(labVessel.getLabel());
                    exportResult.setExportDestinations(noExportDestinations);
                    exportResultSet.add(exportResult);
                }
                return new IsExported.ExportResults(exportResultSet);
            }
        });

//        when(mockTwoDBarcodedTubeDAO.findByBarcode(anyString())).thenReturn(null); // TODO: Make this explicit and required? Currently this is the default behavior even without this call

        tube1 = new TwoDBarcodedTube(MERCURY_TUBE_1);
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(MERCURY_TUBE_1, tube1);
                }});
        when(mockBspSampleDataFetcher.fetchSamplesFromBSP(Arrays.asList("SM-1")))
                .thenReturn(Collections.singletonMap("SM-1", makeBspSampleDTO("Sample1")));

        tube2 = new TwoDBarcodedTube(MERCURY_TUBE_2);
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_2);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(MERCURY_TUBE_2, tube2);
                }});

        tube3 = new TwoDBarcodedTube(MERCURY_TUBE_3);
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_3);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(MERCURY_TUBE_3, tube3);
                }});

        controlTube = new TwoDBarcodedTube(CONTROL_TUBE);
        controlTube.addSample(new MercurySample(CONTROL_SAMPLE_ID));
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(CONTROL_TUBE);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(CONTROL_TUBE, controlTube);
                }});

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
            add(MERCURY_TUBE_2);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(MERCURY_TUBE_1, tube1);
                    put(MERCURY_TUBE_2, tube2);
                }});

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add("squidTube");
            add(MERCURY_TUBE_1);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put("squidTube", null);
                    put(MERCURY_TUBE_1, tube1);
                }});

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
            add(MERCURY_TUBE_2);
            add(CONTROL_TUBE);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(MERCURY_TUBE_1, tube1);
                    put(MERCURY_TUBE_2, tube2);
                    put(CONTROL_TUBE, controlTube);
                }});

        when(mockControlDao.findAllActive())
                .thenReturn(Arrays.asList(new Control(NA12878, Control.ControlType.POSITIVE)));
        when(mockBspSampleDataFetcher.fetchSamplesFromBSP(Arrays.asList(CONTROL_SAMPLE_ID)))
                .thenReturn(Collections.singletonMap(CONTROL_SAMPLE_ID, makeBspSampleDTO(NA12878)));

        plate = new StaticPlate(MERCURY_PLATE, Eppendorf96);
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_PLATE);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(MERCURY_PLATE, plate);
                }});

        testProject = new ResearchProject(101L, "Test Project", "Test project", true);

        ProductFamily family = new ProductFamily("Test Product Family");
        testProduct = new Product("Test Product", family, "Test product", "P-TEST-1", new Date(), new Date(),
                0, 0, 0, 0, "Test samples only", "None", true, Workflow.WHOLE_GENOME, false, "agg type");

        exomeExpress = new Product("Exome Express", family, "Exome express", "P-EX-1", new Date(), new Date(),
                0, 0, 0, 0, "Test exome express samples only", "None", true,
                Workflow.AGILENT_EXOME_EXPRESS, false, "agg type");

        picoBucket = new Bucket("Pico/Plating Bucket");
    }

    private static BSPSampleDTO makeBspSampleDTO(String collaboratorSampleId) {
        Map<BSPSampleSearchColumn, String> dataMap = new EnumMap<>(BSPSampleSearchColumn.class);
        dataMap.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, collaboratorSampleId);
        return new BSPSampleDTO(dataMap);
    }

    /*
     * Tests for routeForTubes()
     */

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForTubesNoneInMercury(ApplicationInstance instance) {
        boolean oldDeployment = Deployment.isCRSP;
        Deployment.isCRSP = (instance == ApplicationInstance.CRSP);
        final List<String> testBarcodes = Arrays.asList("squidTube1", "squidTube2");
        if (Deployment.isCRSP) {
            try {
                systemRouter.routeForVesselBarcodes(testBarcodes);
                Assert.fail("CRSP Deployment should not route to Squid");
            } catch (Exception e) {

            }
        } else {
            assertThat(systemRouter.routeForVesselBarcodes(testBarcodes), is(SQUID));
        }
        verify(mockLabVesselDao).findByBarcodes(testBarcodes);
        Deployment.isCRSP = oldDeployment;
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForTubesSomeInMercuryWithoutOrders(ApplicationInstance instance) {
        final List<String> testTubes = Arrays.asList("squidTube", MERCURY_TUBE_1);
        assertThat(systemRouter.routeForVesselBarcodes(testTubes), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(testTubes);
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForTubesSomeInMercuryWithNonExomeExpressOrders(ApplicationInstance instance) {
        boolean oldDeployment = Deployment.isCRSP;
        Deployment.isCRSP = (instance == ApplicationInstance.CRSP);
        placeOrderForTube(tube1, testProduct);
        final List<String> testTubes = Arrays.asList("squidTube", MERCURY_TUBE_1);
        if (Deployment.isCRSP) {
            try {
                systemRouter.routeForVesselBarcodes(testTubes);
                Assert.fail("CRSP Deployment should never route to Squid");
            } catch (Exception e) {

            }
        } else {
            assertThat(systemRouter.routeForVesselBarcodes(testTubes), is(SQUID));
        }
        verify(mockLabVesselDao).findByBarcodes(testTubes);
        Deployment.isCRSP = oldDeployment;
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForTubesAllInMercuryWithoutExomeExpressOrders(ApplicationInstance instance) {
        boolean oldDeployment = Deployment.isCRSP;
        Deployment.isCRSP = (instance == ApplicationInstance.CRSP);
        placeOrderForTube(tube2, testProduct);
        final List<String> testBarcodes = Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2);
        if (Deployment.isCRSP) {
            try {
                systemRouter.routeForVesselBarcodes(testBarcodes);
                Assert.fail("CRSP Deployment should never route to Squid");
            } catch (Exception e) {

            }
        } else {
            assertThat(systemRouter.routeForVesselBarcodes(testBarcodes),
                    is(SQUID));
        }
        verify(mockLabVesselDao).findByBarcodes(testBarcodes);
        Deployment.isCRSP = oldDeployment;
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForTubesSomeInMercuryWithExomeExpressOrders(ApplicationInstance instance) {
        placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        final List<String> testBarcodes = Arrays.asList("squidTube", MERCURY_TUBE_1);
        try {
            systemRouter.routeForVesselBarcodes(testBarcodes);
            Assert.fail("Expected exception: The Routing cannot be determined for options: [MERCURY, SQUID]");
        } catch (Exception expected) {}
        verify(mockLabVesselDao).findByBarcodes(testBarcodes);
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForTubesAllInMercuryWithOrdersSomeExomeExpress(ApplicationInstance instance) {
        placeOrderForTube(tube1, testProduct);
        placeOrderForTubeAndBucket(tube2, exomeExpress, picoBucket);
        final List<String> testBarcodes = Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2);
        try {
            systemRouter.routeForVesselBarcodes(testBarcodes);
            Assert.fail("Expected exception: The Routing cannot be determined for options: [MERCURY, SQUID]");
        } catch (Exception expected) {}
        verify(mockLabVesselDao).findByBarcodes(testBarcodes );
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForTubesAllInMercuryWithExomeExpressOrders(ApplicationInstance instance) {
        ProductOrder order1 = placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        ProductOrder order2 = placeOrderForTubeAndBucket(tube2, exomeExpress, picoBucket);
        final List<String> testBarcodes = Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2);
        assertThat(systemRouter.routeForVesselBarcodes(testBarcodes),
                is(MERCURY));
        verify(mockLabVesselDao).findByBarcodes(testBarcodes);
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForTubesAllInMercuryWithExomeExpressOrdersWithControls(ApplicationInstance instance) {
        placeOrderForTubesAndBatch(new HashSet<LabVessel>(Arrays.asList(tube1, tube2)), exomeExpress, picoBucket);
        final List<String> testBarcodes = Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2, CONTROL_TUBE);
        assertThat(systemRouter.routeForVesselBarcodes(
                testBarcodes),
                is(MERCURY));
        verify(mockLabVesselDao).findByBarcodes(testBarcodes);
        verify(mockControlDao).findAllActive();
        verify(mockBspSampleDataFetcher).fetchSamplesFromBSP(Arrays.asList(CONTROL_SAMPLE_ID));
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForTubesAllInMercuryWithExomeExpressOrdersWithControlsAfterTransfer(ApplicationInstance instance) {
        final TwoDBarcodedTube target1 = new TwoDBarcodedTube("target1");
        final TwoDBarcodedTube target2 = new TwoDBarcodedTube("target2");
        final TwoDBarcodedTube target3 = new TwoDBarcodedTube("target3");
        final List<String> testBarcodes = Arrays.asList("target1", "target2", "target3");
        when(mockLabVesselDao.findByBarcodes(testBarcodes))
                .thenReturn(new HashMap<String, LabVessel>() {{
                    put("target1", target1);
                    put("target2", target2);
                    put("target3", target3);
                }});

        placeOrderForTubesAndBatch(new HashSet<LabVessel>(Arrays.asList(tube1, tube2)), exomeExpress, picoBucket);
        doSectionTransfer(makeTubeFormation(tube1, tube2, controlTube), plate);
        doSectionTransfer(plate, makeTubeFormation(target1, target2, target3));

        assertThat(systemRouter.routeForVesselBarcodes(testBarcodes),
                is(MERCURY));

        verify(mockLabVesselDao).findByBarcodes(testBarcodes);
        verify(mockControlDao).findAllActive();
        verify(mockBspSampleDataFetcher).fetchSamplesFromBSP(Arrays.asList(CONTROL_SAMPLE_ID));
    }

    /*
     * Tests for routeForPlate()
     */

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForPlateNotInMercury(ApplicationInstance instance) {
        boolean oldDeployment = Deployment.isCRSP;
        Deployment.isCRSP = (instance == ApplicationInstance.CRSP);
        final String testBarcode = "squidPlate";
        if(Deployment.isCRSP) {
            try {
                systemRouter.routeForVessel(testBarcode);

                Assert.fail("CRSP Deployment should never route to Squid");
            } catch (Exception e) {

            }
        } else {
            assertThat(systemRouter.routeForVessel(testBarcode), equalTo(SQUID));
        }
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(testBarcode);
        }});
        Deployment.isCRSP = oldDeployment;
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForPlateInMercuryWithoutOrder(ApplicationInstance instance) {
        assertThat(systemRouter.routeForVessel(MERCURY_PLATE), equalTo(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_PLATE);
        }});
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForPlateInMercuryWithNonExomeExpressOrder(ApplicationInstance instance) {
        boolean oldDeployment = Deployment.isCRSP;
        Deployment.isCRSP = (instance == ApplicationInstance.CRSP);
        placeOrderForTube(tube1, testProduct);
        doSectionTransfer(makeTubeFormation(tube1), plate);
        if (Deployment.isCRSP) {
            try {
                systemRouter.routeForVessel(MERCURY_PLATE);
                Assert.fail("CRSP Deployment should never route to Squid");
            } catch (Exception e) {

            }
        } else {
            assertThat(systemRouter.routeForVessel(MERCURY_PLATE), equalTo(SQUID));
        }
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_PLATE);
        }});
        Deployment.isCRSP = oldDeployment;
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForPlateInMercuryWithOrdersSomeExomeExpress(ApplicationInstance instance) {
        placeOrderForTube(tube1, testProduct);
        placeOrderForTubeAndBucket(tube2, exomeExpress, picoBucket);
        doSectionTransfer(makeTubeFormation(tube1, tube2), plate);
        try {
            systemRouter.routeForVessel(MERCURY_PLATE);
            Assert.fail("Expected exception: The Routing cannot be determined for options: [MERCURY, SQUID]");
        } catch (Exception expected) {}
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_PLATE);
        }});
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForPlateInMercuryWithExomeExpressOrders(ApplicationInstance instance) {
        ProductOrder order1 = placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        ProductOrder order2 = placeOrderForTubeAndBucket(tube2, exomeExpress, picoBucket);
        doSectionTransfer(makeTubeFormation(tube1, tube2), plate);
        assertThat(systemRouter.routeForVessel(MERCURY_PLATE), equalTo(MERCURY));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_PLATE);
        }});
        // must look up one order or the other, but not both since they're both Exome Express
        verify(mockAthenaClientService, atMost(2)).retrieveProductOrderDetails(or(eq(order1.getBusinessKey()),
                eq(order2.getBusinessKey())));
    }

    /*
     * Tests for routeForTube()
     */

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForTubeNotInMercury(ApplicationInstance instance) {
        boolean oldDeployment = Deployment.isCRSP;
        Deployment.isCRSP = (instance == ApplicationInstance.CRSP);
        final String testBarcode = "squidTube";
        if (Deployment.isCRSP) {
            try {
                systemRouter.routeForVessel(testBarcode);
                Assert.fail("CRSP Deployment should never route to Squid");
            } catch (Exception e) {

            }
        } else {
            assertThat(systemRouter.routeForVessel(testBarcode), is(SQUID));
        }
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(testBarcode);
        }});
        Deployment.isCRSP = oldDeployment;
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForTubeInMercuryWithoutOrder(ApplicationInstance instance) {
        // This would go to squid because the tube, at this point in time, is not associated with a product
        assertThat(systemRouter.routeForVessel(MERCURY_TUBE_1), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }});
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForTubeInMercuryWithNonExomeExpressOrder(ApplicationInstance instance) {
        boolean oldDeployment = Deployment.isCRSP;
        Deployment.isCRSP = (instance == ApplicationInstance.CRSP);
        placeOrderForTube(tube1, testProduct);
        if (Deployment.isCRSP) {
            try {
                systemRouter.routeForVessel(MERCURY_TUBE_1);
                Assert.fail("CRSP Deployment should never route to Squid");
            } catch (Exception e) {

            }
        } else {
            assertThat(systemRouter.routeForVessel(MERCURY_TUBE_1), is(SQUID));
        }
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }});
        Deployment.isCRSP = oldDeployment;
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testRouteForTubeInMercuryWithExomeExpressOrder(ApplicationInstance instance) {
        ProductOrder order = placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        assertThat(systemRouter.routeForVessel(MERCURY_TUBE_1), is(MERCURY));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }});
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testGetSystemOfRecordForTubeInSamplesLab(ApplicationInstance instance) {
        IsExported.ExportResult exportResult = new IsExported.ExportResult();
        exportResult.setBarcode(MERCURY_TUBE_1);
        Set<IsExported.ExportResult> exportResultSet = new HashSet<>();
        exportResultSet.add(exportResult);
        IsExported.ExportResults exportResults = new IsExported.ExportResults(exportResultSet);
        when(mockBspExportService.findExportDestinations(Arrays.<LabVessel>asList(tube1))).thenReturn(exportResults);

        // This tube is not in a PDO and does not have any BSP messaging, so it should go to Squid.
        assertThat(systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1), equalTo(SQUID));

        // Add an event that is hard-wired for Mercury as the system of record and routing should now go to Mercury.
        tube1.addInPlaceEvent(new LabEvent(LabEventType.SAMPLE_RECEIPT, new Date(), "SystemRouterTest", 0L, 0L,
                "testRouteForTubeInSamplesLab"));
        exportResult.setExportDestinations(EnumSet.noneOf(IsExported.ExternalSystem.class));
        assertThat(systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1), equalTo(MERCURY));

        // After export from BSP to Squid, Squid should once again be the system of record.
        exportResult.setExportDestinations(EnumSet.of(IsExported.ExternalSystem.Sequencing));
        assertThat(systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1), equalTo(SQUID));

        // After export from BSP to Mercury, Mercury should be the system of record.
        exportResult.setExportDestinations(EnumSet.of(IsExported.ExternalSystem.Mercury));
        assertThat(systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1), equalTo(MERCURY));
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testGetSystemOfRecordForTubeExportedToGAP(ApplicationInstance instance) {
        IsExported.ExportResult exportResult = new IsExported.ExportResult();
        exportResult.setBarcode(MERCURY_TUBE_1);
        Set<IsExported.ExportResult> exportResultSet = new HashSet<>();
        exportResultSet.add(exportResult);
        IsExported.ExportResults exportResults = new IsExported.ExportResults(exportResultSet);
        when(mockBspExportService.findExportDestinations(Arrays.<LabVessel>asList(tube1))).thenReturn(exportResults);

        tube1.addInPlaceEvent(new LabEvent(LabEventType.SAMPLE_RECEIPT, new Date(), "SystemRouterTest", 0L, 0L,
                "testRouteForTubeInSamplesLab"));

        // After export from BSP to GAP, Mercury should not be able to route the tube.
        exportResult.setExportDestinations(EnumSet.of(IsExported.ExternalSystem.GAP));
        try {
            systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1);
            Assert.fail("Expected exception for unexpected export destination: GAP");
        } catch (InformaticsServiceException expected) {}
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testGetSystemOfRecordForTubeWithErrorFromBSPFindExportService(ApplicationInstance instance) {
        IsExported.ExportResult exportResult = new IsExported.ExportResult();
        exportResult.setBarcode(MERCURY_TUBE_1);
        Set<IsExported.ExportResult> exportResultSet = new HashSet<>();
        exportResultSet.add(exportResult);
        IsExported.ExportResults exportResults = new IsExported.ExportResults(exportResultSet);
        when(mockBspExportService.findExportDestinations(Arrays.<LabVessel>asList(tube1))).thenReturn(exportResults);

        tube1.addInPlaceEvent(new LabEvent(LabEventType.SAMPLE_RECEIPT, new Date(), "SystemRouterTest", 0L, 0L,
                "testRouteForTubeInSamplesLab"));

        // If there's an error determining the export destination, ???
        exportResult.setError("Test error");
/*
        try {
            systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1);
            Assert.fail("Expected exception for unexpected export destination: GAP");
        } catch (InformaticsServiceException expected) {}
*/
        assertThat(systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1), equalTo(MERCURY));
    }

    /*
     * Tests for routing and system of record for a validation LCSET
     */

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext", enabled = true)
    public void testGetSystemOfRecordForControlOnly(ApplicationInstance instance) {
        boolean oldDeployment = Deployment.isCRSP;
        Deployment.isCRSP = (instance == ApplicationInstance.CRSP);
        // Override controlDao behavior from setUp so that the sample in MERCURY_TUBE_1 is a control sample.
        when(mockControlDao.findAllActive())
                .thenReturn(Arrays.asList(new Control("Sample1", Control.ControlType.POSITIVE)));
        tube1.addSample(new MercurySample("SM-1"));

        if (Deployment.isCRSP) {
            try {
                systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1);
                Assert.fail("CRSP Deployment should never route to Squid");
            } catch (Exception e) {

            }
        } else {
            assertThat(systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1), is(SQUID));
        }
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }});
        Deployment.isCRSP = oldDeployment;
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext", enabled = true)
    public void testGetSystemOfRecordForVesselInValidationLCSET(ApplicationInstance instance) {
        ProductOrder order = placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        tube1.getAllLabBatches().iterator().next().setValidationBatch(true);
        assertThat(systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1), is(MERCURY));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }});
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext", enabled = true)
    public void testMercuryOnlyRouting(ApplicationInstance instance) {
        boolean oldDeployment = Deployment.isCRSP;
        Deployment.isCRSP = (instance == ApplicationInstance.CRSP);
        expectedRouting = SystemRouter.System.MERCURY;

        final ProductOrder
                productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        AthenaClientServiceStub.addProductOrder(productOrder);
        Date runDate = new Date();
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);

        Calendar postMercuryOnlyLaunchCalendarDate = new GregorianCalendar(2013, 6, 26);

        Date today = new Date();

        if (today.before(postMercuryOnlyLaunchCalendarDate.getTime())) {
            workflowBatch.setCreatedOn(postMercuryOnlyLaunchCalendarDate.getTime());
        }

        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

        /*
         * Bucketing (which is required to find batch and Product key) happens in PicoPlatingEntityBuilder so
         * Routing before bucketing will return Squid
         */
        if (Deployment.isCRSP) {
            try {
                systemRouter.routeForVessels(new HashSet<LabVessel>(mapBarcodeToTube.values()));
                Assert.fail("CRSP Deployment should never route to Squid");
            } catch (Exception e) {

            }
        } else {
            assertThat(systemRouter.routeForVessels(new HashSet<LabVessel>(mapBarcodeToTube.values())),
                    is(SystemRouter.System.SQUID));
        }

        //Build Event History
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, BARCODE_SUFFIX);
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                String.valueOf(runDate.getTime()), BARCODE_SUFFIX, true);

        /*
         * Bucketing (which is required to find batch and Product key) happens in PicoPlatingEntityBuilder so
         * we are doing the routing on those initial tubes after the plating process has run.
         */
        assertThat(systemRouter.routeForVessels(new HashSet<LabVessel>(mapBarcodeToTube.values())),
                is(SystemRouter.System.MERCURY));

        assertThat(systemRouter
                .routeForVessels(new HashSet<LabVessel>(picoPlatingEntityBuilder.getNormBarcodeToTubeMap().values())),
                is(SystemRouter.System.MERCURY));


        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), BARCODE_SUFFIX);

        assertThat(systemRouter
                .routeForVessels(
                        Collections.<LabVessel>singleton(exomeExpressShearingEntityBuilder.getShearingCleanupPlate())),
                is(SystemRouter.System.MERCURY));

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                        exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                        exomeExpressShearingEntityBuilder.getShearingPlate(), BARCODE_SUFFIX);

        TubeFormation pondRegRack = libraryConstructionEntityBuilder.getPondRegRack();


        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (pondRegRack)),
                is(SystemRouter.System.MERCURY));

        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                        libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                        libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), BARCODE_SUFFIX);
        TubeFormation normRack = hybridSelectionEntityBuilder.getNormCatchRack();
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (normRack)),
                is(SystemRouter.System.MERCURY));

        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), Workflow.AGILENT_EXOME_EXPRESS, "1");

        TwoDBarcodedTube denatureTube =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (denatureTube)),
                is(SystemRouter.System.MERCURY));
        String denatureTubeBarcode = denatureTube.getLabel();
        MiSeqReagentKit reagentKit = new MiSeqReagentKit("reagent_kit_barcode");
        LabEvent denatureToReagentKitEvent = new LabEvent(DENATURE_TO_REAGENT_KIT_TRANSFER, new Date(),
                "ZLAB", 1L, 1L, "systemRouterTest");
        final VesselToSectionTransfer sectionTransfer =
                new VesselToSectionTransfer(denatureTube,
                        SBSSection.getBySectionName(MiSeqReagentKit.LOADING_WELL.name()),
                        reagentKit.getContainerRole(), denatureToReagentKitEvent);
        denatureToReagentKitEvent.getVesselToSectionTransfers().add(sectionTransfer);
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (reagentKit)),
                is(SystemRouter.System.MERCURY));


        Set<LabVessel> starterVessels = Collections.singleton((LabVessel) denatureTube);
        //create a couple Miseq batches then one FCT (2500) batch
        LabBatch fctBatch = new LabBatch(FLOWCELL_2500_TICKET, starterVessels, LabBatch.LabBatchType.FCT, BigDecimal.valueOf(
                12.33f));

        HiSeq2500FlowcellEntityBuilder flowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), BARCODE_SUFFIX + "ADXX", FLOWCELL_2500_TICKET,
                        ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null, Workflow.AGILENT_EXOME_EXPRESS);
        TwoDBarcodedTube dilutionTube =
                flowcellEntityBuilder.getDilutionRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (dilutionTube)),
                is(SystemRouter.System.MERCURY));

        String dilutionTubeBarcode = dilutionTube.getLabel();

        IlluminaFlowcell flowcell = flowcellEntityBuilder.getIlluminaFlowcell();
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (flowcell)),
                is(SystemRouter.System.MERCURY));

        String flowcellBarcode = flowcell.getLabel();

        Deployment.isCRSP = oldDeployment;
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext", enabled = true)
    public void testMercuryAndSquidRouting(ApplicationInstance instance) {
        boolean oldDeployment = Deployment.isCRSP;
        Deployment.isCRSP = (instance == ApplicationInstance.CRSP);
        expectedRouting = SystemRouter.System.BOTH;

        final ProductOrder
                productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        AthenaClientServiceStub.addProductOrder(productOrder);
        Date runDate = new Date();
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);

        Calendar july25CalendarDate = new GregorianCalendar(2013, 6, 25);
        Calendar preJuly25CalendarDate = new GregorianCalendar(2013, 6, 24);

        Date today = new Date();

        if (today.after(july25CalendarDate.getTime()) ||
                today.equals(july25CalendarDate.getTime())) {
            workflowBatch.setCreatedOn(preJuly25CalendarDate.getTime());
        }

        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

        /*
         * Bucketing (which is required to find batch and Product key) happens in PicoPlatingEntityBuilder so
         * Routing before bucketing will return Squid
         */
        if (Deployment.isCRSP) {
            try {
                systemRouter.routeForVessels(new HashSet<LabVessel>(mapBarcodeToTube.values()));
                Assert.fail("CRSP Deployment should never route to Squid");
            } catch (Exception e) {

            }
        } else {
            assertThat(systemRouter.routeForVessels(new HashSet<LabVessel>(mapBarcodeToTube.values())),
                    is(SystemRouter.System.SQUID));
        }

        //Build Event History
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, BARCODE_SUFFIX);
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                String.valueOf(runDate.getTime()), BARCODE_SUFFIX, true);

        /*
         * Bucketing (which is required to find batch and Product key) happens in PicoPlatingEntityBuilder so
         * we are doing the routing on those initial tubes after the plating process has run.
         */
        assertThat(systemRouter.routeForVessels(new HashSet<LabVessel>(mapBarcodeToTube.values())),
                is(SystemRouter.System.BOTH));

        assertThat(systemRouter
                .routeForVessels(new HashSet<LabVessel>(picoPlatingEntityBuilder.getNormBarcodeToTubeMap().values())),
                is(SystemRouter.System.BOTH));


        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), BARCODE_SUFFIX);

        assertThat(systemRouter
                .routeForVessels(
                        Collections.<LabVessel>singleton(exomeExpressShearingEntityBuilder.getShearingCleanupPlate())),
                is(SystemRouter.System.BOTH));

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                        exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                        exomeExpressShearingEntityBuilder.getShearingPlate(), BARCODE_SUFFIX);

        TubeFormation pondRegRack = libraryConstructionEntityBuilder.getPondRegRack();


        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (pondRegRack)),
                is(SystemRouter.System.BOTH));

        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                        libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                        libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), BARCODE_SUFFIX);
        TubeFormation normRack = hybridSelectionEntityBuilder.getNormCatchRack();
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (normRack)),
                is(SystemRouter.System.BOTH));

        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), Workflow.AGILENT_EXOME_EXPRESS, "1");

        TwoDBarcodedTube denatureTube =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (denatureTube)),
                is(SystemRouter.System.BOTH));
        String denatureTubeBarcode = denatureTube.getLabel();
        MiSeqReagentKit reagentKit = new MiSeqReagentKit("reagent_kit_barcode");
        LabEvent denatureToReagentKitEvent = new LabEvent(DENATURE_TO_REAGENT_KIT_TRANSFER, new Date(),
                "ZLAB", 1L, 1L, "systemRouterTest");
        final VesselToSectionTransfer sectionTransfer =
                new VesselToSectionTransfer(denatureTube,
                        SBSSection.getBySectionName(MiSeqReagentKit.LOADING_WELL.name()),
                        reagentKit.getContainerRole(), denatureToReagentKitEvent);
        denatureToReagentKitEvent.getVesselToSectionTransfers().add(sectionTransfer);
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (reagentKit)),
                is(SystemRouter.System.BOTH));


        Set<LabVessel> starterVessels = Collections.singleton((LabVessel) denatureTube);
        //create a couple Miseq batches then one FCT (2500) batch
        LabBatch fctBatch = new LabBatch(FLOWCELL_2500_TICKET, starterVessels, LabBatch.LabBatchType.FCT, BigDecimal
                .valueOf(12.33f));

        HiSeq2500FlowcellEntityBuilder flowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), BARCODE_SUFFIX + "ADXX", FLOWCELL_2500_TICKET,
                        ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null, Workflow.AGILENT_EXOME_EXPRESS);
        TwoDBarcodedTube dilutionTube =
                flowcellEntityBuilder.getDilutionRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (dilutionTube)),
                is(SystemRouter.System.BOTH));

        String dilutionTubeBarcode = dilutionTube.getLabel();

        IlluminaFlowcell flowcell = flowcellEntityBuilder.getIlluminaFlowcell();
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (flowcell)),
                is(SystemRouter.System.BOTH));

        String flowcellBarcode = flowcell.getLabel();

        Deployment.isCRSP = oldDeployment;
    }

    /*
     * Test fixture utilities
     */

    private ProductOrder placeOrderForTube(LabVessel tube, Product product) {
        return placeOrderForTubeAndBucket(tube, product, null);
    }

    private ProductOrder placeOrderForTubeAndBucket(LabVessel tube, Product product, Bucket bucket) {
        return placeOrderForTubesAndBatch(Collections.singleton(tube), product, bucket);
    }

    private ProductOrder placeOrderForTubesAndBatch(Set<LabVessel> tubes, Product product, Bucket bucket) {
        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        int sampleNum = 1;
        for (LabVessel tube : tubes) {
            String sampleName = "SM-" + sampleNum;
            productOrderSamples.add(new ProductOrderSample(sampleName));
            tube.addSample(new MercurySample(sampleName));
            sampleNum++;
        }
        ProductOrder order = new ProductOrder(101L, "Test Order", productOrderSamples, "Quote-1", product, testProject);
        productOrderSequence++;
        String jiraTicketKey = "PDO-" + productOrderSequence;
        order.setJiraTicketKey(jiraTicketKey);
        when(mockAthenaClientService.retrieveProductOrderDetails(jiraTicketKey)).thenReturn(order);
        Collection<BucketEntry> bucketEntries = new ArrayList<>();
        if (bucket != null) {
            for (LabVessel tube : tubes) {
                bucketEntries.add(bucket.addEntry(jiraTicketKey, tube, BucketEntry.BucketEntryType.PDO_ENTRY));
            }
            LabBatch labBatch = new LabBatch("LCSET-" + productOrderSequence, tubes,
                    LabBatch.LabBatchType.WORKFLOW);
            labBatch.setWorkflow(product.getWorkflow());
            for (BucketEntry bucketEntry : bucketEntries) {
                bucketEntry.setLabBatch(labBatch);
            }
        }
        return order;
    }

    @DataProvider(name = "deploymentContext")
    public Object[][] sourceDeploymentContext() {
        return new Object[][] {
               new Object[] {ApplicationInstance.CRSP},
               new Object[] {ApplicationInstance.RESEARCH},

        };
    }

    @Test(groups = DATABASE_FREE, dataProvider = "deploymentContext")
    public void testSystemOfRecordForSamplesLabTube(ApplicationInstance instance) {
        assertThat(systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1), is(SQUID));
        LabEvent event = new LabEvent(LabEventType.SAMPLE_RECEIPT, new Date(), "SystemRouterTest", 0L, 0L,
                "testSystemOfRecordForSamplesLabTube");
        tube1.addInPlaceEvent(event);
        assertThat(systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1), is(MERCURY));
    }
}
