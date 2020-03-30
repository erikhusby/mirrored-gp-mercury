package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import edu.mit.broad.prodinfo.thrift.lims.LibraryData;
import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniLane;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniLibrary;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRead;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.thrift.MockThriftService;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftFileAccessor;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemOfRecord;
import org.broadinstitute.gpinformatics.mercury.control.JaxRsUtils;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ImportFromSquidTest;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.zims.DevExperimentDataBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.IndexComponent;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.MolecularIndexingSchemeBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ThriftConversionUtil;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZamboniRead;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZamboniReadType;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.ALTERNATIVES;

@Test(groups = TestGroups.ALTERNATIVES)
@Dependent
public class IlluminaRunResourceTest extends Arquillian {

    public IlluminaRunResourceTest(){}

    @Inject
    private IlluminaRunResource runLaneResource;

    @Inject
    private ProductOrderDao pdoDao;

    // RequestScoped test bean may use multiple instances
    private TZamboniRun zamboniRun = new MockThriftService().fetchRun(RUN_NAME);

    public static final String RUN_NAME = "120320_SL-HBN_0159_AFCC0GHCACXX"; // has bsp samples
    public static final String RUN_BARCODE = "C0GHCACXX120320";
    public static final String MERCURY_RUN_BARCODE = "H7821ADXX131218";

    private final String CHAMBER = "2";

    public static final String WEBSERVICE_URL = "rest/IlluminaRun/query";

    public static final String HUMAN = "Human";

    public static final String BSP_HUMAN = "Homo : Homo sapiens";

    // PDO key applied to everything in the run
    public static final String PDO_KEY = "PDO-36";

    /**
     * Hack alert: we rely on fabricated squid data for this test.
     * If QA squid has been updated, you may need to run this:
     * <p/>
     * update work_request_material_descr wrmd set wrmd.product_order_name = 'PDO-36'
     * where
     * wrmd.work_request_material_id in
     * (select wrm.work_request_material_id from work_request_material wrm where wrm.work_request_id = 29225)
     */
    public static final String PDO_COMPARISON_ERROR_MESSAGE =
            "check run " + RUN_NAME + " in serialized .thrift file to make sure WRs are linked to " + PDO_KEY;

    private Map<Long, ProductOrder> wrIdToPDO = new HashMap<>();

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(TEST, MockThriftService.class)
                .addAsResource(ThriftFileAccessor.RUN_FILE);
    }


    /**
     * Does a test of {@link #RUN_NAME} {@link #CHAMBER}
     * directly in container.
     */
    @Test(groups = ALTERNATIVES)
    public void testZimsInContainer() throws Exception {
        // todo arz update the run in QA squid to link all WRS to the PDO
        wrIdToPDO.put(29225L, pdoDao.findByBusinessKey(PDO_KEY));

        ZimsIlluminaRun runBean = runLaneResource.getRun(RUN_NAME);

        doAssertions(zamboniRun, runBean, wrIdToPDO);
    }

    @Test(groups = ALTERNATIVES)
    public void testFetchByBarcodeInContainer() throws Exception {
        wrIdToPDO.put(29225L, pdoDao.findByBusinessKey(PDO_KEY));
        ZimsIlluminaRun runBean = runLaneResource.getRunByBarcode(RUN_BARCODE);
        doAssertions(zamboniRun, runBean, wrIdToPDO);
    }

    @Test(groups = ALTERNATIVES)
    public void testFetchByBarcodeMercuryInContainer() throws Exception {
        ZimsIlluminaRun runBean = runLaneResource.getRunByBarcode(MERCURY_RUN_BARCODE);
        Assert.assertEquals(runBean.getName(), "131218_SL-HDJ_0267_BFCH7821ADXX");
    }

    /**
     * Ensures that error handling makes it all the way through
     * out to HTTP
     */
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER,
            groups = ALTERNATIVES)
    @RunAsClient
    public void testErrorHandling(@ArquillianResource URL baseUrl)
            throws Exception {
        String url = RestServiceContainerTest.convertUrlToSecure(baseUrl) + WEBSERVICE_URL;

        ClientBuilder clientBuilder = JaxRsUtils.getClientBuilderAcceptCertificate();
//        clientConfig.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE);

        ZimsIlluminaRun run = clientBuilder.build().target(url)
                .request(MediaType.APPLICATION_JSON).get(ZimsIlluminaRun.class);
        Assert.assertNotNull(run);
        Assert.assertNotNull(run.getError());
    }


    /**
     * Does the same test as {@link #testZimsInContainer()},
     * but does it over http, which means it's actually checking
     * that various annotations like {@link javax.xml.bind.annotation.XmlAttribute}
     * are applied properly in {@link LibraryBean}.
     *
     * @param baseUrl
     */
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER,
            groups = ALTERNATIVES)
    @RunAsClient
    public void testZimsOverHttp(@ArquillianResource URL baseUrl)
            throws Exception {
        String url = RestServiceContainerTest.convertUrlToSecure(baseUrl) + WEBSERVICE_URL;

        ClientBuilder clientBuilder = JaxRsUtils.getClientBuilderAcceptCertificate();

        ZimsIlluminaRun run = clientBuilder.build().target(url)
                .queryParam("runName", RUN_NAME)
                .request(MediaType.APPLICATION_JSON).get(ZimsIlluminaRun.class);

        String rawJson = clientBuilder.build().target(url)
                .queryParam("runName", RUN_NAME)
                .request(MediaType.APPLICATION_JSON).get(String.class);
        Assert.assertFalse(rawJson.contains("@")); // might see this if you use XmlAttribute instead of XmlElement
        // KT and others like to see field names present w/ null values instead of missing entirely.
        // LibraryBean.overrideSampleFieldsFromBSP() enforces this rule.
        Assert.assertTrue(rawJson.contains("\"population\":null"));
        Assert.assertFalse(rawJson.contains("\"gssrSample\""));
        Assert.assertTrue(rawJson.contains("\"rootSample\""));
        Assert.assertTrue(rawJson.contains("\"productPartNumber\""));
        Assert.assertTrue(rawJson.contains("\"workRequestType\""));
        Assert.assertTrue(rawJson.contains("\"workRequestDomain\""));
        Assert.assertTrue(rawJson.contains("\"metadataSource\""));
        Assert.assertTrue(rawJson.contains("\"regulatoryDesignation\""));
        Assert.assertTrue(rawJson.contains("\"testType\""));
        Assert.assertTrue(rawJson.contains("\"buickCollectionDate\""));
        Assert.assertTrue(rawJson.contains("\"buickVisit\""));

        Assert.assertNotNull(run);
        Assert.assertEquals(run.getName(), RUN_NAME);

        doAssertions(zamboniRun, run, new HashMap<Long, ProductOrder>());

        boolean foundBspSample = false;
        boolean foundLcSet = false;
        boolean foundPdo = false;
        boolean foundTumor = false;
        for (ZimsIlluminaChamber zimsIlluminaChamber : run.getLanes()) {
            for (LibraryBean libraryBean : zimsIlluminaChamber.getLibraries()) {
                Assert.assertEquals(libraryBean.getRegulatoryDesignation(), "RESEARCH_ONLY");
                Assert.assertNull(libraryBean.getBuickVisit());
                Assert.assertNull(libraryBean.getBuickCollectionDate());
                Assert.assertNull(libraryBean.getBuickVisit());
                Assert.assertNull(libraryBean.getBuickCollectionDate());
                if (libraryBean.getLsid() != null) {
                    if (libraryBean.getLsid().contains("bsp")) {
                        foundBspSample = true;
                        Assert.assertNotNull(libraryBean.getRootSample());
                        Assert.assertNotNull(libraryBean.getCollection());
                        Assert.assertNotNull(libraryBean.getSampleId());
                    }
                    if ("broadinstitute.org:bsp.prod.sample:12MD2".equals(libraryBean.getLsid())) {
                        foundTumor = true;
                        Assert.assertEquals(libraryBean.getSampleType(), BspSampleData.TUMOR_IND);
                    }
                    if (libraryBean.getLcSet() != null) {
                        foundLcSet = true;
                    }
                    if (libraryBean.getProductOrderTitle() != null) {
                        foundPdo = true;
                    }
                    Assert.assertEquals(libraryBean.getMetadataSource(), MercurySample.BSP_METADATA_SOURCE);
                    Assert.assertNull(libraryBean.getTestType());
                }
            }
        }
        Assert.assertTrue(foundBspSample);
        Assert.assertTrue(foundLcSet);
        Assert.assertTrue(foundPdo);
        Assert.assertTrue(foundTumor);

        run = clientBuilder.build().target(url)
                .queryParam("runName", "Cheese ball")
                .request(MediaType.APPLICATION_JSON).get(ZimsIlluminaRun.class);
        Assert.assertNotNull(run.getError());
        // this is important: the pipeline hardcodes the "run isn't registered yet" response
        // and retries later
        Assert.assertTrue(run.getError().contains("Run Cheese ball doesn't appear to have been registered yet"));
    }

    /**
     * Tests Mercury chain of custody, over HTTP.  BettalimsMessageResourceTest.test8Lcsets can be used to create
     * test data.
     */
    @Test(groups = ALTERNATIVES, enabled = false)
    public void testZimsMercury() throws Exception {
        String url = ImportFromSquidTest.TEST_MERCURY_URL + "/rest/IlluminaRun/queryMercury";

        ClientBuilder clientBuilder = JaxRsUtils.getClientBuilderAcceptCertificate();

        ZimsIlluminaRun run = clientBuilder.build().target(url)
                .queryParam("runName", "TestRun03261516351364325439075.txt")
                .request(MediaType.APPLICATION_JSON).get(ZimsIlluminaRun.class);
        Assert.assertEquals(run.getLanes().size(), 8, "Wrong number of lanes");
    }

    // Have to return something other than void, otherwise TestNG will think it's a test.
    public static boolean doAssertions(TZamboniRun thriftRun, ZimsIlluminaRun runBean,
                                       Map<Long, ProductOrder> wrIdToPDO) {
        Assert.assertNull(runBean.getError());
        Assert.assertEquals(runBean.getLanes().size(), thriftRun.getLanes().size());
        Assert.assertEquals(runBean.getFlowcellBarcode(), thriftRun.getFlowcellBarcode());
        Assert.assertEquals(runBean.getSequencer(), thriftRun.getSequencer());
        Assert.assertEquals(runBean.getSequencerModel(), thriftRun.getSequencerModel());
        Assert.assertEquals(runBean.getPairedRun().booleanValue(), thriftRun.isPairedRun());
        Assert.assertEquals(runBean.getActualReadStructure(), thriftRun.getActualReadStructure());
        Assert.assertEquals(runBean.getImagedAreaPerLaneMM2(),
                ThriftConversionUtil.zeroAsNull(thriftRun.getImagedAreaPerLaneMM2())); //actual,exp
        Assert.assertEquals(runBean.getSystemOfRecord(), SystemOfRecord.System.SQUID);
        Assert.assertEquals(runBean.getSetupReadStructure(), thriftRun.getSetupReadStructure());
        Assert.assertEquals(runBean.getLanesSequenced(), thriftRun.getLanesSequenced());
        Assert.assertEquals(runBean.getRunFolder(), thriftRun.getRunFolder());

        for (ZimsIlluminaChamber lane : runBean.getLanes()) {
            int laneNum = Integer.parseInt(lane.getName());
            TZamboniLane zamboniLane = thriftRun.getLanes().get(laneNum - 1);
            Assert.assertEquals(lane.getSequencedLibrary(), zamboniLane.getSequencedLibraryName());
        }

        doReadAssertions(thriftRun, runBean);

        Assert.assertEquals(runBean.getRunDateString(), thriftRun.getRunDate());
        boolean hasRealPreCircSize = false;
        boolean hasRealLabInsertSize = false;
        boolean hasDevAliquotData = false;
        for (TZamboniLane thriftLane : thriftRun.getLanes()) {
            ZimsIlluminaChamber lane = getLane(Short.toString(thriftLane.getLaneNumber()), runBean);
            doAssertions(thriftLane, lane, wrIdToPDO);

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
        Assert.assertTrue(hasDevAliquotData);
        Assert.assertTrue(hasRealLabInsertSize);
        Assert.assertTrue(hasRealPreCircSize);
        return true;
    }

    private static void doReadAssertions(TZamboniRun thriftRun, ZimsIlluminaRun runBean) {
        if (thriftRun.getReads() != null && runBean.getReads() != null) {
            Assert.assertEquals(runBean.getReads().size(), thriftRun.getReads().size());
            for (TZamboniRead thriftRead : thriftRun.getReads()) {
                boolean haveIt = false;
                for (ZamboniRead beanRead : runBean.getReads()) {
                    Integer firstCycle = ThriftConversionUtil.zeroAsNull(thriftRead.getFirstCycle());
                    Integer readLength = ThriftConversionUtil.zeroAsNull(thriftRead.getLength());

                    if (firstCycle.equals(beanRead.getFirstCycle()) && readLength.equals(beanRead.getLength())) {
                        if (thriftRead.getReadType() != null) {
                            String readTypeName = thriftRead.getReadType().name();
                            switch (readTypeName) {
                            case ZamboniRead.INDEX:
                                Assert.assertEquals(beanRead.getReadType(), ZamboniReadType.INDEX);
                                haveIt = true;
                                break;
                            case ZamboniRead.TEMPLATE:
                                Assert.assertEquals(beanRead.getReadType(), ZamboniReadType.TEMPLATE);
                                haveIt = true;
                                break;
                            default:
                                Assert.fail("Read type " + thriftRead.getReadType() + " is unknown");
                                break;
                            }
                        } else {
                            Assert.assertNull(beanRead.getReadType());
                        }
                    }
                }
                Assert.assertTrue(haveIt);
            }
        } else if (thriftRun.getReads() == null && runBean.getReads() == null) {
            // ok
        } else {
            Assert.fail("Reads are not the same");
        }
    }

    private static void doAssertions(TZamboniLane zLane, ZimsIlluminaChamber laneBean,
                                     Map<Long, ProductOrder> wrIdToPDO) {
        Assert.assertEquals(laneBean.getName(), Short.toString(zLane.getLaneNumber()));
        Assert.assertEquals(laneBean.getPrimer(), zLane.getPrimer());
        Assert.assertEquals(laneBean.getLibraries().size(), zLane.getLibraries().size());

        Assert.assertNotNull(laneBean.getSequencedLibrary());
        Assert.assertFalse(laneBean.getSequencedLibrary().isEmpty());
        Assert.assertEquals(laneBean.getSequencedLibrary(), zLane.getSequencedLibraryName());

        for (TZamboniLibrary thriftLib : zLane.getLibraries()) {
            doAssertions(thriftLib, laneBean.getLibraries(), wrIdToPDO.get(thriftLib.getWorkRequestId()));
        }
    }

    private static void doAssertions(TZamboniLibrary zLib, Collection<LibraryBean> libBeans, ProductOrder pdo) {
        boolean foundIt = false;
        boolean foundPDO = false;
        boolean foundLcSet = false;

        for (LibraryBean libBean : libBeans) {
            Assert.assertNotNull(libBean.getLibrary());
            if (libBean.getLibrary().equals(zLib.getLibrary())) {
                foundIt = true;
                Assert.assertEquals(libBean.getProject(), zLib.getProject());
                Assert.assertEquals(libBean.getWorkRequestId().longValue(), zLib.getWorkRequestId());
                if (libBean.getIsGssrSample()) {
                    Assert.assertEquals(libBean.getCollaboratorSampleId(), zLib.getSampleAlias());
                    Assert.assertEquals(libBean.getCollaboratorParticipantId(), zLib.getIndividual());
                    Assert.assertEquals(libBean.getMaterialType(), zLib.getGssrSampleType());
                }
                // else gssr copy and bsp copy may be different, and this is not reliable in our testing environment
                Assert.assertEquals(libBean.getAligner(), zLib.getAligner());
                Assert.assertEquals(libBean.getAnalysisType(), zLib.getAnalysisType());
                Assert.assertEquals(libBean.getBaitSetName(), zLib.getBaitSetName());
                Assert.assertEquals(libBean.getExpectedInsertSize(), zLib.getExpectedInsertSize());
                Assert.assertEquals(libBean.getInitiative(), zLib.getInitiative());
                Assert.assertEquals(libBean.getLabMeasuredInsertSize(),
                        zLib.getLabMeasuredInsertSize() == 0 ? null : zLib.getLabMeasuredInsertSize());
                Assert.assertEquals(libBean.getLibrary(), zLib.getLibrary());
                Assert.assertEquals(libBean.getReferenceSequence(), zLib.getReferenceSequence());
                Assert.assertEquals(libBean.getReferenceSequenceVersion(), zLib.getReferenceSequenceVersion());
                Assert.assertEquals(libBean.getRestrictionEnzyme(), zLib.getRestrictionEnzyme());
                Assert.assertEquals(libBean.getRrbsSizeRange(), zLib.getRrbsSizeRange());
                Assert.assertEquals(libBean.doAggregation().booleanValue(), zLib.aggregate);

                if (libBean.getIsGssrSample()) {
                    Assert.assertEquals(libBean.getSpecies(),
                            zLib.getOrganism() + ":" + zLib.getSpecies() + ":" + zLib.getStrain());
                } else {
                    if (HUMAN.equals(zLib.getOrganism())) {
                        if (!(HUMAN.equals(libBean.getSpecies()) || BSP_HUMAN.equals(libBean.getSpecies()))) {
                            Assert.fail("Not the right human:" + libBean.getSpecies());
                        }
                    } else {
                        Assert.fail("Can't grok organism " + zLib.getOrganism());
                    }
                }
                Assert.assertEquals(libBean.getLsid(), zLib.getLsid());
                checkEquality(zLib.getMolecularIndexes(), libBean.getMolecularIndexingScheme());
                Assert.assertEquals(libBean.getGssrBarcodes(), zLib.getGssrBarcodes());
                checkEquality(libBean.getDevExperimentData(), zLib.getDevExperimentData());
                Assert.assertEquals(libBean.getCustomAmpliconSetNames(), zLib.getCustomAmpliconSetNames());

                if (zLib.getPdoKey() != null) {
                    foundPDO = true;
                    if (pdo != null) {
                        Assert.assertEquals(libBean.getProductOrderTitle(), pdo.getTitle(),
                                PDO_COMPARISON_ERROR_MESSAGE);
                        Assert.assertEquals(libBean.getResearchProjectId(), pdo.getResearchProject().getBusinessKey(),
                                PDO_COMPARISON_ERROR_MESSAGE);
                        Assert.assertEquals(libBean.getProductOrderKey(), pdo.getBusinessKey(),
                                PDO_COMPARISON_ERROR_MESSAGE);
                        Assert.assertEquals(libBean.getResearchProjectName(), pdo.getResearchProject().getTitle(),
                                PDO_COMPARISON_ERROR_MESSAGE);
                        Assert.assertEquals(libBean.getProductPartNumber(), pdo.getProduct().getPartNumber());
                    }
                }

                if (zLib.getLcset() != null) {
                    foundLcSet = true;
                    Assert.assertEquals(libBean.getLcSet(), zLib.getLcset());
                }

                if (libBean.getLcSet() != null && zLib.getLcset() == null) {
                    Assert.fail("bean has lcset " + libBean.getLcSet() + ", but thrift library has no lc set.");
                }
                Assert.assertEquals(libBean.getWorkRequestType(), zLib.getWorkRequestType());
                Assert.assertEquals(libBean.getWorkRequestDomain(), zLib.getWorkRequestDomain());
            }
        }

        Assert.assertTrue((foundLcSet && zLib.getLcset() != null) || (zLib.getLcset() == null));
        Assert.assertTrue(foundIt);
        Assert.assertTrue(foundPDO || pdo == null);

    }

    private static void checkEquality(DevExperimentDataBean devExperimentBean,
                                      TZDevExperimentData thriftExperimentData) {
        if (devExperimentBean != null && thriftExperimentData != null) {
            Assert.assertEquals(devExperimentBean.getConditions().size(),
                    thriftExperimentData.getConditionChain().size());
            Collection<String> thriftConditions = thriftExperimentData.getConditionChain();
            for (String condition : devExperimentBean.getConditions()) {
                Assert.assertTrue(thriftConditions.contains(condition));
            }
            Assert.assertEquals(devExperimentBean.getExperiment(), thriftExperimentData.getExperiment());
        } else if (devExperimentBean == null && thriftExperimentData == null) {
            return;
        } else {
            Assert.fail("dev experiment bean and thrift bean do not agree.");
        }
    }

    private static void checkEquality(MolecularIndexingScheme thriftScheme, MolecularIndexingSchemeBean beanScheme) {
        if (thriftScheme == null && beanScheme == null) {
            return;
        } else {
            if (thriftScheme != null && beanScheme != null) {
                Assert.assertEquals(beanScheme.getName(), thriftScheme.getName());
                Assert.assertEquals(beanScheme.getSequences().size(), thriftScheme.getSequences().size());

                for (Map.Entry<IndexPosition, String> thriftEntry : thriftScheme.getSequences().entrySet()) {
                    IndexComponent singleIndex = new IndexComponent(thriftEntry.getKey(), thriftEntry.getValue());
                    Assert.assertTrue(beanScheme.getSequences().contains(singleIndex));
                }
            } else {
                Assert.fail("Thrift scheme " + thriftScheme + " is different from scheme bean " + beanScheme);
            }
        }
    }

    private static ZimsIlluminaChamber getLane(String laneName, ZimsIlluminaRun run) {
        for (ZimsIlluminaChamber lane : run.getLanes()) {
            if (laneName.equals(lane.getName())) {
                return lane;
            }
        }
        return null;
    }

    public List<LibraryData> fetchLibraryDetailsByLibraryName(List<String> libraryNames) {
        List<LibraryData> libraryDataList = new ArrayList<>();
        for (String libraryName : libraryNames) {
            LibraryData libraryData = new LibraryData();
            libraryData.setLibraryName(libraryName);
            libraryData.setLibraryNameIsSet(true);
            libraryDataList.add(libraryData);
        }

        return libraryDataList;
    }
}
