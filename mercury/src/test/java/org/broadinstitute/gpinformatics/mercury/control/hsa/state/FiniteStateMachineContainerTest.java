package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.apache.commons.lang3.tuple.MutablePair;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenAppContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenSimulator;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenSimulatorTest;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.TaskManager;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForFileTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineEngine;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

/**
 * Test persistence of finite state machines
 */
@Test(groups = TestGroups.STANDARD)
public class FiniteStateMachineContainerTest extends Arquillian {

    @Inject
    private FlowcellDesignationEjb flowcellDesignationEjb;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private SolexaRunResource solexaRunResource;

    @Inject
    private IlluminaFlowcellDao illuminaFlowcellDao;

    @Inject
    private FiniteStateMachineEngine finiteStateMachineEngine;

    private static final String DENATURE_BARCODE = "0311158846";

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testBasic() throws IOException {
        IlluminaSequencingRun sequencingRun = createSequencingRun();

        // Build Finite State Machine
        String baseDirectory = System.getProperty("java.io.tmpdir");
        File outputFolder = new File(baseDirectory, sequencingRun.getRunBarcode());
        FiniteStateMachine finiteStateMachine = DragenSimulatorTest.createStateMachine(sequencingRun, outputFolder);

        DragenAppContext appContext = new DragenAppContext(new DragenSimulator());

        finiteStateMachineEngine.setContext(appContext);

        finiteStateMachineEngine.resumeMachine(finiteStateMachine);

        // Create RTA File
        File rtaComplete = new File(sequencingRun.getRunDirectory(), "RTAComplete.txt");
        rtaComplete.createNewFile();

        finiteStateMachineEngine.resumeMachine(finiteStateMachine);

        // should be in demultiplex
        Assert.assertEquals(2, finiteStateMachine.getActiveStates().size());
        for (State state: finiteStateMachine.getActiveStates()) {
            assertThat(state, instanceOf(DemultiplexState.class));
            assertThat(state.getTask(), instanceOf(DemultiplexTask.class));
        }

        finiteStateMachineEngine.resumeMachine(finiteStateMachine);

        // should now be in Alignment for each unique sample
        for (State state: finiteStateMachine.getActiveStates()) {
            assertThat(state, instanceOf(AlignmentState.class));
            assertThat(state.getTask(), instanceOf(AlignmentTask.class));
        }

        finiteStateMachineEngine.resumeMachine(finiteStateMachine);

        Assert.assertEquals(true, finiteStateMachine.isComplete());
        Assert.assertNotNull(finiteStateMachine.getDateQueued());
        Assert.assertNotNull(finiteStateMachine.getDateStarted());
        Assert.assertNotNull(finiteStateMachine.getActiveStates());

        // Lookup Mercury Sample to verify tasks
    }

    private IlluminaSequencingRun createSequencingRun() {
        String fctName = createFlowcellDesignation();

        // Create Denature to dilution to flowcell to sequencing run
        String dilutionTubeBarcode = sendDenatureToDilution(fctName);
        String flowcell = sendDilutionToFlowcell(dilutionTubeBarcode);

        // Create run folder
        String runBarcode = System.currentTimeMillis() + "_SL-NVA_A" + flowcell;
        String baseDirectory = System.getProperty("java.io.tmpdir");
        String runFileDirectory =
                baseDirectory + File.separator + "SL-NVA" + File.separator
                + runBarcode;
        File runFolder = new File(runFileDirectory);
        runFolder.mkdirs();

        SolexaRunBean solexaRunBean = new SolexaRunBean();
        solexaRunBean.setRunDirectory(runFolder.getPath());
        solexaRunBean.setFlowcellBarcode(flowcell);
        solexaRunBean.setMachineName("SL-NVA");
        solexaRunBean.setRunBarcode(System.currentTimeMillis() + "HS" + flowcell);
        solexaRunBean.setRunDate(new Date());

        return solexaRunResource.registerRun(solexaRunBean, illuminaFlowcellDao.findByBarcode(flowcell));
    }

    private String sendDenatureToDilution(String fctName) {
        BettaLimsMessageTestFactory testFactory = new BettaLimsMessageTestFactory(true);

        List<String> sourceTubes = Collections.singletonList(DENATURE_BARCODE);
        List<List<String>> sourceTubesList = new ArrayList<>();
        sourceTubesList.add(sourceTubes);

        String dilutionTubeBarcode = DENATURE_BARCODE + "_dil" + System.currentTimeMillis();
        List<String> targetTubes = Collections.singletonList(dilutionTubeBarcode);
        List<List<String>> targetTubesList = new ArrayList<>();
        targetTubesList.add(targetTubes);

        String srcRackBarcode = "HsaTestSrc" + System.currentTimeMillis();
        String destinationRackBar = "HsaTestDest" + System.currentTimeMillis();
        BettaLimsMessageTestFactory.CherryPick cherryPick = new BettaLimsMessageTestFactory.CherryPick(
                srcRackBarcode, "A01", destinationRackBar, "A01");
        List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = Collections.singletonList(cherryPick);

        PlateCherryPickEvent plateCherryPickEvent =
                testFactory.buildCherryPick("DenatureToDilutionTransfer", Collections.singletonList(
                        srcRackBarcode),
                        sourceTubesList, Collections.singletonList(destinationRackBar),
                        targetTubesList, cherryPicks);

        ReceptacleType destinationReceptacle = plateCherryPickEvent.getPositionMap().get(0).getReceptacle().iterator().next();

        MetadataType metadataType = new MetadataType();
        metadataType.setName("FCT");
        metadataType.setValue(fctName);

        destinationReceptacle.getMetadata().add(metadataType);

        BettaLIMSMessage message = new BettaLIMSMessage();
        message.getPlateCherryPickEvent().add(plateCherryPickEvent);

        labEventFactory.buildFromBettaLims(message);
        return dilutionTubeBarcode;
    }

    private String sendDilutionToFlowcell(String dilutionTubeBarcode) {
        ReceptaclePlateTransferEvent event = new ReceptaclePlateTransferEvent();
        event.setEventType("DilutionToFlowcellTransfer");
        event.setStation("batman");
        event.setOperator("jowalsh");
        event.setProgram("Hamilton");
        event.setStart(new Date());
        event.setEnd(new Date());
        event.setDisambiguator(1L);

        ReceptacleType sourceReceptacle = new ReceptacleType();
        sourceReceptacle.setBarcode(dilutionTubeBarcode);
        sourceReceptacle.setReceptacleType("tube");

        String flowcell = System.currentTimeMillis() + "HSAFCDMXX";
        PlateType destinationPlateLaneOne = new PlateType();
        destinationPlateLaneOne.setBarcode(flowcell);
        destinationPlateLaneOne.setSection("ALL2");
        destinationPlateLaneOne.setPhysType("Flowcell2Lane");

        event.setSourceReceptacle(sourceReceptacle);
        event.setDestinationPlate(destinationPlateLaneOne);
        BettaLIMSMessage message = new BettaLIMSMessage();
        message.getReceptaclePlateTransferEvent().add(event);
        labEventFactory.buildFromBettaLims(message);
        return flowcell;
    }

    private String createFlowcellDesignation() {
        DesignationDto dto = new DesignationDto();
        dto.setBarcode(DENATURE_BARCODE);
        dto.setLcset("LCSET-14767");
        dto.setProduct("PCR-Free Human WGS - 20x v2");
        dto.setNumberSamples(1);
        dto.setRegulatoryDesignation(DesignationUtils.RESEARCH);
        dto.setSequencerModel(IlluminaFlowcell.FlowcellType.NovaSeqFlowcell);
        dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
        dto.setReadLength(76);
        dto.setLoadingConc(BigDecimal.TEN);
        dto.setNumberLanes(2);
        dto.setStatus(FlowcellDesignation.Status.QUEUED);
        dto.setPoolTest(false);
        dto.setPairedEndRead(true);
        dto.setSelected(true);

        List<DesignationDto> dtos = Collections.singletonList(dto);
        DesignationUtils.updateDesignationsAndDtos(dtos,
                EnumSet.allOf(FlowcellDesignation.Status.class), flowcellDesignationEjb);

        dto.setSelected(true);
        // Make the FCTs.
        final StringBuilder messages = new StringBuilder();
        List<MutablePair<String, String>> fctUrls = labBatchEjb.makeFcts(dtos, "jowalsh",
                new MessageReporter() {
                    @Override
                    public String addMessage(String message, Object... arguments) {
                        messages.append(String.format(message, arguments)).append("\n");
                        return "";
                    }
                }, false);
        Assert.assertFalse(messages.toString().contains(" invalid "), messages.toString());

        labBatchDao.flush();

        MutablePair<String, String> fctUrl = fctUrls.iterator().next();
        String fctName = fctUrl.getLeft();
        return fctName;
    }
}
