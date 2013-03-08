package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import edu.mit.broad.prodinfo.thrift.lims.*;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.thrift.*;
import org.broadinstitute.gpinformatics.mercury.entity.zims.*;
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
import java.util.*;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.*;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.*;

public class IlluminaRunResourceTest extends Arquillian {

    @Inject
    IlluminaRunResource runLaneResource;

    @Inject
    ProductOrderDao pdoDao;

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
        return DeploymentBuilder.buildMercuryWarWithAlternatives(TEST,MockThriftService.class).addAsResource(ThriftFileAccessor.RUN_FILE);
    }


    /**
     * Does a test of {@link #RUN_NAME} {@link #CHAMBER}
     * directly in container.
     */
    @Test(groups = EXTERNAL_INTEGRATION)
    public void test_zims_in_container() throws Exception {
        // todo arz update the run in QA squid to link all WRS to the PDO
        wrIdToPDO.put(29225L,pdoDao.findByBusinessKey(PDO_KEY));

        ZimsIlluminaRun runBean = runLaneResource.getRun(RUN_NAME);

        doAssertions(zamboniRun,runBean,wrIdToPDO);
    }

    /**
     * Ensures that error handling makes it all the way through
     * out to HTTP
     */
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER,
          groups = EXTERNAL_INTEGRATION)
    @RunAsClient
    public void test_error_handling(@ArquillianResource URL baseUrl) throws Exception {
        String url = baseUrl.toExternalForm() + WEBSERVICE_URL;

        DefaultClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);


        ZimsIlluminaRun run = Client.create(clientConfig).resource(url)
                .accept(MediaType.APPLICATION_JSON).get(ZimsIlluminaRun.class);
        assertNotNull(run);
        assertNotNull(run.getError());
    }


    /**
    * Does the same test as {@link #test_zims_in_container()},
    * but does it over http, which means it's actually checking
    * that various annotations like {@link javax.xml.bind.annotation.XmlAttribute}
    * are applied properly in {@link LibraryBean}.
    * @param baseUrl
    */
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER,
        groups = EXTERNAL_INTEGRATION)
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
        assertFalse(rawJson.contains("\"gssrSample\""));
        assertTrue(rawJson.contains("\"rootSample\""));

        assertNotNull(run);
        assertEquals(run.getName(),RUN_NAME);

        doAssertions(zamboniRun,run,new HashMap<Long,ProductOrder>());
        boolean foundBspSample = false;
        boolean foundLcSet = false;
        boolean foundPdo = false;
        boolean foundTumor = false;
        for (ZimsIlluminaChamber zimsIlluminaChamber : run.getLanes()) {
            for (LibraryBean libraryBean : zimsIlluminaChamber.getLibraries()) {
                if (libraryBean.getLsid() != null) {
                    if (libraryBean.getLsid().contains("bsp")) {
                        foundBspSample = true;
                        assertNotNull(libraryBean.getRootSample());
                        assertNotNull(libraryBean.getCollection());
                        assertNotNull(libraryBean.getSampleId());
                    }
                    if ("broadinstitute.org:bsp.prod.sample:12MD2".equals(libraryBean.getLsid())) {
                        foundTumor = true;
                        assertEquals(libraryBean.getSampleType(), BSPSampleDTO.TUMOR_IND);
                    }
                    if (libraryBean.getLcSet() != null) {
                        foundLcSet = true;
                    }
                    if (libraryBean.getProductOrderTitle() != null) {
                        foundPdo = true;
                    }
                }
            }
        }
        assertTrue(foundBspSample);
        assertTrue(foundLcSet);
        assertTrue(foundPdo);
        assertTrue(foundTumor);

        run = Client.create(clientConfig).resource(url)
                .queryParam("runName", "Cheese ball")
                .accept(MediaType.APPLICATION_JSON).get(ZimsIlluminaRun.class);
        assertNotNull(run.getError());
        // this is important: the pipeline hardcodes the "run isn't registered yet" response
        // and retries later
        assertTrue(run.getError().contains("Run Cheese ball doesn't appear to have been registered yet"));
    }

    public static void doAssertions(TZamboniRun thriftRun,ZimsIlluminaRun runBean,Map<Long,ProductOrder> wrIdToPDO) {
        assertNull(runBean.getError());
        assertEquals(runBean.getLanes().size(),thriftRun.getLanes().size());
        assertEquals(runBean.getFlowcellBarcode(),thriftRun.getFlowcellBarcode());
        assertEquals(runBean.getSequencer(),thriftRun.getSequencer());
        assertEquals(runBean.getSequencerModel(),thriftRun.getSequencerModel());
        assertEquals(runBean.getPairedRun().booleanValue(),thriftRun.isPairedRun());
        assertEquals(runBean.getActualReadStructure(), thriftRun.getActualReadStructure());
        assertEquals(runBean.getImagedAreaPerLaneMM2(), ThriftConversionUtil.zeroAsNull(thriftRun.getImagedAreaPerLaneMM2())); //actual,exp

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
                assertEquals(libBean.getProject(),zLib.getProject());
                assertEquals(libBean.getWorkRequestId().longValue(),zLib.getWorkRequestId());
                if (libBean.getIsGssrSample()) {
                    assertEquals(libBean.getCollaboratorSampleId(),zLib.getSampleAlias());
                    assertEquals(libBean.getCollaboratorParticipantId(),zLib.getIndividual());
                    assertEquals(libBean.getMaterialType(),zLib.getGssrSampleType());
                }
                // else gssr copy and bsp copy may be different, and this is not reliable in our testing environment
                assertEquals(libBean.getAligner(),zLib.getAligner());
                assertEquals(libBean.getAnalysisType(),zLib.getAnalysisType());
                assertEquals(libBean.getBaitSetName(),zLib.getBaitSetName());
                assertEquals(libBean.getExpectedInsertSize(),zLib.getExpectedInsertSize());
                assertEquals(libBean.getInitiative(),zLib.getInitiative());
                assertEquals(libBean.getLabMeasuredInsertSize(),zLib.getLabMeasuredInsertSize() == 0 ? null : zLib.getLabMeasuredInsertSize());
                assertEquals(libBean.getLibrary(),zLib.getLibrary());
                assertEquals(libBean.getReferenceSequence(),zLib.getReferenceSequence());
                assertEquals(libBean.getReferenceSequenceVersion(),zLib.getReferenceSequenceVersion());
                assertEquals(libBean.getRestrictionEnzyme(),zLib.getRestrictionEnzyme());
                assertEquals(libBean.getRrbsSizeRange(),zLib.getRrbsSizeRange());
                assertEquals(libBean.doAggregation().booleanValue(),zLib.aggregate);

                if (libBean.getIsGssrSample()) {
                    assertEquals(libBean.getSpecies(),zLib.getOrganism() + ":" + zLib.getSpecies() + ":" + zLib.getStrain());
                }
                else {
                    if (HUMAN.equals(zLib.getOrganism())) {
                        if (!(HUMAN.equals(libBean.getSpecies()) || BSP_HUMAN.equals(libBean.getSpecies()))) {
                            fail("Not the right human:" + libBean.getSpecies());
                        }
                    }
                    else {
                        fail("Can't grok organism " + zLib.getOrganism());
                    }
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
                        assertEquals(libBean.getResearchProjectId(),pdo.getResearchProject().getBusinessKey(),PDO_COMPARISON_ERROR_MESSAGE);
                        assertEquals(libBean.getProductOrderKey(),pdo.getBusinessKey(),PDO_COMPARISON_ERROR_MESSAGE);
                        assertEquals(libBean.getResearchProjectName(),pdo.getResearchProject().getTitle(),PDO_COMPARISON_ERROR_MESSAGE);
                        assertEquals(libBean.getProductPartNumber(), pdo.getProduct().getPartNumber());
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
