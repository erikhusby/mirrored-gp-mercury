package org.broadinstitute.gpinformatics.mercury.control.zims;

//import com.jprofiler.api.agent.Controller;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Test the factory
 */
public class LibraryBeanFactoryTest extends ContainerTest {

    @Inject
    private LibraryBeanFactory libraryBeanFactory;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testBuildLibraries() {
        // todo jmt fix this
        String runName = "TestRun0510120516";
//        Controller.startCPURecording(true);
//        LibrariesBean librariesBean = libraryBeanFactory.buildLibraries(runName);
//        Controller.stopCPURecording();
//        Assert.assertNotNull("no libraries", librariesBean);
    }
}
