package org.broadinstitute.sequel.boundary.zims;

import com.sun.jersey.api.client.Client;
import edu.mit.broad.prodinfo.thrift.lims.*;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.broadinstitute.sequel.bsp.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.sequel.entity.zims.*;

import static org.testng.Assert.*;

import org.broadinstitute.sequel.infrastructure.bsp.BSPLSIDUtil;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.sequel.infrastructure.thrift.QAThriftConfiguration;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftConfiguration;
import org.broadinstitute.sequel.test.ContainerTest;
import org.broadinstitute.sequel.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class IlluminaRunResourceTest extends Arquillian  {

    @Inject
    IlluminaRunResource runLaneResource;

    private TZamboniRun zamboniRun;
    
    final ThriftConfiguration thriftConfig = new QAThriftConfiguration();
    
    public static final String RUN_NAME = "120320_SL-HBN_0159_AFCC0GHCACXX"; // has bsp samples

    private final String CHAMBER = "2";
    
    private final String WEBSERVICE_URL = "/rest/IlluminaRun/query";

    public static final String HUMAN = "Human";

    public static final String BSP_HUMAN = "Homo : Homo sapiens";

    @Deployment
    public static WebArchive buildSequelWar() {
        return DeploymentBuilder.buildSequelWarWithAlternatives(EverythingYouAskForYouGetAndItsHuman.class);
    }

    /**
     * Does a test of {@link #RUN_NAME} {@link #CHAMBER}
     * directly in container.
     */
    @Test(groups = EXTERNAL_INTEGRATION)
    public void test_zims_in_container() throws Exception {
        ZimsIlluminaRun runBean = runLaneResource.getRun(RUN_NAME);
        doAssertions(zamboniRun,runBean);
    }
    
    /**
     * Does the same test as {@link #test_zims_in_container()},
     * but does it over http, which means it's actually checking
     * that various annotations like {@link javax.xml.bind.annotation.XmlAttribute}
     * are applied properly in {@link LibraryBean}.
     * @param baseUrl
     */
    @Test(groups = EXTERNAL_INTEGRATION,dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void test_zims_over_http(@ArquillianResource URL baseUrl) throws Exception {
        String url = baseUrl.toExternalForm() + WEBSERVICE_URL;

        ZimsIlluminaRun run = Client.create().resource(url)
                .queryParam("runName", RUN_NAME)
                .accept(MediaType.APPLICATION_XML).get(ZimsIlluminaRun.class);

        assertNotNull(run);
        assertEquals(run.getRunName(),RUN_NAME);
        doAssertions(zamboniRun,run);
    }      
    
    public static void doAssertions(TZamboniRun thriftRun,ZimsIlluminaRun runBean) {
        assertEquals(runBean.getChambers().size(),thriftRun.getLanes().size());
        assertEquals(runBean.getFlowcellBarcode(),thriftRun.getFlowcellBarcode());
        assertEquals(runBean.getSequencer(),thriftRun.getSequencer());
        assertEquals(runBean.getSequencerModel(),thriftRun.getSequencerModel());
        assertEquals(runBean.getFirstCycle(),new Integer(thriftRun.getFirstCycle()));
        assertEquals(runBean.getFirstCycleReadLength(),new Integer(thriftRun.getFirstCycleReadLength()));
        assertEquals(runBean.getMolecularBarcodeCycle(),new Integer(thriftRun.getMolBarcodeCycle()));
        assertEquals(runBean.getMolecularBarcodeLength(),new Integer(thriftRun.getMolBarcodeLength()));
        assertEquals(runBean.getIsPaired(),thriftRun.isPairedRun());
        assertEquals(runBean.getLastCycle(),new Integer(thriftRun.getLastCycle()));

        doReadAssertions(thriftRun,runBean);

        assertEquals(runBean.getRunDateString(),thriftRun.getRunDate());
        for (TZamboniLane thriftLane : thriftRun.getLanes()) {
            ZimsIlluminaChamber lane = getLane(Short.toString(thriftLane.getLaneNumber()),runBean);
            doAssertions(thriftLane, lane);
        }
    }

    private static void doReadAssertions(TZamboniRun thriftRun,ZimsIlluminaRun runBean) {
        if (thriftRun.getReads() != null && runBean.getReads() != null ) {
            assertEquals(runBean.getReads().size(),thriftRun.getReads().size());
            for (TZamboniRead thriftRead : thriftRun.getReads()) {
                boolean haveIt = false;
                for (ZamboniRead beanRead : runBean.getReads()) {
                    Integer firstCycle = ThriftConversionUtil.zeroAsNull(thriftRead.getFirstCycle());
                    Integer readLength = ThriftConversionUtil.zeroAsNull(thriftRead.getLength());

                    if (firstCycle.equals(beanRead.getFirstCycle()) && readLength.equals(beanRead.getLength())) {
                        if (thriftRead.getReadType() != null) {
                            String readTypeName = thriftRead.getReadType().name();
                            if (readTypeName.equals(ZamboniRead.INDEX)) {
                                assertEquals(beanRead.getReadType(),ZamboniReadType.INDEX);
                                haveIt = true;
                            }
                            else if (readTypeName.equals(ZamboniRead.TEMPLATE)) {
                                assertEquals(beanRead.getReadType(),ZamboniReadType.TEMPLATE);
                                haveIt = true;
                            }
                            else {
                                fail("Read type " + thriftRead.getReadType() + " is unknown");
                            }
                        }
                        else {
                            assertNull(beanRead.getReadType());
                        }
                    }
                }
                assertTrue(haveIt);
            }
        }
        else if (thriftRun.getReads() == null && runBean.getReads() == null) {
            // ok
        }
        else {
            fail("Reads are not the same");
        }
    }
    
    private static void doAssertions(TZamboniLane zLane,ZimsIlluminaChamber laneBean) {
        assertEquals(laneBean.getChamberName(),Short.toString(zLane.getLaneNumber()));
        assertEquals(laneBean.getPrimer(),zLane.getPrimer());
        assertEquals(laneBean.getLibraries().size(), zLane.getLibraries().size());

        for (TZamboniLibrary thriftLib : zLane.getLibraries()) {
            doAssertions(thriftLib,laneBean.getLibraries());
        }
    }
    
    private static void doAssertions(TZamboniLibrary zLib,Collection<LibraryBean> libBeans) {
        boolean foundIt = false;

        for (LibraryBean libBean : libBeans) {
            assertNotNull(libBean.getLibrary());
            if (libBean.getLibrary().equals(zLib.getLibrary())) {
                foundIt = true;
                assertEquals(libBean.getPrecircularizationDnaSize(),zLib.getPrecircularizationDnaSize() == 0 ? null : zLib.getPrecircularizationDnaSize());
                assertEquals(libBean.getProject(),zLib.getProject());
                assertEquals(libBean.getWorkRequest().longValue(),zLib.getWorkRequestId());
                assertEquals(libBean.getCellLine(),zLib.getCellLine());
                assertEquals(libBean.getCollaboratorSampleName(),zLib.getSampleAlias());
                assertEquals(libBean.getIndividual(),zLib.getIndividual());
                assertEquals(libBean.getAligner(),zLib.getAligner());
                assertEquals(libBean.getAnalysisType(),zLib.getAnalysisType());
                assertEquals(libBean.getBaitSetName(),zLib.getBaitSetName());
                assertEquals(libBean.getExpectedInsertSize(),zLib.getExpectedInsertSize());
                assertEquals(libBean.getGssrSampleType(),zLib.getGssrSampleType());
                assertEquals(libBean.getInitiative(),zLib.getInitiative());
                assertEquals(libBean.getLabMeasuredInsertSize(),zLib.getLabMeasuredInsertSize());
                assertEquals(libBean.getLibrary(),zLib.getLibrary());
                assertEquals(libBean.getReferenceSequence(),zLib.getReferenceSequence());
                assertEquals(libBean.getReferenceSequenceVersion(),zLib.getReferenceSequenceVersion());
                assertEquals(libBean.getRestrictionEnzyme(),zLib.getRestrictionEnzyme());
                assertEquals(libBean.getRrbsSizeRange(),zLib.getRrbsSizeRange());
                assertEquals(libBean.getSampleCollaborator(),zLib.getSampleCollaborator());
                assertEquals(libBean.getStrain(),zLib.getStrain());
                assertEquals(libBean.getSpecies(),zLib.getSpecies());
                assertEquals(libBean.getTargetLaneCoverage(),new Short(zLib.getTargetLaneCoverage()));
                assertEquals(libBean.getTissueType(),zLib.getTissueType());
                assertEquals(libBean.getWeirdness(),zLib.getWeirdness());

                if (HUMAN.equals(zLib.getOrganism())) {
                    if (!(HUMAN.equals(libBean.getOrganism()) || BSP_HUMAN.equals(libBean.getOrganism()))) {
                        fail("Not the right human:" + libBean.getOrganism());
                    }
                }
                else {
                    assertEquals(libBean.getOrganism(),zLib.getOrganism());
                }
                assertEquals(libBean.getSampleLSID(),zLib.getLsid());
                checkEquality(zLib.getMolecularIndexes(), libBean.getIndexingScheme());
                assertEquals(libBean.getGssrBarcodes(),zLib.getGssrBarcodes());
            }
        }
        
        assertTrue(foundIt);
    }

    private static void checkEquality(MolecularIndexingScheme thriftScheme,MolecularIndexingSchemeBean beanScheme) {
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

    private static ZimsIlluminaChamber getLane(String laneName,ZimsIlluminaRun run) {
        for (ZimsIlluminaChamber lane : run.getChambers()) {
            if (laneName.equals(lane.getChamberName())) {
                return lane;
            }
        }
        return null;
    }

    @BeforeClass
    private void getZamboniRun() throws Exception {
        TTransport transport = new TSocket(thriftConfig.getHost(), thriftConfig.getPort());
        TProtocol protocol = new TBinaryProtocol(transport);
        LIMQueries.Client client = new LIMQueries.Client(protocol);
        transport.open();

        TZamboniRun run = null;

        try {
            run = client.fetchRun(RUN_NAME);
        }
        finally {
            transport.close();
        }
        zamboniRun = run;
    }


}
