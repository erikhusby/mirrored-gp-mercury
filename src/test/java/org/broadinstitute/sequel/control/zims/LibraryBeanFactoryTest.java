package org.broadinstitute.sequel.control.zims;

import com.jprofiler.api.agent.Controller;
import junit.framework.Assert;
import org.broadinstitute.sequel.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.sequel.entity.zims.LibrariesBean;
import org.broadinstitute.sequel.test.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * Test the factory
 */
public class LibraryBeanFactoryTest extends ContainerTest {

    @Inject
    private LibraryBeanFactory libraryBeanFactory;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    @Test
    public void testBuildLibraries() {
        String runName = "TestRun0510120516";
        Controller.startCPURecording(true);
        LibrariesBean librariesBean = libraryBeanFactory.buildLibraries(runName);
        Controller.stopCPURecording();
        Assert.assertNotNull("no libraries", librariesBean);
    }
}
