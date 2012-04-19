package org.broadinstitute.sequel.boundary.zims;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import org.broadinstitute.sequel.WeldBooter;
import org.broadinstitute.sequel.control.AbstractJerseyClientService;
import org.broadinstitute.sequel.entity.zims.LibrariesBean;
import org.broadinstitute.sequel.entity.zims.LibraryBean;
import static org.testng.Assert.*;

import org.broadinstitute.sequel.test.ContainerTest;
import org.broadinstitute.sequel.test.RunEmbeddedSequel;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Test;


import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

import java.util.Collection;


public class RunLaneResourceTest extends RunEmbeddedSequel {

    @Inject
    RunLaneResource runLaneResource;


    @Test(groups = EXTERNAL_INTEGRATION)
    public void test_zims() {
        System.out.println("Here i am");
        String url = baseURL + "rest/RunLane/query";

        LibrariesBean libs = Client.create().resource(url)
                .queryParam("runName","110623_SL-HAU_0282_AFCB0152ACXX")
                .queryParam("chamber","2")
                .accept(MediaType.APPLICATION_XML).get(LibrariesBean.class);

        assertNotNull(libs);
        Collection<LibraryBean> libraries = libs.getLibraries();

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
