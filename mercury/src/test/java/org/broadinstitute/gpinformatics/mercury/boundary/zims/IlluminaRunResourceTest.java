package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import edu.mit.broad.prodinfo.thrift.lims.*;
import org.broadinstitute.gpinformatics.mercury.bsp.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.gpinformatics.mercury.entity.zims.*;
import org.broadinstitute.gpinformatics.infrastructure.thrift.MockThriftService;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftFileAccessor;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.*;

public class IlluminaRunResourceTest extends ContainerTest {

    @Inject
    IlluminaRunResource runLaneResource;

    private TZamboniRun zamboniRun;

    public static final String RUN_NAME = "120320_SL-HBN_0159_AFCC0GHCACXX"; // has bsp samples

    private final String CHAMBER = "2";
    
    private final String WEBSERVICE_URL = "rest/IlluminaRun/query";

    public static final String HUMAN = "Human";

    public static final String BSP_HUMAN = "Homo : Homo sapiens";

    @Deployment
    public static WebArchive buildSequelWar() {
        return DeploymentBuilder.buildSequelWarWithAlternatives(EverythingYouAskForYouGetAndItsHuman.class,
                                                                MockThriftService.class)
                .addAsResource(ThriftFileAccessor.RUN_FILE);
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

        DefaultClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);


         ZimsIlluminaRun run = Client.create(clientConfig).resource(url)
                .queryParam("runName", RUN_NAME)
                .accept(MediaType.APPLICATION_JSON).get(ZimsIlluminaRun.class);

        String rawJson = Client.create(clientConfig).resource(url)
                .queryParam("runName", RUN_NAME)
                .accept(MediaType.APPLICATION_JSON).get(String.class);
        assertFalse(rawJson.contains("@")); // might see this if you use XmlAttribute instead of XmlElement
        assertTrue(rawJson.contains("null")); // KT and others like to see field names present w/ null values instead of missing entirely

        assertNotNull(run);
        assertEquals(run.getName(),RUN_NAME);
        doAssertions(zamboniRun,run);

    }
    
    public static void doAssertions(TZamboniRun thriftRun,ZimsIlluminaRun runBean) {
        assertEquals(runBean.getLanes().size(),thriftRun.getLanes().size());
        assertEquals(runBean.getFlowcellBarcode(),thriftRun.getFlowcellBarcode());
        assertEquals(runBean.getSequencer(),thriftRun.getSequencer());
        assertEquals(runBean.getSequencerModel(),thriftRun.getSequencerModel());
        assertEquals(runBean.getFirstCycle(),new Integer(thriftRun.getFirstCycle()));
        assertEquals(runBean.getFirstCycleReadLength(),new Integer(thriftRun.getFirstCycleReadLength()));
        assertEquals(runBean.getMolecularBarcodeCycle(),new Integer(thriftRun.getMolBarcodeCycle()));
        assertEquals(runBean.getMolecularBarcodeLength(),new Integer(thriftRun.getMolBarcodeLength()));
        assertEquals(runBean.getPairedRun(),thriftRun.isPairedRun());
        assertEquals(runBean.getLastCycle(),new Integer(thriftRun.getLastCycle()));

        doReadAssertions(thriftRun,runBean);

        assertEquals(runBean.getRunDateString(),thriftRun.getRunDate());
        boolean hasRealPreCircSize = false;
        boolean hasRealLabInsertSize = false;
        boolean hasDevAliquotData = false;
        for (TZamboniLane thriftLane : thriftRun.getLanes()) {
            ZimsIlluminaChamber lane = getLane(Short.toString(thriftLane.getLaneNumber()),runBean);
            doAssertions(thriftLane, lane);

            for (TZamboniLane zamboniLane : thriftRun.getLanes()) {
                for (TZamboniLibrary thriftLib : zamboniLane.getLibraries()) {
                    if (thriftLib.getPrecircularizationDnaSize() > 0) {
                        hasRealPreCircSize = true;
                    }
                    if (thriftLib.getLabMeasuredInsertSize() > 0) {
                        hasRealLabInsertSize = true;
                    }
                    String experimentName = thriftLib.getDevExperimentData().getExperiment();
                    if (experimentName != null) {
                        experimentName = experimentName.trim();
                    }
                    if (experimentName != null && experimentName.length() > 0) {
                        if (!thriftLib.getDevExperimentData().getConditionChain().isEmpty()) {
                            hasDevAliquotData = true;
                        }
                    }
                }
            }
        }
        assertTrue(hasDevAliquotData);
        assertTrue(hasRealLabInsertSize);
        assertTrue(hasRealPreCircSize);
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
        assertEquals(laneBean.getName(),Short.toString(zLane.getLaneNumber()));
        assertEquals(laneBean.getPrimer(),zLane.getPrimer());
        assertEquals(laneBean.getLibraries().size(), zLane.getLibraries().size());

        assertNotNull(laneBean.getSequencedLibrary());
        assertFalse(laneBean.getSequencedLibrary().isEmpty());
        assertEquals(laneBean.getSequencedLibrary(), zLane.getSequencedLibraryName());

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
                assertEquals(libBean.getPreCircularizationDnaSize(), zLib.getPrecircularizationDnaSize() == 0 ? null : zLib.getPrecircularizationDnaSize(),
                        "Precircularization size is wrong for " + libBean.getLibrary());
                assertEquals(libBean.getProject(),zLib.getProject());
                assertEquals(libBean.getWorkRequestId().longValue(),zLib.getWorkRequestId());
                assertEquals(libBean.getCellLine(),zLib.getCellLine());
                assertEquals(libBean.getSampleAlias(),zLib.getSampleAlias());
                assertEquals(libBean.getIndividual(),zLib.getIndividual());
                assertEquals(libBean.getAligner(),zLib.getAligner());
                assertEquals(libBean.getAnalysisType(),zLib.getAnalysisType());
                assertEquals(libBean.getBaitSetName(),zLib.getBaitSetName());
                assertEquals(libBean.getExpectedInsertSize(),zLib.getExpectedInsertSize());
                assertEquals(libBean.getGssrSampleType(),zLib.getGssrSampleType());
                assertEquals(libBean.getInitiative(),zLib.getInitiative());
                assertEquals(libBean.getLabMeasuredInsertSize(),zLib.getLabMeasuredInsertSize() == 0 ? null : zLib.getLabMeasuredInsertSize());
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
                assertEquals(libBean.doAggregation().booleanValue(),zLib.aggregate);

                if (HUMAN.equals(zLib.getOrganism())) {
                    if (!(HUMAN.equals(libBean.getOrganism()) || BSP_HUMAN.equals(libBean.getOrganism()))) {
                        fail("Not the right human:" + libBean.getOrganism());
                    }
                }
                else {
                    assertEquals(libBean.getOrganism(),zLib.getOrganism());
                }
                assertEquals(libBean.getLsid(),zLib.getLsid());
                checkEquality(zLib.getMolecularIndexes(), libBean.getMolecularIndexingScheme());
                assertEquals(libBean.getGssrBarcodes(),zLib.getGssrBarcodes());
                checkEquality(libBean.getDevExperimentData(),zLib.getDevExperimentData());
                assertEquals(libBean.getCustomAmpliconSetNames(), zLib.getCustomAmpliconSetNames());
            }
        }
        
        assertTrue(foundIt);
    }

    private static void checkEquality(DevExperimentDataBean devExperimentBean,TZDevExperimentData thriftExperimentData) {
        if (devExperimentBean != null && thriftExperimentData != null) {
            assertEquals(devExperimentBean.getConditions().size(),thriftExperimentData.getConditionChain().size());
            Collection<String> thriftConditions = thriftExperimentData.getConditionChain();
            for (String condition : devExperimentBean.getConditions()) {
                assertTrue(thriftConditions.contains(condition));
            }
            assertEquals(devExperimentBean.getExperiment(),thriftExperimentData.getExperiment());
        }
        else if (devExperimentBean == null && thriftExperimentData == null) {
            return;
        }
        else {
            fail("dev experiment bean and thrift bean do not agree.");
        }
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
                    IndexComponent singleIndex = new IndexComponent(thriftEntry.getKey(),thriftEntry.getValue());
                    assertTrue(beanScheme.getSequences().contains(singleIndex));
                }
            }
            else {
                fail("Thrift scheme " + thriftScheme + " is different from scheme bean " + beanScheme);
            }
        }
    }

    private static ZimsIlluminaChamber getLane(String laneName,ZimsIlluminaRun run) {
        for (ZimsIlluminaChamber lane : run.getLanes()) {
            if (laneName.equals(lane.getName())) {
                return lane;
            }
        }
        return null;
    }

    @BeforeClass
    private void getZamboniRun() throws Exception {
        zamboniRun = new MockThriftService().fetchRun(RUN_NAME);
    }


   // @Override/
    //@Test
    public List<LibraryData> fetchLibraryDetailsByLibraryName(List<String> libraryNames) {
        List<LibraryData> libraryDataList = new ArrayList<LibraryData>();
        for (String libraryName : libraryNames) {
            LibraryData libraryData = new LibraryData();
            libraryData.setLibraryName(libraryName);
            libraryData.setLibraryNameIsSet(true);
            libraryDataList.add(libraryData);
        }

        return libraryDataList;
    }
}
