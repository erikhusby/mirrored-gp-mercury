package org.broadinstitute.sequel.control.zims;

//import com.jprofiler.api.agent.Controller;
import junit.framework.Assert;
import org.broadinstitute.sequel.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

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
