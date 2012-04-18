package org.broadinstitute.sequel.boundary.zims;


import clover.org.apache.velocity.runtime.parser.node.ASTSetDirective;
import org.broadinstitute.sequel.WeldBooter;
import org.broadinstitute.sequel.entity.zims.LibraryBean;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

import java.util.Collection;

public class RunLaneResourceTest extends WeldBooter {

    @Test(groups = EXTERNAL_INTEGRATION)
    public void test_zims() {
        RunLaneResource runLaneResource = weldUtil.getFromContainer(RunLaneResource.class);
        Collection<LibraryBean> libraries = runLaneResource.getLibraries("110623_SL-HAU_0282_AFCB0152ACXX", "2");
        boolean foundSample1 = false;
        
        assertNotNull(libraries);
        assertFalse(libraries.isEmpty());
        assertEquals(libraries.size(),12);

        for (LibraryBean library : libraries) {
            assertEquals(library.getWorkRequest(),new Long(25661));
            assertEquals(library.getProject(),"G1715");
            assertEquals(library.getOrganism(),"Human");

            if ("BROAD:SEQUENCING_SAMPLE:107508.0".equals(library.getSampleLSID())) {
                foundSample1 = true;
                assertEquals(library.getCollaboratorSampleName(),"BioSam 220");
                assertEquals(library.getSampleLSID(),"BROAD:SEQUENCING_SAMPLE:107508.0");
                assertEquals(library.getCellLine(),"Ewing Sarcoma");
                assertEquals(library.getIndividual(),"Donor 77 - donor #4");
            }
        }
        
        assertTrue(foundSample1);
    }
}
