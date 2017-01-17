package org.broadinstitute.gpinformatics.mercury.control.vessel;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ChildVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
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
import org.testng.annotations.BeforeMethod;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.ALL384;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.ALL96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.P384_96TIP_1INTERVAL_B2;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf384;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class PicoToBspContainerTest extends Arquillian {
    private static final Log logger = LogFactory.getLog(PicoToBspContainerTest.class);
    private static final String RACK_ROWS = "ABCDEFGH";
    private static final int RACK_COLUMNS = 12;
    private static Multimap<VesselPosition, VesselPosition> map96to384 = HashMultimap.create();

    static {
        // Makes a static map of 96 rack positions to their corresponding 384 well positions.
        for (int i = 0; i < 96; ++i) {
            VesselPosition key = ALL96.getWells().get(i);
            map96to384.put(key, SBSSection.P384_96TIP_1INTERVAL_A1.getWells().get(i));
            map96to384.put(key, SBSSection.P384_96TIP_1INTERVAL_A2.getWells().get(i));
            map96to384.put(key, SBSSection.P384_96TIP_1INTERVAL_B1.getWells().get(i));
            map96to384.put(key, P384_96TIP_1INTERVAL_B2.getWells().get(i));
        }
    }

    // Research tubes, i.e. currently existing BSP genomic DNA tubes and their sample names.
    private final SortedMap<String, String> BSP_TUBES = new TreeMap<String, String>(){{
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

    private BettaLimsMessageTestFactory bettalimsFactory = new BettaLimsMessageTestFactory(true);
    private long testStartInSec = System.currentTimeMillis() / 1000;
    private int tubeCounter = 1;
    private MessageCollection messageCollection;

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

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

   @BeforeMethod
    public void beforeMethod() {
        messageCollection = new MessageCollection();
    }

    /** Duplicate pico. Transfers are: rack96 -> 1st blackPlate96 and rack96 -> 2nd blackPlate96 */
    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testDuplicateQuants() throws Exception {
        int research = 2;
        int crsp = 3;
        int quantCardinality = 2;

        Dto dto = makeVessels(quantCardinality, research, crsp, null, Eppendorf96);
        sendMessages(quantCardinality, dto);
        sendSpreadsheet(dto);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
        examineMetrics(dto);
        Assert.assertEquals(dto.getRunAndFormation().getLeft().getLabMetrics().size(),
                (research + crsp) * (quantCardinality + 1));
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
    }

    /** Duplicate quant with only research tubes. */
    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testDuplicateResearchOnly() throws Exception {
        int research = 12;
        int crsp = 0;
        int quantCardinality = 2;

        Dto dto = makeVessels(quantCardinality, research, crsp, null, Eppendorf96);
        sendMessages(quantCardinality, dto);
        sendSpreadsheet(dto);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
        examineMetrics(dto);
        Assert.assertEquals(dto.getRunAndFormation().getLeft().getLabMetrics().size(),
                (research + crsp) * (quantCardinality + 1));
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
    }

    /** Duplicate quant with only crsp tubes. */
    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testDuplicateCrspOnly() throws Exception {
        int research = 0;
        int crsp = 1;
        int quantCardinality = 2;

        Dto dto = makeVessels(quantCardinality, research, crsp, null, Eppendorf96);
        sendMessages(quantCardinality, dto);
        sendSpreadsheet(dto);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
        examineMetrics(dto);
        Assert.assertEquals(dto.getRunAndFormation().getLeft().getLabMetrics().size(),
                (research + crsp) * (quantCardinality + 1));
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
    }

    /**  Triplicate pico. Transfers are: rack96 -> blackPlate384 in 3 checkerboard sections (e.g. CO-20552814) */
    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testTriplicateNoDilutionAllResearch() throws Exception {
        int research = 96;
        int crsp = 0;
        int quantCardinality = 3;

        Dto dto = makeVessels(quantCardinality, research, crsp, null, Eppendorf384);
        sendMessages(quantCardinality, dto);
        sendSpreadsheet(dto);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
        examineMetrics(dto);
        Assert.assertEquals(dto.getRunAndFormation().getLeft().getLabMetrics().size(),
                (research + crsp) * (quantCardinality + 1));
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
    }

    /**  Triplicate pico. Transfers are: rack96 -> blackPlate384 in 3 checkerboard sections (e.g. CO-20552814) */
    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testTriplicateNoDilutionAllCrsp() throws Exception {
        int research = 0;
        int crsp = 96;
        int quantCardinality = 3;

        Dto dto = makeVessels(quantCardinality, research, crsp, null, Eppendorf384);
        sendMessages(quantCardinality, dto);
        sendSpreadsheet(dto);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
        examineMetrics(dto);
        Assert.assertEquals(dto.getRunAndFormation().getLeft().getLabMetrics().size(),
                (research + crsp) * (quantCardinality + 1));
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
    }

    /**  Triplicate pico. Transfers are: rack96 -> blackPlate384 in 3 checkerboard sections (e.g. CO-20552814) */
    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testTriplicateNoDilution() throws Exception {
        int research = 50;
        int crsp = 46;
        int quantCardinality = 3;

        Dto dto = makeVessels(quantCardinality, research, crsp, null, Eppendorf384);
        sendMessages(quantCardinality, dto);
        sendSpreadsheet(dto);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
        examineMetrics(dto);
        Assert.assertEquals(dto.getRunAndFormation().getLeft().getLabMetrics().size(),
                (research + crsp) * (quantCardinality + 1));
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
    }

    /**  Triplicate pico. Transfers are: rack96 -> dilutionPlate96 -> blackPlate384 in 3 checkerboard sections */
    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testTriplicateDilution96() throws Exception {
        int research = 48;
        int crsp = 48;
        int quantCardinality = 3;

        Dto dto = makeVessels(quantCardinality, research, crsp, Eppendorf96, Eppendorf384);
        sendMessages(quantCardinality, dto);
        sendSpreadsheet(dto);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
        examineMetrics(dto);
        Assert.assertEquals(dto.getRunAndFormation().getLeft().getLabMetrics().size(),
                (research + crsp) * (quantCardinality + 1));
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
    }

    /** Triplicate pico. Transfers are: rack96 -> dilutionPlate384 in 3 checkerboard sections -> blackPlate384 */
    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testTriplicateDilution384() throws Exception {
        int research = 48;
        int crsp = 48;
        int quantCardinality = 3;

        Dto dto = makeVessels(quantCardinality, research, crsp, Eppendorf384, Eppendorf384);
        sendMessages(quantCardinality, dto);
        sendSpreadsheet(dto);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
        examineMetrics(dto);
        Assert.assertEquals(dto.getRunAndFormation().getLeft().getLabMetrics().size(),
                (research + crsp) * (quantCardinality + 1));
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
    }

    /** Single pico (as a retest of a triplicate pico). Transfers are: rack96 -> dilutionPlate96 -> blackPlate96 */
    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testSingle() throws Exception {
        int research = 40;
        int crsp = 56;
        int quantCardinality = 1;

        Dto dto = makeVessels(quantCardinality, research, crsp, Eppendorf96, Eppendorf96);
        sendMessages(quantCardinality, dto);
        sendSpreadsheet(dto);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
        examineMetrics(dto);
        Assert.assertEquals(dto.getRunAndFormation().getLeft().getLabMetrics().size(),
                (research + crsp) * (quantCardinality + 1));
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
    }

    private void examineMetrics(Dto dto) {
        List<String> actualBarcodes = new ArrayList<>();
        List<String> actualPositions = new ArrayList<>();
        for (LabMetric labMetric : dto.getRunAndFormation().getLeft().getLabMetrics()) {
            actualBarcodes.add(labMetric.getLabVessel().getLabel());
            actualPositions.add(labMetric.getVesselPosition());
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

    private Dto makeVessels(int quantCardinality, int numberResearchTubes, int numberCrspTubes,
                            StaticPlate.PlateType dilutionPlateType, StaticPlate.PlateType microfluorPlateType)
            throws Exception {

        // Generates the rack and plate barcodes.
        int numberMicrofluorPlates = microfluorPlateType == Eppendorf384 ? 1 : quantCardinality;
        int barcodeEnding = 0;
        String rackBarcode = String.format("%011d%d", testStartInSec, barcodeEnding++);
        String dilutionPlateBarcode = (dilutionPlateType != null) ?
                String.format("%011d%d", testStartInSec, barcodeEnding++) : null;
        String[] blackPlateBarcodes = new String[numberMicrofluorPlates];
        for (int i = 0; i < numberMicrofluorPlates; ++i) {
            blackPlateBarcodes[i] = String.format("%011d%d", testStartInSec, barcodeEnding++);
        }

        // Creates new CRISP tubes (and MercurySamples), and reuses existing research tubes known to BSP.
        List<ChildVesselBean> tubeBeans = new ArrayList<>();
        Map<String, String> mapTubeToPosition = new HashMap<>();

        Iterator<String> bspTubeIterator = BSP_TUBES.keySet().iterator();
        for (int i = 0; i < (numberCrspTubes + numberResearchTubes); ++i) {
            boolean isBspTube = i < numberResearchTubes; // research tubes are put first

            Assert.assertTrue(!isBspTube || bspTubeIterator.hasNext());
            String tubeBarcode = isBspTube ? bspTubeIterator.next() :
                    String.format("%d%03d", testStartInSec, tubeCounter++);
            String well = String.format("%s%02d", RACK_ROWS.charAt(i / RACK_COLUMNS), (i % RACK_COLUMNS) + 1);
            mapTubeToPosition.put(tubeBarcode, well);

            BarcodedTube tube = OrmUtil.proxySafeCast(labVesselDao.findByIdentifier(tubeBarcode), BarcodedTube.class);
            if (tube == null) {
                Assert.assertFalse(isBspTube);
                tube = new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075);
                labVesselDao.persist(tube);
            } else {
                Assert.assertTrue(isBspTube);
            }
            tube.setVolume(new BigDecimal("60"));

            if (CollectionUtils.isEmpty(tube.getMercurySamples())) {
                String materialName = MaterialType.DNA_DNA_GENOMIC.getDisplayName();
                MercurySample mercurySample = isBspTube ?
                        new MercurySample(BSP_TUBES.get(tubeBarcode),
                                new BspSampleData(ImmutableMap.of(BSPSampleSearchColumn.MATERIAL_TYPE, materialName))) :
                        new MercurySample("SM-" + tubeBarcode,
                                ImmutableSet.of(new Metadata(Metadata.Key.MATERIAL_TYPE, materialName)));
                Assert.assertTrue((mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP) == isBspTube);
                mercurySample.addLabVessel(tube);
                labVesselDao.persist(mercurySample);
            }

            tubeBeans.add(new ChildVesselBean(tubeBarcode, tube.getMercurySamples().iterator().next().getSampleKey(),
                    tube.getTubeType().getAutomationName(), well));
        }

        // Makes a rack containing the above tubes.
        List<ParentVesselBean> parentVessels = Arrays.asList(new ParentVesselBean(rackBarcode, rackBarcode,
                RackOfTubes.RackType.Matrix96.getDisplayName(), tubeBeans));
        List<LabVessel> labVessels = labVesselFactory.buildLabVessels(parentVessels, "epolk", new Date(), null,
                MercurySample.MetadataSource.MERCURY);
        labVesselDao.persistAll(labVessels);
        labVesselDao.flush();

        return new Dto(rackBarcode, mapTubeToPosition, blackPlateBarcodes, dilutionPlateBarcode,
                dilutionPlateType, microfluorPlateType);
    }


    private void sendMessages(int quantCardinality, Dto dto) {

        if (quantCardinality == 1 || quantCardinality == 2) {

            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            for (String blackPlateBarcode : dto.getBlackPlateBarcodes()) {
                PlateTransferEventType picoTransfer = bettalimsFactory.buildRackToPlate(
                        LabEventType.PICO_TRANSFER.getName(), dto.getRackBarcode(), dto.getTubeToPosition(),
                        blackPlateBarcode);
                picoTransfer.getPlate().setPhysType(dto.getMicrofluorPlateType().getAutomationName());
                bettaLIMSMessage.getPlateTransferEvent().add(picoTransfer);
                bettalimsFactory.advanceTime();
            }
            labEventFactory.buildFromBettaLims(bettaLIMSMessage);

        } else if (quantCardinality == 3) {
            Assert.assertEquals(dto.getMicrofluorPlateType(), Eppendorf384);
            Assert.assertEquals(dto.getBlackPlateBarcodes().length, 1);

            // The 96 to 384 transfer has 3 events and is either rack to dilution, or dilution to microfluor.
            int dilutionTransferCount = (dto.getDilutionPlateType() == null) ? 0 :
                    (dto.getDilutionPlateType() == Eppendorf384) ? 3 : 1;
            int microfluorTransferCount = (dto.getDilutionPlateType() == null) ? 3 :
                    (dto.getDilutionPlateType() == Eppendorf384) ? 1 : 3;

            if (dto.getDilutionPlateType() != null) {
                Assert.assertTrue(dto.getDilutionPlateType() == Eppendorf96 ||
                                  dto.getDilutionPlateType() == Eppendorf384);

                BettaLIMSMessage dilutionMessage = new BettaLIMSMessage();
                for (long i = 0; i < dilutionTransferCount; ++i) {
                    PlateTransferEventType dilutionTransfer = bettalimsFactory.buildRackToPlate(
                            LabEventType.PICO_DILUTION_TRANSFER.getName(), dto.getRackBarcode(),
                            dto.getTubeToPosition(), dto.getDilutionPlateBarcode());

                    dilutionTransfer.getPlate().setPhysType(dto.getDilutionPlateType().getAutomationName());
                    dilutionTransfer.getPlate().setSection((dilutionTransferCount == 1 ? ALL96 :
                            i == 0 ? SBSSection.P384_96TIP_1INTERVAL_A1 :
                                    i == 1 ? SBSSection.P384_96TIP_1INTERVAL_A2 :
                                            i == 2 ? SBSSection.P384_96TIP_1INTERVAL_B1 :
                                                    P384_96TIP_1INTERVAL_B2).getSectionName());

                    // Sets <metadata value="10" name="DilutionFactor"/>
                    MetadataType metadata = new MetadataType();
                    metadata.setName("DilutionFactor");
                    metadata.setValue("10");
                    dilutionTransfer.getMetadata().add(metadata);

                    // Sets <reagent kitType="HS buffer" barcode="RG-10095"/>
                    ReagentType reagent = new ReagentType();
                    reagent.setBarcode("RG-12126");
                    reagent.setKitType("HS buffer");
                    dilutionTransfer.getReagent().add(reagent);
                    dilutionTransfer.setDisambiguator(i);
                    dilutionMessage.getPlateTransferEvent().add(dilutionTransfer);
                }
                bettalimsFactory.advanceTime();
                labEventFactory.buildFromBettaLims(dilutionMessage);

                BettaLIMSMessage microfluorMessage = new BettaLIMSMessage();
                for (long i = 0; i < microfluorTransferCount; ++i) {
                    PlateTransferEventType microfluorTransfer = bettalimsFactory.buildPlateToPlate(
                            LabEventType.PICO_MICROFLUOR_TRANSFER.getName(), dto.getDilutionPlateBarcode(),
                            dto.getBlackPlateBarcodes()[0]);

                    microfluorTransfer.getSourcePlate().setPhysType(dto.getDilutionPlateType().getAutomationName());
                    microfluorTransfer.getSourcePlate().setSection(
                            (dto.getDilutionPlateType() == Eppendorf384 ? ALL384 : ALL96).getSectionName());

                    microfluorTransfer.getPlate().setPhysType(dto.getMicrofluorPlateType().getAutomationName());
                    microfluorTransfer.getPlate().setSection((microfluorTransferCount == 1 ? ALL384 :
                            i == 0 ? SBSSection.P384_96TIP_1INTERVAL_A1 :
                                    i == 1 ? SBSSection.P384_96TIP_1INTERVAL_A2 :
                                            i == 2 ? SBSSection.P384_96TIP_1INTERVAL_B1 :
                                                    P384_96TIP_1INTERVAL_B2).getSectionName());

                    microfluorTransfer.setDisambiguator(i);
                    microfluorMessage.getPlateTransferEvent().add(microfluorTransfer);
                }
                bettalimsFactory.advanceTime();
                labEventFactory.buildFromBettaLims(microfluorMessage);

            } else {
                // Rack to microfluor with no dilution.

                BettaLIMSMessage microfluorMessage = new BettaLIMSMessage();
                for (long i = 0; i < microfluorTransferCount; ++i) {
                    PlateTransferEventType microfluorTransfer = bettalimsFactory.buildRackToPlate(
                            LabEventType.PICO_MICROFLUOR_TRANSFER.getName(), dto.getRackBarcode(),
                            dto.getTubeToPosition(), dto.getBlackPlateBarcodes()[0]);

                    microfluorTransfer.getPlate().setPhysType(dto.getMicrofluorPlateType().getAutomationName());
                    microfluorTransfer.getPlate().setSection((microfluorTransferCount == 1 ? ALL384 :
                            i == 0 ? SBSSection.P384_96TIP_1INTERVAL_A1 :
                                    i == 1 ? SBSSection.P384_96TIP_1INTERVAL_A2 :
                                            i == 2 ? SBSSection.P384_96TIP_1INTERVAL_B1 :
                                                    P384_96TIP_1INTERVAL_B2).getSectionName());

                    microfluorTransfer.setDisambiguator(i);
                    microfluorMessage.getPlateTransferEvent().add(microfluorTransfer);
                }
                bettalimsFactory.advanceTime();
                labEventFactory.buildFromBettaLims(microfluorMessage);
            }
        } else {
            Assert.fail("No support for quant cardinality of " + quantCardinality);
        }
    }

    private Dto sendSpreadsheet(Dto dto) throws Exception {

        // Makes a varioskan quant spreadsheet for the microfluor plate(s).
        InputStream quantStream = makeVarioskanTestFile(dto);

        // Calls UploadQuantsActionBean to send the spreadsheet to Mercury and a
        // filtered spreadsheet containing only the research sample quants to BSP.
        userBean.loginTestUser();
        dto.setRunAndFormation(UploadQuantsActionBean.spreadsheetToMercuryAndBsp(vesselEjb,
                messageCollection, quantStream, LabMetric.MetricType.INITIAL_PICO, userBean, true));
        return dto;
    }


    private InputStream makeVarioskanTestFile(Dto dto) throws Exception {

        BufferedInputStream quantStream = varioskanParserContainerTest.makeVarioskanSpreadsheet(
                dto.getBlackPlateBarcodes(), dto.getMicrofluorPlateType() == Eppendorf384 ?
                        VarioskanParserTest.VARIOSKAN_384_OUTPUT : VarioskanParserTest.VARIOSKAN_OUTPUT,
                String.valueOf(testStartInSec));

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
        return vesselEjb.filterOutRows(quantStream, positionsWithoutTube);
    }


    private class Dto {
        private String rackBarcode;
        private Map<String, String> tubeToPosition;
        private String[] blackPlateBarcodes;
        private String dilutionPlateBarcode;
        private StaticPlate.PlateType dilutionPlateType;
        private StaticPlate.PlateType microfluorPlateType;
        private Pair<LabMetricRun, String> runAndFormation;

        public Dto(String rackBarcode, Map<String, String> tubeToPosition, String[] blackPlateBarcodes,
                   String dilutionPlateBarcode,
                   StaticPlate.PlateType dilutionPlateType,
                   StaticPlate.PlateType microfluorPlateType) {
            this.rackBarcode = rackBarcode;
            this.tubeToPosition = tubeToPosition;
            this.blackPlateBarcodes = blackPlateBarcodes;
            this.dilutionPlateBarcode = dilutionPlateBarcode;
            this.dilutionPlateType = dilutionPlateType;
            this.microfluorPlateType = microfluorPlateType;
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

        public void setRunAndFormation(
                Pair<LabMetricRun, String> runAndFormation) {
            this.runAndFormation = runAndFormation;
        }
    }
}
