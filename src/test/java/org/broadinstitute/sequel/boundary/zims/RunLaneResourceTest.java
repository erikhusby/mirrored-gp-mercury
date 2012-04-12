package org.broadinstitute.sequel.boundary.zims;


import clover.org.apache.velocity.runtime.parser.node.ASTSetDirective;
import org.broadinstitute.sequel.WeldBooter;
import org.broadinstitute.sequel.entity.zims.LibraryBean;
import org.testng.Assert;
import org.testng.annotations.Test;


import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

import java.util.Collection;

public class RunLaneResourceTest extends WeldBooter {

    @Test(groups = EXTERNAL_INTEGRATION)
    public void test_zims() {
        RunLaneResource runLaneResource = weldUtil.getFromContainer(RunLaneResource.class);
        Collection<LibraryBean> libraries = runLaneResource.getLibraries("110623_SL-HAU_0282_AFCB0152ACXX", "2");
        
        Assert.assertNotNull(libraries);
        Assert.assertFalse(libraries.isEmpty());
        Assert.assertEquals(libraries.size(),12);

        for (LibraryBean library : libraries) {
            Assert.assertEquals(library.getWorkRequest(),new Long(25661));
            Assert.assertEquals(library.getProject(),"G1715");
            Assert.assertEquals(library.getOrganism(),"Human");
        }
    }
}
