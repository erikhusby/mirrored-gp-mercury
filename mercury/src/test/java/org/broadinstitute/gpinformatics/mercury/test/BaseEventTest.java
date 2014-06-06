package org.broadinstitute.gpinformatics.mercury.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentration;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentrationProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory.CherryPick;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Graph;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferEntityGrapher;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferVisualizer;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventRefDataFetcher;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.DenatureToDilutionTubeHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.EventHandlerSelector;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.FlowcellMessageHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.SamplesDaughterPlateHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.transfervis.TransferVisualizerClient;
import org.broadinstitute.gpinformatics.mercury.presentation.transfervis.TransferVisualizerFrame;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.IceEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.KapaQCEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.MiSeqReagentKitEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PreFlightEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SageEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ShearingEntityBuilder;
import org.easymock.EasyMock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This class handles setting up various factories and EJBs for use in any lab event test.
 */
public class BaseEventTest {
    public static final String POSITIVE_CONTROL = "NA12878";
    public static final String NEGATIVE_CONTROL = "WATER_CONTROL";
    private static Log log = LogFactory.getLog(BaseEventTest.class);

    public static final int NUM_POSITIONS_IN_RACK = 96;

    // todo jmt find a better way to do this, without propagating it to every call to validateWorkflow.
    /**
     * Referenced in validation of routing.
     */
    protected static SystemRouter.System expectedRouting = SystemRouter.System.MERCURY;

    private BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);

    private LabEventFactory labEventFactory;

    private LabBatchEjb labBatchEJB;

    private BucketEjb bucketEjb;

    /**
     * The date on which Exome Express is routed to Mercury only.
     */
    public static final GregorianCalendar EX_EX_IN_MERCURY_CALENDAR = new GregorianCalendar(2013, 6, 26);

    protected static Map<String, BSPSampleDTO> mapSampleNameToDto = new HashMap<>();

    protected final LabEventRefDataFetcher labEventRefDataFetcher =
            new LabEventRefDataFetcher() {

                @Override
                public BspUser getOperator(String userId) {
                    return new BSPUserList.QADudeUser("Test", BSPManagerFactoryStub.QA_DUDE_USER_ID);
                }

                @Override
                public BspUser getOperator(Long bspUserId) {
                    return new BSPUserList.QADudeUser("Test", BSPManagerFactoryStub.QA_DUDE_USER_ID);
                }

                @Override
                public LabBatch getLabBatch(String labBatchName) {
                    return null;
                }
            };

    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        labBatchEJB = new LabBatchEjb();
        JiraService jiraService = JiraServiceProducer.stubInstance();
        labBatchEJB.setJiraService(jiraService);
        labBatchEJB.setLabBatchDao(EasyMock.createMock(LabBatchDao.class));

        JiraTicketDao mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
        labBatchEJB.setJiraTicketDao(mockJira);

        ProductOrderDao mockProductOrderDao = Mockito.mock(ProductOrderDao.class);
        Mockito.when(mockProductOrderDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {

                Object[] arguments = invocationOnMock.getArguments();

                return ProductOrderTestFactory.createDummyProductOrder((String) arguments[0]);
            }
        });
        labBatchEJB.setProductOrderDao(mockProductOrderDao);


        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        BSPSetVolumeConcentration bspSetVolumeConcentration = BSPSetVolumeConcentrationProducer.stubInstance();
        labEventFactory = new LabEventFactory(testUserList, bspSetVolumeConcentration);
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);

        final FlowcellMessageHandler flowcellMessageHandler =
                new FlowcellMessageHandler();
        flowcellMessageHandler.setJiraService(JiraServiceProducer.stubInstance());
        flowcellMessageHandler.setEmailSender(new EmailSender());
        flowcellMessageHandler.setAppConfig(new AppConfig(
                Deployment.DEV));

        EventHandlerSelector eventHandlerSelector =
                new EventHandlerSelector(new DenatureToDilutionTubeHandler(),
                                         flowcellMessageHandler, new SamplesDaughterPlateHandler());
        labEventFactory.setEventHandlerSelector(eventHandlerSelector);

        bucketEjb = new BucketEjb(labEventFactory, jiraService, null, null, null, null,
                                  null, null, null, EasyMock.createNiceMock(ProductOrderDao.class));
    }

    /**
     * This method builds the initial tube rack for starting an event test.  This will log an error if you attempt to
     * create the rack from a product order than has more than NUM_POSITIONS_IN_RACK tubes.
     *
     * @param productOrder      The product order to create the initial rack from
     * @param tubeBarcodePrefix prefix for each tube barcode
     *
     * @return Returns a map of String barcodes to their tube objects.
     */
    public Map<String, BarcodedTube> createInitialRack(ProductOrder productOrder, String tubeBarcodePrefix) {
        Map<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        int rackPosition = 1;
        for (ProductOrderSample poSample : productOrder.getSamples()) {
            String barcode = tubeBarcodePrefix + rackPosition;
            BarcodedTube bspAliquot = new BarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(poSample.getName()));
            mapBarcodeToTube.put(barcode, bspAliquot);
            Map<BSPSampleSearchColumn, String> dataMap = new HashMap<>();
            dataMap.put(BSPSampleSearchColumn.SAMPLE_ID, poSample.getName());
            mapSampleNameToDto.put(poSample.getName(), new BSPSampleDTO(dataMap));
            if (rackPosition > NUM_POSITIONS_IN_RACK) {
                log.error(
                        "More product order samples than allowed in a single rack. " + productOrder.getSamples().size()
                        + " > " + NUM_POSITIONS_IN_RACK);
                break;
            }
            rackPosition++;
        }
        return mapBarcodeToTube;
    }

    /**
     * This method will create a bucket and populate the bucket with tubes.
     *
     * @param mapBarcodeToTube A map of barcode to tubes that will be used to populate the bucket.
     * @param productOrder     The product order to use for the bucket entry.
     * @param bucketName       The name of the bucket to create.
     *
     * @return Returns a bucket populated with tubes.
     */
    protected Bucket createAndPopulateBucket(Map<String, BarcodedTube> mapBarcodeToTube, ProductOrder productOrder,
                                             String bucketName) {
        Bucket workingBucket = new Bucket(bucketName);

        for (BarcodedTube tube : mapBarcodeToTube.values()) {
            workingBucket.addEntry(productOrder, tube, BucketEntry.BucketEntryType.PDO_ENTRY);
        }
        return workingBucket;
    }


    protected LabEventHandler getLabEventHandler() {
        return new LabEventHandler(new WorkflowLoader());
    }

    /**
     * @param mapBarcodeToTube A map of barcodes to tubes that will be added to the bucket and drained into the
     *                         batch.
     * @param productOrder     The product order to use for bucket entries.
     * @param workflowBatch    The batch that will be used for this process.
     * @param lcsetSuffix      Set this non-null to override the lcset id number.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public Bucket bucketBatchAndDrain(Map<String, BarcodedTube> mapBarcodeToTube, final ProductOrder productOrder,
                                      LabBatch workflowBatch, String lcsetSuffix) {
        Bucket workingBucket = createAndPopulateBucket(mapBarcodeToTube, productOrder, "Pico/Plating Bucket");

        // Controls what the created lcset id is by temporarily overriding the static variable.
        String defaultLcsetSuffix = JiraServiceStub.getCreatedIssueSuffix();
        if (lcsetSuffix != null) {
            JiraServiceStub.setCreatedIssueSuffix(lcsetSuffix);
        }
        ProductOrderDao mockProductOrderDao = Mockito.mock(ProductOrderDao.class);
        Mockito.when(mockProductOrderDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {

                Object[] arguments = invocationOnMock.getArguments();

                ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder((String) arguments[0]);
                if((arguments[0]).equals(productOrder.getBusinessKey())) {
                    dummyProductOrder = productOrder;
                }
                return dummyProductOrder;
            }
        });
        labBatchEJB.setProductOrderDao(mockProductOrderDao);

        labBatchEJB.createLabBatch(workflowBatch, "scottmat", CreateFields.IssueType.EXOME_EXPRESS);
        JiraServiceStub.setCreatedIssueSuffix(defaultLcsetSuffix);

        drainBucket(workingBucket);
        return workingBucket;
    }

    public void drainBucket(Bucket workingBucket) {
        bucketEjb.moveFromBucketToCommonBatch(workingBucket.getBucketEntries());
    }

    public void archiveBucketEntries(Bucket bucket) {
        for (BucketEntry picoEntries : bucket.getBucketEntries()) {
            picoEntries.setStatus(BucketEntry.Status.Archived);
        }
    }

    /**
     * This method runs the entities through the pico/plating process.
     *
     * @param mapBarcodeToTube     A map of barcodes to tubes that will be run the starting point of the pico/plating process.
     * @param rackBarcodeSuffix    rack barcode suffix.
     * @param barcodeSuffix        Uniquifies the generated vessel barcodes. NOT date if test quickly invokes twice.
     * @param archiveBucketEntries allows a DBFree test case to force "ancestor" tubes to be currently in a bucket
     *                             for scenarios where that state is important to the test
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public PicoPlatingEntityBuilder runPicoPlatingProcess(Map<String, BarcodedTube> mapBarcodeToTube,
                                                          String rackBarcodeSuffix, String barcodeSuffix,
                                                          boolean archiveBucketEntries) {
        String rackBarcode = "REXEX" + rackBarcodeSuffix;

        return new PicoPlatingEntityBuilder(bettaLimsMessageTestFactory,
                                            labEventFactory, getLabEventHandler(),
                                            mapBarcodeToTube, rackBarcode, barcodeSuffix).invoke();
    }

    /**
     * This method runs the entities through the ExEx Kapa QC process.
     *
     * @param normBarcodeToTubeMap A map of barcodes to tubes that will be run the starting point of the ExEx kapa QC process.
     * @param normTubeFormation    The tube formation that represents the entities coming out of pico/plating.
     * @param normBarcode          The rack barcode of the tube formation.
     * @param barcodeSuffix        Uniquifies the generated vessel barcodes. NOT date if test quickly invokes twice.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public KapaQCEntityBuilder runKapaQCProcess(
            Map<String, BarcodedTube> normBarcodeToTubeMap,
            TubeFormation normTubeFormation,
            String normBarcode, String barcodeSuffix) {

        return new KapaQCEntityBuilder(normBarcodeToTubeMap, normTubeFormation,
                bettaLimsMessageTestFactory, labEventFactory,
                getLabEventHandler(), normBarcode, barcodeSuffix).invoke();
    }

    /**
     * This method runs the entities through the ExEx shearing process.
     *
     * @param normBarcodeToTubeMap A map of barcodes to tubes that will be run the starting point of the ExEx shearing process.
     * @param normTubeFormation    The tube formation that represents the entities coming out of pico/plating.
     * @param normBarcode          The rack barcode of the tube formation.
     * @param barcodeSuffix        Uniquifies the generated vessel barcodes. NOT date if test quickly invokes twice.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public ExomeExpressShearingEntityBuilder runExomeExpressShearingProcess(
            Map<String, BarcodedTube> normBarcodeToTubeMap,
            TubeFormation normTubeFormation,
            String normBarcode, String barcodeSuffix) {

        return new ExomeExpressShearingEntityBuilder(normBarcodeToTubeMap, normTubeFormation,
                                                     bettaLimsMessageTestFactory, labEventFactory,
                                                     getLabEventHandler(), normBarcode, barcodeSuffix).invoke();
    }

    /**
     * This method runs the entities through the preflight process.
     *
     * @param mapBarcodeToTube A map of barcodes to tubes that will be run the starting point of the preflight process.
     * @param barcodeSuffix    Uniquifies the generated vessel barcodes. NOT date if test quickly invokes twice.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public PreFlightEntityBuilder runPreflightProcess(Map<String, BarcodedTube> mapBarcodeToTube,
                                                      String barcodeSuffix) {
        return new PreFlightEntityBuilder(bettaLimsMessageTestFactory,
                                          labEventFactory, getLabEventHandler(),
                                          mapBarcodeToTube, barcodeSuffix).invoke();
    }

    /**
     * This method runs the entities through the shearing process.
     *
     * @param mapBarcodeToTube A map of barcodes to tubes that will be run the starting point of the shearing process.
     * @param tubeFormation    The tube formation that represents the entities coming out of pico/plating.
     * @param rackBarcode      The rack barcode of the tube formation.
     * @param barcodeSuffix    Uniquifies the generated vessel barcodes. NOT date if test quickly invokes twice.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public ShearingEntityBuilder runShearingProcess(Map<String, BarcodedTube> mapBarcodeToTube,
                                                    TubeFormation tubeFormation,
                                                    String rackBarcode, String barcodeSuffix) {

        return new ShearingEntityBuilder(mapBarcodeToTube, tubeFormation,
                                         bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                                         rackBarcode, barcodeSuffix).invoke();
    }

    /**
     * This method runs the entities through the library construction process.
     *
     * @param shearingCleanupPlate   The shearing cleanup plate from the shearing process.
     * @param shearCleanPlateBarcode The shearing clean plate barcode.
     * @param shearingPlate          The shearing plate from the shearing process.
     * @param barcodeSuffix          Uniquifies the generated vessel barcodes. NOT date if test quickly invokes twice.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public LibraryConstructionEntityBuilder runLibraryConstructionProcess(StaticPlate shearingCleanupPlate,
                                                                          String shearCleanPlateBarcode,
                                                                          StaticPlate shearingPlate,
                                                                          String barcodeSuffix) {

        return new LibraryConstructionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                shearingCleanupPlate, shearCleanPlateBarcode, shearingPlate, NUM_POSITIONS_IN_RACK, barcodeSuffix,
                LibraryConstructionEntityBuilder.Indexing.DUAL).invoke();
    }

    /**
     * This method runs the entities through the hybrid selection process.
     *
     * @param pondRegRack         The pond registration rack coming out of the library construction process.
     * @param pondRegRackBarcode  The pond registration rack barcode.
     * @param pondRegTubeBarcodes A list of pond registration tube barcodes.
     * @param barcodeSuffix       Uniquifies the generated vessel barcodes. NOT date if test quickly invoked twice.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public HybridSelectionEntityBuilder runHybridSelectionProcess(TubeFormation pondRegRack, String pondRegRackBarcode,
                                                                  List<String> pondRegTubeBarcodes,
                                                                  String barcodeSuffix) {

        return new HybridSelectionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                pondRegRack, pondRegRackBarcode, pondRegTubeBarcodes, barcodeSuffix).invoke();
    }

    /**
     * Creates an entity graph for Illumina Content Exome.
     *
     * @param pondRegRack         The pond registration rack coming out of the library construction process.
     * @param pondRegRackBarcode  The pond registration rack barcode.
     * @param pondRegTubeBarcodes A list of pond registration tube barcodes.
     * @param barcodeSuffix       Makes unique the generated vessel barcodes. Don't use date if test quickly invoked twice.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public IceEntityBuilder runIceProcess(TubeFormation pondRegRack, String pondRegRackBarcode,
                                          List<String> pondRegTubeBarcodes, String barcodeSuffix) {
        return new IceEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(), pondRegRack,
                                    pondRegRackBarcode, pondRegTubeBarcodes, barcodeSuffix).invoke();
    }

    /**
     * This method runs the entities through the QTP process.
     *
     * @param rack             The tube rack coming out of hybrid selection
     * @param tubeBarcodes     A list of the tube barcodes in the rack.
     * @param mapBarcodeToTube A map of barcodes to tubes that will be run the starting point of the pico/plating process.
     * @param barcodeSuffix    Uniquifies the generated vessel barcodes. NOT date if test quickly invokes twice.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public QtpEntityBuilder runQtpProcess(TubeFormation rack, List<String> tubeBarcodes,
                                          Map<String, BarcodedTube> mapBarcodeToTube,
                                          String barcodeSuffix) {

        return new QtpEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                Collections.singletonList(rack),
                Collections.singletonList(rack.getLabel()),
                Collections.singletonList(tubeBarcodes),
                mapBarcodeToTube, barcodeSuffix).invoke();
    }

    /**
     * This method runs the entities through the HiSeq2500 process.
     *
     * @param denatureRack           The denature tube rack.
     * @param barcodeSuffix          Uniquifies the generated vessel barcodes. NOT date if test quickly invokes twice.
     * @param productionFlowcellPath
     * @param designationName        Name of the designation created in Squid to support testing the systems running in
     *                               parallel
     * @param workflow
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public HiSeq2500FlowcellEntityBuilder runHiSeq2500FlowcellProcess(TubeFormation denatureRack, String barcodeSuffix,
                                                                      String fctTicket,
                                                                      ProductionFlowcellPath productionFlowcellPath,
                                                                      String designationName, Workflow workflow) {
        int flowcellLanes = 8;
        if (workflow == Workflow.AGILENT_EXOME_EXPRESS) {
            flowcellLanes = 2;
        }
        String flowcellBarcode = "flowcell" + new Date().getTime() + "ADXX";
        return new HiSeq2500FlowcellEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                                                  denatureRack, flowcellBarcode, barcodeSuffix, fctTicket,
                                                  productionFlowcellPath,
                                                  designationName, flowcellLanes).invoke();
    }

    public MiSeqReagentKitEntityBuilder runMiSeqReagentEntityBuilder(TubeFormation denatureRack, String barcodeSuffix,
                                                                     String reagentBlockBarcode) {

        char[] pool = {
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
                'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
                's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1',
                '2', '3', '4', '5', '6', '7', '8', '9', '0'};

        Random rnd = new Random();

        String flowcellBarcode =
                "A" + pool[rnd.nextInt(pool.length)] + pool[rnd.nextInt(pool.length)] + pool[rnd.nextInt(pool.length)]
                + pool[rnd.nextInt(pool.length)];
        return new MiSeqReagentKitEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                                                reagentBlockBarcode, denatureRack, flowcellBarcode).invoke();
    }

    /**
     * This method runs the entities through the Sage process.
     *
     * @param pondRegRack         The pond registration rack coming out of the library construction process.
     * @param pondRegRackBarcode  The pond registration rack barcode.
     * @param pondRegTubeBarcodes A list of pond registration tube barcodes.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public SageEntityBuilder runSageProcess(TubeFormation pondRegRack, String pondRegRackBarcode,
                                            List<String> pondRegTubeBarcodes) {
        return new SageEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                                     pondRegRackBarcode, pondRegRack, pondRegTubeBarcodes).invoke();
    }

    /**
     * Simulates a BSP daughter plate transfer, prior to export from BSP to Mercury, then does a re-array to add
     * controls.
     *
     * @param mapBarcodeToTube source tubes
     *
     * @return destination tube formation
     */
    public TubeFormation daughterPlateTransfer(Map<String, BarcodedTube> mapBarcodeToTube) {
        List<String> daughterTubeBarcodes = generateDaughterTubeBarcodes(mapBarcodeToTube);
        return getDaughterTubeFormation(mapBarcodeToTube, daughterTubeBarcodes);
    }

    /**
     * Simulates a BSP daughter plate transfer with a mismatched layout prior to export from BSP to Mercury,
     * then does a re-array to add controls.
     *
     * @param mapBarcodeToTube  source tubes
     * @param mapBarcodeToTube2 second rack of source tubes
     *
     * @return destination tube formation
     */
    public TubeFormation mismatchedDaughterPlateTransfer(Map<String, BarcodedTube> mapBarcodeToTube,
                                                         Map<String, BarcodedTube> mapBarcodeToTube2,
                                                         List<Integer> wellsToReplace) {
        List<String> daughterTubeBarcodes = generateDaughterTubeBarcodes(mapBarcodeToTube);
        return getDaughterTubeFormationCherryPick(mapBarcodeToTube, mapBarcodeToTube2, daughterTubeBarcodes,
                                                  wellsToReplace);
    }

    /**
     * Generates the daughter tube barcodes for the destination tube formation .
     *
     * @param mapBarcodeToTube source tubes
     *
     * @return list of daughter tube barcodes
     */
    private List<String> generateDaughterTubeBarcodes(Map<String, BarcodedTube> mapBarcodeToTube) {
        // Daughter plate transfer that doesn't include controls
        List<String> daughterTubeBarcodes = new ArrayList<>();
        for (int i = 0; i < mapBarcodeToTube.size(); i++) {
            daughterTubeBarcodes.add("D" + i);
        }
        return daughterTubeBarcodes;
    }

    /**
     * Creates a daughter tube formation using a rack to rack transfer.
     *
     * @param mapBarcodeToTube     source tubes
     * @param daughterTubeBarcodes destination tubes
     *
     * @return destination tube formation
     */
    private TubeFormation getDaughterTubeFormation(Map<String, BarcodedTube> mapBarcodeToTube,
                                                   List<String> daughterTubeBarcodes) {
        PlateTransferEventType daughterPlateTransferJaxb =
                bettaLimsMessageTestFactory.buildRackToRack("SamplesDaughterPlateCreation", "MotherRack",
                                                            new ArrayList<>(mapBarcodeToTube.keySet()), "DaughterRack",
                                                            daughterTubeBarcodes);
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<String, LabVessel>(mapBarcodeToTube);
        LabEvent daughterPlateTransferEntity =
                labEventFactory.buildFromBettaLims(daughterPlateTransferJaxb, mapBarcodeToVessel);
        TubeFormation daughterPlate =
                (TubeFormation) daughterPlateTransferEntity.getTargetLabVessels().iterator().next();

        Map<VesselPosition, BarcodedTube> mapBarcodeToDaughterTube = new EnumMap<>(VesselPosition.class);
        for (BarcodedTube barcodedTube : daughterPlate.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(daughterPlate.getContainerRole().getPositionOfVessel(barcodedTube),
                                         barcodedTube);
        }

        // Controls are added in a re-array
        BarcodedTube posControlTube = new BarcodedTube("C1");
        BSPSampleDTO bspSampleDtoPos = new BSPSampleDTO(
                new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                    put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, POSITIVE_CONTROL);
                }});
        posControlTube.addSample(new MercurySample(POSITIVE_CONTROL, bspSampleDtoPos));
        mapSampleNameToDto.put(POSITIVE_CONTROL, bspSampleDtoPos);
        mapBarcodeToDaughterTube.put(VesselPosition.H11, posControlTube);

        BarcodedTube negControlTube = new BarcodedTube("C2");
        BSPSampleDTO bspSampleDtoNeg = new BSPSampleDTO(
                new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                    put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, NEGATIVE_CONTROL);
                }});
        negControlTube.addSample(new MercurySample(NEGATIVE_CONTROL, bspSampleDtoNeg));
        mapSampleNameToDto.put(NEGATIVE_CONTROL, bspSampleDtoNeg);
        mapBarcodeToDaughterTube.put(VesselPosition.H12, negControlTube);

        return new TubeFormation(mapBarcodeToDaughterTube, RackOfTubes.RackType.Matrix96);
    }

    /**
     * Creates a daughter tube formation using cherry pick transfers.
     *
     * @param mapBarcodeToTube     source tubes
     * @param mapBarcodeToTube2    second rack of source tubes
     * @param daughterTubeBarcodes destination tubes
     * @param wellsToReplace       wells to switch from rack 1 to rack 2
     *
     * @return destination tube formation
     */
    private TubeFormation getDaughterTubeFormationCherryPick(Map<String, BarcodedTube> mapBarcodeToTube,
                                                             Map<String, BarcodedTube> mapBarcodeToTube2,
                                                             List<String> daughterTubeBarcodes,
                                                             List<Integer> wellsToReplace) {

        List<String> sourceTubeBarcodes = new ArrayList<>(mapBarcodeToTube.keySet());
        List<String> sourceTubeBarcodes2 = new ArrayList<>(mapBarcodeToTube2.keySet());

        List<CherryPick> cherryPicks = new ArrayList<>();

        for (int i = 0; i < mapBarcodeToTube.size(); i++) {
            if (!wellsToReplace.isEmpty() && wellsToReplace.contains(i)) {
                cherryPicks.add(new CherryPick(
                        "MotherRack2",
                        VesselGeometry.G12x8.getVesselPositions()[i].name(),
                        "DaughterRack",
                        VesselGeometry.G12x8.getVesselPositions()[i].name()));
            } else {
                cherryPicks.add(new CherryPick(
                        "MotherRack",
                        VesselGeometry.G12x8.getVesselPositions()[i].name(),
                        "DaughterRack",
                        VesselGeometry.G12x8.getVesselPositions()[i].name()));
            }
        }

        List<String> sourceRacks = new ArrayList<>();
        sourceRacks.add("MotherRack");
        sourceRacks.add("MotherRack2");

        List<String> destinationRacks = new ArrayList<>();
        destinationRacks.add("DaughterRack");

        List<List<String>> sourceTubes = new ArrayList<>();
        sourceTubes.add(sourceTubeBarcodes);
        sourceTubes.add(sourceTubeBarcodes2);

        List<List<String>> daughterTubes = new ArrayList<>();
        daughterTubes.add(daughterTubeBarcodes);

        PlateCherryPickEvent daughterPlateTransferJaxb =
                bettaLimsMessageTestFactory.buildCherryPick("SamplesDaughterPlateCreation", sourceRacks,
                                                            sourceTubes, destinationRacks, daughterTubes, cherryPicks);
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<String, LabVessel>(mapBarcodeToTube);
        mapBarcodeToVessel.putAll(mapBarcodeToTube2);
        LabEvent daughterPlateTransferEntity =
                labEventFactory.buildFromBettaLims(daughterPlateTransferJaxb, mapBarcodeToVessel);
        TubeFormation daughterPlate =
                (TubeFormation) daughterPlateTransferEntity.getTargetLabVessels().iterator().next();

        Map<VesselPosition, BarcodedTube> mapBarcodeToDaughterTube = new EnumMap<>(VesselPosition.class);
        for (BarcodedTube barcodedTube : daughterPlate.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(daughterPlate.getContainerRole().getPositionOfVessel(barcodedTube),
                                         barcodedTube);
        }

        // Controls are added in a re-array
        BarcodedTube posControlTube = new BarcodedTube("C1");
        BSPSampleDTO bspSampleDtoPos = new BSPSampleDTO(
                new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                    put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, POSITIVE_CONTROL);
                }});
        posControlTube.addSample(new MercurySample(POSITIVE_CONTROL, bspSampleDtoPos));
        mapSampleNameToDto.put(POSITIVE_CONTROL, bspSampleDtoPos);
        mapBarcodeToDaughterTube.put(VesselPosition.H11, posControlTube);

        BarcodedTube negControlTube = new BarcodedTube("C2");
        BSPSampleDTO bspSampleDtoNeg = new BSPSampleDTO(
                new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                    put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, NEGATIVE_CONTROL);
                }});
        negControlTube.addSample(new MercurySample(NEGATIVE_CONTROL, bspSampleDtoNeg));
        mapSampleNameToDto.put(NEGATIVE_CONTROL, bspSampleDtoNeg);
        mapBarcodeToDaughterTube.put(VesselPosition.H12, negControlTube);

        return new TubeFormation(mapBarcodeToDaughterTube, RackOfTubes.RackType.Matrix96);
    }

    public BettaLimsMessageTestFactory getBettaLimsMessageTestFactory() {
        return bettaLimsMessageTestFactory;
    }

    public LabEventFactory getLabEventFactory() {
        return labEventFactory;
    }

    /**
     * Allows Transfer Visualizer to be run inside a test, so developer can verify that vessel transfer graph is
     * constructed correctly.
     *
     * @param labVessel starting point in graph.
     */
    public static void runTransferVisualizer(LabVessel labVessel) {
        // Disabled by default, because it would block Bamboo tests.
        if (false) {
            TransferEntityGrapher transferEntityGrapher = new TransferEntityGrapher();
            // "More Transfers" buttons won't work when there's no server, so render all vessels in first "request"
            transferEntityGrapher.setMaxNumVesselsPerRequest(10000);
            Graph graph = new Graph();
            ArrayList<TransferVisualizer.AlternativeId> alternativeIds = new ArrayList<>();
            if (false) {
                alternativeIds.add(TransferVisualizer.AlternativeId.SAMPLE_ID);
                alternativeIds.add(TransferVisualizer.AlternativeId.LCSET);
            }
            transferEntityGrapher.startWithTube((BarcodedTube) labVessel, graph, alternativeIds);

            TransferVisualizerClient transferVisualizerClient = new TransferVisualizerClient(
                    labVessel.getLabel(), alternativeIds);
            transferVisualizerClient.setGraph(graph);

            TransferVisualizerFrame transferVisualizerFrame = new TransferVisualizerFrame();
            transferVisualizerFrame.setTransferVisualizerClient(transferVisualizerClient);
            // Suspend the test thread, to give the user time to scroll around the graph.
            try {
                Thread.sleep(500000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
