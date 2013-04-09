package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.test.builders.*;
import org.easymock.EasyMock;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.BeforeClass;

import java.util.*;

public class BaseEventTest {
    public static final int NUM_POSITIONS_IN_RACK = 96;

    private BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();

    private LabEventFactory labEventFactory = new LabEventFactory();

    private LabBatchEjb labBatchEJB;

    private JiraTicketDao mockJira;

    private BucketBean bucketBeanEJB;

    public LabBatchEjb getLabBatchEJB() {
        return labBatchEJB;
    }

    public void setLabBatchEJB(LabBatchEjb labBatchEJB) {
        this.labBatchEJB = labBatchEJB;
    }

    public JiraTicketDao getMockJira() {
        return mockJira;
    }

    public void setMockJira(JiraTicketDao mockJira) {
        this.mockJira = mockJira;
    }

    public BettaLimsMessageTestFactory getBettaLimsMessageTestFactory() {
        return bettaLimsMessageTestFactory;
    }

    public void setBettaLimsMessageTestFactory(BettaLimsMessageTestFactory bettaLimsMessageTestFactory) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
    }

    public LabEventFactory getLabEventFactory() {
        return labEventFactory;
    }

    public void setLabEventFactory(LabEventFactory labEventFactory) {
        this.labEventFactory = labEventFactory;
    }

    public BucketBean getBucketBeanEJB() {
        return bucketBeanEJB;
    }

    public void setBucketBeanEJB(BucketBean bucketBeanEJB) {
        this.bucketBeanEJB = bucketBeanEJB;
    }

    protected final LabEventFactory.LabEventRefDataFetcher labEventRefDataFetcher =
            new LabEventFactory.LabEventRefDataFetcher() {

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
        labBatchEJB.setAthenaClientService(AthenaClientProducer.stubInstance());
        labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());

        mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
        labBatchEJB.setJiraTicketDao(mockJira);
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);
        bucketBeanEJB = new BucketBean(getLabEventFactory(), JiraServiceProducer.stubInstance(), getLabBatchEJB());
    }

    public Map<String, TwoDBarcodedTube> createInitialRack(ProductOrder productOrder) {
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        int rackPosition = 1;
        for (ProductOrderSample poSample : productOrder.getSamples()) {
            String barcode = "R" + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(productOrder.getBusinessKey(), poSample.getSampleName()));
            mapBarcodeToTube.put(barcode, bspAliquot);
            if(rackPosition > NUM_POSITIONS_IN_RACK){
                break;
            }
            rackPosition++;
        }
        return mapBarcodeToTube;
    }

    protected Bucket createAndPopulateBucket(Map<String, TwoDBarcodedTube> mapBarcodeToTube, ProductOrder productOrder,
                                           String bucketName) {
        Bucket workingBucket = new Bucket(bucketName);

        for (TwoDBarcodedTube tube : mapBarcodeToTube.values()) {
            workingBucket.addEntry(productOrder.getBusinessKey(), tube);
        }
        return workingBucket;
    }

    public PicoPlatingEntityBuilder runPicoPlatingProcess(Map<String, TwoDBarcodedTube> mapBarcodeToTube, ProductOrder productOrder,
                                                          LabBatch workflowBatch) {
        String rackBarcode = "REXEX" + new Date().toString();
        getLabBatchEJB().createLabBatch(workflowBatch, "scottmat");

        Bucket workingBucket = createAndPopulateBucket(mapBarcodeToTube, productOrder, "Pico/Plating Bucket");

        BucketDao mockBucketDao = EasyMock.createMock(BucketDao.class);
        EasyMock.expect(mockBucketDao.findByName("Pico/Plating Bucket")).andReturn(workingBucket);
        EasyMock.expect(mockBucketDao.findByName(EasyMock.anyObject(String.class))).andReturn(new Bucket("FAKEBUCKET"));
        EasyMock.replay(mockBucketDao);

        return new PicoPlatingEntityBuilder(bettaLimsMessageTestFactory,
                labEventFactory, getLabEventHandler(mockBucketDao),
                mapBarcodeToTube, rackBarcode).invoke();
    }

    public ExomeExpressShearingEntityBuilder runExomeExpressShearingProcess(ProductOrder productOrder,
                                                                            PicoPlatingEntityBuilder picoPlatingEntityBuilder) {
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = picoPlatingEntityBuilder.getNormBarcodeToTubeMap();
        Bucket workingBucket = createAndPopulateBucket(mapBarcodeToTube, productOrder, "Shearing Bucket");

        BucketDao mockBucketDao = EasyMock.createNiceMock(BucketDao.class);
        EasyMock.expect(mockBucketDao.findByName("Shearing Bucket")).andReturn(workingBucket);
        EasyMock.expect(mockBucketDao.findByName(EasyMock.anyObject(String.class))).andReturn(new Bucket("FAKEBUCKET"));
        EasyMock.replay(mockBucketDao);

        return new ExomeExpressShearingEntityBuilder(mapBarcodeToTube,
                picoPlatingEntityBuilder.getNormTubeFormation(), bettaLimsMessageTestFactory, labEventFactory,
                getLabEventHandler(mockBucketDao), picoPlatingEntityBuilder.getNormalizationBarcode()).invoke();
    }

    public PreFlightEntityBuilder runPreflightProcess(Map<String, TwoDBarcodedTube> mapBarcodeToTube, ProductOrder productOrder,
                                                      LabBatch workflowBatch) {
        getLabBatchEJB().createLabBatch(workflowBatch, "scotmatt");

        Bucket workingBucket = createAndPopulateBucket(mapBarcodeToTube, productOrder, "Preflight Bucket");

        BucketDao mockBucketDao = EasyMock.createNiceMock(BucketDao.class);
        EasyMock.expect(mockBucketDao.findByName("Preflight Bucket")).andReturn(workingBucket);
        EasyMock.expect(mockBucketDao.findByName(EasyMock.anyObject(String.class))).andReturn(new Bucket("FAKEBUCKET"));
        EasyMock.replay(mockBucketDao);

        return new PreFlightEntityBuilder(bettaLimsMessageTestFactory,
                labEventFactory, getLabEventHandler(mockBucketDao),
                mapBarcodeToTube).invoke();
    }

    public ShearingEntityBuilder runShearingProcess(Map<String, TwoDBarcodedTube> mapBarcodeToTube, PreFlightEntityBuilder preFlightEntityBuilder) {

        return new ShearingEntityBuilder(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(null), preFlightEntityBuilder.getRackBarcode()).invoke();
    }

    public LibraryConstructionEntityBuilder runLibraryConstructionProcess(ShearingEntityBuilder shearingEntityBuilder) {

        return new LibraryConstructionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(null),
                shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                shearingEntityBuilder.getShearingPlate(), NUM_POSITIONS_IN_RACK).invoke();
    }

    public HybridSelectionEntityBuilder runHybridSelectionProcess(LibraryConstructionEntityBuilder libraryConstructionEntityBuilder) {

        return new HybridSelectionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(null),
                libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes()).invoke();
    }
    public QtpEntityBuilder runQtpProcess(TubeFormation rack, List<String> tubeBarcodes,
                                          Map<String, TwoDBarcodedTube> mapBarcodeToTube, WorkflowName workflowName) {

        return new QtpEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(null),
                Collections.singletonList(rack),
                Collections.singletonList(rack.getLabel()),
                Collections.singletonList(tubeBarcodes),
                mapBarcodeToTube, workflowName).invoke();
    }

    protected LabEventHandler getLabEventHandler(@Nullable BucketDao bucketDAO) {
        AthenaClientService athenaClientService = AthenaClientProducer.stubInstance();
        return new LabEventHandler(new WorkflowLoader(), athenaClientService, bucketBeanEJB, bucketDAO,
                        new BSPUserList(BSPManagerFactoryProducer.stubInstance()));
    }

    public  LibraryConstructionEntityBuilder runLibraryConstructionProcess(ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder) {
        return new LibraryConstructionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(null),
                exomeExpressShearingEntityBuilder.getShearingCleanupPlate(), exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                exomeExpressShearingEntityBuilder.getShearingPlate(), NUM_POSITIONS_IN_RACK).invoke();
    }

    public HiSeq2500FlowcellEntityBuilder runHiSeq2500FlowcellProcess(QtpEntityBuilder qtpEntityBuilder){

        String flowcellBarcode = "flowcell" + new Date().getTime();
        return new HiSeq2500FlowcellEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(null),
                        qtpEntityBuilder.getDenatureRack(), flowcellBarcode).invoke();
    }

    public SageEntityBuilder runSageProcess(LibraryConstructionEntityBuilder libraryConstructionEntityBuilder){
        return new SageEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(null),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(), libraryConstructionEntityBuilder.getPondRegRack(),
               libraryConstructionEntityBuilder.getPondRegTubeBarcodes()).invoke();
    }
}
