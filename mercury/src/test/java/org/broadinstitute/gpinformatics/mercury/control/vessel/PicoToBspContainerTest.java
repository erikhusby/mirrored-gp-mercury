package org.broadinstitute.gpinformatics.mercury.control.vessel;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
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
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf384;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class PicoToBspContainerTest extends Arquillian {
    private static final Log logger = LogFactory.getLog(PicoToBspContainerTest.class);
    private static final int RACK_COLUMNS = 12;
    private static Multimap<VesselPosition, VesselPosition> map96to384 = HashMultimap.create();

    static {
        // Maps each of the 96 rack positions to the corresponding 384 well positions.
        for (int i = 0; i < 96; ++i) {
            VesselPosition key = SBSSection.ALL96.getWells().get(i);
            map96to384.put(key, SBSSection.P384_96TIP_1INTERVAL_A1.getWells().get(i));
            map96to384.put(key, SBSSection.P384_96TIP_1INTERVAL_A2.getWells().get(i));
            map96to384.put(key, SBSSection.P384_96TIP_1INTERVAL_B1.getWells().get(i));
            map96to384.put(key, SBSSection.P384_96TIP_1INTERVAL_B2.getWells().get(i));
        }
    }

    // Research tubes, i.e. currently existing BSP genomic DNA tubes and their sample names.
    private final Map<String, String> BSP_TUBES = new HashMap<String, String>(){{
        put("1140117899", "SM-D7G11");
        put("1140145281", "SM-DBQFT");
        put("0185598828", "SM-B119Z");
        put("1124898652", "SM-B11AJ");
        put("1124900180", "SM-B11AK");
        put("1124900218", "SM-B11AL");
        put("1124900209", "SM-B11AM");
        put("1124900234", "SM-B11AN");
        put("0183572063", "SM-B11AO");
        put("1109156003", "SM-B11AP");
        put("1124900161", "SM-B11AQ");
        put("0183572020", "SM-B11AR");
        put("1124900208", "SM-B11BB");
        put("1124900210", "SM-B11BC");
        put("1109155877", "SM-B11BD");
        put("1109155699", "SM-B11BE");
        put("0183572053", "SM-B11BF");
        put("1124900248", "SM-B11BG");
        put("1124900198", "SM-B11BH");
        put("0178812354", "SM-B11BI");
        put("1109155946", "SM-B11BJ");
        put("0185783236", "SM-B11SM");
        put("0185783237", "SM-B11SN");
        put("0185783234", "SM-B11SO");
        put("0185783235", "SM-B11SP");
        put("0185783232", "SM-B11SQ");
        put("0185783233", "SM-B11SR");
        put("0185783245", "SM-B11SS");
        put("0185783244", "SM-B11ST");
        put("0185783247", "SM-B11SU");
        put("0185783246", "SM-B11SV");
        put("0185783249", "SM-B11SW");
        put("0185783248", "SM-B11SX");
        put("0185783251", "SM-B11SY");
        put("0185783250", "SM-B11SZ");
        put("0185783252", "SM-B11T1");
    }};

    private Iterator<String> bspTubeIterator = BSP_TUBES.keySet().iterator();

    private BettaLimsMessageTestFactory bettalimsFactory = new BettaLimsMessageTestFactory(true);
    private long testStartInSec = System.currentTimeMillis() / 1000;
    private int tubeCounter = 1;

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

    // These are the cases to be tested.
    //
    //   - Duplicate pico: rack96 -> 1st blackPlate96 and rack96 -> 2nd blackPlate96
    //   - Triplicate pico: rack96 -> dilutionPlate96 -> blackPlate384 in 3 staggered checkerboard sections
    //   - Triplicate pico: rack96 -> dilutionPlate384 in 3 staggered checkerboard sections -> blackPlate384
    //   - Singulate pico (as a retest of a triplicate pico): rack96 -> dilutionPlate96 -> blackPlate96
    // Each case should be tested with a mix of clinical and research tubes.
    // Also test one of the cases with only clinical tubes, and with only research tubes.

    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testDuplicateQuants() throws Exception {
        makeAndSendQuants(2, 2, 3, null, Eppendorf96);
    }

    private void makeAndSendQuants(int quantCardinality, int numberResearchTubes, int numberCrspTubes,
                                   StaticPlate.PlateType dilutionPlateType, StaticPlate.PlateType microfluorPlateType)
            throws Exception {
        // Makes a rack conatining both clinical and research samples and does initial pico transfer to plates.
        // The rack barcode and research tubes must be known to BSP.
        // The plate barcodes are generated along with the clinical samples and their tubes.
        int numberMicrofluorPlates = microfluorPlateType == Eppendorf384 ? 1 : quantCardinality;
        int barcodeEnding = 0;
        String rackBarcode = String.format("%011d%d", testStartInSec, barcodeEnding++);
        String dilutionPlateBarcode = (dilutionPlateType != null) ?
                String.format("%011d%d", testStartInSec, barcodeEnding++) : null;
        String[] blackPlateBarcodes = new String[numberMicrofluorPlates];
        for (int i = 0; i < numberMicrofluorPlates; ++i) {
            blackPlateBarcodes[i] = String.format("%011d%d", testStartInSec, barcodeEnding++);
        }

        // Creates new CRISP tubes (and MercurySamples), and reuses existing research tubes.
        List<ChildVesselBean> tubeBeans = new ArrayList<>();
        Map<String, String> mapTubeToPosition = new HashMap<>();
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        for (int i = 0; i < (numberCrspTubes + numberResearchTubes); ++i) {
            boolean isBspTube = i < numberResearchTubes; // first tubes are BSP/research
            Assert.assertTrue(!isBspTube || bspTubeIterator.hasNext());
            String tubeBarcode = isBspTube ? bspTubeIterator.next() :
                    String.format("%d%03d", testStartInSec, tubeCounter++);
            String well = String.format("%s%02d", "ABCDEFGH".charAt(i / RACK_COLUMNS), (i % RACK_COLUMNS) + 1);
            VesselPosition wellPosition = VesselPosition.getByName(well);

            BarcodedTube tube = OrmUtil.proxySafeCast(labVesselDao.findByIdentifier(tubeBarcode), BarcodedTube.class);
            Assert.assertTrue((tube != null) == isBspTube);
            if (tube == null) {
                tube = new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075);
                labVesselDao.persist(tube);
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

            mapTubeToPosition.put(tubeBarcode, well);
            mapPositionToTube.put(wellPosition, tube);
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

        BettaLIMSMessage bettaLIMSMessage;

        switch(quantCardinality) {
        case 2:
            // Duplicate quant transfers from rack to two 96-well microfluor plates.
            bettaLIMSMessage = new BettaLIMSMessage();
            for (String blackPlateBarcode : blackPlateBarcodes) {
                PlateTransferEventType picoTransfer = bettalimsFactory.buildRackToPlate(
                        LabEventType.PICO_TRANSFER.getName(), rackBarcode, mapTubeToPosition,
                        blackPlateBarcode);
                picoTransfer.getPlate().setPhysType(microfluorPlateType.getAutomationName());
                bettaLIMSMessage.getPlateTransferEvent().add(picoTransfer);
                bettalimsFactory.advanceTime();
            }
            labEventFactory.buildFromBettaLims(bettaLIMSMessage);
            break;

        case 3:
            // Triplicate quant transfers from rack to dilution plate having 96 wells, then dilution
            // plate to microfluor plate having 384 wells using 3 staggered section transfers.
            PlateTransferEventType dilutionTransfer = bettalimsFactory.buildRackToPlate(
                    LabEventType.PICO_DILUTION_TRANSFER.getName(), rackBarcode, mapTubeToPosition,
                    dilutionPlateBarcode);

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

            bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateTransferEvent().add(dilutionTransfer);
            bettalimsFactory.advanceTime();
            labEventFactory.buildFromBettaLims(bettaLIMSMessage);

            bettaLIMSMessage = new BettaLIMSMessage();
            for (String blackPlateBarcode : blackPlateBarcodes) {
                PlateTransferEventType microfluorTransfer = bettalimsFactory.buildPlateToPlate(
                        LabEventType.PICO_MICROFLUOR_TRANSFER.getName(), dilutionPlateBarcode, blackPlateBarcode);
                microfluorTransfer.getPlate().setPhysType(microfluorPlateType.getAutomationName());
                bettaLIMSMessage.getPlateTransferEvent().add(microfluorTransfer);
                bettalimsFactory.advanceTime();
            }
            labEventFactory.buildFromBettaLims(bettaLIMSMessage);
            break;

        default:
            Assert.fail("No support for quant cardinality of " + quantCardinality);
        }

        // Makes a varioskan quant spreadsheet for the microfluor plate(s).
        byte[] quantStreamBytes = IOUtils.toByteArray(makeVarioskanTestFile(blackPlateBarcodes,
                String.valueOf(testStartInSec), mapTubeToPosition, microfluorPlateType));

        InputStream quantStream = makeVarioskanTestFile(blackPlateBarcodes, String.valueOf(testStartInSec),
                mapTubeToPosition, microfluorPlateType);

        // Calls UploadQuantsActionBean to send the spreadsheet to Mercury and a
        // filtered spreadsheet containing only the research sample quants to BSP.
        MessageCollection messageCollection = new MessageCollection();
        userBean.loginTestUser();
        Pair<LabMetricRun, String> runAndRackOfTubes = UploadQuantsActionBean.spreadsheetToMercuryAndBsp(
                vesselEjb, messageCollection, quantStream, LabMetric.MetricType.INITIAL_PICO, userBean, true);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertEquals(runAndRackOfTubes.getLeft().getLabMetrics().size(),
                (numberCrspTubes + numberResearchTubes) * 3);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " \n "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getErrors(), " \n "));
    }


    private InputStream makeVarioskanTestFile(String[] blackPlateBarcodes, String namePrefix,
                                              Map<String, String> tubeToPosition,
                                              StaticPlate.PlateType microfluorPlateType)
            throws Exception {

        Assert.assertEquals(blackPlateBarcodes.length, 2, "Only support for duplicate quant spreadsheet");
        BufferedInputStream quantStream = varioskanParserContainerTest.makeVarioskanSpreadsheet(
                blackPlateBarcodes[0], blackPlateBarcodes[1], namePrefix);

        // Makes a set of all microfluor positions that didn't derive from a tube.
        Set<VesselPosition> positionsWithoutTube = new HashSet<>();
        if (microfluorPlateType == Eppendorf96) {
            positionsWithoutTube.addAll(SBSSection.ALL96.getWells());
        } else if (microfluorPlateType == Eppendorf384) {
            positionsWithoutTube.addAll(SBSSection.ALL384.getWells());
        } else {
            Assert.fail("No support for " + microfluorPlateType.getAutomationName() + " microfluor");
        }
        for (String positionName : tubeToPosition.values()) {
            VesselPosition position = VesselPosition.getByName(positionName);
            if (microfluorPlateType == Eppendorf96) {
                positionsWithoutTube.remove(position);
            } else if (microfluorPlateType == Eppendorf384) {
                positionsWithoutTube.removeAll(map96to384.get(position));
            }
        }
        return vesselEjb.filterOutRows(quantStream, positionsWithoutTube);
    }
}
