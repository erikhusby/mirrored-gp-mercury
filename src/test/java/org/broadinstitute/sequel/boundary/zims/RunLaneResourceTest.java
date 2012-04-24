package org.broadinstitute.sequel.boundary.zims;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import edu.mit.broad.prodinfo.thrift.lims.*;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.broadinstitute.sequel.WeldBooter;
import org.broadinstitute.sequel.control.AbstractJerseyClientService;
import org.broadinstitute.sequel.entity.zims.IndexPositionBean;
import org.broadinstitute.sequel.entity.zims.LibrariesBean;
import org.broadinstitute.sequel.entity.zims.LibraryBean;
import static org.testng.Assert.*;

import org.broadinstitute.sequel.entity.zims.MolecularIndexingSchemeBean;
import org.broadinstitute.sequel.infrastructure.thrift.ProductionThriftConfiguration;
import org.broadinstitute.sequel.infrastructure.thrift.QAThriftConfiguration;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftConfiguration;
import org.broadinstitute.sequel.test.ContainerTest;
import org.broadinstitute.sequel.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.ws.rs.core.MediaType;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

public class RunLaneResourceTest extends ContainerTest {

    @Inject
    RunLaneResource runLaneResource;

    final ThriftConfiguration thriftConfig = new QAThriftConfiguration();
    
    private final String RUN_NAME = "120320_SL-HBN_0159_AFCC0GHCACXX"; // has bsp samples

    private final String CHAMBER = "2";
    
    private final String WEBSERVICE_URL = "rest/RunLane/query";

    /**
     * Does a test of {@link #RUN_NAME} {@link #CHAMBER}
     * directly in container.
     */
    @Test(groups = EXTERNAL_INTEGRATION)
    public void test_zims_in_container() throws Exception {
        LibrariesBean libsBean = runLaneResource.getLibraries(RUN_NAME,CHAMBER);
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
    public void test_zims_over_http(@ArquillianResource URL baseUrl) throws Exception {
        String url = baseUrl.toExternalForm() + WEBSERVICE_URL;

        LibrariesBean libs = Client.create().resource(url)
                .queryParam("runName", RUN_NAME)
                .queryParam("chamber", CHAMBER)
                .accept(MediaType.APPLICATION_XML).get(LibrariesBean.class);

        assertNotNull(libs);
        doAssertions(libs.getLibraries());
        
        try {
            Thread.sleep(5000000);
        }
        catch(InterruptedException e) {}
    }      
    
    private void doLibraryAssertions(TZamboniLibrary zLib,Collection<LibraryBean> libBeans) {
        boolean foundIt = false;

        for (LibraryBean libBean : libBeans) {
            if (libBean.getLibrary().equals(zLib.getLibrary())) {
                foundIt = true;
                assertEquals(zLib.getPrecircularizationDnaSize(),libBean.getPrecircularizationDnaSize());
                assertEquals(zLib.getProject(),libBean.getProject());
                assertEquals(zLib.getWorkRequestId(),libBean.getWorkRequest().longValue());
                assertEquals(zLib.getCellLine(),libBean.getCellLine());
                assertEquals(zLib.getSampleAlias(),libBean.getCollaboratorSampleName());
                assertEquals(zLib.getIndividual(),libBean.getIndividual());
                
                if ("Human".equals(zLib.getOrganism())) {
                    assertEquals(libBean.getOrganism(),"Homo : Homo sapiens");
                }
                else {
                    assertEquals(zLib.getOrganism(),libBean.getOrganism());
                }
                assertEquals(zLib.getLsid(),libBean.getSampleLSID());
                checkEquality(zLib.getMolecularIndexes(), libBean.getIndexingScheme());
            }
        }
        
        assertTrue(foundIt);
    }

    private void checkEquality(MolecularIndexingScheme thriftScheme,MolecularIndexingSchemeBean beanScheme) {
        if (thriftScheme == null && beanScheme == null) {
            return;    
        }
        else {
            if (thriftScheme != null && beanScheme != null) {
                assertEquals(beanScheme.getName(),thriftScheme.getName());
                assertEquals(beanScheme.getSequences().size(),thriftScheme.getSequences().size());

                for (Map.Entry<IndexPosition, String> thriftEntry : thriftScheme.getSequences().entrySet()) {
                    String beanSequence = beanScheme.getSequences().get(new IndexPositionBean(thriftEntry.getKey()));
                    assertEquals(beanSequence,thriftEntry.getValue());
                }
            }
            else {
                fail("Thrift scheme " + thriftScheme + " is different from scheme bean " + beanScheme);
            }
        }
    }

    @BeforeClass
    private TZamboniLane getZamboniLane() throws Exception {
        TTransport transport = new TSocket(thriftConfig.getHost(), thriftConfig.getPort());
        TProtocol protocol = new TBinaryProtocol(transport);
        LIMQueries.Client client = new LIMQueries.Client(protocol);
        transport.open();

        TZamboniLane lane = null;

        try {
            lane = client.fetchSingleLane(RUN_NAME,Short.parseShort(CHAMBER)).getLanes().iterator().next();
        }
        finally {
            transport.close();
        }
        return lane;
    }
    
    /**
     * Does the assertions for run {@link #RUN_NAME} chamber {@link #CHAMBER}
     * @param libraries
     */
    private void doAssertions(Collection<LibraryBean> libraries) throws Exception {
        assertNotNull(libraries);
        assertFalse(libraries.isEmpty());
        assertEquals(libraries.size(),94);

        TZamboniLane zamboniLane = getZamboniLane();
        
        assertEquals(zamboniLane.getLibraries().size(),libraries.size());
        for (TZamboniLibrary zLib : zamboniLane.getLibraries()) {
            doLibraryAssertions(zLib,libraries);    
        }
    }

}
