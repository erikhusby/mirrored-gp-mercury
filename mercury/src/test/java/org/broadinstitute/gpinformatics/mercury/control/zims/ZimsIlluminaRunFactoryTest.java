package org.broadinstitute.gpinformatics.mercury.control.zims;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaRunConfiguration;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.xml.datatype.DatatypeFactory;
import java.util.*;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.*;
import static org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell.FLOWCELL_TYPE.EIGHT_LANE;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes.RackType.Matrix96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.ALL96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition.B04;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * @author breilly
 */
public class ZimsIlluminaRunFactoryTest {

    private ZimsIlluminaRunFactory zimsIlluminaRunFactory;
    private LabEventFactory labEventFactory;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() throws Exception {
        ProductWorkflowDef testWorkflow = new ProductWorkflowDef("Test Workflow");
        ProductWorkflowDefVersion productWorkflowDefVersion = new ProductWorkflowDefVersion("1", new Date());
        testWorkflow.addProductWorkflowDefVersion(productWorkflowDefVersion);
        WorkflowProcessDef testProcess = new WorkflowProcessDef("Test Process");
        WorkflowProcessDefVersion testProcess1 = new WorkflowProcessDefVersion("1", new Date());
        testProcess.addWorkflowProcessDefVersion(testProcess1);
        WorkflowStepDef step1 = new WorkflowStepDef("Step 1");
        step1.addLabEvent(A_BASE);
        testProcess1.addStep(step1);
        WorkflowStepDef step2 = new WorkflowStepDef("Step 2"); // TODO: signify that this step is important to picard
        step2.addLabEvent(INDEXED_ADAPTER_LIGATION);
        testProcess1.addStep(step2);
        WorkflowStepDef step3 = new WorkflowStepDef("Step 3");
        step3.addLabEvent(POOLING_TRANSFER);
        testProcess1.addStep(step3);
        productWorkflowDefVersion.addWorkflowProcessDef(testProcess);

        Product testProduct = new Product("Test Product", new ProductFamily("Test Product Family"), "Test product",
                "P-TEST-1", new Date(), new Date(), 0, 0, 0, 0, "Test samples only", "None", true,
                "Test Workflow", false);

        zimsIlluminaRunFactory = new ZimsIlluminaRunFactory();
        labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(new LabEventFactory.LabEventRefDataFetcher() {
            @Override
            public BspUser getOperator(String userId) {
                return new BSPUserList.QADudeUser("Test", 101L);
            }

            @Override
            public BspUser getOperator(Long bspUserId) {
                return new BSPUserList.QADudeUser("Test", bspUserId);
            }

            @Override
            public LabBatch getLabBatch(String labBatchName) {
                return null;
            }
        });
    }

    @Test(groups = DATABASE_FREE)
    public void testMakeZimsIlluminaRun() throws Exception {
        PlateCherryPickEvent stripTubeBTransferEvent = new PlateCherryPickEvent();
        stripTubeBTransferEvent.setEventType("StripTubeBTransfer");
        stripTubeBTransferEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2013-01-23T14:30:00-05:00"));
        PositionMapType positionMap = new PositionMapType();
        ReceptacleType stripTube = new ReceptacleType();
        stripTube.setReceptacleType("StripTube");
        positionMap.getReceptacle().add(stripTube);
        stripTubeBTransferEvent.setPositionMap(positionMap);
        TwoDBarcodedTube testTube = new TwoDBarcodedTube("testTube");
        Map<String, TubeFormation> barcodeToSourceTubeFormation = Collections.singletonMap("testRack", new TubeFormation(Collections.singletonMap(B04, testTube), Matrix96));
        Map<String, TwoDBarcodedTube> barcodeToSourceTube = Collections.singletonMap("B04", testTube);
        Map<String, TubeFormation> barcodeToTargetTubeFormation = null; // not even used by buildCherryPickRackToStripTubeDbFree!!!
        Map<String, StripTube> barcodeToTargetStripTube = Collections.singletonMap("testStripTube", new StripTube("testStripTube"));
        Map<String, RackOfTubes> barcodeToSourceRackOfTubes = Collections.singletonMap("testRack", new RackOfTubes("testRack", Matrix96));
        LabEvent stripTubeBTransfer = labEventFactory.buildCherryPickRackToStripTubeDbFree(stripTubeBTransferEvent, barcodeToSourceTubeFormation, barcodeToSourceTube, barcodeToTargetTubeFormation, barcodeToTargetStripTube, barcodeToSourceRackOfTubes);

        IlluminaFlowcell flowcell = new IlluminaFlowcell(EIGHT_LANE, "testFlowcell", new IlluminaRunConfiguration(76, true));
        PlateTransferEventType flowcellTransferEvent = new PlateTransferEventType();
        flowcellTransferEvent.setEventType("FlowcellTransfer");
        flowcellTransferEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar("2013-01-23T15:17:00-05:00"));
        labEventFactory.buildFromBettaLimsPlateToPlateDbFree(flowcellTransferEvent, new StripTube("testStripTube"), flowcell);


        Date runDate = new Date(1358889107084L);
        IlluminaSequencingRun sequencingRun = new IlluminaSequencingRun(flowcell, "TestRun", "Run-123", "IlluminaRunServiceImplTest", 101L, true, runDate);
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(sequencingRun);
//        LibraryBeanFactory libraryBeanFactory = new LibraryBeanFactory();
//        ZimsIlluminaRun zimsIlluminaRun = libraryBeanFactory.buildLibraries(sequencingRun);

        assertThat(zimsIlluminaRun.getError(), nullValue());
        assertThat(zimsIlluminaRun.getName(), equalTo("TestRun"));
        assertThat(zimsIlluminaRun.getBarcode(), equalTo("Run-123"));
        assertThat(zimsIlluminaRun.getSequencer(), equalTo("IlluminaRunServiceImplTest"));
        assertThat(zimsIlluminaRun.getFlowcellBarcode(), equalTo("testFlowcell"));
        assertThat(zimsIlluminaRun.getRunDateString(), equalTo("01/22/2013 16:11"));
//        assertThat(zimsIlluminaRun.getPairedRun(), is(true)); // TODO
//        assertThat(zimsIlluminaRun.getSequencerModel(), equalTo("HiSeq")); // TODO
//        assertThat(zimsIlluminaRun.getLanes().size(), equalTo(8));

        for (ZimsIlluminaChamber lane : zimsIlluminaRun.getLanes()) {
            if (lane.getName().equals("1")) {
                assertThat(lane.getLibraries().size(), is(1));
            } else {
                assertThat(lane.getLibraries().size(), is(0));
            }
        }
    }
}
