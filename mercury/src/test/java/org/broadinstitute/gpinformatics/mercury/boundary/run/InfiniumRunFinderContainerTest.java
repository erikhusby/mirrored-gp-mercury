package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
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
import java.util.Arrays;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * User: jowalsh
 * Date: 4/3/16
 */
@Test(groups = TestGroups.STANDARD)
public class InfiniumRunFinderContainerTest extends Arquillian {

    @Inject
    private LabEventDao labEventDao;

    @Inject
    UserTransaction utx;

    private File tmpDir;
    private File runDir;
    private LabEvent xStainEvent;
    private LabVessel chip;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        if (utx == null) {
            return;
        }

        utx.begin();

        xStainEvent = labEventDao.findById(LabEvent.class, 1205421L);
        chip = xStainEvent.getAllLabVessels().iterator().next();
        String chipBarcode = chip.getLabel();
        assertThat(chipBarcode, equalTo("3999595020"));

        String tmpDirPath = System.getProperty("java.io.tmpdir");
        tmpDir = new File(tmpDirPath);
        runDir = new File(tmpDir, chipBarcode);
        runDir.createNewFile();
        runDir.deleteOnExit();
        for (VesselPosition vesselPosition: chip.getVesselGeometry().getVesselPositions()) {
            String red = String.format("%s_%s_Red.idat", chipBarcode, vesselPosition.name());
            String green = String.format("%s_%s_Grn.idat", chipBarcode, vesselPosition.name());

            File fRed = new File(runDir, red);
            fRed.createNewFile();
            fRed.deleteOnExit();

            File fGreen = new File(runDir, green);
            fGreen.createNewFile();
            fGreen.deleteOnExit();
        }
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }

        utx.rollback();

        if (runDir != null && runDir.exists()) {
            FileUtils.deleteDirectory(runDir);
        }
    }

    @Test
    public void testPersistence() throws IOException {
        InfiniumRunFinder runFinder = new InfiniumRunFinder();
        InfiniumRunFinder runFinderSpy = spy(runFinder);

        doReturn(Arrays.asList(xStainEvent)).when(runFinderSpy.listPendingXStainChips());

        InfiniumStarterConfig config = new InfiniumStarterConfig(STUBBY);
        config.setMinimumIdatFileLength(-1);
        config.setDataPath(tmpDir.getPath());
        InfiniumRunProcessor runProcessor = new InfiniumRunProcessor();
        runFinder.setInfiniumRunProcessor(runProcessor);

        InfiniumPipelineClient mockPipelineClient = mock(InfiniumPipelineClient.class);
        when(mockPipelineClient.callStarterOnWell(any(StaticPlate.class), any(VesselPosition.class))).thenReturn(true);
        runFinder.setInfiniumPipelineClient(mockPipelineClient);

        runFinder.find();

        verify(mockPipelineClient, times(chip.getVesselGeometry().getVesselPositions().length)).
                callStarterOnWell(any(StaticPlate.class), any(VesselPosition.class));

        //TODO verify that Autocall All Started was called

    }
}
