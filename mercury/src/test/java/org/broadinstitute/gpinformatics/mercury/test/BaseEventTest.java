package org.broadinstitute.gpinformatics.mercury.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.test.builders.*;
import org.easymock.EasyMock;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.BeforeClass;

import java.util.*;

/**
 * This class handles setting up various factories and EJBs for use in any lab event test.
 */
public class BaseEventTest {
    private static Log log = LogFactory.getLog(BaseEventTest.class);

    public static final int NUM_POSITIONS_IN_RACK = 96;

    private BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();

    private LabEventFactory labEventFactory = new LabEventFactory();

    private LabBatchEjb labBatchEJB;

    private BucketBean bucketBeanEJB;

    public LabBatchEjb getLabBatchEJB() {
        return labBatchEJB;
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

        JiraTicketDao mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
        labBatchEJB.setJiraTicketDao(mockJira);
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);
        bucketBeanEJB = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance(), getLabBatchEJB());
    }

    /**
     * This method builds the initial tube rack for starting an event test.  This will log an error if you attempt to
     * create the rack from a product order than has more than NUM_POSITIONS_IN_RACK tubes.
     *
     * @param productOrder The product order to create the initial rack from
     * @return Returns a map of String barcodes to their tube objects.
     */
    public Map<String, TwoDBarcodedTube> createInitialRack(ProductOrder productOrder) {
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        int rackPosition = 1;
        for (ProductOrderSample poSample : productOrder.getSamples()) {
            String barcode = "R" + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(productOrder.getBusinessKey(), poSample.getSampleName()));
            mapBarcodeToTube.put(barcode, bspAliquot);
            if (rackPosition > NUM_POSITIONS_IN_RACK) {
                log.error("More product order samples than allowed in a single rack. " + productOrder.getSamples().size()
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
     * @return Returns a bucket populated with tubes.
     */
    protected Bucket createAndPopulateBucket(Map<String, TwoDBarcodedTube> mapBarcodeToTube, ProductOrder productOrder,
                                             String bucketName) {
        Bucket workingBucket = new Bucket(bucketName);

        for (TwoDBarcodedTube tube : mapBarcodeToTube.values()) {
            workingBucket.addEntry(productOrder.getBusinessKey(), tube);
        }
        return workingBucket;
    }

    /**
     * This method runs the entities through the pico/plating process.
     *
     * @param mapBarcodeToTube A map of barcodes to tubes that will be run the starting point of the pico/plating process.
     * @param productOrder     The product order to use for bucket entries.
     * @param workflowBatch    The batch that will be used for this process.
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
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

    /**
     * This method runs the entities through the ExEx shearing process.
     *
     * @param productOrder         The product order to use for bucket entries.
     * @param normBarcodeToTubeMap A map of barcodes to tubes that will be run the starting point of the ExEx shearing process.
     * @param normTubeFormation    The tube formation that represents the entities coming out of pico/plating.
     * @param normBarcode          The rack barcode of the tube formation.
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public ExomeExpressShearingEntityBuilder runExomeExpressShearingProcess(ProductOrder productOrder,
                                                                            Map<String, TwoDBarcodedTube> normBarcodeToTubeMap,
                                                                            TubeFormation normTubeFormation,
                                                                            String normBarcode) {
        Bucket workingBucket = createAndPopulateBucket(normBarcodeToTubeMap, productOrder, "Shearing Bucket");

        BucketDao mockBucketDao = EasyMock.createNiceMock(BucketDao.class);
        EasyMock.expect(mockBucketDao.findByName("Shearing Bucket")).andReturn(workingBucket);
        EasyMock.expect(mockBucketDao.findByName(EasyMock.anyObject(String.class))).andReturn(new Bucket("FAKEBUCKET"));
        EasyMock.replay(mockBucketDao);

        return new ExomeExpressShearingEntityBuilder(normBarcodeToTubeMap, normTubeFormation, bettaLimsMessageTestFactory, labEventFactory,
                getLabEventHandler(mockBucketDao), normBarcode).invoke();
    }

    /**
     * This method runs the entities through the preflight process.
     *
     * @param mapBarcodeToTube A map of barcodes to tubes that will be run the starting point of the preflight process.
     * @param productOrder     The product order to use for bucket entries.
     * @param workflowBatch    The batch that will be used for this process.
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
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

    /**
     * This method runs the entities through the shearing process.
     *
     * @param mapBarcodeToTube A map of barcodes to tubes that will be run the starting point of the shearing process.
     * @param tubeFormation    The tube formation that represents the entities coming out of pico/plating.
     * @param rackBarcode      The rack barcode of the tube formation.
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public ShearingEntityBuilder runShearingProcess(Map<String, TwoDBarcodedTube> mapBarcodeToTube, TubeFormation tubeFormation,
                                                    String rackBarcode) {

        return new ShearingEntityBuilder(mapBarcodeToTube, tubeFormation,
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(null), rackBarcode).invoke();
    }

    /**
     * This method runs the entities through the library construction process.
     *
     * @param shearingCleanupPlate   The shearing cleanup plate from the shearing process.
     * @param shearCleanPlateBarcode The shearing clean plate barcode.
     * @param shearingPlate          The shearing plate from the shearing process.
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public LibraryConstructionEntityBuilder runLibraryConstructionProcess(StaticPlate shearingCleanupPlate,
                                                                          String shearCleanPlateBarcode, StaticPlate shearingPlate) {

        return new LibraryConstructionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(null),
                shearingCleanupPlate, shearCleanPlateBarcode,
                shearingPlate, NUM_POSITIONS_IN_RACK).invoke();
    }

    /**
     * This method runs the entities through the hybrid selection process.
     *
     * @param pondRegRack         The pond registration rack coming out of the library construction process.
     * @param pondRegRackBarcode  The pond registration rack barcode.
     * @param pondRegTubeBarcodes A list of pond registration tube barcodes.
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public HybridSelectionEntityBuilder runHybridSelectionProcess(TubeFormation pondRegRack, String pondRegRackBarcode,
                                                                  List<String> pondRegTubeBarcodes) {

        return new HybridSelectionEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(null),
                pondRegRack, pondRegRackBarcode, pondRegTubeBarcodes).invoke();
    }

    /**
     * This method runs the entities through the QTP process.
     *
     * @param rack             The tube rack coming out of hybrid selection
     * @param tubeBarcodes     A list of the tube barcodes in the rack.
     * @param mapBarcodeToTube A map of barcodes to tubes that will be run the starting point of the pico/plating process.
     * @param workflowName     The workflow name for the current workflow.
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
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

    /**
     * This method runs the entities through the HiSeq2500 process.
     *
     * @param denatureRack The denature tube rack.
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public HiSeq2500FlowcellEntityBuilder runHiSeq2500FlowcellProcess(TubeFormation denatureRack) {

        String flowcellBarcode = "flowcell" + new Date().getTime();
        return new HiSeq2500FlowcellEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(null),
                denatureRack, flowcellBarcode).invoke();
    }

    /**
     * This method runs the entities through the Sage process.
     *
     * @param pondRegRack         The pond registration rack coming out of the library construction process.
     * @param pondRegRackBarcode  The pond registration rack barcode.
     * @param pondRegTubeBarcodes A list of pond registration tube barcodes.
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public SageEntityBuilder runSageProcess(TubeFormation pondRegRack, String pondRegRackBarcode, List<String> pondRegTubeBarcodes) {
        return new SageEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, getLabEventHandler(null),
                pondRegRackBarcode, pondRegRack, pondRegTubeBarcodes).invoke();
    }
}
