package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.GapHandler;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * Database test for the Infinium Run Finder logic
 */
@Test(groups = TestGroups.STANDARD)
public class InfiniumRunFinderContainerTest extends Arquillian {

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private InfiniumRunFinder infiniumRunFinder;

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private InfiniumPendingChipFinder infiniumPendingChipFinder;

    private GapHandler mockGapHandler;

    public static final String POST_PCR_PLATE = "000009981052";

    private File tmpDir;
    private File runDir;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (runDir != null && runDir.exists()) {
            FileUtils.deleteDirectory(runDir);
        }
    }

    @Test
    public void testNewChipCreation() throws Exception {
        mockGapHandler = mock(GapHandler.class);
        doNothing().when(mockGapHandler).postToGap(any(BettaLIMSMessage.class));
        labEventFactory.setGapHandler(mockGapHandler);
        bettaLimsMessageResource.setLabEventFactory(labEventFactory);

        String chipSuffix = new SimpleDateFormat("MMddHHmmss").format(new Date());
        String chipBarcode = "InfTestChip24" + chipSuffix;
        sendHybAndXStainMessages(POST_PCR_PLATE, chipBarcode);

        //Test to see if can find as a 'Pending' Chip
        List<LabVessel> pendingXStainChips = infiniumPendingChipFinder.listPendingXStainChips();
        LabVessel infiniumChip = null;
        boolean foundPendingChip = false;
        for (LabVessel labVessel: pendingXStainChips) {
            if (labVessel.getLabel().equals(chipBarcode)) {
                infiniumChip = labVessel;
                foundPendingChip = true;
                break;
            }
        }
        assertThat(foundPendingChip, is(true));
        assertThat(infiniumChip, notNullValue());

        //Setup run directory
        String tmpDirPath = System.getProperty("java.io.tmpdir");
        tmpDir = new File(tmpDirPath);
        runDir = new File(tmpDir, chipBarcode);
        runDir.mkdir();
        System.out.println(runDir.getPath());
        for (VesselPosition vesselPosition: infiniumChip.getVesselGeometry().getVesselPositions()) {
            String red = String.format("%s_%s_Red.idat", chipBarcode, vesselPosition.name());
            String green = String.format("%s_%s_Grn.idat", chipBarcode, vesselPosition.name());

            File fRed = new File(runDir, red);
            fRed.createNewFile();

            File fGreen = new File(runDir, green);
            fGreen.createNewFile();
        }

        //Mock chip finder so run finder doesn't grab every old run.
        InfiniumPendingChipFinder mockChipFinder = mock(InfiniumPendingChipFinder.class);
        when(mockChipFinder.listPendingXStainChips()).thenReturn(Arrays.asList(infiniumChip));
        infiniumRunFinder.setInfiniumPendingChipFinder(mockChipFinder);

        InfiniumStarterConfig config = new InfiniumStarterConfig(STUBBY);
        config.setMinimumIdatFileLength(-1);
        config.setDataPath(tmpDir.getPath());
        InfiniumRunProcessor runProcessor = new InfiniumRunProcessor(config);
        infiniumRunFinder.setInfiniumRunProcessor(runProcessor);

        InfiniumPipelineClient mockPipelineClient = mock(InfiniumPipelineClient.class);
        when(mockPipelineClient.callStarterOnWell(any(StaticPlate.class), any(VesselPosition.class))).thenReturn(true);
        infiniumRunFinder.setInfiniumPipelineClient(mockPipelineClient);

        InfiniumRunFinder runFinderSpy = spy(infiniumRunFinder);
        runFinderSpy.find();

        verify(mockPipelineClient, times(infiniumChip.getVesselGeometry().getVesselPositions().length)).
                callStarterOnWell(any(StaticPlate.class), any(VesselPosition.class));
    }

    public void sendHybAndXStainMessages(String ampPlate, String chipBarcode) {

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.setMode(LabEventFactory.MODE_MERCURY);
        List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = new ArrayList<>();
        int wellIndex = 1;
        for (VesselPosition vesselPosition : VesselGeometry.INFINIUM_24_CHIP.getVesselPositions()) {
            String sourceWell = bettaLimsMessageTestFactory.buildWellName(wellIndex,
                    BettaLimsMessageTestFactory.WellNameType.LONG);
            BettaLimsMessageTestFactory.CherryPick cherryPick = new BettaLimsMessageTestFactory.CherryPick(ampPlate,
                    sourceWell, chipBarcode, vesselPosition.name())   ;
            cherryPicks.add(cherryPick);
            wellIndex++;
        }
        PlateCherryPickEvent infiniumHybridization = bettaLimsMessageTestFactory.buildPlateToPlateCherryPick(
                "InfiniumHybridization", ampPlate, Collections.singletonList(chipBarcode),
                cherryPicks);
        infiniumHybridization.getPlate().get(0).setPhysType("InfiniumChip24");
        infiniumHybridization.getSourcePlate().get(0).setPhysType("DeepWell96");
        bettaLIMSMessage.getPlateCherryPickEvent().add(infiniumHybridization);
        bettaLimsMessageResource.processMessage(bettaLIMSMessage);
        bettaLimsMessageTestFactory.advanceTime();

        PlateEventType infiniumXStain = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumXStain", chipBarcode);
        infiniumXStain.getPlate().setPhysType("InfiniumChip24");
        bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateEvent().add(infiniumXStain);
        bettaLimsMessageResource.processMessage(bettaLIMSMessage);

    }
}
