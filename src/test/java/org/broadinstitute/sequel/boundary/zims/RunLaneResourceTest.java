package org.broadinstitute.sequel.boundary.zims;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import org.broadinstitute.sequel.WeldBooter;
import org.broadinstitute.sequel.control.AbstractJerseyClientService;
import org.broadinstitute.sequel.entity.zims.LibrariesBean;
import org.broadinstitute.sequel.entity.zims.LibraryBean;
import static org.testng.Assert.*;

import org.broadinstitute.sequel.infrastructure.thrift.ThriftConfiguration;
import org.broadinstitute.sequel.test.ContainerTest;
import org.broadinstitute.sequel.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;


import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

import java.net.URL;
import java.util.Collection;

public class RunLaneResourceTest extends ContainerTest {

    @Inject
    RunLaneResource runLaneResource;

    @Inject ThriftConfiguration thriftConfig;
    
    private final String RUN_NAME = "110623_SL-HAU_0282_AFCB0152ACXX";
    
    private final String CHAMBER = "2";
    
    private final String WEBSERVICE_URL = "rest/RunLane/query";

    /**
     * Does a test of {@link #RUN_NAME} {@link #CHAMBER}
     * directly in container.
     */
    @Test(groups = EXTERNAL_INTEGRATION)
    public void test_zims_in_container() {
        LibrariesBean libsBean = runLaneResource.getLibraries("110623_SL-HAU_0282_AFCB0152ACXX", "2");
        assertNotNull(libsBean);
        
        doAssertions(libsBean.getLibraries());
    }
    
    /**
     * Does the same test as {@link #test_zims_in_container()},
     * but does it over http.
     * @param baseUrl
     */
    @Test(groups = EXTERNAL_INTEGRATION,dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void test_zims_over_http(@ArquillianResource URL baseUrl) {
        String url = baseUrl.toExternalForm() + WEBSERVICE_URL;

        LibrariesBean libs = Client.create().resource(url)
                .queryParam("runName", RUN_NAME)
                .queryParam("chamber", CHAMBER)
                .accept(MediaType.APPLICATION_XML).get(LibrariesBean.class);

        assertNotNull(libs);
        doAssertions(libs.getLibraries());
    }

    /**
     * Does the assertions for run {@link #RUN_NAME} chamber {@link #CHAMBER}
     * @param libraries
     */
    private void doAssertions(Collection<LibraryBean> libraries) {
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

    @Alternative
    public static class MockThriftConfiguration implements ThriftConfiguration {
        @Override
        public String getHost() {
            return "foo.com";
        }

        @Override
        public int getPort() {
            return 8003;
        }
    }
}
