package org.broadinstitute.gpinformatics.mercury.control.vessel;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentration;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentrationImpl;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory.ReagentDto;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ChildVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.UploadQuantsActionBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata.LabEventMetadataType.DilutionFactor;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata.LabEventMetadataType.SensitivityFactor;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.ALL384;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.ALL96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.P384_96TIP_1INTERVAL_A1;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.P384_96TIP_1INTERVAL_A2;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.P384_96TIP_1INTERVAL_B1;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.P384_96TIP_1INTERVAL_B2;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf384;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class PicoToBspContainerTest extends Arquillian {
    private static final String RACK_ROWS = "ABCDEFGH";
    private static final int RACK_COLUMNS = 12;
    private static final Log log = LogFactory.getLog(PicoToBspContainerTest.class);
    private static final boolean PRINT_QUANT_VALUES = false;
    private static final BigDecimal BIG_DECIMAL_60 = new BigDecimal("60");
    private static final List<ReagentDto> REAGENT_DTOS = new ArrayList<ReagentDto>() {{
        add(new ReagentDto("HS buffer", "RG-12134", DateUtils.addYears(new Date(), 1)));
    }};

    @Inject
    private VesselEjb vesselEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private VarioskanParserContainerTest varioskanParserContainerTest;

    @Inject
    private LabVesselFactory labVesselFactory;

    @Inject
    private UserBean userBean;

    @Inject
    private UploadQuantsActionBean uploadQuantsActionBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    private Multimap<VesselPosition, VesselPosition> map96to384 = HashMultimap.create();
    private BSPConfig bspConfig = BSPConfig.produce(DEV);
    private BSPSampleSearchService bspSampleSearchService = new BSPSampleSearchServiceImpl(bspConfig);
    private BSPSampleDataFetcher dataFetcher = new BSPSampleDataFetcher(bspSampleSearchService, bspConfig);
    private BSPSetVolumeConcentrationImpl bspSetVolumeConcentration = new BSPSetVolumeConcentrationImpl(bspConfig);
    private BettaLimsMessageTestFactory bettalimsFactory = new BettaLimsMessageTestFactory(true);
    private long testTimestamp = System.currentTimeMillis() / 1000;
    private MessageCollection messageCollection;

    // Each test case is a row in this array. The fields are:
    // #research, #crsp, quantType, #dilutionWells, #microfluorWells, sensitivityFactor, dilutionFactor, bsp quant
    private Object[][] testCases = {
            // Duplicate pico, Rack -> 1st blackPlate(96 well) and rack -> 2nd blackPlate(96 well).
            {41, 54, 2, 0, 96, 1, 1, 3.3348},
            {96,  0, 2, 0, 96, 1, 1, 3.3348},
            {0,   1, 2, 0, 96, 1, 1, 3.3348},
            {40, 55, 2, 0, 96, 2, 1, 1.6674},
            // Triplicate pico, rack -> blackPlate(384 well) e.g. CO-20552814.
            {96,  0, 3, 0, 384, 1, 1, 53.745},
            {0,  96, 3, 0, 384, 1, 1, 53.745},
            {50, 46, 3, 0, 384, 1, 1, 53.745},
            {4,   4, 3, 0, 384, 2, 1, 26.872},
            // Triplicate pico, rack -> dilutionPlate(96) -> blackPlate(384).
            {47, 47, 3, 96, 384, 1,  1, 53.745},
            {96,  0, 3, 96, 384, 1, 10, 537.45},
            // Triplicate pico, rack -> dilutionPlate(384) -> blackPlate(384).
            {24, 24, 3, 384, 384, 1,  1, 53.745},
            {6,   0, 3, 384, 384, 2, 10, 268.72},
            {96,  0, 3, 384, 384, 1, 10, 537.45},
            // Single pico (typically a retest of a triplicate pico), rack -> dilutionPlate(96) -> blackPlate(96).
            {40, 55, 1, 96, 96, 1,  1, 3.3779},
            {4,   5, 1, 96, 96, 2,  1, 1.6889},
            {96,  0, 1, 96, 96, 1, 10, 33.779},
    };

    // Research tubes, i.e. currently existing BSP genomic DNA tubes and their sample names.
    private SortedMap<String, String> bspTubeMap = new TreeMap<String, String>() {{
        put("1140117899", "SM-D7G11");
        put("1140145281", "SM-DBQFT");
        put("1140145341", "SM-DBQHS");
        put("1140145283", "SM-DBQFV");
        put("1140145313", "SM-DBQGQ");
        put("1140145285", "SM-DBQFX");
        put("1140145286", "SM-DBQFY");
        put("1140141779", "SM-DBQFZ");
        put("1140141772", "SM-DBQG1");
        put("1140145289", "SM-DBQG2");
        put("1140145356", "SM-DBQHY");
        put("1140145291", "SM-DBQG4");
        put("1140145303", "SM-DBQG5");
        put("1140145302", "SM-DBQG6");
        put("1140145301", "SM-DBQG7");
        put("1140145300", "SM-DBQG8");
        put("1140145330", "SM-DBQH8");
        put("1140145290", "SM-DBQG3");
        put("1140141803", "SM-DBQGB");
        put("1140145296", "SM-DBQGC");
        put("1140145306", "SM-DBQGJ");
        put("1140145294", "SM-DBQGE");
        put("1140145293", "SM-DBQGF");
        put("1140145354", "SM-DBQHW");
        put("1140145304", "SM-DBQGH");
        put("1140145305", "SM-DBQGI");
        put("1140145284", "SM-DBQFW");
        put("1140145312", "SM-DBQGP");
        put("1140145328", "SM-DBQH6");
        put("1140145309", "SM-DBQGM");
        put("1140145310", "SM-DBQGN");
        put("1140145311", "SM-DBQGO");
        put("1140145295", "SM-DBQGD");
        put("1140145320", "SM-DBQH1");
        put("1140145314", "SM-DBQGR");
        put("1140145315", "SM-DBQGS");
        put("1140145327", "SM-DBQGT");
        put("1140145342", "SM-DBQHR");
        put("1140145370", "SM-DBQIC");
        put("1140145324", "SM-DBQGW");
        put("1140145331", "SM-DBQH9");
        put("1140145322", "SM-DBQGY");
        put("1140145321", "SM-DBQGZ");
        put("1140145338", "SM-DBQHG");
        put("1140145323", "SM-DBQGX");
        put("1140145292", "SM-DBQGG");
        put("1140145317", "SM-DBQH4");
        put("1140145316", "SM-DBQH5");
        put("1140145362", "SM-DBQI5");
        put("1140145329", "SM-DBQH7");
        put("1140145280", "SM-DBQFS");
        put("1140145307", "SM-DBQGK");
        put("1140145319", "SM-DBQH2");
        put("1140145352", "SM-DBQHU");
        put("1140145334", "SM-DBQHC");
        put("1140145335", "SM-DBQHD");
        put("1140145336", "SM-DBQHE");
        put("1140145373", "SM-DBQI9");
        put("1140145345", "SM-DBQHO");
        put("1140145339", "SM-DBQHH");
        put("1140145351", "SM-DBQHI");
        put("1140145350", "SM-DBQHJ");
        put("1140145349", "SM-DBQHK");
        put("1140145346", "SM-DBQHN");
        put("1140145359", "SM-DBQI2");
        put("1140145299", "SM-DBQG9");
        put("1140145358", "SM-DBQI1");
        put("1140145344", "SM-DBQHP");
        put("1140145357", "SM-DBQHZ");
        put("1140145348", "SM-DBQHL");
        put("1140145372", "SM-DBQIA");
        put("1140145340", "SM-DBQHT");
        put("1140145343", "SM-DBQHQ");
        put("1140145360", "SM-DBQI3");
        put("1140145308", "SM-DBQGL");
        put("1140145355", "SM-DBQHX");
        put("1140145353", "SM-DBQHV");
        put("1140145326", "SM-DBQGU");
        put("1140145298", "SM-DBQGA");
        put("1140145325", "SM-DBQGV");
        put("1140145337", "SM-DBQHF");
        put("1140145361", "SM-DBQI4");
        put("1140145347", "SM-DBQHM");
        put("1140145363", "SM-DBQI6");
        put("1140145375", "SM-DBQI7");
        put("1140145374", "SM-DBQI8");
        put("1140145318", "SM-DBQH3");
        put("1140145282", "SM-DBQFU");
        put("1140145371", "SM-DBQIB");
        put("1140145333", "SM-DBQHB");
        put("1140145369", "SM-DBQID");
        put("1140145368", "SM-DBQIE");
        put("1140145367", "SM-DBQIF");
        put("1140145366", "SM-DBQIG");
        put("1140145365", "SM-DBQIH");
        put("1140145332", "SM-DBQHA");
    }};

    private List<BarcodedTube> mercuryTubes = new ArrayList<>();
    private List<BarcodedTube> bspTubes = new ArrayList<>();

    private void oneTimeInit() {
        // Makes the 96 crsp tubes that are used for all tests.
        for (int i = 0; i < 96; ++i) {
            String tubeBarcode = String.format("%011d%03d", testTimestamp, i);
            BarcodedTube tube = new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075);
            tube.setVolume(BIG_DECIMAL_60);
            labVesselDao.persist(tube);

            String materialName = MaterialType.DNA_DNA_GENOMIC.getDisplayName();
            MercurySample mercurySample = new MercurySample("SM-" + tubeBarcode,
                    ImmutableSet.of(new Metadata(Metadata.Key.MATERIAL_TYPE, materialName)));
            mercurySample.addLabVessel(tube);
            labVesselDao.persist(mercurySample);

            mercuryTubes.add(tube);
        }

        // Looks up the 96 research tubes that are used for all tests.
        for (Map.Entry<String, String> entry : bspTubeMap.entrySet()) {
            BarcodedTube tube = OrmUtil.proxySafeCast(labVesselDao.findByIdentifier(entry.getKey()),
                    BarcodedTube.class);
            tube.setVolume(BIG_DECIMAL_60);
            bspTubes.add(tube);
        }

        // Makes a map of 96 rack positions to their corresponding 384 well positions.
        for (int i = 0; i < 96; ++i) {
            VesselPosition key = ALL96.getWells().get(i);
            map96to384.put(key, P384_96TIP_1INTERVAL_A1.getWells().get(i));
            map96to384.put(key, P384_96TIP_1INTERVAL_A2.getWells().get(i));
            map96to384.put(key, P384_96TIP_1INTERVAL_B1.getWells().get(i));
        }
    }

    private void testCaseInit() {
        // Ensures each test case makes unique rack/plate barcodes.
        testTimestamp += 10;
        messageCollection = new MessageCollection();

        // Zeros the value BSP has for concentration on our test samples.
        for (String smId : bspTubeMap.values()) {
            String result = bspSetVolumeConcentration.setVolumeAndConcentration(smId, BIG_DECIMAL_60,
                    BigDecimal.ZERO, null);
            Assert.assertEquals(result, BSPSetVolumeConcentration.RESULT_OK);
        }
        // Verifies zero.
        Map<String, BspSampleData> bspSampleDataMap = dataFetcher.fetchSampleData(bspTubeMap.values(),
                BSPSampleSearchColumn.CONCENTRATION);
        for (String smId : bspTubeMap.values()) {
            Assert.assertEquals(bspSampleDataMap.get(smId).getConcentration(), 0d);
        }
    }

    /** Runs all the test cases. */
    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testCases() throws Exception {
        oneTimeInit();
        int caseIdx = 0;
        while (caseIdx < testCases.length) {
            testCaseInit();
            // Parses the testCase array into test parameters, then runs the quant import test.
            int tokenIdx = 0;
            int research = (Integer)testCases[caseIdx][tokenIdx++];
            int crsp = (Integer)testCases[caseIdx][tokenIdx++];
            int quantCardinality = (Integer)testCases[caseIdx][tokenIdx++];
            int dilutionWells = (Integer)testCases[caseIdx][tokenIdx++];
            int microfluorWells = (Integer)testCases[caseIdx][tokenIdx++];
            int sensitivityFactor = (Integer)testCases[caseIdx][tokenIdx++];
            int dilutionFactor = (Integer)testCases[caseIdx][tokenIdx++];
            double bspA01Quant = (Double)testCases[caseIdx][tokenIdx++];
            caseIdx++;

            Assert.assertTrue(research + crsp <= Eppendorf96.getVesselGeometry().getCapacity());
            Assert.assertTrue(quantCardinality > 0 && quantCardinality < 4);

            StaticPlate.PlateType dilutionPlate = dilutionWells == 96 ? Eppendorf96 :
                    dilutionWells == 384 ? Eppendorf384 : null;
            StaticPlate.PlateType microfluorPlate = microfluorWells == 96 ? Eppendorf96 :
                    microfluorWells == 384 ? Eppendorf384 : null;
            Assert.assertTrue(dilutionWells == 0 || dilutionPlate != null, "No support for " + dilutionWells);
            Assert.assertNotNull(microfluorPlate, "No support for " + microfluorWells);

            log.info("Testing " +
                    (quantCardinality == 1 ? "single" : quantCardinality == 2 ? "duplicate" : "triplicate") +
                     " quant, " + (dilutionPlate == null ? "no" : dilutionWells) + " dilution, " +
                    research + " research + " +  crsp + " clinical, " +
                    " sensitivity=" + sensitivityFactor + ", dilution=" + dilutionFactor);

            Dto dto = new Dto(quantCardinality, research, crsp, dilutionPlate, microfluorPlate,
                    sensitivityFactor, dilutionFactor);

            makeVessels(dto);
            makeTransferEvents(dto);
            sendSpreadsheet(dto);
            Assert.assertEquals(dto.getRunAndFormation().getLeft().getLabMetrics().size(),
                    (research + crsp) * (quantCardinality + 1));
            extractResearchTubeQuantFromMercury(dto);
            checkMissingMetrics(dto);
            validateBspMetrics(dto, bspA01Quant);
        }
    }

    /** Validates BSP sample concentration with Mercury's lab metric. */
    private void validateBspMetrics(Dto dto, double bspA01Quant) {
        // Fetches sample concentrations from BSP.
        Map<String, BspSampleData> bspSampleDataMap = dataFetcher.fetchSampleData(bspTubeMap.values(),
                BSPSampleSearchColumn.CONCENTRATION);
        for (Map.Entry<String, String> entry : bspTubeMap.entrySet()) {
            String barcode = entry.getKey();
            String smId = bspTubeMap.get(barcode);
            double bspConc = bspSampleDataMap.get(smId).getConcentration();

            if (dto.getTubeToPosition().containsKey(barcode)) {
                String rackPosition = dto.getTubeToPosition().get(barcode);
                double mercury = dto.getSmIdQuant().get(smId);
                boolean miscompare = Math.abs(bspConc - mercury) > 0.05;
                if (PRINT_QUANT_VALUES) {
                    log.info(String.format("At %s %s  bsp: %-10s mercury: %-6s %s",
                            rackPosition, smId, String.valueOf(bspSampleDataMap.get(smId).getConcentration()),
                            String.valueOf(dto.getSmIdQuant().get(smId)), (miscompare ?
                                    ("MISCOMPARE (wells " + StringUtils.join(map96to384.get(
                                            VesselPosition.valueOf(rackPosition)), " ") + ")") : "")));
                } else {
                    Assert.assertFalse(miscompare,
                            rackPosition + " " + smId + " bsp has " + bspConc + " mercury has " + mercury);

                    // Tests the expected bsp quant for position A01.
                    if (rackPosition.equals("A01")) {
                        Assert.assertTrue(Math.abs(bspConc - bspA01Quant) < 0.01,
                                rackPosition + " " + smId + " bsp has " + bspConc + " expected " + bspA01Quant);
                    }
                }
            } else {
                // Verify that BSP conc was not set for an unused tube.
                Assert.assertTrue(MathUtils.isSame(bspConc, 0d),
                        "Unused tube " + smId + " should be 0.0 and bsp has " + bspConc);
            }
        }
    }

    private void extractResearchTubeQuantFromMercury(Dto dto) {
        for (LabMetric labMetric : dto.getRunAndFormation().getLeft().getLabMetrics()) {
            if (labMetric.getName() == LabMetric.MetricType.INITIAL_PICO &&
                    OrmUtil.proxySafeIsInstance(labMetric.getLabVessel(), BarcodedTube.class)) {

                LabVessel labVessel = labMetric.getLabVessel();
                MercurySample mercurySample = labVessel.getMercurySamples().iterator().next();
                if (mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                    dto.getSmIdQuant().put(mercurySample.getSampleKey(), labMetric.getValue().doubleValue());
                }
            }
        }
    }

    private void checkMissingMetrics(Dto dto) {
        List<String> actualBarcodes = new ArrayList<>();
        List<String> actualPositions = new ArrayList<>();
        for (LabMetric labMetric : dto.getRunAndFormation().getLeft().getLabMetrics()) {
            LabVessel labVessel = labMetric.getLabVessel();
            actualBarcodes.add(labVessel.getLabel());
            actualPositions.add(labMetric.getVesselPosition());
            if (OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                MercurySample mercurySample = labVessel.getMercurySamples().iterator().next();
                if (mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                    dto.getSmIdQuant().put(mercurySample.getSampleKey(), labMetric.getValue().doubleValue());
                }
            }
        }
        Collection<String> test = CollectionUtils.subtract(dto.getTubeToPosition().keySet(), actualBarcodes);
        if (!test.isEmpty()) {
            List<String> list = new ArrayList<>(test);
            Collections.sort(list);
            Assert.fail("Missing " + StringUtils.join(test, "\n "));
        }
        test = CollectionUtils.subtract(dto.getTubeToPosition().values(), actualPositions);
        if (!test.isEmpty()) {
            List<String> list = new ArrayList<>(test);
            Collections.sort(list);
            Assert.fail("Missing " + StringUtils.join(test, "\n "));
        }
    }

    /** Returns a position in a 96 tube rack for the given tube counter. */
    private String positionFor(int i) {
        return String.format("%s%02d", RACK_ROWS.charAt(i / RACK_COLUMNS), (i % RACK_COLUMNS) + 1);
    }

    private void makeVessels(Dto dto) throws Exception {
        // Generates the rack and plate barcodes.
        int numberMicrofluorPlates = dto.getMicrofluorPlateType() == Eppendorf384 ? 1 : dto.getQuantCardinality();
        String rackBarcode = String.format("%011d%1d", testTimestamp, 0);
        String dilutionPlateBarcode = (dto.getDilutionPlateType() != null) ?
                String.format("%011d%1d", testTimestamp, 1) : null;
        String[] blackPlateBarcodes = new String[numberMicrofluorPlates];
        for (int i = 0; i < numberMicrofluorPlates; ++i) {
            blackPlateBarcodes[i] = String.format("%011d%1d", testTimestamp, i + 2);
        }
        // Makes the tube mix.
        List<ChildVesselBean> tubeBeans = new ArrayList<>();
        Map<String, String> mapTubeToPosition = new HashMap<>();

        for (int i = 0; i < dto.getTotalTubeCount(); ++i) {
            BarcodedTube tube = (i < dto.getResearchTubeCount() ? bspTubes : mercuryTubes).get(i);
            String barcode = tube.getLabel();
            String position = positionFor(i);
            mapTubeToPosition.put(barcode, position);
            Assert.assertTrue(CollectionUtils.isNotEmpty(tube.getMercurySamples()), "barcode " + barcode);
            tubeBeans.add(new ChildVesselBean(barcode, tube.getMercurySamples().iterator().next().getSampleKey(),
                    tube.getTubeType().getAutomationName(), position));
        }
        // Builds the racks and plates.
        List<LabVessel> labVessels = labVesselFactory.buildLabVessels(Arrays.asList(new ParentVesselBean(
                        rackBarcode, rackBarcode, RackOfTubes.RackType.Matrix96.getDisplayName(), tubeBeans)),
                "epolk", new Date(), null, MercurySample.MetadataSource.MERCURY);
        labVesselDao.persistAll(labVessels);
        labVesselDao.flush();

        dto.setRackBarcode(rackBarcode);
        dto.setTubeToPosition(mapTubeToPosition);
        dto.setBlackPlateBarcodes(blackPlateBarcodes);
        dto.setDilutionPlateBarcode(dilutionPlateBarcode);
    }


    private void makeTransferEvents(Dto dto) {
        Assert.assertTrue(dto.getDilutionFactor() == 1 || dto.getDilutionPlateType() != null,
                "No support for dilution factor without a dilution plate");

        if (dto.getQuantCardinality() == 1 || dto.getQuantCardinality() == 2) {
            Assert.assertEquals(dto.getMicrofluorPlateType(), Eppendorf96);
            Assert.assertEquals(dto.getBlackPlateBarcodes().length, dto.getQuantCardinality());

            if (dto.getDilutionPlateType() != null) {
                Assert.assertTrue(dto.getDilutionPlateType() == Eppendorf96);

                BettaLIMSMessage dilutionMessage = new BettaLIMSMessage();
                PlateTransferEventType dilutionTransfer = bettalimsFactory.buildRackToPlate(
                        LabEventType.PICO_DILUTION_TRANSFER.getName(), dto.getRackBarcode(),
                        dto.getTubeToPosition(), dto.getDilutionPlateBarcode());

                dilutionTransfer.getPlate().setPhysType(dto.getDilutionPlateType().getAutomationName());
                if (dto.getDilutionFactor() != 1) {
                    setFactor(dilutionTransfer, dto.getDilutionFactor(), DilutionFactor);
                }
                dilutionMessage.getPlateTransferEvent().add(dilutionTransfer);
                bettalimsFactory.advanceTime();
                labEventFactory.buildFromBettaLims(dilutionMessage);
            }

            // Either Rack to microfluor plate, or Dilution plate to microfluor plate.
            BettaLIMSMessage microfluorMessage = new BettaLIMSMessage();
            for (String blackPlateBarcode : dto.getBlackPlateBarcodes()) {
                PlateTransferEventType microfluorTransfer;

                if (dto.getDilutionPlateType() == null) {
                    microfluorTransfer = bettalimsFactory.buildRackToPlate(
                            LabEventType.PICO_TRANSFER.getName(),
                            dto.getRackBarcode(), dto.getTubeToPosition(), blackPlateBarcode);
                } else {
                    microfluorTransfer = bettalimsFactory.buildPlateToPlate(
                            LabEventType.PICO_MICROFLUOR_TRANSFER.getName(),
                            dto.getDilutionPlateBarcode(), blackPlateBarcode);
                    microfluorTransfer.getSourcePlate().setPhysType(dto.getDilutionPlateType().getAutomationName());
                    microfluorTransfer.getSourcePlate().setSection(ALL96.getSectionName());
                }

                microfluorTransfer.getPlate().setPhysType(dto.getMicrofluorPlateType().getAutomationName());
                microfluorTransfer.getPlate().setSection(ALL96.getSectionName());
                microfluorMessage.getPlateTransferEvent().add(microfluorTransfer);
                bettalimsFactory.advanceTime();
            }
            labEventFactory.buildFromBettaLims(microfluorMessage);

            if (dto.getSensitivityFactor() != 1) {
                BettaLIMSMessage bufferAdditionMessage = new BettaLIMSMessage();
                for (String blackPlateBarcode : dto.getBlackPlateBarcodes()) {
                    PlateEventType bufferAddition = bettalimsFactory.buildPlateEvent("PicoBufferAddition",
                            blackPlateBarcode, REAGENT_DTOS);
                    bufferAddition.getPlate().setPhysType(dto.getMicrofluorPlateType().getAutomationName());
                    bufferAddition.getPlate().setSection(ALL96.getSectionName());
                    setFactor(bufferAddition, dto.getSensitivityFactor(), SensitivityFactor);
                    bufferAdditionMessage.getPlateEvent().add(bufferAddition);
                    bettalimsFactory.advanceTime();
                }
                labEventFactory.buildFromBettaLims(bufferAdditionMessage);
            }

        } else if (dto.getQuantCardinality() == 3) {
            Assert.assertEquals(dto.getMicrofluorPlateType(), Eppendorf384);
            Assert.assertEquals(dto.getBlackPlateBarcodes().length, 1);

            if (dto.getDilutionPlateType() != null) {
                Assert.assertTrue(dto.getDilutionPlateType() == Eppendorf96 ||
                        dto.getDilutionPlateType() == Eppendorf384);

                BettaLIMSMessage dilutionMessage = new BettaLIMSMessage();
                // Either the dilution or the microfluor transfer needs to be 96 to 384.
                int dilutionTransferCount = (dto.getDilutionPlateType() == Eppendorf384) ? 3 : 1;
                for (int i = 0; i < dilutionTransferCount; ++i) {
                    PlateTransferEventType dilutionTransfer = bettalimsFactory.buildRackToPlate(
                            LabEventType.PICO_DILUTION_TRANSFER.getName(), dto.getRackBarcode(),
                            dto.getTubeToPosition(), dto.getDilutionPlateBarcode());

                    dilutionTransfer.getPlate().setPhysType(dto.getDilutionPlateType().getAutomationName());
                    dilutionTransfer.getPlate().setSection((dilutionTransferCount == 1 ?
                            ALL96 : sectionType(i)).getSectionName());
                    if (dto.getDilutionFactor() != 1) {
                        setFactor(dilutionTransfer, dto.getDilutionFactor(), DilutionFactor);
                    }
                    // Sets <reagent kitType="HS buffer" barcode="RG-10095"/>
                    ReagentType reagent = new ReagentType();
                    reagent.setBarcode("RG-12126");
                    reagent.setKitType("HS buffer");
                    dilutionTransfer.getReagent().add(reagent);
                    dilutionTransfer.setDisambiguator(1L + i);
                    dilutionMessage.getPlateTransferEvent().add(dilutionTransfer);
                }
                bettalimsFactory.advanceTime();
                labEventFactory.buildFromBettaLims(dilutionMessage);
            }

            // Either Rack to microfluor plate, or Dilution plate to microfluor plate.
            // The microfluor transfer needs to be a 96 to 384 if the dilution wasn't.
            BettaLIMSMessage microfluorMessage = new BettaLIMSMessage();
            int microfluorTransferCount = (dto.getDilutionPlateType() == null) ? 3 :
                    (dto.getDilutionPlateType() == Eppendorf384) ? 1 : 3;
            for (int i = 0; i < microfluorTransferCount; ++i) {
                PlateTransferEventType microfluorTransfer;

                if (dto.getDilutionPlateType() == null) {
                    microfluorTransfer = bettalimsFactory.buildRackToPlate(
                            LabEventType.PICO_MICROFLUOR_TRANSFER.getName(),
                            dto.getRackBarcode(), dto.getTubeToPosition(), dto.getBlackPlateBarcodes()[0]);
                } else {
                    microfluorTransfer = bettalimsFactory.buildPlateToPlate(
                            LabEventType.PICO_MICROFLUOR_TRANSFER.getName(),
                            dto.getDilutionPlateBarcode(), dto.getBlackPlateBarcodes()[0]);
                    microfluorTransfer.getSourcePlate().setPhysType(dto.getDilutionPlateType().getAutomationName());
                    microfluorTransfer.getSourcePlate().setSection(
                            (dto.getDilutionPlateType() == Eppendorf384 ? ALL384 : ALL96).getSectionName());
                }

                microfluorTransfer.getPlate().setPhysType(dto.getMicrofluorPlateType().getAutomationName());
                microfluorTransfer.getPlate().setSection((microfluorTransferCount == 1 ?
                        ALL384 : sectionType(i)).getSectionName());

                microfluorTransfer.setDisambiguator(1L + i);
                microfluorMessage.getPlateTransferEvent().add(microfluorTransfer);

            }
            bettalimsFactory.advanceTime();
            labEventFactory.buildFromBettaLims(microfluorMessage);

            if (dto.getSensitivityFactor() != 1) {
                BettaLIMSMessage bufferAdditionMessage = new BettaLIMSMessage();
                for (int i = 0; i < microfluorTransferCount; ++i) {
                    PlateEventType bufferAddition = bettalimsFactory.buildPlateEvent("PicoBufferAddition",
                            dto.getBlackPlateBarcodes()[0], REAGENT_DTOS);
                    bufferAddition.getPlate().setPhysType(dto.getMicrofluorPlateType().getAutomationName());
                    bufferAddition.getPlate().setSection((microfluorTransferCount == 1 ?
                            ALL384 : sectionType(i)).getSectionName());
                    bufferAddition.setDisambiguator(1L + i);
                    setFactor(bufferAddition, dto.getSensitivityFactor(), SensitivityFactor);
                    bufferAdditionMessage.getPlateEvent().add(bufferAddition);
                    bettalimsFactory.advanceTime();
                }
                labEventFactory.buildFromBettaLims(bufferAdditionMessage);
            }
        }
    }

    private SBSSection sectionType(int quadrant) {
        return quadrant == 0 ? P384_96TIP_1INTERVAL_A1 :
                quadrant == 1 ? P384_96TIP_1INTERVAL_A2 :
                        quadrant == 2 ? P384_96TIP_1INTERVAL_B1 : P384_96TIP_1INTERVAL_B2;
    }

    /** Adds metadata to the event, such as   <metadata value="2" name="SensitivityFactor"/> */
    private void setFactor(PlateEventType event, int factor, LabEventMetadata.LabEventMetadataType type) {
        MetadataType metadata = new MetadataType();
        metadata.setName(type.getDisplayName());
        metadata.setValue(String.valueOf(factor));
        event.getMetadata().add(metadata);
    }

    private Dto sendSpreadsheet(Dto dto) throws Exception {

        // Makes a varioskan quant spreadsheet for the microfluor plate(s).
        InputStream quantStream = makeVarioskanTestFile(dto);

        // Calls UploadQuantsActionBean to send the spreadsheet to Mercury and a
        // filtered spreadsheet containing only the research sample quants to BSP.
        userBean.login("bspuser");
        userBean.ensureUserValid();
        dto.setRunAndFormation(uploadQuantsActionBean.spreadsheetToMercuryAndBsp(messageCollection,
                quantStream, LabMetric.MetricType.INITIAL_PICO, userBean, true));

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
        return dto;
    }


    private InputStream makeVarioskanTestFile(Dto dto) throws Exception {

        BufferedInputStream quantStream = varioskanParserContainerTest.makeVarioskanSpreadsheet(
                dto.getBlackPlateBarcodes(),
                dto.getMicrofluorPlateType() == Eppendorf384 ?
                        VarioskanParserTest.VARIOSKAN_384_OUTPUT : VarioskanParserTest.VARIOSKAN_OUTPUT,
                String.valueOf(testTimestamp));

        // Finds the tube-derived microfluor positions that don't have a source tube transfer into them.
        Set<VesselPosition> positionsWithoutTube = new HashSet<>(dto.getMicrofluorPlateType() == Eppendorf96 ?
                ALL96.getWells() : CollectionUtils.subtract(ALL384.getWells(), P384_96TIP_1INTERVAL_B2.getWells()));
        for (String positionName : dto.getTubeToPosition().values()) {
            VesselPosition position = VesselPosition.getByName(positionName);
            if (dto.getMicrofluorPlateType() == Eppendorf96) {
                positionsWithoutTube.remove(position);
            } else if (dto.getMicrofluorPlateType() == Eppendorf384) {
                positionsWithoutTube.removeAll(map96to384.get(position));
            } else {
                Assert.fail("No support for " + dto.getMicrofluorPlateType().getAutomationName() + " microfluor");
            }
        }
        // Trims rows from spreadsheet if their wells were not present in the microfluor
        // transfer. This is not where crisp samples get filtered out.
        return VesselEjb.filterOutRows(quantStream, positionsWithoutTube);
    }

    private class Dto {
        private final int quantCardinality;
        private final int sensitivityFactor;
        private final int dilutionFactor;
        private String rackBarcode;
        private Map<String, String> tubeToPosition;
        private String[] blackPlateBarcodes;
        private String dilutionPlateBarcode;
        private final StaticPlate.PlateType dilutionPlateType;
        private final StaticPlate.PlateType microfluorPlateType;
        private Pair<LabMetricRun, String> runAndFormation;
        private final Map<String, Double> smIdQuant = new HashMap<>();
        private final int numberResearchTubes;
        private final int numberCrspTubes;

        public Dto(int quantCardinality, int numberResearchTubes, int numberCrspTubes,
                StaticPlate.PlateType dilutionPlateType, StaticPlate.PlateType microfluorPlateType,
                int sensitivityFactor, int dilutionFactor) {
            this.dilutionPlateType = dilutionPlateType;
            this.microfluorPlateType = microfluorPlateType;
            this.sensitivityFactor = sensitivityFactor;
            this.dilutionFactor = dilutionFactor;
            this.quantCardinality = quantCardinality;
            this.numberResearchTubes = numberResearchTubes;
            this.numberCrspTubes = numberCrspTubes;
        }

        public void setRackBarcode(String rackBarcode) {
            this.rackBarcode = rackBarcode;
        }

        public void setTubeToPosition(Map<String, String> tubeToPosition) {
            this.tubeToPosition = tubeToPosition;
        }

        public void setBlackPlateBarcodes(String[] blackPlateBarcodes) {
            this.blackPlateBarcodes = blackPlateBarcodes;
        }

        public void setDilutionPlateBarcode(String dilutionPlateBarcode) {
            this.dilutionPlateBarcode = dilutionPlateBarcode;
        }

        public String getRackBarcode() {
            return rackBarcode;
        }

        public Map<String, String> getTubeToPosition() {
            return tubeToPosition;
        }

        public String[] getBlackPlateBarcodes() {
            return blackPlateBarcodes;
        }

        public String getDilutionPlateBarcode() {
            return dilutionPlateBarcode;
        }

        public StaticPlate.PlateType getDilutionPlateType() {
            return dilutionPlateType;
        }

        public StaticPlate.PlateType getMicrofluorPlateType() {
            return microfluorPlateType;
        }

        public Pair<LabMetricRun, String> getRunAndFormation() {
            return runAndFormation;
        }

        public void setRunAndFormation(Pair<LabMetricRun, String> runAndFormation) {
            this.runAndFormation = runAndFormation;
        }

        public Map<String, Double> getSmIdQuant() {
            return smIdQuant;
        }

        public int getQuantCardinality() {
            return quantCardinality;
        }

        public int getSensitivityFactor() {
            return sensitivityFactor;
        }

        public int getDilutionFactor() {
            return dilutionFactor;
        }

        public int getTotalTubeCount() {
            return numberResearchTubes + numberCrspTubes;
        }

        public int getResearchTubeCount() {
            return numberResearchTubes;
        }
    }
}
