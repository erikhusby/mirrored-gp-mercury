package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PipelineDataType;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
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
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.addInPlaceEvent;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemOfRecord.System.MERCURY;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemOfRecord.System.SQUID;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.A_BASE;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.PREFLIGHT_CLEANUP;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.SAMPLE_RECEIPT;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the logic of determining the system of record.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SystemOfRecordDbFreeTest extends BaseEventTest {

    private static final String MERCURY_TUBE_1 = "mercuryTube1";
    private static final String MERCURY_TUBE_2 = "mercuryTube2";
    private static final String MERCURY_TUBE_3 = "mercuryTube3";
    private static final String CONTROL_TUBE = "controlTube";
    private static final String CONTROL1 = "control1";
    private static final String CONTROL2 = "control2";
    public static final String CONTROL_SAMPLE_ID = "SM-CONTROL1";
    public static final String NA12878 = "NA12878";
    public static final String MERCURY_PLATE = "mercuryPlate";
    public static final String BARCODE_SUFFIX = "mosRte";
    public static final String FLOWCELL_2500_TICKET = "FCT-3mosrte";


    private SystemOfRecord systemOfRecord;

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
        systemOfRecord = new SystemOfRecord(mockLabVesselDao, new WorkflowLoader().getWorkflowConfig(),
                mockBspExportService);

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

        controlWithoutWorkflow1 = new BarcodedTube(CONTROL1);
        controlWithoutWorkflow1.addSample(new MercurySample(CONTROL_SAMPLE_ID, MercurySample.MetadataSource.BSP));
        controlWithoutWorkflow2 = new BarcodedTube(CONTROL2);
        controlWithoutWorkflow2.addSample(new MercurySample(CONTROL_SAMPLE_ID, MercurySample.MetadataSource.BSP));

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(CONTROL1);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(CONTROL1, controlWithoutWorkflow1);
                }});

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(CONTROL1);
            add(MERCURY_TUBE_1);
            add(MERCURY_TUBE_2);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(CONTROL1, controlWithoutWorkflow1);
                    put(MERCURY_TUBE_1, tube1);
                    put(MERCURY_TUBE_2, tube2);
                }});

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add(CONTROL1);
            add(CONTROL2);
            add(MERCURY_TUBE_1);
            add(MERCURY_TUBE_2);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(CONTROL1, controlWithoutWorkflow1);
                    put(CONTROL2, controlWithoutWorkflow2);
                    put(MERCURY_TUBE_1, tube1);
                    put(MERCURY_TUBE_2, tube2);
                }});

        when(mockLabVesselDao.findByBarcodes(new ArrayList<String>() {{
            add("squidTube");
            add(CONTROL1);
        }})).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put("squidTube", null);
                    put(CONTROL1, controlWithoutWorkflow1);
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
     * {@link #testSystemOfRecord(String, KnownToMercury, InPDO, MercuryEvent, KnownToBSP, ExportFromBSP, SystemOfRecord.System)}.
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
     * {@link SystemOfRecord#getSystemOfRecord(String)}
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
                                   SystemOfRecord.System expectedSystemOfRecord) {

        LabVessel tube = configureMercury(tubeBarcode, knownToMercury, inPDO, mercuryEvent);
        // If Mercury doesn't know the tube, it should never ask BSP anything about it.
        if (tube != null) {
            configureBSP(tube, knownToBSP, exportFromBSP);
        }

        try {
            SystemOfRecord.System systemOfRecord = this.systemOfRecord.getSystemOfRecord(tubeBarcode);
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
     * Tests system of record for a validation LCSET
     */

    @Test(groups = DATABASE_FREE)
    public void testGetSystemOfRecordForControlOnly() {
        tube1.addSample(new MercurySample("SM-1", MercurySample.MetadataSource.BSP));

            assertThat(systemOfRecord.getSystemOfRecord(MERCURY_TUBE_1), is(SQUID));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }});
    }

    @Test(groups = DATABASE_FREE)
    public void testGetSystemOfRecordForVesselInValidationLCSET() {
        placeOrderForTubeAndBucket(tube1, exomeExpress, picoBucket);
        tube1.getAllLabBatches().iterator().next().setValidationBatch(true);
        assertThat(systemOfRecord.getSystemOfRecord(MERCURY_TUBE_1), is(MERCURY));
        verify(mockLabVesselDao).findByBarcodes(new ArrayList<String>() {{
            add(MERCURY_TUBE_1);
        }});
    }

    @Test(groups = DATABASE_FREE)
    public void testMercuryOnlyRouting() {

        final ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        Date runDate = new Date();
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        // An unbatched vessel defaults to Squid.
        assertThat(systemOfRecord.getSystemOfRecord(mapBarcodeToTube.keySet()), is(SQUID));

        //Build Event History
        expectedRouting = SystemOfRecord.System.MERCURY;
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
        assertThat(systemOfRecord.getSystemOfRecordForVessels(new HashSet<>(mapBarcodeToTube.values())),
                is(SystemOfRecord.System.MERCURY));

        assertThat(systemOfRecord.getSystemOfRecordForVessels(
                new HashSet<>(picoPlatingEntityBuilder.getNormBarcodeToTubeMap().values())),
                is(SystemOfRecord.System.MERCURY));


        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                                               picoPlatingEntityBuilder.getNormTubeFormation(),
                                               picoPlatingEntityBuilder.getNormalizationBarcode(), BARCODE_SUFFIX);

        assertThat(systemOfRecord.getSystemOfRecordForVessel(exomeExpressShearingEntityBuilder.getShearingCleanupPlate()),
                   is(SystemOfRecord.System.MERCURY));

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                                              exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                                              exomeExpressShearingEntityBuilder.getShearingPlate(), BARCODE_SUFFIX);

        TubeFormation pondRegRack = libraryConstructionEntityBuilder.getPondRegRack();


        assertThat(systemOfRecord.getSystemOfRecordForVessel(pondRegRack), is(SystemOfRecord.System.MERCURY));

        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                                          libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                                          libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), BARCODE_SUFFIX);
        TubeFormation normRack = hybridSelectionEntityBuilder.getNormCatchRack();
        assertThat(systemOfRecord.getSystemOfRecordForVessel(normRack), is(SystemOfRecord.System.MERCURY));

        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                                                          hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                                                          hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                                                          "1");

        BarcodedTube denatureTube =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        assertThat(systemOfRecord.getSystemOfRecordForVessel(denatureTube), is(SystemOfRecord.System.MERCURY));
        MiSeqReagentKit reagentKit = new MiSeqReagentKit("reagent_kit_barcode");
        LabEvent denatureToReagentKitEvent = new LabEvent(DENATURE_TO_REAGENT_KIT_TRANSFER, new Date(),
                                                          "ZLAB", 1L, 1L, "systemRouterTest");
        final VesselToSectionTransfer sectionTransfer = new VesselToSectionTransfer(denatureTube,
                SBSSection.getBySectionName(MiSeqReagentKit.LOADING_WELL.name()),
                reagentKit.getContainerRole(), null, denatureToReagentKitEvent);
        denatureToReagentKitEvent.getVesselToSectionTransfers().add(sectionTransfer);
        assertThat(systemOfRecord.getSystemOfRecordForVessel(reagentKit), is(SystemOfRecord.System.MERCURY));


        LabBatch fctBatch = new LabBatch(FLOWCELL_2500_TICKET, LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, denatureTube, BigDecimal.valueOf(12.33f));

        HiSeq2500FlowcellEntityBuilder flowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), BARCODE_SUFFIX + "ADXX",
                                            FLOWCELL_2500_TICKET,
                                            ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null,
                                            Workflow.AGILENT_EXOME_EXPRESS);
        BarcodedTube dilutionTube =
                flowcellEntityBuilder.getDilutionRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        assertThat(systemOfRecord.getSystemOfRecordForVessel(dilutionTube), is(SystemOfRecord.System.MERCURY));

        IlluminaFlowcell flowcell = flowcellEntityBuilder.getIlluminaFlowcell();
        assertThat(systemOfRecord.getSystemOfRecordForVessel(flowcell), is(SystemOfRecord.System.MERCURY));
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
        assertThat(systemOfRecord.getSystemOfRecord(MERCURY_TUBE_1), is(SQUID));
        LabEventTestFactory.addInPlaceEvent(LabEventType.SAMPLE_RECEIPT, tube1);
        assertThat(systemOfRecord.getSystemOfRecord(MERCURY_TUBE_1), is(MERCURY));
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
