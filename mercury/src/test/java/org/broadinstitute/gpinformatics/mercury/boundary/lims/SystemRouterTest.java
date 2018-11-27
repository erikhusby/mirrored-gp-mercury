package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PipelineDataType;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.BSPExportsService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.addInPlaceEvent;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.doSectionTransfer;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.makeTubeFormation;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter.System.MERCURY;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter.System.SQUID;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.A_BASE;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.PREFLIGHT_CLEANUP;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.SAMPLE_RECEIPT;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
@Test(groups = TestGroups.DATABASE_FREE)
public class SystemRouterTest extends BaseEventTest {

    private static final String MERCURY_TUBE_1 = "mercuryTube1";
    private static final String MERCURY_TUBE_2 = "mercuryTube2";
    private static final String MERCURY_TUBE_3 = "mercuryTube3";
    private static final String CONTROL_TUBE = "controlTube";
    // todo jmt not sure what "control without workflow" means; all controls are without bucket entries
    private static final String CONTROL_WITHOUT_WORKFLOW1 = "controlWoWorkflow1";
    private static final String CONTROL_WITHOUT_WORKFLOW2 = "controlWoWorkflow2";
    public static final String CONTROL_SAMPLE_ID = "SM-CONTROL1";
    public static final String NA12878 = "NA12878";
    public static final String MERCURY_PLATE = "mercuryPlate";
    public static final String BARCODE_SUFFIX = "mosRte";
    public static final String FLOWCELL_2500_TICKET = "FCT-3mosrte";


    private SystemRouter systemRouter;

    private LabVesselDao mockLabVesselDao;
    private BSPExportsService mockBspExportService;
    private int productOrderSequence = 1;

    private BarcodedTube tube1;
    private BarcodedTube tube2;
    private BarcodedTube tube3;
    private BarcodedTube controlTube;
    private BarcodedTube controlWithoutWorkflow1;
    private BarcodedTube controlWithoutWorkflow2;
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
        mockBspExportService = mock(BSPExportsService.class);
        systemRouter = new SystemRouter(mockLabVesselDao, new WorkflowLoader().load(), mockBspExportService);

        // By default, make BSP answer that it knows about all vessels and returns that they have not been exported.
        when(mockBspExportService.findExportDestinations(anyCollectionOf(LabVessel.class))).thenAnswer(
                new Answer<IsExported.ExportResults>() {
                    @Override
                    public IsExported.ExportResults answer(InvocationOnMock invocation) throws Throwable {
                        List<IsExported.ExportResult> exportResults = new ArrayList<>();
                        @SuppressWarnings("unchecked")
                        Collection<LabVessel> labVessels = (Collection<LabVessel>) invocation.getArguments()[0];
                        for (LabVessel labVessel : labVessels) {
                            exportResults.add(new IsExported.ExportResult(labVessel.getLabel(), null));
                        }
                        return new IsExported.ExportResults(exportResults);
                    }
                });

//        when(mockBarcodedTubeDAO.findByBarcode(anyString())).thenReturn(null); // TODO: Make this explicit and required? Currently this is the default behavior even without this call

        tube1 = new BarcodedTube(MERCURY_TUBE_1);
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(MERCURY_TUBE_1, tube1);
                }});

        tube2 = new BarcodedTube(MERCURY_TUBE_2);
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_2);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(MERCURY_TUBE_2, tube2);
                }});

        tube3 = new BarcodedTube(MERCURY_TUBE_3);
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_3);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(MERCURY_TUBE_3, tube3);
                }});

        controlTube = new BarcodedTube(CONTROL_TUBE);
        controlTube.addSample(new MercurySample(CONTROL_SAMPLE_ID, MercurySample.MetadataSource.BSP));
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

        controlWithoutWorkflow1 = new BarcodedTube(CONTROL_WITHOUT_WORKFLOW1);
        controlWithoutWorkflow1.addSample(new MercurySample(CONTROL_SAMPLE_ID, MercurySample.MetadataSource.BSP));
        controlWithoutWorkflow2 = new BarcodedTube(CONTROL_WITHOUT_WORKFLOW2);
        controlWithoutWorkflow2.addSample(new MercurySample(CONTROL_SAMPLE_ID, MercurySample.MetadataSource.BSP));

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(CONTROL_WITHOUT_WORKFLOW1);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(CONTROL_WITHOUT_WORKFLOW1, controlWithoutWorkflow1);
                }});

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(CONTROL_WITHOUT_WORKFLOW1);
            add(MERCURY_TUBE_1);
            add(MERCURY_TUBE_2);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(CONTROL_WITHOUT_WORKFLOW1, controlWithoutWorkflow1);
                    put(MERCURY_TUBE_1, tube1);
                    put(MERCURY_TUBE_2, tube2);
                }});

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(CONTROL_WITHOUT_WORKFLOW1);
            add(CONTROL_WITHOUT_WORKFLOW2);
            add(MERCURY_TUBE_1);
            add(MERCURY_TUBE_2);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(CONTROL_WITHOUT_WORKFLOW1, controlWithoutWorkflow1);
                    put(CONTROL_WITHOUT_WORKFLOW2, controlWithoutWorkflow2);
                    put(MERCURY_TUBE_1, tube1);
                    put(MERCURY_TUBE_2, tube2);
                }});

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add("squidTube");
            add(CONTROL_WITHOUT_WORKFLOW1);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put("squidTube", null);
                    put(CONTROL_WITHOUT_WORKFLOW1, controlWithoutWorkflow1);
                }});

        plate = new StaticPlate(MERCURY_PLATE, Eppendorf96);
        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_PLATE);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(MERCURY_PLATE, plate);
                }});

        testProject = new ResearchProject(101L, "Test Project", "Test project", true,
                                          ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);

        ProductFamily family = new ProductFamily("Test Product Family");
        testProduct = new Product("Test Product", family, "Test product", "P-TEST-1", new Date(), new Date(),
                                  0, 0, 0, 0, "Test samples only", "None", true, Workflow.WHOLE_GENOME, false,
            new PipelineDataType(Aggregation.DATA_TYPE_EXOME, true));

        exomeExpress = new Product("Exome Express", family, "Exome express", "P-EX-1", new Date(), new Date(),
                                   0, 0, 0, 0, "Test exome express samples only", "None", true,
                                   Workflow.AGILENT_EXOME_EXPRESS, false,
            new PipelineDataType(Aggregation.DATA_TYPE_EXOME, true));

        picoBucket = new Bucket("Pico/Plating Bucket");
    }

    private static SampleData makeBspSampleData(String collaboratorSampleId) {
        Map<BSPSampleSearchColumn, String> dataMap = new EnumMap<>(BSPSampleSearchColumn.class);
        dataMap.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, collaboratorSampleId);
        dataMap.put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, collaboratorSampleId);
        return new BspSampleData(dataMap);
    }

    /*
     * Tests for routeForTubes()
     */

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesNoneInMercury() {
        final List<String> testBarcodes = Arrays.asList("squidTube1", "squidTube2");
            assertThat(systemRouter.routeForVesselBarcodes(testBarcodes), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(testBarcodes);
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithoutOrders() {
        final List<String> testTubes = Arrays.asList("squidTube", MERCURY_TUBE_1);
        assertThat(systemRouter.routeForVesselBarcodes(testTubes), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(testTubes);
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithNonExomeExpressOrders() {
        placeOrderForTube(tube1, testProduct);
        final List<String> testTubes = Arrays.asList("squidTube", MERCURY_TUBE_1);
            assertThat(systemRouter.routeForVesselBarcodes(testTubes), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(testTubes);
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithoutExomeExpressOrders() {
        placeOrderForTube(tube2, testProduct);
        final List<String> testBarcodes = Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2);
            assertThat(systemRouter.routeForVesselBarcodes(testBarcodes),
                       is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(testBarcodes);
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesSomeInMercuryWithExomeExpressOrders() {
        placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        final List<String> testBarcodes = Arrays.asList("squidTube", MERCURY_TUBE_1);
        try {
            systemRouter.routeForVesselBarcodes(testBarcodes);
            Assert.fail("Expected exception: The Routing cannot be determined for options: [MERCURY, SQUID]");
        } catch (Exception expected) {
        }
        verify(mockLabVesselDao).findByBarcodes(testBarcodes);
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithOrdersSomeExomeExpress() {
        placeOrderForTube(tube1, testProduct);
        placeOrderForTubeAndBucket(tube2, exomeExpress, picoBucket);
        final List<String> testBarcodes = Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2);
        try {
            systemRouter.routeForVesselBarcodes(testBarcodes);
            Assert.fail("Expected exception: The Routing cannot be determined for options: [MERCURY, SQUID]");
        } catch (Exception expected) {
        }
        verify(mockLabVesselDao).findByBarcodes(testBarcodes);
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithExomeExpressOrders() {
        placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        placeOrderForTubeAndBucket(tube2, exomeExpress, picoBucket);
        final List<String> testBarcodes = Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2);

        assertThat(systemRouter.routeForVesselBarcodes(testBarcodes), is(MERCURY));

        verify(mockLabVesselDao).findByBarcodes(testBarcodes);
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithExomeExpressOrdersWithControls() {
        placeOrderForTubesAndBatch(new HashSet<LabVessel>(Arrays.asList(tube1, tube2)), exomeExpress, picoBucket);
        tube1.getLabBatches().iterator().next().addLabVessel(controlTube);
        final List<String> testBarcodes = Arrays.asList(MERCURY_TUBE_1, MERCURY_TUBE_2, CONTROL_TUBE);

        assertThat(systemRouter.routeForVesselBarcodes(testBarcodes), is(MERCURY));

        verify(mockLabVesselDao).findByBarcodes(testBarcodes);
    }

    @Test(groups = DATABASE_FREE)
    public void testControlWithoutWorkflowAndMercuryTubes() {
        // Make a mixed LCSET rack so that control's implied batch is null.
        placeOrderForTubesAndBatch(new HashSet<LabVessel>(Arrays.asList(tube1)), exomeExpress, picoBucket);
        placeOrderForTubesAndBatch(new HashSet<LabVessel>(Arrays.asList(tube2)), exomeExpress, picoBucket);
        tube1.getLabBatches().iterator().next().addLabVessel(controlWithoutWorkflow1);
        final List<String> testBarcodes = Arrays.asList(CONTROL_WITHOUT_WORKFLOW1, MERCURY_TUBE_1, MERCURY_TUBE_2);

        // Need to have tubes in a container for this test.
        TubeFormation tubeFormation = new TubeFormation(new HashMap<VesselPosition, BarcodedTube>(){{
            put(VesselPosition.A01, controlWithoutWorkflow1);
            put(VesselPosition.A02, tube1);
            put(VesselPosition.A03, tube2);
        }}, RackOfTubes.RackType.Matrix96);
        controlWithoutWorkflow1.addToContainer(tubeFormation.getContainerRole());
        tube1.addToContainer(tubeFormation.getContainerRole());
        tube2.addToContainer(tubeFormation.getContainerRole());

        assertThat(controlWithoutWorkflow1.getSampleInstancesV2().iterator().next().getSingleBucketEntry(),
                is(nullValue()));

        assertThat(systemRouter.routeForVesselBarcodes(testBarcodes), is(MERCURY));
    }

    @Test(groups = DATABASE_FREE)
    public void testControlWithoutWorkflowAndSquidTube() {
        final List<String> testBarcodes = Arrays.asList("squidTube", CONTROL_WITHOUT_WORKFLOW1);

        // Need to have tubes in a container for this test.
        TubeFormation tubeFormation = new TubeFormation(new HashMap<VesselPosition, BarcodedTube>(){{
            put(VesselPosition.A01, tube1);
            put(VesselPosition.A02, controlWithoutWorkflow1);
        }}, RackOfTubes.RackType.Matrix96);
        tube1.addToContainer(tubeFormation.getContainerRole());
        controlWithoutWorkflow1.addToContainer(tubeFormation.getContainerRole());

        assertThat(controlWithoutWorkflow1.getSampleInstancesV2().iterator().next().getSingleBatch(),
                is(nullValue()));

        assertThat(systemRouter.routeForVesselBarcodes(testBarcodes), is(SQUID));
    }

    @Test(groups = DATABASE_FREE)
    public void testControlWithoutWorkflowAlone() {
        final List<String> testBarcodes = Arrays.asList(CONTROL_WITHOUT_WORKFLOW1);

        // Need to have tubes in a container for this test.
        TubeFormation tubeFormation = new TubeFormation(new HashMap<VesselPosition, BarcodedTube>(){{
            put(VesselPosition.A01, controlWithoutWorkflow1);
        }}, RackOfTubes.RackType.Matrix96);
        controlWithoutWorkflow1.addToContainer(tubeFormation.getContainerRole());

        assertThat(controlWithoutWorkflow1.getSampleInstancesV2().iterator().next().getSingleBatch(),
                is(nullValue()));

        assertThat(systemRouter.routeForVesselBarcodes(testBarcodes), is(SQUID));
    }

    // This test uses two controls without workflow in a rack in order to check the code
    // that tries to determine routing for accompanying vessels.
    @Test(groups = DATABASE_FREE)
    public void testTwoControlsWithoutWorkflowAndMercuryTubes() {
        // Make a mixed LCSET rack so that control's implied batch is null.
        placeOrderForTubesAndBatch(new HashSet<LabVessel>(Arrays.asList(tube1)), exomeExpress, picoBucket);
        placeOrderForTubesAndBatch(new HashSet<LabVessel>(Arrays.asList(tube2)), exomeExpress, picoBucket);
        LabBatch labBatch = tube1.getLabBatches().iterator().next();
        labBatch.addLabVessel(controlWithoutWorkflow1);
        labBatch.addLabVessel(controlWithoutWorkflow2);
        final List<String> testBarcodes = Arrays.asList(CONTROL_WITHOUT_WORKFLOW1, CONTROL_WITHOUT_WORKFLOW2,
                MERCURY_TUBE_1, MERCURY_TUBE_2);

        // Need to have tubes in a container for this test.
        TubeFormation tubeFormation = new TubeFormation(new HashMap<VesselPosition, BarcodedTube>(){{
            put(VesselPosition.A01, controlWithoutWorkflow1);
            put(VesselPosition.A02, controlWithoutWorkflow2);
            put(VesselPosition.A03, tube1);
            put(VesselPosition.A04, tube2);
        }}, RackOfTubes.RackType.Matrix96);

        controlWithoutWorkflow1.addToContainer(tubeFormation.getContainerRole());
        controlWithoutWorkflow2.addToContainer(tubeFormation.getContainerRole());
        tube1.addToContainer(tubeFormation.getContainerRole());
        tube2.addToContainer(tubeFormation.getContainerRole());

        assertThat(controlWithoutWorkflow1.getSampleInstancesV2().iterator().next().getSingleBucketEntry(),
                is(nullValue()));
        assertThat(controlWithoutWorkflow2.getSampleInstancesV2().iterator().next().getSingleBucketEntry(),
                is(nullValue()));

        assertThat(systemRouter.routeForVesselBarcodes(testBarcodes), is(MERCURY));
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubesAllInMercuryWithExomeExpressOrdersWithControlsAfterTransfer(
            ) {
        final BarcodedTube target1 = new BarcodedTube("target1");
        final BarcodedTube target2 = new BarcodedTube("target2");
        final BarcodedTube target3 = new BarcodedTube("target3");
        final List<String> testBarcodes = Arrays.asList("target1", "target2", "target3");
        when(mockLabVesselDao.findByBarcodes(testBarcodes))
                .thenReturn(new HashMap<String, LabVessel>() {{
                    put("target1", target1);
                    put("target2", target2);
                    put("target3", target3);
                }});

        placeOrderForTubesAndBatch(new HashSet<LabVessel>(Arrays.asList(tube1, tube2)), exomeExpress, picoBucket);
        tube1.getLabBatches().iterator().next().addLabVessel(controlTube);
        doSectionTransfer(makeTubeFormation(tube1, tube2, controlTube), plate);
        doSectionTransfer(plate, makeTubeFormation(target1, target2, target3));

        assertThat(systemRouter.routeForVesselBarcodes(testBarcodes), is(MERCURY));

        verify(mockLabVesselDao).findByBarcodes(testBarcodes);
    }

    /*
     * Tests for routeForPlate()
     */

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateNotInMercury() {
        final String testBarcode = "squidPlate";
            assertThat(systemRouter.routeForVessel(testBarcode), equalTo(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(testBarcode);
        }});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithoutOrder() {
        assertThat(systemRouter.routeForVessel(MERCURY_PLATE), equalTo(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_PLATE);
        }});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithNonExomeExpressOrder() {
        placeOrderForTube(tube1, testProduct);
        doSectionTransfer(makeTubeFormation(tube1), plate);
            assertThat(systemRouter.routeForVessel(MERCURY_PLATE), equalTo(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_PLATE);
        }});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithOrdersSomeExomeExpress() {
        placeOrderForTube(tube1, testProduct);
        placeOrderForTubeAndBucket(tube2, exomeExpress, picoBucket);
        doSectionTransfer(makeTubeFormation(tube1, tube2), plate);
        try {
            systemRouter.routeForVessel(MERCURY_PLATE);
            Assert.fail("Expected exception: The Routing cannot be determined for options: [MERCURY, SQUID]");
        } catch (Exception expected) {
        }
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_PLATE);
        }});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForPlateInMercuryWithExomeExpressOrders() {
        ProductOrder order1 = placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        ProductOrder order2 = placeOrderForTubeAndBucket(tube2, exomeExpress, picoBucket);
        doSectionTransfer(makeTubeFormation(tube1, tube2), plate);
        assertThat(systemRouter.routeForVessel(MERCURY_PLATE), equalTo(MERCURY));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_PLATE);
        }});
    }

    /*
     * Tests for routeForTube()
     */

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeNotInMercury() {
        final String testBarcode = "squidTube";
            assertThat(systemRouter.routeForVessel(testBarcode), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(testBarcode);
        }});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithoutOrder() {
        // This would go to squid because the tube, at this point in time, is not associated with a product
        assertThat(systemRouter.routeForVessel(MERCURY_TUBE_1), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithNonExomeExpressOrder() {
        placeOrderForTube(tube1, testProduct);
            assertThat(systemRouter.routeForVessel(MERCURY_TUBE_1), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }});
    }

    @Test(groups = DATABASE_FREE)
    public void testRouteForTubeInMercuryWithExomeExpressOrder() {
        placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        assertThat(systemRouter.routeForVessel(MERCURY_TUBE_1), is(MERCURY));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }});
    }

    /*
     * Tests for system-of-record for samples lab vessels.
     *
     * Unlike for routing, system-of-record doesn't have the advantage of a LabEventType to help with making the
     * decision. This tends to call Squid the system-of-record for lab vessels that are actually in the samples lab.
     * Since Squid doesn't handle any sample lab informatics, these should all be handled by Mercury.
     *
     * The solution is to expand the scope of Mercury's system-of-record to include vessels that are the target of a
     * LabEventType configured with {@link SystemOfRecord.MERCURY} AND has not been exported from BSP to "Sequencing".
     * These test methods test system-of-record questions with those conditions.
     */

    /**
     * Determines how to configure ExportResult's notFound and error properties returned by mockBspExportService.
     */
    private enum KnownToBSP {
        YES, NO, ERROR
    }

    /**
     * Determines how to configure ExportResult's exportDestinations property returned by mockBspExportService.
     */
    private enum ExportFromBSP {
        NONE, GAP, MERCURY, SEQUENCING, PARALLEL_VALIDATION
    }

    /**
     * Determines whether or not to use a tube barcode that mockLabVesselDao will return a LabVessel for.
     */
    private enum KnownToMercury {
        YES, NO
    }

    /**
     * Determines whether or not to put the tube in a PDO and what type of PDO to use. When testing system-of-record,
     * NON_EXEX can be used to test both Squid-only and parallel work because Squid is the system of record in both
     * cases.
     */
    private enum InPDO {
        NONE, EXEX, NON_EXEX
    }

    /**
     * Determines whether or not to put any events on the tube and what the event type's system of record should be. The
     * specific event type is unspecified because only the type's system-of-record that matters here.
     */
    private enum MercuryEvent {
        NONE, MERCURY, SQUID, WORKFLOW_DEPENDENT
    }

    /**
     * Scenarios for
     * {@link #testSystemOfRecord(String, KnownToMercury, InPDO, MercuryEvent, KnownToBSP, ExportFromBSP, SystemRouter.System)}.
     */
    @DataProvider(name = "systemOfRecordScenarios")
    public Object[][] getSystemOfRecordScenarios() {
        // @formatter:off
        return new Object[][]{
                new Object[] { "squidIntermediateLCTube",    KnownToMercury.NO,  InPDO.NONE,     MercuryEvent.NONE,               KnownToBSP.NO,    ExportFromBSP.NONE,                SQUID },
                new Object[] { "bspIntermediateTube",        KnownToMercury.NO,  InPDO.NONE,     MercuryEvent.NONE,               KnownToBSP.YES,   ExportFromBSP.NONE,                SQUID },
                new Object[] { "inSamplesLab",               KnownToMercury.YES, InPDO.NONE,     MercuryEvent.MERCURY,            KnownToBSP.YES,   ExportFromBSP.NONE,                MERCURY },
                new Object[] { "exportedToMercury",          KnownToMercury.YES, InPDO.NONE,     MercuryEvent.MERCURY,            KnownToBSP.YES,   ExportFromBSP.MERCURY,             MERCURY },
                new Object[] { "exportedParallelValidation", KnownToMercury.YES, InPDO.NONE,     MercuryEvent.MERCURY,            KnownToBSP.YES,   ExportFromBSP.PARALLEL_VALIDATION, SQUID },
                new Object[] { "exportedToSequencing",       KnownToMercury.YES, InPDO.NONE,     MercuryEvent.MERCURY,            KnownToBSP.YES,   ExportFromBSP.SEQUENCING,          SQUID },
                new Object[] { "exportedToGap",              KnownToMercury.YES, InPDO.NONE,     MercuryEvent.MERCURY,            KnownToBSP.YES,   ExportFromBSP.GAP,                 null },
                new Object[] { "errorFromBSP",               KnownToMercury.YES, InPDO.NONE,     MercuryEvent.MERCURY,            KnownToBSP.ERROR, ExportFromBSP.NONE,                null },
                new Object[] { "parallelTubeInSamplesLab",   KnownToMercury.YES, InPDO.NON_EXEX, MercuryEvent.MERCURY,            KnownToBSP.YES,   ExportFromBSP.NONE,                MERCURY },
                new Object[] { "parallelTubeExported",       KnownToMercury.YES, InPDO.NON_EXEX, MercuryEvent.MERCURY,            KnownToBSP.YES,   ExportFromBSP.SEQUENCING,          SQUID },
                new Object[] { "parallelTubeInSeqLab",       KnownToMercury.YES, InPDO.NON_EXEX, MercuryEvent.WORKFLOW_DEPENDENT, KnownToBSP.NO,    ExportFromBSP.NONE,                SQUID },
                new Object[] { "exExTubeInSamplesLab",       KnownToMercury.YES, InPDO.EXEX,     MercuryEvent.WORKFLOW_DEPENDENT, KnownToBSP.YES,   ExportFromBSP.NONE,                MERCURY },
                new Object[] { "exExTubeExported",           KnownToMercury.YES, InPDO.EXEX,     MercuryEvent.WORKFLOW_DEPENDENT, KnownToBSP.YES,   ExportFromBSP.MERCURY,             MERCURY },
                new Object[] { "exExTubeInSeqLab",           KnownToMercury.YES, InPDO.EXEX,     MercuryEvent.WORKFLOW_DEPENDENT, KnownToBSP.NO,    ExportFromBSP.NONE,                MERCURY },
        };
        // @formatter:on
    }

    /**
     * Test
     * {@link org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter#getSystemOfRecordForVessel(String)}
     * using various scenarios specified by a data provider. See enums for details about the various conditions. A
     * expectedSystemOfRecord value of null is a signal that an exception is expected to be thrown.
     *
     * @param tubeBarcode            the tube barcode to query; also a short description of the scenario
     * @param knownToMercury         whether or not the tube is known to Mercury
     * @param inPDO                  whether or not the tube is related to a PDO and what type of PDO
     * @param mercuryEvent           the system-of-record classification of the event targeting the tube
     * @param knownToBSP             whether or not the tube is known to BSP
     * @param exportFromBSP          whether or not the tube has been exported from BSP and where it was exported to
     * @param expectedSystemOfRecord the expected system-of-record for the scenario; null if an exception is expected
     */
    @Test(groups = DATABASE_FREE, dataProvider = "systemOfRecordScenarios")
    public void testSystemOfRecord(String tubeBarcode, KnownToMercury knownToMercury, InPDO inPDO,
                                   MercuryEvent mercuryEvent, KnownToBSP knownToBSP, ExportFromBSP exportFromBSP,
                                   SystemRouter.System expectedSystemOfRecord) {

        LabVessel tube = configureMercury(tubeBarcode, knownToMercury, inPDO, mercuryEvent);
        // If Mercury doesn't know the tube, it should never ask BSP anything about it.
        if (tube != null) {
            configureBSP(tube, knownToBSP, exportFromBSP);
        }

        try {
            SystemRouter.System systemOfRecord = systemRouter.getSystemOfRecordForVessel(tubeBarcode);
            if (expectedSystemOfRecord == null) {
                Assert.fail("Expected an exception");
            }
            assertThat(systemOfRecord, equalTo(expectedSystemOfRecord));
        } catch (InformaticsServiceException e) {
            if (expectedSystemOfRecord != null) {
                throw e;
            }
        }

        // If Mercury doesn't know about the tube, it should not attempt to find out the export status from BSP.
        if (knownToMercury == KnownToMercury.NO) {
            verify(mockBspExportService, never()).findExportDestinations(anyCollectionOf(LabVessel.class));
        }
    }

    /**
     * Create an object graph appropriate for the given scenario conditions.
     *
     * @param tubeBarcode    the barcode of the tube to (possibly) create in Mercury
     * @param knownToMercury whether or not the tube is known to Mercury
     * @param inPDO          whether or not the tube is related to a PDO and what type of PDO
     * @param mercuryEvent   the system-of-record classification of the event targeting the tube
     *
     * @return a new tube if Mercury should know about the tube; null otherwise
     */
    private LabVessel configureMercury(final String tubeBarcode, KnownToMercury knownToMercury, InPDO inPDO,
                                       MercuryEvent mercuryEvent) {
        switch (knownToMercury) {
        case NO:
            assertThat(inPDO, equalTo(InPDO.NONE));
            assertThat(mercuryEvent, equalTo(MercuryEvent.NONE));
            return null;
        case YES:
            final BarcodedTube tube = new BarcodedTube(tubeBarcode);
            when(mockLabVesselDao.findByBarcodes(Arrays.asList(tubeBarcode)))
                    .thenReturn(new HashMap<String, LabVessel>() {{
                        put(tubeBarcode, tube);
                    }});

            switch (inPDO) {
            case EXEX:
                placeOrderForTubeAndBucket(tube, exomeExpress, picoBucket);
                break;
            case NON_EXEX:
                placeOrderForTube(tube, testProduct);
                break;
            case NONE:
                // do nothing
                break;
            }

            switch (mercuryEvent) {
            case MERCURY:
                assertThat(LabEventType.SAMPLE_RECEIPT.getSystemOfRecord(), equalTo(
                        LabEventType.SystemOfRecord.MERCURY));
                addInPlaceEvent(SAMPLE_RECEIPT, tube);
                break;
            case SQUID:
                assertThat(LabEventType.PREFLIGHT_CLEANUP.getSystemOfRecord(), equalTo(
                        LabEventType.SystemOfRecord.SQUID));
                addInPlaceEvent(PREFLIGHT_CLEANUP, tube);
                break;
            case WORKFLOW_DEPENDENT:
                assertThat(LabEventType.A_BASE.getSystemOfRecord(), equalTo(
                        LabEventType.SystemOfRecord.WORKFLOW_DEPENDENT));
                addInPlaceEvent(A_BASE, tube);
                break;
            case NONE:
                // do nothing
                break;
            }
            return tube;
        default:
            throw new RuntimeException("Unrecognized KnownToMercury value: " + knownToMercury);
        }
    }

    /**
     * Configure BSP mock/stub behavior appropriate for the given scenario conditions.
     *
     * @param tube          the tube that will be queried
     * @param knownToBSP    whether or not the tube is known to BSP
     * @param exportFromBSP whether or not the tube has been exported from BSP and where it was exported to
     */
    private void configureBSP(@Nonnull LabVessel tube, KnownToBSP knownToBSP, ExportFromBSP exportFromBSP) {
        IsExported.ExportResults exportResults;
        switch (knownToBSP) {
        case ERROR:
            exportResults = makeExportResultsError(tube.getLabel(), "BSP error");
            break;
        case NO:
            exportResults = makeExportResultsNotFound(tube.getLabel(), "Unknown tube");
            break;
        case YES:
            Set<IsExported.ExternalSystem> externalSystems = new HashSet<>();
            switch (exportFromBSP) {
            case GAP:
                externalSystems.add(IsExported.ExternalSystem.GAP);
                break;
            case MERCURY:
                externalSystems.add(IsExported.ExternalSystem.Mercury);
                break;
            case SEQUENCING:
                externalSystems.add(IsExported.ExternalSystem.Sequencing);
                break;
            case PARALLEL_VALIDATION:
                externalSystems.add(IsExported.ExternalSystem.Mercury);
                externalSystems.add(IsExported.ExternalSystem.Sequencing);
                break;
            case NONE:
                // Add nothing.
                break;
            default:
                throw new InformaticsServiceException("Should not get here!");
            }
            exportResults = makeExportResults(tube.getLabel(), externalSystems);
            break;
        default:
            throw new InformaticsServiceException("Should not get here!");
        }
        when(mockFindExportDestinations(tube)).thenReturn(exportResults);
    }

    /*
     * Tests for routing and system of record for a validation LCSET
     */

    @Test(groups = DATABASE_FREE, enabled = true)
    public void testGetSystemOfRecordForControlOnly() {
        tube1.addSample(new MercurySample("SM-1", MercurySample.MetadataSource.BSP));

            assertThat(systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }});
    }

    @Test(groups = DATABASE_FREE, enabled = true)
    public void testGetSystemOfRecordForVesselInValidationLCSET() {
        placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        tube1.getAllLabBatches().iterator().next().setValidationBatch(true);
        assertThat(systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1), is(MERCURY));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }});
    }

    @Test(groups = DATABASE_FREE, enabled = true)
    public void testMercuryOnlyRouting() {
        expectedRouting = SystemRouter.System.MERCURY;

        final ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        Date runDate = new Date();
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        /*
         * Bucketing (which is required to find batch and Product key) happens in PicoPlatingEntityBuilder so
         * Routing before bucketing will return Squid
         */
            assertThat(systemRouter.routeForVessels(new HashSet<LabVessel>(mapBarcodeToTube.values())),
                       is(SystemRouter.System.SQUID));

        //Build Event History
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);

        Calendar postMercuryOnlyLaunchCalendarDate = new GregorianCalendar(2013, 6, 26);

        Date today = new Date();

        if (today.before(postMercuryOnlyLaunchCalendarDate.getTime())) {
            workflowBatch.setCreatedOn(postMercuryOnlyLaunchCalendarDate.getTime());
        }

        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, BARCODE_SUFFIX);
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                                                                                  String.valueOf(runDate.getTime()),
                                                                                  BARCODE_SUFFIX, true);

        /*
         * Bucketing (which is required to find batch and Product key) happens in PicoPlatingEntityBuilder so
         * we are doing the routing on those initial tubes after the plating process has run.
         */
        assertThat(systemRouter.routeForVessels(new HashSet<LabVessel>(mapBarcodeToTube.values())),
                   is(SystemRouter.System.MERCURY));

        assertThat(systemRouter
                           .routeForVessels(new HashSet<LabVessel>(
                                   picoPlatingEntityBuilder.getNormBarcodeToTubeMap().values())),
                   is(SystemRouter.System.MERCURY));


        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                                               picoPlatingEntityBuilder.getNormTubeFormation(),
                                               picoPlatingEntityBuilder.getNormalizationBarcode(), BARCODE_SUFFIX);

        assertThat(systemRouter
                           .routeForVessels(
                                   Collections.<LabVessel>singleton(
                                           exomeExpressShearingEntityBuilder.getShearingCleanupPlate())),
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
                                                          hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                                                          "1");

        BarcodedTube denatureTube =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (denatureTube)),
                   is(SystemRouter.System.MERCURY));
        String denatureTubeBarcode = denatureTube.getLabel();
        MiSeqReagentKit reagentKit = new MiSeqReagentKit("reagent_kit_barcode");
        LabEvent denatureToReagentKitEvent = new LabEvent(DENATURE_TO_REAGENT_KIT_TRANSFER, new Date(),
                                                          "ZLAB", 1L, 1L, "systemRouterTest");
        final VesselToSectionTransfer sectionTransfer = new VesselToSectionTransfer(denatureTube,
                SBSSection.getBySectionName(MiSeqReagentKit.LOADING_WELL.name()),
                reagentKit.getContainerRole(), null, denatureToReagentKitEvent);
        denatureToReagentKitEvent.getVesselToSectionTransfers().add(sectionTransfer);
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (reagentKit)),
                   is(SystemRouter.System.MERCURY));


        LabBatch fctBatch = new LabBatch(FLOWCELL_2500_TICKET, LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, denatureTube, BigDecimal.valueOf(12.33f));

        HiSeq2500FlowcellEntityBuilder flowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), BARCODE_SUFFIX + "ADXX",
                                            FLOWCELL_2500_TICKET,
                                            ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null,
                                            Workflow.AGILENT_EXOME_EXPRESS);
        BarcodedTube dilutionTube =
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

    }

    @Test(groups = DATABASE_FREE, enabled = true)
    public void testMercuryAndSquidRouting() {
        expectedRouting = SystemRouter.System.BOTH;

        final ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        Date runDate = new Date();
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        /*
         * Bucketing (which is required to find batch and Product key) happens in PicoPlatingEntityBuilder so
         * Routing before bucketing will return Squid
         */
            assertThat(systemRouter.routeForVessels(new HashSet<LabVessel>(mapBarcodeToTube.values())),
                       is(SystemRouter.System.SQUID));

        //Build Event History
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        Calendar july25CalendarDate = new GregorianCalendar(2013, 6, 25);
        Calendar preJuly25CalendarDate = new GregorianCalendar(2013, 6, 24);

        Date today = new Date();

        if (today.after(july25CalendarDate.getTime()) ||
                today.equals(july25CalendarDate.getTime())) {
            workflowBatch.setCreatedOn(preJuly25CalendarDate.getTime());
        }

        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, BARCODE_SUFFIX);
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                                                                                  String.valueOf(runDate.getTime()),
                                                                                  BARCODE_SUFFIX, true);

        /*
         * Bucketing (which is required to find batch and Product key) happens in PicoPlatingEntityBuilder so
         * we are doing the routing on those initial tubes after the plating process has run.
         */
        assertThat(systemRouter.routeForVessels(new HashSet<LabVessel>(mapBarcodeToTube.values())),
                   is(SystemRouter.System.BOTH));

        assertThat(systemRouter
                           .routeForVessels(new HashSet<LabVessel>(
                                   picoPlatingEntityBuilder.getNormBarcodeToTubeMap().values())),
                   is(SystemRouter.System.BOTH));


        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                                               picoPlatingEntityBuilder.getNormTubeFormation(),
                                               picoPlatingEntityBuilder.getNormalizationBarcode(), BARCODE_SUFFIX);

        assertThat(systemRouter
                           .routeForVessels(
                                   Collections.<LabVessel>singleton(
                                           exomeExpressShearingEntityBuilder.getShearingCleanupPlate())),
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
                                                          hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                                                          "1");

        BarcodedTube denatureTube =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (denatureTube)),
                   is(SystemRouter.System.BOTH));
        String denatureTubeBarcode = denatureTube.getLabel();
        MiSeqReagentKit reagentKit = new MiSeqReagentKit("reagent_kit_barcode");
        LabEvent denatureToReagentKitEvent = new LabEvent(DENATURE_TO_REAGENT_KIT_TRANSFER, new Date(),
                                                          "ZLAB", 1L, 1L, "systemRouterTest");
        final VesselToSectionTransfer sectionTransfer = new VesselToSectionTransfer(denatureTube,
                SBSSection.getBySectionName(MiSeqReagentKit.LOADING_WELL.name()),
                reagentKit.getContainerRole(), null, denatureToReagentKitEvent);
        denatureToReagentKitEvent.getVesselToSectionTransfers().add(sectionTransfer);
        assertThat(systemRouter.routeForVessels(Collections.<LabVessel>singleton
                (reagentKit)),
                   is(SystemRouter.System.BOTH));

        LabBatch fctBatch = new LabBatch(FLOWCELL_2500_TICKET, LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, denatureTube, BigDecimal.valueOf(12.33f));

        HiSeq2500FlowcellEntityBuilder flowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), BARCODE_SUFFIX + "ADXX",
                                            FLOWCELL_2500_TICKET,
                                            ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null,
                                            Workflow.AGILENT_EXOME_EXPRESS);
        BarcodedTube dilutionTube =
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

    private int sampleNum = 1;
    private ProductOrder placeOrderForTubesAndBatch(Set<LabVessel> tubes, Product product, Bucket bucket) {
        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        for (LabVessel tube : tubes) {
            String sampleName = "SM-" + sampleNum;
            productOrderSamples.add(new ProductOrderSample(sampleName));
            tube.addSample(new MercurySample(sampleName, MercurySample.MetadataSource.BSP));
            sampleNum++;
        }
        ProductOrder order = new ProductOrder(101L, "Test Order", productOrderSamples, "Quote-1", product, testProject);
        productOrderSequence++;
        String jiraTicketKey = "PDO-" + productOrderSequence;
        order.setJiraTicketKey(jiraTicketKey);
        Collection<BucketEntry> bucketEntries = new ArrayList<>();
        if (bucket != null) {
            for (LabVessel tube : tubes) {
                bucketEntries.add(bucket.addEntry(order, tube, BucketEntry.BucketEntryType.PDO_ENTRY));
            }
            LabBatch labBatch = new LabBatch("LCSET-" + productOrderSequence, tubes,
                                             LabBatch.LabBatchType.WORKFLOW);
            labBatch.setWorkflow(product.getWorkflowName());
            for (BucketEntry bucketEntry : bucketEntries) {
                bucketEntry.setLabBatch(labBatch);
            }
        }
        return order;
    }

    @Test(groups = DATABASE_FREE)
    public void testSystemOfRecordForSamplesLabTube() {
        assertThat(systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1), is(SQUID));
        LabEventTestFactory.addInPlaceEvent(LabEventType.SAMPLE_RECEIPT, tube1);
        assertThat(systemRouter.getSystemOfRecordForVessel(MERCURY_TUBE_1), is(MERCURY));
    }

    /*
     * Utility and factory methods for dealing with mocking results from the BSP export query service.
     */

    /**
     * Convenience for specifying a mock call to
     * {@link org.broadinstitute.gpinformatics.infrastructure.bsp.exports.BSPExportsService#findExportDestinations(java.util.Collection)}
     * that uses varargs instead of making the caller construct a collection.
     *
     * @param labVessels the vessels to expect
     *
     * @return the mock result, suitable as an argument to {@link org.mockito.Mockito#when(Object)}
     */
    private IsExported.ExportResults mockFindExportDestinations(LabVessel... labVessels) {
        /*
         * Must use a List here in order for argument matching to work because {@link SystemRouter} currently uses a
         * List in its implementation. The parameter type is Collection<LabVessel> and the specific collection type
         * could be changed without affecting the behavior. In that case, the type passed in here needs to change to
         * match.
         *
         * It would be better if we didn't have to do this, but doing so would require a matcher that enforces the
         * contract of Collection.equals() without enforcing the contract of any specific collection type. It may be
         * possible to use argThat() with a Hamcrest collection matcher.
         */
        return mockBspExportService.findExportDestinations(Arrays.<LabVessel>asList(labVessels));
    }

    /**
     * Make an ExportResults object with a result for a single vessel.
     *
     * @param tubeBarcode tube barcode
     * @param exportsSet  the systems to use in the result
     *
     * @return a new ExportResults
     */
    private static IsExported.ExportResults makeExportResults(String tubeBarcode,
                                                              @Nonnull Set<IsExported.ExternalSystem> exportsSet) {
        // If the system is null create an empty Set, otherwise a singleton Set.
        return new IsExported.ExportResults(Arrays.asList(new IsExported.ExportResult(tubeBarcode, exportsSet)));
    }

    private static IsExported.ExportResults makeExportResultsNotFound(String tubeBarcode, String notFoundMessage) {
        return new IsExported.ExportResults(Arrays.asList(makeExportResultNotFound(tubeBarcode, notFoundMessage)));
    }

    private static IsExported.ExportResults makeExportResultsError(String tubeBarcode, String errorMessage) {
        return new IsExported.ExportResults(Arrays.asList(makeExportResultError(tubeBarcode, errorMessage)));
    }

    private static IsExported.ExportResult makeExportResultNotFound(String tubeBarcode, String notFoundMessage) {
        IsExported.ExportResult exportResult = new IsExported.ExportResult();
        exportResult.setBarcode(tubeBarcode);
        exportResult.setNotFound(notFoundMessage);
        return exportResult;
    }

    private static IsExported.ExportResult makeExportResultError(String tubeBarcode, String errorMessage) {
        IsExported.ExportResult exportResult = new IsExported.ExportResult();
        exportResult.setBarcode(tubeBarcode);
        exportResult.setError(errorMessage);
        return exportResult;
    }
}
