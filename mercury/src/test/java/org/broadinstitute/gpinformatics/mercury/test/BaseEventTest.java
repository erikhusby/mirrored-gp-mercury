package org.broadinstitute.gpinformatics.mercury.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentration;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentrationStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory.CherryPick;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemOfRecord;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferVisualizerV2;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.CrspPipelineUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventRefDataFetcher;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.BspNewRootHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.CreateLabBatchHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.DenatureToDilutionTubeHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.EventHandlerSelector;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.FlowcellLoadedHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.FlowcellMessageHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowValidator;
import org.broadinstitute.gpinformatics.mercury.control.zims.ZimsIlluminaRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.test.builders.ArrayPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.CrspRiboPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.FPEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.FingerprintingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq4000FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.IceEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.IceJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.InfiniumEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.InfiniumJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionCellFreeUMIEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.MiSeqReagentKitEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PreFlightEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SageEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SingleCell10XEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SingleCellHashingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SingleCellSmartSeqEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SingleCellVdjEnrichmentEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.StoolTNAEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.TenXEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.TruSeqStrandSpecificEntityBuilder;
import org.easymock.EasyMock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
    public static SystemOfRecord.System expectedRouting = SystemOfRecord.System.MERCURY;
    private final CrspPipelineUtils crspPipelineUtils = new CrspPipelineUtils();

    private BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);

    private LabEventFactory labEventFactory;

    private LabBatchEjb labBatchEJB;

    private BucketEjb bucketEjb;

    /**
     * Controls are referenced in the routing logic
     */
    private static final List<Control> controlList = new ArrayList<>();
    private static final List<String> controlCollaboratorIdList = new ArrayList<>();

    static {
        controlList.add(new Control("NA12878", Control.ControlType.POSITIVE));
        controlList.add(new Control("WATER_CONTROL", Control.ControlType.NEGATIVE));

        for (Control control : controlList) {
            controlCollaboratorIdList.add(control.getCollaboratorParticipantId());
        }
    }


    /**
     * The date on which Exome Express is routed to Mercury only.
     */
    public static final GregorianCalendar EX_EX_IN_MERCURY_CALENDAR = new GregorianCalendar(2013, 6, 26);

    protected static Map<String, SampleData> nameToSampleData = new HashMap<>();

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
        JiraService jiraService = JiraServiceTestProducer.stubInstance();
        labBatchEJB.setJiraService(jiraService);
        labBatchEJB.setLabBatchDao(EasyMock.createMock(LabBatchDao.class));

        ProductOrderDao mockProductOrderDao = Mockito.mock(ProductOrderDao.class);
        Mockito.when(mockProductOrderDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {

                Object[] arguments = invocationOnMock.getArguments();

                return ProductOrderTestFactory.createDummyProductOrder((String) arguments[0]);
            }
        });
        labBatchEJB.setProductOrderDao(mockProductOrderDao);
        labBatchEJB.setWorkflowConfig(new WorkflowLoader().getWorkflowConfig());

        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        BSPSetVolumeConcentration bspSetVolumeConcentration =  new BSPSetVolumeConcentrationStub();
        labEventFactory = new LabEventFactory(testUserList, bspSetVolumeConcentration);
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);

        AppConfig appConfig = new AppConfig(Deployment.DEV);
        EmailSender emailSender = new EmailSender();

        FlowcellMessageHandler flowcellMessageHandler = new FlowcellMessageHandler();
        flowcellMessageHandler.setJiraService(JiraServiceTestProducer.stubInstance());
        flowcellMessageHandler.setEmailSender(emailSender);
        flowcellMessageHandler.setAppConfig(appConfig);

        FlowcellLoadedHandler flowcellLoadedHandler = new FlowcellLoadedHandler();
        flowcellLoadedHandler.setJiraService(JiraServiceTestProducer.stubInstance());
        flowcellLoadedHandler.setEmailSender(emailSender);
        flowcellLoadedHandler.setAppConfig(appConfig);

        EventHandlerSelector eventHandlerSelector = new EventHandlerSelector(
                new DenatureToDilutionTubeHandler(), flowcellMessageHandler, flowcellLoadedHandler,
                new BspNewRootHandler(), new CreateLabBatchHandler());
        labEventFactory.setEventHandlerSelector(eventHandlerSelector);

        bucketEjb = new BucketEjb(labEventFactory, jiraService, null, null, null, null,
                null, null, null, EasyMock.createNiceMock(ProductOrderDao.class),
                Mockito.mock(MercurySampleDao.class));
    }

    public Map<String, BarcodedTube> createInitialRack(ProductOrder productOrder, String tubeBarcodePrefix) {
        return createInitialRack(productOrder, tubeBarcodePrefix, MercurySample.MetadataSource.BSP);
    }

    /**
     * This method builds the initial tube rack for starting an event test.  This will log an error if you attempt to
     * create the rack from a product order than has more than NUM_POSITIONS_IN_RACK tubes.
     *
     * @param productOrder      The product order to create the initial rack from
     * @param tubeBarcodePrefix prefix for each tube barcode
     * @param metadataSource    BSP or Mercury
     * @return Returns a map of String barcodes to their tube objects.
     */
    public Map<String, BarcodedTube> createInitialRack(ProductOrder productOrder, String tubeBarcodePrefix,
            MercurySample.MetadataSource metadataSource) {
        Map<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        int rackPosition = 1;
        for (ProductOrderSample poSample : productOrder.getSamples()) {
            String barcode = tubeBarcodePrefix + rackPosition;
            BarcodedTube bspAliquot = new BarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(poSample.getName(), metadataSource));
            mapBarcodeToTube.put(barcode, bspAliquot);
            Map<BSPSampleSearchColumn, String> dataMap = new HashMap<>();
            dataMap.put(BSPSampleSearchColumn.SAMPLE_ID, poSample.getName());
            nameToSampleData.put(poSample.getName(), new BspSampleData(dataMap));
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
        // Uses map of sample name to pdoSample for making the link between pdoSample to MercurySample.
        Map<String, ProductOrderSample> mapSampleNameToPdoSample = new HashMap<>();
        for (ProductOrderSample pdoSample : productOrder.getSamples()) {
            mapSampleNameToPdoSample.put(pdoSample.getSampleKey(), pdoSample);
        }
        Bucket workingBucket = new Bucket(bucketName);
        for (BarcodedTube tube : mapBarcodeToTube.values()) {
            workingBucket.addEntry(productOrder, tube, BucketEntry.BucketEntryType.PDO_ENTRY);
            for (MercurySample mercurySample : tube.getMercurySamples()) {
                if (mapSampleNameToPdoSample.containsKey(mercurySample.getSampleKey())) {
                    mercurySample.addProductOrderSample(mapSampleNameToPdoSample.get(mercurySample.getSampleKey()));
                }
            }
        }
        return workingBucket;
    }

    /**
     * This method will create a bucket and populate the bucket with plates.
     *
     * @param mapBarcodeToPlate A map of barcode to plates that will be used to populate the bucket.
     * @param productOrder     The product order to use for the bucket entry.
     * @param bucketName       The name of the bucket to create.
     *
     * @return Returns a bucket populated with tubes.
     */
    protected Bucket createAndPopulatePlateBucket(Map<String, PlateWell> mapBarcodeToPlate, ProductOrder productOrder,
                                             String bucketName) {
        // Uses map of sample name to pdoSample for making the link between pdoSample to MercurySample.
        Map<String, ProductOrderSample> mapSampleNameToPdoSample = new HashMap<>();
        for (ProductOrderSample pdoSample : productOrder.getSamples()) {
            mapSampleNameToPdoSample.put(pdoSample.getSampleKey(), pdoSample);
        }
        Bucket workingBucket = new Bucket(bucketName);
        for (PlateWell plateWell : mapBarcodeToPlate.values()) {
            workingBucket.addEntry(productOrder, plateWell, BucketEntry.BucketEntryType.PDO_ENTRY);
            for (MercurySample mercurySample : plateWell.getMercurySamples()) {
                if (mapSampleNameToPdoSample.containsKey(mercurySample.getSampleKey())) {
                    mercurySample.addProductOrderSample(mapSampleNameToPdoSample.get(mercurySample.getSampleKey()));
                }
            }
        }
        return workingBucket;
    }


    protected LabEventHandler getLabEventHandler() {
        return new LabEventHandler();
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
        for (BarcodedTube twoDBarcodedTube : mapBarcodeToTube.values()) {
            twoDBarcodedTube.clearCaches();
        }

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

        for (BarcodedTube barcodedTube : mapBarcodeToTube.values()) {
            for (BucketEntry bucketEntry : barcodedTube.getBucketEntries()) {
                workflowBatch.addBucketEntry(bucketEntry);
            }
        }

        labBatchEJB.createLabBatch(workflowBatch, "scottmat", CreateFields.IssueType.EXOME_EXPRESS,
                CreateFields.ProjectType.LCSET_PROJECT);
        JiraServiceStub.setCreatedIssueSuffix(defaultLcsetSuffix);

        drainBucket(workingBucket);
        return workingBucket;
    }

    /**
     * @param mapBarcodeToPlate A map of barcodes to tubes that will be added to the bucket and drained into the
     *                         batch.
     * @param productOrder     The product order to use for bucket entries.
     * @param workflowBatch    The batch that will be used for this process.
     * @param lcsetSuffix      Set this non-null to override the lcset id number.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public Bucket bucketPlateBatchAndDrain(Map<String, PlateWell> mapBarcodeToPlate, final ProductOrder productOrder,
                                      LabBatch workflowBatch, String lcsetSuffix) {
        for (PlateWell plate : mapBarcodeToPlate.values()) {
            plate.clearCaches();
        }

        Bucket workingBucket = createAndPopulatePlateBucket(mapBarcodeToPlate, productOrder, "Pico/Plating Bucket");

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

        for (PlateWell staticPlate : mapBarcodeToPlate.values()) {
            for (BucketEntry bucketEntry : staticPlate.getBucketEntries()) {
                workflowBatch.addBucketEntry(bucketEntry);
            }
        }

        labBatchEJB.createLabBatch(workflowBatch, "scottmat", CreateFields.IssueType.EXOME_EXPRESS,
                CreateFields.ProjectType.LCSET_PROJECT);
        JiraServiceStub.setCreatedIssueSuffix(defaultLcsetSuffix);

        drainBucket(workingBucket);
        return workingBucket;
    }

    public void drainBucket(Bucket workingBucket) {
        bucketEjb.moveFromBucketToCommonBatch(workingBucket.getBucketEntries());
    }

    public void drainBucket(Bucket workingBucket, LabBatch labBatch) {
        bucketEjb.moveFromBucketToBatch(workingBucket.getBucketEntries(), labBatch);
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

        return runLibraryConstructionProcess(
                shearingCleanupPlate, shearCleanPlateBarcode, shearingPlate, barcodeSuffix, NUM_POSITIONS_IN_RACK);
    }

    /**
     * This method runs the entities through the library construction process.
     *
     * @param shearingCleanupPlate   The shearing cleanup plate from the shearing process.
     * @param shearCleanPlateBarcode The shearing clean plate barcode.
     * @param shearingPlate          The shearing plate from the shearing process.
     * @param barcodeSuffix          Uniquifies the generated vessel barcodes. NOT date if test quickly invokes twice.
     * @param numSamples             Number of samples run through the process.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public LibraryConstructionEntityBuilder runLibraryConstructionProcess(StaticPlate shearingCleanupPlate,
            String shearCleanPlateBarcode,
            StaticPlate shearingPlate,
            String barcodeSuffix,
            int numSamples) {
        return runLibraryConstructionProcess(shearingCleanupPlate, shearCleanPlateBarcode, shearingPlate, barcodeSuffix,
                numSamples, LibraryConstructionJaxbBuilder.PondType.REGULAR);
    }

    /**
     * This method runs the entities through the library construction process.
     *
     * @param shearingCleanupPlate   The shearing cleanup plate from the shearing process.
     * @param shearCleanPlateBarcode The shearing clean plate barcode.
     * @param shearingPlate          The shearing plate from the shearing process.
     * @param barcodeSuffix          Uniquifies the generated vessel barcodes. NOT date if test quickly invokes twice.
     * @param numSamples             Number of samples run through the process.
     * @param pondType               PCR Free, PCR Plus etc.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public LibraryConstructionEntityBuilder runLibraryConstructionProcess(StaticPlate shearingCleanupPlate,
            String shearCleanPlateBarcode,
            StaticPlate shearingPlate,
            String barcodeSuffix,
            int numSamples,
            LibraryConstructionJaxbBuilder.PondType pondType) {
        return new LibraryConstructionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                shearingCleanupPlate, shearCleanPlateBarcode, shearingPlate, numSamples, barcodeSuffix,
                LibraryConstructionEntityBuilder.Indexing.DUAL,
                pondType).invoke();
    }

    /**
     * This method runs the entities through the library construction process with UMI.
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public LibraryConstructionCellFreeUMIEntityBuilder runLibraryConstructionProcessWithUMI(
            Map<String, BarcodedTube> mapBarcodeToVessel, TubeFormation initialRack, LibraryConstructionEntityBuilder.Umi umi) {
        LibraryConstructionCellFreeUMIEntityBuilder builder = new LibraryConstructionCellFreeUMIEntityBuilder(
                mapBarcodeToVessel, initialRack, bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                NUM_POSITIONS_IN_RACK, "CellFreeUMI", umi);
        return builder.invoke();
    }

    public LibraryConstructionEntityBuilder runWgsLibraryConstructionProcessWithUMI(StaticPlate shearingCleanupPlate,
                                                                                 String shearCleanPlateBarcode,
                                                                                 StaticPlate shearingPlate,
                                                                                 String barcodeSuffix,
                                                                                 LibraryConstructionJaxbBuilder.PondType pondType,
                                                                                 LibraryConstructionEntityBuilder.Indexing indexing,
                                                                                 LibraryConstructionEntityBuilder.Umi umi) {
        LibraryConstructionEntityBuilder builder = new LibraryConstructionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                shearingCleanupPlate, shearCleanPlateBarcode, shearingPlate, NUM_POSITIONS_IN_RACK, barcodeSuffix,
                indexing,
                pondType, umi);
        builder.setIncludeUmi(true);
        return builder.invoke();
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
     * @param pondRegRacks         The pond registration racks coming out of the library construction process.
     * @param barcodeSuffix       Makes unique the generated vessel barcodes. Don't use date if test quickly invoked twice.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public IceEntityBuilder runIceProcess(List<TubeFormation> pondRegRacks, String barcodeSuffix) {
        return new IceEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(), pondRegRacks,
                barcodeSuffix, IceJaxbBuilder.PlexType.PLEX96, IceJaxbBuilder.PrepType.ICE).invoke();
    }

    /**
     * Creates an entity graph for HyperPrep Illumina Content Exome.
     *
     * @param pondRegRacks         The pond registration racks coming out of the library construction process.
     * @param barcodeSuffix       Makes unique the generated vessel barcodes. Don't use date if test quickly invoked twice.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public IceEntityBuilder runHyperPrepIceProcess(List<TubeFormation> pondRegRacks, String barcodeSuffix) {
        return new IceEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(), pondRegRacks,
                barcodeSuffix, IceJaxbBuilder.PlexType.PLEX96, IceJaxbBuilder.PrepType.HYPER_PREP_ICE).invoke();
    }

    /**
     * Creates an entity graph for Custom Selection.
     *
     * @param pondRegRacks        The pond registration racks coming out of the library construction process.
     * @param barcodeSuffix       Makes unique the generated vessel barcodes. Don't use date if test quickly invoked twice.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public SelectionEntityBuilder runSelectionProcess(List<TubeFormation> pondRegRacks, String barcodeSuffix) {
        return new SelectionEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(), pondRegRacks,
                barcodeSuffix).invoke();
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
                Collections.singletonList(rack.getRacksOfTubes().iterator().next().getLabel()),
                Collections.singletonList(tubeBarcodes),
                mapBarcodeToTube, barcodeSuffix).invoke();
    }

    public QtpEntityBuilder runQtpProcess(TubeFormation rack, List<String> tubeBarcodes,
                                          Map<String, BarcodedTube> mapBarcodeToTube,
                                          String barcodeSuffix, QtpJaxbBuilder.PcrType pcrType) {

        return new QtpEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                Collections.singletonList(rack),
                Collections.singletonList(rack.getRacksOfTubes().iterator().next().getLabel()),
                Collections.singletonList(tubeBarcodes),
                mapBarcodeToTube, barcodeSuffix).invoke(true, pcrType);
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
                                                                      String designationName, String workflow) {
        int flowcellLanes = 8;
        if (workflow.equals(Workflow.AGILENT_EXOME_EXPRESS)) {
            flowcellLanes = 2;
        }
        String flowcellBarcode = "flowcell" + new Date().getTime() + "ADXX";
        return new HiSeq2500FlowcellEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                                                  denatureRack, flowcellBarcode, barcodeSuffix, fctTicket,
                                                  productionFlowcellPath,
                                                  designationName, flowcellLanes).invoke();
    }

    /**
     * This method runs the entities through the HiSeq4000 process.
     *
     * @param denatureRack           The denature tube rack.
     * @param barcodeSuffix          Uniquifies the generated vessel barcodes. NOT date if test quickly invokes twice.
     * @param fctTicket
     * @param designationName        Name of the designation created in Squid to support testing the systems running in
 *                               parallel
*    @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public HiSeq4000FlowcellEntityBuilder runHiSeq4000FlowcellProcess(TubeFormation denatureRack, TubeFormation normRack,
                                                                      String barcodeSuffix, LabBatch fctTicket,
                                                                      String designationName,
                                                                      HiSeq4000FlowcellEntityBuilder.FCTCreationPoint fctCreationPoint) {
        String flowcellBarcode = "flowcell" + new Date().getTime() + "BBXX";
        return new HiSeq4000FlowcellEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                denatureRack, flowcellBarcode, barcodeSuffix, fctTicket,
                designationName, 8, normRack, fctCreationPoint).invoke();
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

    public FPEntityBuilder runFPProcess(List<StaticPlate> sourcePlates, int numSamples, String barcodeSuffix) {
        return new FPEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                sourcePlates, numSamples, barcodeSuffix).invoke();
    }

    public SingleCellSmartSeqEntityBuilder runSingleCellSmartSeqProcess(List<StaticPlate> sourcePlates, int numSamples, String barcodeSuffix) {
        return new SingleCellSmartSeqEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                sourcePlates, numSamples, barcodeSuffix).invoke();
    }

    public SingleCell10XEntityBuilder runSingleCell10XProcess(Map<String, BarcodedTube> mapBarcodeToTube,
                                                              TubeFormation rack, String rackBarcode, String barcodeSuffix) {
        return new SingleCell10XEntityBuilder(mapBarcodeToTube, rack, bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                rackBarcode, barcodeSuffix).invoke();
    }

    public SingleCellVdjEnrichmentEntityBuilder runSingleCellVdjProcess(TubeFormation tcrRack, TubeFormation bcrRack,
                                                                        Map<String, BarcodedTube> tcrTubeMap, Map<String, BarcodedTube> bcrTubeMap,
                                                                        String barcodeSuffix) {
        return new SingleCellVdjEnrichmentEntityBuilder(tcrRack, bcrRack, tcrTubeMap, bcrTubeMap, bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                barcodeSuffix).invoke();
    }

    public SingleCellHashingEntityBuilder runSingleCellHashingProcess(Map<String, BarcodedTube> mapBarcodeToTube,
                                                                    TubeFormation rack, String rackBarcode,
                                                                    String barcodeSuffix) {
        return new SingleCellHashingEntityBuilder(mapBarcodeToTube, rack, bettaLimsMessageTestFactory, labEventFactory,
                getLabEventHandler(), rackBarcode, barcodeSuffix).invoke();
    }

    public InfiniumEntityBuilder runInfiniumProcess(StaticPlate sourcePlate, String barcodeSuffix) {
        return runInfiniumProcessWithMethylation(sourcePlate, barcodeSuffix,
                InfiniumJaxbBuilder.IncludeMethylation.FALSE);
    }

    public InfiniumEntityBuilder runInfiniumProcessWithMethylation(
            StaticPlate sourcePlate, String barcodeSuffix, InfiniumJaxbBuilder.IncludeMethylation includeMethylation) {
        return new InfiniumEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                sourcePlate, barcodeSuffix, includeMethylation).invoke();
    }

    public ArrayPlatingEntityBuilder runArrayPlatingProcess( Map<String, BarcodedTube> mapBarcodeToTube,
                                                             String barcodeSuffix) {
        return new ArrayPlatingEntityBuilder(mapBarcodeToTube, bettaLimsMessageTestFactory,
                labEventFactory, getLabEventHandler(), barcodeSuffix).invoke();
    }

    public TenXEntityBuilder runTenXProcess(Map<String, BarcodedTube> mapBarcodeToTube,
                                                    String barcodeSuffix) {
        return new TenXEntityBuilder(mapBarcodeToTube, bettaLimsMessageTestFactory,
                labEventFactory, getLabEventHandler(), barcodeSuffix).invoke();
    }

    public CrspRiboPlatingEntityBuilder runRiboPlatingProcess(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                                       LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                                       Map<String, BarcodedTube> mapBarcodeToTube, String rackBarcode,
                                                       String prefix) {
        return new CrspRiboPlatingEntityBuilder(bettaLimsMessageTestFactory,
                labEventFactory, labEventHandler, mapBarcodeToTube, rackBarcode, prefix).invoke();
    }

    public StoolTNAEntityBuilder runStoolExtractionToTNAProcess(StaticPlate sourcePlate,
                                                                int numSamples, String prefix) {
        return new StoolTNAEntityBuilder(sourcePlate, numSamples, bettaLimsMessageTestFactory,
                labEventFactory, getLabEventHandler(), prefix).invoke();
    }

    public FingerprintingEntityBuilder runFingerprintingProcess(StaticPlate sourcePlate, String barcodeSuffix) {
        return new FingerprintingEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                sourcePlate, barcodeSuffix).invoke();
    }

    /**
     * This method runs the entities through the TruSeqStrandSpecific process.
     *
     * @param mapBarcodeToTube A map of barcodes to tubes that will be run the starting point of the TruSeq SS process.
     * @param tubeFormation    The tube formation that represents the entities coming out of pico/plating.
     * @param rackBarcode      The rack barcode of the tube formation.
     * @param barcodeSuffix    Uniquifies the generated vessel barcodes. NOT date if test quickly invokes twice.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public TruSeqStrandSpecificEntityBuilder runTruSeqStrandSpecificProcess(Map<String, BarcodedTube> mapBarcodeToTube,
                                                    TubeFormation tubeFormation,
                                                    String rackBarcode,
                                                    String barcodeSuffix) {

        return new TruSeqStrandSpecificEntityBuilder(mapBarcodeToTube, tubeFormation,
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(),
                rackBarcode, barcodeSuffix).invoke();
    }

    /**
     * Simulates a BSP daughter plate transfer, prior to export from BSP to Mercury, then does a re-array to add
     * controls.
     *
     * @param mapBarcodeToTube source tubes
     *
     * @param workflowBatch
     * @return destination tube formation
     */
    public TubeFormation daughterPlateTransfer(Map<String, BarcodedTube> mapBarcodeToTube, LabBatch workflowBatch) {
        List<String> daughterTubeBarcodes = generateDaughterTubeBarcodes(mapBarcodeToTube);
        return getDaughterTubeFormation(mapBarcodeToTube, daughterTubeBarcodes, workflowBatch);
    }

    /**
     * Simulate a BSP daughter plate transfer without adding controls
     */
    public TubeFormation daughterPlateTransferNoWorkflow(Map<String, BarcodedTube> mapBarcodeToTube, String prefix) {
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
     * @param workflowBatch
     * @return destination tube formation
     */
    public TubeFormation mismatchedDaughterPlateTransfer(Map<String, BarcodedTube> mapBarcodeToTube,
            Map<String, BarcodedTube> mapBarcodeToTube2,
            List<Integer> wellsToReplace, LabBatch workflowBatch) {
        List<String> daughterTubeBarcodes = generateDaughterTubeBarcodes(mapBarcodeToTube);
        return getDaughterTubeFormationCherryPick(mapBarcodeToTube, mapBarcodeToTube2, daughterTubeBarcodes,
                                                  wellsToReplace, workflowBatch);
    }

    /**
     * Generates the daughter tube barcodes for the destination tube formation .
     *
     * @param mapBarcodeToTube source tubes
     *
     * @return list of daughter tube barcodes
     */
    private List<String> generateDaughterTubeBarcodes(Map<String, BarcodedTube> mapBarcodeToTube) {
        return generateDaughterTubeBarcodes(mapBarcodeToTube, "D");
    }

    /**
     * Generates the daughter tube barcodes for the destination tube formation .
     *
     * @param mapBarcodeToTube source tubes
     *
     * @return list of daughter tube barcodes
     */
    private List<String> generateDaughterTubeBarcodes(Map<String, BarcodedTube> mapBarcodeToTube, String prefix) {
        // Daughter plate transfer that doesn't include controls
        List<String> daughterTubeBarcodes = new ArrayList<>();
        for (int i = 0; i < mapBarcodeToTube.size(); i++) {
            daughterTubeBarcodes.add(prefix + i);
        }
        return daughterTubeBarcodes;
    }

    /**
     * Creates a daughter tube formation using a rack to rack transfer.
     *
     * @param mapBarcodeToTube     source tubes
     * @param daughterTubeBarcodes destination tubes
     *
     * @param workflowBatch
     * @return destination tube formation
     */
    private TubeFormation getDaughterTubeFormation(Map<String, BarcodedTube> mapBarcodeToTube,
            List<String> daughterTubeBarcodes, LabBatch workflowBatch) {
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
        BspSampleData bspSampleDataPos = new BspSampleData(
                new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                    put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, POSITIVE_CONTROL);
                }});
        posControlTube.addSample(new MercurySample(POSITIVE_CONTROL, bspSampleDataPos));
        nameToSampleData.put(POSITIVE_CONTROL, bspSampleDataPos);
        mapBarcodeToDaughterTube.put(VesselPosition.H11, posControlTube);
        workflowBatch.addLabVessel(posControlTube);

        BarcodedTube negControlTube = new BarcodedTube("C2");
        BspSampleData bspSampleDataNeg = new BspSampleData(
                new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                    put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, NEGATIVE_CONTROL);
                }});
        negControlTube.addSample(new MercurySample(NEGATIVE_CONTROL, bspSampleDataNeg));
        nameToSampleData.put(NEGATIVE_CONTROL, bspSampleDataNeg);
        mapBarcodeToDaughterTube.put(VesselPosition.H12, negControlTube);
        workflowBatch.addLabVessel(negControlTube);

        return new TubeFormation(mapBarcodeToDaughterTube, RackOfTubes.RackType.Matrix96);
    }

    private TubeFormation getDaughterTubeFormation(Map<String, BarcodedTube> mapBarcodeToTube, List<String> daughterTubeBarcodes) {
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
            List<Integer> wellsToReplace,
            LabBatch workflowBatch) {

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
        BspSampleData bspSampleDataPos = new BspSampleData(
                new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                    put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, POSITIVE_CONTROL);
                    put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, POSITIVE_CONTROL);
                }});
        posControlTube.addSample(new MercurySample(POSITIVE_CONTROL, bspSampleDataPos));
        nameToSampleData.put(POSITIVE_CONTROL, bspSampleDataPos);
        mapBarcodeToDaughterTube.put(VesselPosition.H11, posControlTube);
        workflowBatch.addLabVessel(posControlTube);

        BarcodedTube negControlTube = new BarcodedTube("C2");
        BspSampleData bspSampleDataNeg = new BspSampleData(
                new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                    put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, NEGATIVE_CONTROL);
                    put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, NEGATIVE_CONTROL);
                }});
        negControlTube.addSample(new MercurySample(NEGATIVE_CONTROL, bspSampleDataNeg));
        nameToSampleData.put(NEGATIVE_CONTROL, bspSampleDataNeg);
        mapBarcodeToDaughterTube.put(VesselPosition.H12, negControlTube);
        workflowBatch.addLabVessel(negControlTube);

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
        try {
            TransferVisualizerV2 transferVisualizerV2 = new TransferVisualizerV2();
            File xfrVis = File.createTempFile("XfrVis", ".json");
            FileWriter fileWriter = new FileWriter(xfrVis);
            transferVisualizerV2.jsonForVessels(
                    Collections.singletonList(labVessel),
                    Arrays.asList(TransferTraverserCriteria.TraversalDirection.Ancestors,
                            TransferTraverserCriteria.TraversalDirection.Descendants),
                    fileWriter,
                    Arrays.asList(TransferVisualizerV2.AlternativeIds.SAMPLE_ID,
                            TransferVisualizerV2.AlternativeIds.INFERRED_LCSET));
            fileWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void validateWorkflow(String nextEventTypeName, Collection<? extends LabVessel> tubes) {
        List<LabVessel> labVessels = new ArrayList<>(tubes);
        validateWorkflow(nextEventTypeName, labVessels);
    }

    public static void validateWorkflow(String nextEventTypeName, LabVessel labVessel) {
        validateWorkflow(nextEventTypeName, Collections.singletonList(labVessel));
    }

    public static void validateWorkflow(String nextEventTypeName, List<LabVessel> labVessels) {
        WorkflowConfig workflowConfig = new WorkflowLoader().getWorkflowConfig();

        // All messages are now routed to Mercury.
        Assert.assertEquals(SystemOfRecord.System.MERCURY, expectedRouting);

        WorkflowValidator workflowValidator = new WorkflowValidator();
        workflowValidator.setWorkflowConfig(workflowConfig);
        ProductOrderDao mockProductOrderDao = Mockito.mock(ProductOrderDao.class);
        Mockito.when(mockProductOrderDao.findByBusinessKey(Mockito.anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {

                Object[] arguments = invocationOnMock.getArguments();

                return ProductOrderTestFactory.createDummyProductOrder((String) arguments[0]);
            }
        });

        workflowValidator.setProductOrderDao(mockProductOrderDao);
        List<WorkflowValidator.WorkflowValidationError> workflowValidationErrors =
                workflowValidator.validateWorkflow(labVessels, nextEventTypeName);
        if (!workflowValidationErrors.isEmpty()) {
            WorkflowValidator.WorkflowValidationError workflowValidationError = workflowValidationErrors.get(0);
            ProductWorkflowDefVersion.ValidationError validationError = workflowValidationError.getErrors().get(0);
            Assert.fail(validationError.getMessage() + " expected " + validationError.getExpectedEventNames() +
                        " actual " + validationError.getActualEventNames());
        }
    }

    public ZimsIlluminaRunFactory constructZimsIlluminaRunFactory(final ProductOrder productOrder,
                                                           List<FlowcellDesignation> flowcellDesignations) {
        ProductOrderDao productOrderDao = Mockito.mock(ProductOrderDao.class);
        FlowcellDesignationEjb flowcellDesignationEjb = Mockito.mock(FlowcellDesignationEjb.class);
        Mockito.when(productOrderDao.findByBusinessKey(Mockito.anyString())).thenReturn(productOrder);
        Mockito.when(flowcellDesignationEjb.getFlowcellDesignations(Mockito.any(LabBatch.class))).
                thenReturn(flowcellDesignations);
        AttributeArchetypeDao attributeArchetypeDao = Mockito.mock(AttributeArchetypeDao.class);
        Mockito.when(attributeArchetypeDao.findWorkflowMetadata(Mockito.anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return null;
            }
        });
        SequencingTemplateFactory sequencingTemplateFactory = new SequencingTemplateFactory();
        sequencingTemplateFactory.setFlowcellDesignationEjb(flowcellDesignationEjb);
        sequencingTemplateFactory.setWorkflowConfig(new WorkflowLoader().getWorkflowConfig());
        return new ZimsIlluminaRunFactory(
                new SampleDataFetcher() {
                    @Override
                    public Map<String, SampleData> fetchSampleData(@Nonnull Collection<String> sampleNames) {
                       return nameToSampleData;
                    }
                },
                new ControlDao() {
                    @Override
                    public List<Control> findAllActive() {
                        return controlList;
                    }
                },
                sequencingTemplateFactory,
                productOrderDao,
                crspPipelineUtils, flowcellDesignationEjb, attributeArchetypeDao
        );
    }

    public LabBatchEjb getLabBatchEJB() {
        return labBatchEJB;
    }
}
