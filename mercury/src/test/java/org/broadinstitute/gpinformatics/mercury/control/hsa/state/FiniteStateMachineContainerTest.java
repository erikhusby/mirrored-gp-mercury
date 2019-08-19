package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.apache.commons.lang3.tuple.MutablePair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
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
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineEngine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerControllerStub;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationUtils;
import org.easymock.EasyMock;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
    private FiniteStateMachineEngine finiteStateMachineEngine;

    @Inject
    private FiniteStateMachineFactory stateMachineFactory;

    @Inject
    private DragenConfig dragenConfig;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    private static final String DENATURE_BARCODE = "0311158846";

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testBasic() throws IOException, SystemException {
        IlluminaSequencingRun sequencingRun = createSequencingRun();
        Set<IlluminaSequencingRunChamber> sequencingRunChambers = sequencingRun.getSequencingRunChambers();
        System.out.println(sequencingRunChambers);

        // Build Finite State Machine
        MessageCollection messageCollection = new MessageCollection();

        dragenConfig.setDemultiplexOutputPath(System.getProperty("java.io.tmpdir"));
        stateMachineFactory.setDragenConfig(dragenConfig);

        FiniteStateMachine finiteStateMachine =
                stateMachineFactory.createFiniteStateMachineForRun(sequencingRun, sequencingRun.getRunName(), messageCollection);

        SchedulerContext schedulerContext = new SchedulerContext(new SchedulerControllerStub());

        finiteStateMachineEngine.setContext(schedulerContext);
        finiteStateMachineEngine.resumeMachine(finiteStateMachine);

        // Create RTA File
        File rtaComplete = new File(sequencingRun.getRunDirectory(), "RTAComplete.txt");
        rtaComplete.createNewFile();

        finiteStateMachineEngine.resumeMachine(finiteStateMachine);

        // Should be in demultiplex
        Assert.assertEquals(finiteStateMachine.getActiveStates().size(), 1);
        State demultiplexState = finiteStateMachine.getActiveStates().iterator().next();
        Assert.assertEquals(demultiplexState.getTasks().size(), 1);
        Task demultiplexProcessTask = demultiplexState.getTasks().iterator().next();
        assertThat(demultiplexProcessTask, instanceOf(DemultiplexTask.class));

        Assert.assertEquals(demultiplexState.getExitTask().isPresent(), true);
        Task demultiplexMetricsProcessTask = demultiplexState.getExitTask().get();
        assertThat(demultiplexMetricsProcessTask, instanceOf(DemultiplexMetricsTask.class));

        finiteStateMachineEngine.resumeMachine(finiteStateMachine);

        Assert.assertEquals(1, finiteStateMachine.getActiveStates().size());
        State alignmentState = finiteStateMachine.getActiveStates().iterator().next();
        Assert.assertEquals(alignmentState.getTasks().size(), 24);

        // should now be in Alignment for each unique sample
        for (Task task: alignmentState.getTasks()) {
            assertThat(task, instanceOf(AlignmentTask.class));
        }

        Assert.assertEquals(alignmentState.getExitTask().isPresent(), true);
        Task alignmentMetricsProcessTask = alignmentState.getExitTask().get();
        assertThat(alignmentMetricsProcessTask, instanceOf(AlignmentMetricsTask.class));

        finiteStateMachineEngine.resumeMachine(finiteStateMachine);

        Assert.assertEquals(true, finiteStateMachine.isComplete());
        Assert.assertNotNull(finiteStateMachine.getDateQueued());
        Assert.assertNotNull(finiteStateMachine.getDateStarted());
        Assert.assertNotNull(finiteStateMachine.getActiveStates());
        Assert.assertNotNull(finiteStateMachine.getDateCompleted());

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
                baseDirectory + "SL-NVA" + File.separator
                + runBarcode;
        File runFolder = new File(runFileDirectory);
        runFolder.mkdirs();

        SolexaRunBean solexaRunBean = new SolexaRunBean();
        solexaRunBean.setRunDirectory(runFolder.getPath());
        solexaRunBean.setFlowcellBarcode(flowcell);
        solexaRunBean.setMachineName("SL-NVA");
        solexaRunBean.setRunBarcode(System.currentTimeMillis() + "HS" + flowcell);
        solexaRunBean.setRunDate(new Date());

        UriInfo uriInfo = EasyMock.createMock(UriInfo.class);
        UriBuilder uriBuilder1 = EasyMock.createMock(UriBuilder.class);
        UriBuilder uriBuilder2 = EasyMock.createMock(UriBuilder.class);

        EasyMock.expect(uriInfo.getAbsolutePathBuilder()).andReturn(uriBuilder1);
        EasyMock.expect(uriBuilder1.path(EasyMock.anyObject(String.class))).andReturn(uriBuilder2);
        EasyMock.expectLastCall().times(2);
        try {
            EasyMock.expect(uriBuilder2.build()).andReturn(new URI("http://xyz"));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        EasyMock.expectLastCall().times(2);
        EasyMock.replay(uriBuilder1, uriBuilder2, uriInfo);

        Response response = solexaRunResource.createRun(solexaRunBean, uriInfo);
        System.out.println(response);

        /*SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        ReadStructureRequest req = new ReadStructureRequest();
        req.setRunBarcode(flowcell + sdf.format(new Date()));
        req.setRunName(runBarcode);
        req.setImagedArea(999.293923);
        req.setActualReadStructure("76T8B8B76T");
        req.setSetupReadStructure("76T8B8B76T");
        req.setLanesSequenced("2");

        for (int i = 1; i <= 2; i++) {
            LaneReadStructure laneReadStructure = new LaneReadStructure();
            laneReadStructure.setLaneNumber(i);
            laneReadStructure.setActualReadStructure("76T8B8B76T");
            req.getLaneStructures().add(laneReadStructure);
        } */

        //Response readStructureResp = runResource.storeRunReadStructure(req);
        //System.out.println(readStructureResp);
        return illuminaSequencingRunDao.findByRunName(runBarcode);
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
