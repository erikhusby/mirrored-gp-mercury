package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import edu.mit.broad.prodinfo.thrift.lims.*;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.thrift.*;
import org.broadinstitute.gpinformatics.mercury.bsp.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.gpinformatics.mercury.entity.zims.*;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.util.*;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.*;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.*;

@Test(groups = EXTERNAL_INTEGRATION)
public class IlluminaRunResourceTest extends Arquillian {

    @Inject
    IlluminaRunResource runLaneResource;

    @Inject
    ProductOrderDao pdoDao;

    @Inject ThriftService thriftService;

    private TZamboniRun zamboniRun;

    public static final String RUN_NAME = "120320_SL-HBN_0159_AFCC0GHCACXX"; // has bsp samples

    private final String CHAMBER = "2";
    
    private final String WEBSERVICE_URL = "rest/IlluminaRun/query";

    public static final String HUMAN = "Human";

    public static final String BSP_HUMAN = "Homo : Homo sapiens";

    // PDO key applied to everything in the run
    public static final String PDO_KEY = "PDO-36";

    /**
     * Hack alert: we rely on fabricated squid data for this test.
     * If QA squid has been updated, you may need to run this:
     *
         update work_request_material_descr wrmd set wrmd.product_order_name = 'PDO-36'
         where
         wrmd.work_request_material_id in
         (select wrm.work_request_material_id from work_request_material wrm where wrm.work_request_id = 29225)
     */
    public static final String PDO_COMPARISON_ERROR_MESSAGE = "check run " + RUN_NAME + " in serialized .thrift file to make sure WRs are linked to " + PDO_KEY;

    private Map<Long,ProductOrder> wrIdToPDO = new HashMap<Long, ProductOrder>();

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(EverythingYouAskForYouGetAndItsHuman.class,
                MockThriftService.class)
                .addAsResource(ThriftFileAccessor.RUN_FILE);
    }


    /**
     * Does a test of {@link #RUN_NAME} {@link #CHAMBER}
     * directly in container.
     */
    @Test
    public void test_zims_in_container() throws Exception {
        // todo arz update the run in QA squid to link all WRS to the PDO
        wrIdToPDO.put(29225L,pdoDao.findByBusinessKey(PDO_KEY));

        ZimsIlluminaRun runBean = runLaneResource.getRun(RUN_NAME);

        doAssertions(zamboniRun,runBean,wrIdToPDO);
    }
    
    /**
     * Does the same test as {@link #test_zims_in_container()},
     * but does it over http, which means it's actually checking
     * that various annotations like {@link javax.xml.bind.annotation.XmlAttribute}
     * are applied properly in {@link LibraryBean}.
     * @param baseUrl
     */
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
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
        doAssertions(zamboniRun,run,wrIdToPDO);

        System.out.println(rawJson);

        boolean foundBspSample = false;
        for (ZimsIlluminaChamber zimsIlluminaChamber : run.getLanes()) {
            for (LibraryBean libraryBean : zimsIlluminaChamber.getLibraries()) {
                if (libraryBean.getLsid() != null) {
                    if (libraryBean.getLsid().contains("bsp")) {
                        foundBspSample = true;
                        assertNotNull(libraryBean.getBspRootSample());
                        assertNotNull(libraryBean.getBspCollection());
                        assertNotNull(libraryBean.getBspSampleId());
                    }
                }
            }
        }
        assertTrue(foundBspSample);
    }
    
    public static void doAssertions(TZamboniRun thriftRun,ZimsIlluminaRun runBean,Map<Long,ProductOrder> wrIdToPDO) {
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
            doAssertions(thriftLane, lane,wrIdToPDO);

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
    
    private static void doAssertions(TZamboniLane zLane,ZimsIlluminaChamber laneBean,Map<Long,ProductOrder> wrIdToPDO) {
        assertEquals(laneBean.getName(),Short.toString(zLane.getLaneNumber()));
        assertEquals(laneBean.getPrimer(),zLane.getPrimer());
        assertEquals(laneBean.getLibraries().size(), zLane.getLibraries().size());

        assertNotNull(laneBean.getSequencedLibrary());
        assertFalse(laneBean.getSequencedLibrary().isEmpty());
        assertEquals(laneBean.getSequencedLibrary(), zLane.getSequencedLibraryName());

        for (TZamboniLibrary thriftLib : zLane.getLibraries()) {
            doAssertions(thriftLib,laneBean.getLibraries(),wrIdToPDO.get(thriftLib.getWorkRequestId()));
        }
    }
    
    private static void doAssertions(TZamboniLibrary zLib,Collection<LibraryBean> libBeans,ProductOrder pdo) {
        boolean foundIt = false;
        boolean foundPDO = false;
        boolean foundLcSet = false;

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

                if (zLib.getPdoKey() != null) {
                    foundPDO = true;
                    if (pdo != null) {
                        // todo arz other fields
                        assertEquals(libBean.getProductOrderTitle(),pdo.getTitle(),PDO_COMPARISON_ERROR_MESSAGE);
                        assertEquals(libBean.getMercuryProjectKey(),pdo.getResearchProject().getBusinessKey(),PDO_COMPARISON_ERROR_MESSAGE);
                        assertEquals(libBean.getProductOrderKey(),pdo.getBusinessKey(),PDO_COMPARISON_ERROR_MESSAGE);
                        assertEquals(libBean.getMercuryProjectTitle(),pdo.getResearchProject().getTitle(),PDO_COMPARISON_ERROR_MESSAGE);
                    }
                    else {
                        throw new RuntimeException("No PDO passed in; I can't compare.");
                    }
                }
                if (zLib.getLcset() != null) {
                    foundLcSet = true;
                    assertEquals(libBean.getLcSet(),zLib.getLcset());
                }
                if (libBean.getLcSet() != null && zLib.getLcset() == null) {
                    fail("bean has lcset " + libBean.getLcSet() + ", but thrift library has no lc set.");
                }
            }
        }

        assertTrue((foundLcSet && zLib.getLcset() != null)  || (zLib.getLcset() == null));
        assertTrue(foundIt);
        assertTrue(foundPDO || pdo == null);
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
