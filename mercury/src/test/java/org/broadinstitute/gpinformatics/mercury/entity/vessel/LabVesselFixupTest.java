package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import com.opencsv.CSVReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme_;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent_;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixup production Lab Vessel entities
 */
@Test(groups = TestGroups.FIXUP)
public class LabVesselFixupTest extends Arquillian {

    public static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private TubeFormationDao tubeFormationDao;

    @Inject
    private RackOfTubesDao rackOfTubesDao;

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private UserBean userBean;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private IlluminaFlowcellDao illuminaFlowcellDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void fixupBsp346() {
        /*
20130516_112916210.xml 1074074993 CO-6710301 -> CO-6728638
20130516_113804684.xml 1072310155 CO-6710357 -> CO-6728640
20130516_114433985.xml 1072309963 CO-6710299 -> CO-6728642
20130516_115240588.xml 1074077105 CO-6710245 -> CO-6728644
20130516_115911519.xml 1074074897 CO-6710291 -> CO-6728646
20130516_120551538.xml 1077065050 CO-6710389 -> CO-6728648
20130516_121246504.xml 1082013659 CO-6710373 -> CO-6728650
20130516_121838716.xml 1072310635 CO-6710359 -> CO-6728652
20130516_122522682.xml 1072310201 CO-6710363 -> CO-6728654
20130516_123146396.xml 1082013179 CO-6710387 -> CO-6728656
20130516_124745864.xml 1082012795 CO-6710369 -> CO-6728658
         */

        List<String> tubeList = new ArrayList<>();
        tubeList.add("1074074993");
        tubeList.add("1072310155");
        tubeList.add("1072309963");
//        tubeList.add("1074077105"); // two racks "0" and CO-6710245
        tubeList.add("1074074897");
        tubeList.add("1077065050");
        tubeList.add("1082013659");
        tubeList.add("1072310635");
        tubeList.add("1072310201");
        tubeList.add("1082013179");
        tubeList.add("1082012795");

        List<String> rackList = new ArrayList<>();
        rackList.add("CO-6710301");
        rackList.add("CO-6710357");
        rackList.add("CO-6710299");
//        rackList.add("CO-6710245");
        rackList.add("CO-6710291");
        rackList.add("CO-6710389");
        rackList.add("CO-6710373");
        rackList.add("CO-6710359");
        rackList.add("CO-6710363");
        rackList.add("CO-6710387");
        rackList.add("CO-6710369");

        for (int i = 0; i < tubeList.size(); i++) {
            LabVessel tube = labVesselDao.findByIdentifier(tubeList.get(i));
            Assert.assertEquals(tube.getVesselContainers().size(), 1, "Wrong number of containers");
            TubeFormation tubeFormation = (TubeFormation) (tube.getVesselContainers().iterator().next().getEmbedder());
            Set<RackOfTubes> racksOfTubes = tubeFormation.getRacksOfTubes();
            Assert.assertEquals(racksOfTubes.size(), 1, "Wrong number of racks");
            if (racksOfTubes.iterator().next().getLabel().equals("0")) {
                racksOfTubes.clear();
                RackOfTubes rackOfTubes = new RackOfTubes(rackList.get(i), RackOfTubes.RackType.Matrix96);
                racksOfTubes.add(rackOfTubes);
                tubeFormation.addRackOfTubes(rackOfTubes);
                labVesselDao.flush();
            }
        }
    }

    @Test(enabled = false)
    public void fixupGplim1336() {
        // There was a unique constraint on a re-arrayed rack, so rename it until bug is fixed.
        LabVessel labVessel = labVesselDao.findByIdentifier("CO-6735551");
        labVessel.setLabel(labVessel.getLabel() + "x");
        labVesselDao.flush();
    }

    @Test(enabled = false)
    public void fixupBsp1914() {
        LabVessel labVessel = labVesselDao.findByIdentifier("CO-9202643");
        labVessel.setLabel("CO-9624594");
        labVesselDao.flush();
    }

    @Test(enabled = false)
    public void fixupZeroRacks() {
        TubeFormation tubeFormation = tubeFormationDao.findByDigest("31003665b6e8cf20071a0f6c530da6e7");
        deleteZeroRack(tubeFormation);
        tubeFormation = tubeFormationDao.findByDigest("b3c6de8a4a89728f926dcaff238cfc44");
        deleteZeroRack(tubeFormation);
        tubeFormation = tubeFormationDao.findByDigest("dc80785d49304bcc077b513e080cacaf");
        deleteZeroRack(tubeFormation);
        RackOfTubes zeroRack = rackOfTubesDao.findByBarcode("0");
        rackOfTubesDao.remove(zeroRack);
    }

    private void deleteZeroRack(TubeFormation tubeFormation) {
        RackOfTubes deleteRack = null;
        for (RackOfTubes rackOfTubes : tubeFormation.getRacksOfTubes()) {
            if (rackOfTubes.getLabel().equals("0")) {
                deleteRack = rackOfTubes;
            }
        }
        if (deleteRack != null) {
            tubeFormation.getRacksOfTubes().remove(deleteRack);
        }
        labVesselDao.flush();
    }

    /**
     * Insert water control vessels, for backfill of BSP daughter plate transfers.
     */
    @Test(enabled = false)
    public void insertWaterControls() {
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new EnumMap<>(VesselPosition.class);

        BarcodedTube barcodedTube = new BarcodedTube("0114382737");
        barcodedTube.addSample(new MercurySample("SM-2AY8L", MercurySample.MetadataSource.BSP));
        mapPositionToTube.put(VesselPosition.A01, barcodedTube);

        BarcodedTube barcodedTube1 = new BarcodedTube("SM-22GXJ");
        barcodedTube.addSample(new MercurySample("SM-22GXJ", MercurySample.MetadataSource.BSP));
        mapPositionToTube.put(VesselPosition.A02, barcodedTube1);

        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        tubeFormation.addRackOfTubes(new RackOfTubes("CO-5971666", RackOfTubes.RackType.Matrix96));

        labVesselDao.persist(tubeFormation);

        mapPositionToTube.clear();
        BarcodedTube barcodedTube2 = new BarcodedTube("SM-29FPE");
        barcodedTube2.addSample(new MercurySample("SM-29FPE", MercurySample.MetadataSource.BSP));
        mapPositionToTube.put(VesselPosition.A01, barcodedTube2);

        tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        tubeFormation.addRackOfTubes(new RackOfTubes("CO-3468604", RackOfTubes.RackType.Matrix96));
        labVesselDao.persist(tubeFormation);
    }

    @Test(enabled = false)
    public void fixupBsp581() {
        // The physTypes in the message were Eppendorf96, but should have been TubeRack.
        String[] rackBarcodes = {
                "CO-6584145",
                "CO-6940995",
                "CO-6940991",
                "CO-6661848",
                "CO-6940992",
                "CO-6656416",
                "CO-6641388",
                "CO-6630669",
                "CO-6629602",
                "CO-6665938",
                "CO-6660878",
                "CO-6661363",
                "CO-4405034",
                "CO-4301567"};
        for (String rackBarcode : rackBarcodes) {
            StaticPlate staticPlate = staticPlateDao.findByBarcode(rackBarcode);
            Iterator<SectionTransfer> iterator = staticPlate.getContainerRole().getSectionTransfersFrom().iterator();
            while (iterator.hasNext()) {
                SectionTransfer sectionTransfer = iterator.next();
                LabEvent labEvent = sectionTransfer.getLabEvent();
                labEvent.getReagents().clear();
                labEvent.getSectionTransfers().clear();
                staticPlateDao.remove(labEvent);
                iterator.remove();
            }
            staticPlateDao.remove(staticPlate);
        }
        staticPlateDao.flush();
    }

    @Test(enabled = false)
    public void fixupBsp934And1005And1218() {
        Map<String, String> kitToContainer = new HashMap<String, String>() {{
//            put("SK-247T", "CO-7496163");
//            put("SK-24EI", "CO-6635472");
//            put("SK-24EK", "CO-7506733");
//            put("SK-24EM", "CO-7506736");
//            put("SK-24EO", "CO-7507178");
//            put("SK-24EQ", "CO-6633920");
//            put("SK-24ET", "CO-7507180");
//            put("SK-24EU", "CO-7507181");
//            put("SK-24EV", "CO-6633823");
//            put("SK-24EY", "CO-7506735");
//            put("SK-24F1", "CO-6580265");
//            put("SK-24F2", "CO-7506732");
//
//            put("SK-24EL", "CO-7506734");
//            put("SK-24EW", "CO-6578422");
//            put("SK-24F3", "CO-6633726");
            put("SK-28CT", "CO-7158262");
            put("SK-28CU", "CO-7160207");
        }};

        for (Map.Entry<String, String> entry : kitToContainer.entrySet()) {
            String sampleKitBarcode = entry.getKey();
            String containerBarcode = entry.getValue();
            LabVessel vessel = labVesselDao.findByIdentifier(sampleKitBarcode);
            vessel.setLabel(containerBarcode);
        }

        labVesselDao.flush();
    }

    @Test(enabled = false)
    public void fixupMisbatchesBSP_1005() {
        final String [] toRemoveFromBatch = new String [] {
                "CO-6580265",
                "CO-6635472",
                "CO-7507180",
                "CO-7507181",
                "CO-7506733",
        };

        final String batchToRemoveFrom = "BP-44457";

        for (LabVessel vessel : labVesselDao.findByListIdentifiers(Arrays.asList(toRemoveFromBatch))) {
            for (TubeFormation tubeFormation : ((RackOfTubes) vessel).getTubeFormations()) {
                for (SectionTransfer sectionTransfer : tubeFormation.getContainerRole().getSectionTransfersFrom()) {
                    LabEvent labEvent = sectionTransfer.getLabEvent();
                    if (labEvent.getLabBatch().getBatchName().equals(batchToRemoveFrom)) {
                        // VESSEL_TRANSFER has a FK constraint to LAB_EVENT, so this must be deleted first.
                        labVesselDao.remove(sectionTransfer);
                        labVesselDao.remove(labEvent);
                    }
                }
            }
        }
    }


    @Test(enabled = false)
    public void fixupGplim2362() {
        List<Pair<String, String>> listOfPairs = new ArrayList<Pair<String, String>>() {{
            add(new ImmutablePair<>("1084962682", "0150675790"));
            add(new ImmutablePair<>("0156293762", "0154845206"));
            add(new ImmutablePair<>("0156293822", "0154845124"));
            add(new ImmutablePair<>("0156293797", "0145504452"));
            add(new ImmutablePair<>("0156293819", "0154845172"));
            add(new ImmutablePair<>("0156490381", "0154845129"));
            add(new ImmutablePair<>("0156293799", "0154845134"));
            add(new ImmutablePair<>("0156293157", "0154845205"));
            add(new ImmutablePair<>("0156300375", "0154845207"));
            add(new ImmutablePair<>("0156300374", "0154845138"));
            add(new ImmutablePair<>("0156300376", "0154845182"));
            add(new ImmutablePair<>("0156300380", "0154861404"));
            add(new ImmutablePair<>("0156300373", "0150675776"));
            add(new ImmutablePair<>("0156300378", "0154449829"));
            add(new ImmutablePair<>("0156300249", "0154845148"));
            add(new ImmutablePair<>("0156300379", "0154845186"));
            add(new ImmutablePair<>("0156300381", "0154862171"));
            add(new ImmutablePair<>("0156300377", "0154845133"));
            add(new ImmutablePair<>("0156300250", "0150673621"));
            add(new ImmutablePair<>("0156293801", "0150675799"));
        }};
        fixupBucketEntries(listOfPairs, "LCSET-4641");
        labVesselDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim2604() {
        List<Pair<String, String>> listOfPairs = new ArrayList<Pair<String, String>>() {{
            add(new ImmutablePair<>("1084965340", "0154862189"));
            add(new ImmutablePair<>("1084961967", "0154845147"));
            add(new ImmutablePair<>("1084965330", "0154862178"));
            add(new ImmutablePair<>("1084964546", "0154862117"));
            add(new ImmutablePair<>("1084965338", "0154862113"));
            add(new ImmutablePair<>("1084965341", "0154862190"));
            add(new ImmutablePair<>("1084964562", "0154862141"));
            add(new ImmutablePair<>("1084961960", "0154862193"));
            add(new ImmutablePair<>("1084965334", "0154862165"));
            add(new ImmutablePair<>("1084961979", "0154862203"));
            add(new ImmutablePair<>("1084961978", "0154862118"));
            add(new ImmutablePair<>("1084964588", "0154862151"));
            add(new ImmutablePair<>("1084965332", "0154862187"));
            add(new ImmutablePair<>("1084962684", "0154862129"));
            add(new ImmutablePair<>("1084961964", "0154862156"));
            add(new ImmutablePair<>("1084962683", "0154862158"));
            add(new ImmutablePair<>("1084965352", "0154862166"));
            add(new ImmutablePair<>("1084961961", "0154862143"));
            add(new ImmutablePair<>("1084965351", "0154862197"));
            add(new ImmutablePair<>("1084965346", "0154862191"));
            add(new ImmutablePair<>("1084961965", "0154862139"));
            add(new ImmutablePair<>("1084965347", "0154862200"));
            add(new ImmutablePair<>("1084965343", "0154862152"));
            add(new ImmutablePair<>("1084962681", "0154862167"));
            add(new ImmutablePair<>("1084961813", "0154862128"));
            add(new ImmutablePair<>("1084961983", "0154862144"));
            add(new ImmutablePair<>("1084957972", "0154862112"));
            add(new ImmutablePair<>("1084961988", "0154862125"));
            add(new ImmutablePair<>("1084961987", "0154862198"));
        }};
        fixupBucketEntries(listOfPairs, "LCSET-5239");
        labVesselDao.flush();
    }

    private void fixupBucketEntries(List<Pair<String, String>> listOfPairsOldNew, String lcset) {
        for (Pair<String, String> pair : listOfPairsOldNew) {
            LabVessel labVesselOld = labVesselDao.findByIdentifier(pair.getLeft());
            LabVessel labVesselNew = labVesselDao.findByIdentifier(pair.getRight());
            Set<BucketEntry> bucketEntries = labVesselOld.getBucketEntries();
            BucketEntry bucketEntry = null;
            for (BucketEntry currentBucketEntry : bucketEntries) {
                if (currentBucketEntry.getLabBatch().getBatchName().equals(lcset)) {
                    bucketEntry = currentBucketEntry;
                }
            }
            if (bucketEntry == null) {
                throw new RuntimeException("Failed to find bucket entry for " + labVesselOld.getLabel());
            }
            bucketEntry.setLabVessel(labVesselNew);
            labVesselNew.addBucketEntry(bucketEntry);
            bucketEntries.remove(bucketEntry);
        }
    }

    @Test(enabled = false)
    public void fixupGplim2367() {
        // the source tubeformation for the ShearingTransfer doesn't seem to have been used in any other transfer,
        // so it can be altered
        BarcodedTube barcodedTube = barcodedTubeDao.findByBarcode("0159873624");
        boolean found = false;
        for (VesselContainer<?> vesselContainer : barcodedTube.getVesselContainers()) {
            for (LabEvent labEvent : vesselContainer.getTransfersFrom()) {
                if (labEvent.getLabEventType() == LabEventType.SHEARING_TRANSFER) {
                    found = true;
                    TubeFormation tubeFormation = (TubeFormation) vesselContainer.getEmbedder();
                    Map<VesselPosition, BarcodedTube> mapPositionToVessel =
                            (Map<VesselPosition, BarcodedTube>) vesselContainer.getMapPositionToVessel();
                    changePosition(mapPositionToVessel, VesselPosition.A01, VesselPosition.A10);
                    changePosition(mapPositionToVessel, VesselPosition.A02, VesselPosition.A11);
                    changePosition(mapPositionToVessel, VesselPosition.A03, VesselPosition.A12);
                    changePosition(mapPositionToVessel, VesselPosition.B01, VesselPosition.B10);
                    changePosition(mapPositionToVessel, VesselPosition.B02, VesselPosition.B11);
                    changePosition(mapPositionToVessel, VesselPosition.B03, VesselPosition.B12);
                    changePosition(mapPositionToVessel, VesselPosition.C01, VesselPosition.C10);
                    changePosition(mapPositionToVessel, VesselPosition.C02, VesselPosition.C11);
                    changePosition(mapPositionToVessel, VesselPosition.C03, VesselPosition.C12);
                    changePosition(mapPositionToVessel, VesselPosition.D01, VesselPosition.D10);
                    changePosition(mapPositionToVessel, VesselPosition.D02, VesselPosition.D11);
                    changePosition(mapPositionToVessel, VesselPosition.E01, VesselPosition.E10);
                    changePosition(mapPositionToVessel, VesselPosition.E02, VesselPosition.E11);
                    changePosition(mapPositionToVessel, VesselPosition.F01, VesselPosition.F10);
                    changePosition(mapPositionToVessel, VesselPosition.F02, VesselPosition.F11);
                    changePosition(mapPositionToVessel, VesselPosition.G01, VesselPosition.G10);
                    changePosition(mapPositionToVessel, VesselPosition.G02, VesselPosition.G11);
                    changePosition(mapPositionToVessel, VesselPosition.H01, VesselPosition.H10);
                    changePosition(mapPositionToVessel, VesselPosition.H02, VesselPosition.H11);

                    tubeFormation.setLabel(TubeFormation.makeDigest(mapPositionToVessel));
                }
            }
        }
        if (!found) {
            throw new RuntimeException("Failed to find tube formation for shearing transfer");
        }
        barcodedTubeDao.flush();
    }

    private void changePosition(Map<VesselPosition, BarcodedTube> mapPositionToVessel, VesselPosition oldPosition,
            VesselPosition newPosition) {
        BarcodedTube barcodedTube1 = mapPositionToVessel.get(oldPosition);
        mapPositionToVessel.put(newPosition, barcodedTube1);
        mapPositionToVessel.remove(oldPosition);
    }

    @Test(enabled = false)
    public void fixupGplim2375() {
        BarcodedTube barcodedTube = barcodedTubeDao.findByBarcode("0116400440");
        LabMetric labMetric = barcodedTube.getMetrics().iterator().next();
        labMetric.setValue(new BigDecimal("21.75"));
        barcodedTubeDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim2926() {
        BarcodedTube barcodedTube = barcodedTubeDao.findByBarcode("0116404348");
        LabMetric labMetric = barcodedTube.getMetrics().iterator().next();
        labMetric.setValue(new BigDecimal("25.35"));
        barcodedTubeDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim2367Part2() {
        BarcodedTube barcodedTube = barcodedTubeDao.findByBarcode("0156349661");
        boolean found = false;
        for (VesselContainer<?> vesselContainer : barcodedTube.getVesselContainers()) {
            for (LabEvent labEvent : vesselContainer.getTransfersTo()) {
                if (labEvent.getLabEventType() == LabEventType.SAMPLES_DAUGHTER_PLATE_CREATION) {
                    found = true;
                    // Intended to change position of two tubes, but discovered that the changed tube formation already
                    // exists.
//                    TubeFormation tubeFormation = (TubeFormation) vesselContainer.getEmbedder();
//                    Map<VesselPosition, BarcodedTube> mapPositionToVessel =
//                            (Map<VesselPosition, BarcodedTube>) vesselContainer.getMapPositionToVessel();
//                    changePosition(mapPositionToVessel, VesselPosition.F02, VesselPosition.B01);
//                    changePosition(mapPositionToVessel, VesselPosition.E02, VesselPosition.F01);
//                    tubeFormation.setLabel(TubeFormation.makeDigest(mapPositionToVessel));
                    SectionTransfer sectionTransfer = labEvent.getSectionTransfers().iterator().next();
                    LabVessel labVessel = labVesselDao.findByIdentifier("627037c8cfd770a62af72e8a1eb92b43");
                    sectionTransfer.setTargetVesselContainer(labVessel.getContainerRole());
                }
            }
        }
        if (!found) {
            throw new RuntimeException("Failed to find tube formation for daughter plate transfer");
        }
        barcodedTubeDao.flush();
    }

    /*
    * BSP had a rearray done prior to pico, and Mercury must reflect it so quant upload works.
    */
    @Test(enabled = false)
    public void fixupIpi612227() {
        BarcodedTube tube5558 = barcodedTubeDao.findByBarcode("1107705558");
        BarcodedTube tube5548 = barcodedTubeDao.findByBarcode("1107705548");
        BarcodedTube tube1537 = barcodedTubeDao.findByBarcode("1107851537");
        BarcodedTube tube5557 = barcodedTubeDao.findByBarcode("1107705557");

        // Get rack & tube formation from database, just because I had already done the query.
        // select v2.label, pv.mapkey, vc.containers, v.label tube_barcode
        // from lab_vessel v, lab_vessel_containers vc, lab_vessel_racks_of_tubes rot,
        //      lab_vessel v2, lv_map_position_to_vessel pv
        // where v.lab_vessel_id = vc.lab_vessel
        // and v.lab_vessel_id = pv.map_position_to_vessel
        // and vc.containers = rot.lab_vessel
        // and rot.racks_of_tubes = v2.lab_vessel_id
        // and rot.lab_vessel = pv.lab_vessel
        // and v2.label in ('CO-9565972', 'CO-9604477')
        // and v.label  in ('1107705558', '1107705548', '1107851537', '1107705557')
        // order by tube_barcode;
        //
        // CO-9565972 B02 1221051 1107705548
        // CO-9604477 A01 1221054 1107705548
        // CO-9565972 H01 1221051 1107705557
        // CO-9604477 C01 1221054 1107705557
        // CO-9565972 A02 1221051 1107705558
        // CO-9565972 C02 1221051 1107851537
        // CO-9604477 B01 1221054 1107851537

        VesselContainer<?> co72 = getContainerForTube(tube5548, 1221051L);
        VesselContainer<?> co77 = getContainerForTube(tube5548, 1221054L);

        Assert.assertNotNull(co72);
        Assert.assertNotNull(co77);

        TubeFormation tf72 = (TubeFormation) co72.getEmbedder();
        TubeFormation tf77 = (TubeFormation) co77.getEmbedder();

        Assert.assertNotNull(tf72);
        Assert.assertNotNull(tf77);

        Map<VesselPosition, BarcodedTube> mapPv72 =
                (Map<VesselPosition, BarcodedTube>) co72.getMapPositionToVessel();
        Map<VesselPosition, BarcodedTube> mapPv77 =
                (Map<VesselPosition, BarcodedTube>) co77.getMapPositionToVessel();

        // Want this:
        //CO-9565972.H01 1107705558  SM-6AI7J
        //CO-9565972.A02 null
        //CO-9565972.B02 null
        //CO-9565972.C02 null
        //CO-9604477.A01 1107705548  SM-6AI7K
        //CO-9604477.B01 1107851537  SM-6AI92
        //CO-9604477.C01 1107705557  SM-6AI7I

        mapPv72.put(VesselPosition.H01, tube5558);
        mapPv72.remove(VesselPosition.A02);
        mapPv72.remove(VesselPosition.B02);
        mapPv72.remove(VesselPosition.C02);
        mapPv77.put(VesselPosition.A01, tube5548);
        mapPv77.put(VesselPosition.B01, tube1537);
        mapPv77.put(VesselPosition.C01, tube5557);

        tf72.setLabel(TubeFormation.makeDigest(mapPv72));
        tf77.setLabel(TubeFormation.makeDigest(mapPv77));

        barcodedTubeDao.flush();
    }

    private VesselContainer<?> getContainerForTube(BarcodedTube barcodedTube, Long tubeFormationId) {
        for (VesselContainer<?> container : barcodedTube.getVesselContainers()) {
            if (container.getEmbedder().getLabVesselId().equals(tubeFormationId)) {
                return container;
            }
        }
        return null;
    }

    @Test(enabled = false)
    public void fixupGplim2449() {
        // SM-4VFD1 is in the Pico bucket twice: 0150466237, 0156371090
        BarcodedTube oldTube = barcodedTubeDao.findByBarcode("0156371090");
        barcodedTubeDao.remove(oldTube);
    }

    /**
     * This is done before importing index plates from Squid.
     */
    @Test(enabled = false)
    public void fixupGplim3164() {
        userBean.loginOSUser();
        StaticPlate staticPlate = staticPlateDao.findByBarcode("000001814423");
        System.out.println("Renaming plate " + staticPlate.getLabel());
        staticPlate.setLabel("000001814423-GPLIM-3164");
        staticPlateDao.flush();
    }

    @Test(enabled = false)
    public void gplim3103UpdateVolumes() {
        userBean.loginOSUser();
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(Arrays.asList(
                "0175362322",
                "0175362261",
                "0175362291",
                "0175362304",
                "0175362254",
                "0175362302",
                "0175362332",
                "0175362266",
                "0175362256",
                "0175362249",
                "0175362320",
                "0175362281",
                "0175362316",
                "0175362293",
                "0175362279",
                "0175362308",
                "0175362257",
                "0175362309",
                "0175362298",
                "0175362271",
                "0175362314",
                "0175362295",
                "0175362306",
                "0175362334",
                "0175362258",
                "0175362268",
                "0175362286",
                "0175362319",
                "0175362335",
                "0175362312",
                "0175362297",
                "0175362274",
                "0175362326",
                "0175362289",
                "0175362310",
                "0175362246",
                "0175362255",
                "0175362273",
                "0175362270",
                "0175362294",
                "0175362305",
                "0175362265",
                "0175362260",
                "0175362267",
                "0175362243",
                "0175362247",
                "0175362282",
                "0175362292",
                "0175362245",
                "0175362330",
                "0175362325",
                "0175362290",
                "0175362315",
                "0175362301",
                "0175362285",
                "0175362303",
                "0175362283",
                "0175362280",
                "0175358893",
                "0175362327",
                "0175362263",
                "0175362277",
                "0175362264",
                "0175362272",
                "0175362262",
                "0175362288",
                "0175362328",
                "0175362248",
                "0175362311",
                "0175362296",
                "0175362318",
                "0175362284",
                "0175362241",
                "0175362331",
                "0175362329",
                "0175362278",
                "0175362269",
                "0175362244",
                "0175362333",
                "0175362287",
                "0175362317",
                "0175362307",
                "0175362313",
                "0175362242",
                "0175362259"));
        for (Map.Entry<String, BarcodedTube> stringBarcodedTubeEntry : mapBarcodeToTube.entrySet()) {
            BarcodedTube barcodedTube = stringBarcodedTubeEntry.getValue();
            if (barcodedTube == null) {
                throw new RuntimeException("Failed to find tube " + stringBarcodedTubeEntry.getKey());
            }
            System.out.println("Updating volume in " + barcodedTube.getLabel());
            barcodedTube.setVolume(new BigDecimal("61"));
        }
        barcodedTubeDao.flush();
    }

    @Test(enabled = false)
    public void gplim3103UpdateVolumesNegControl() {
        userBean.loginOSUser();
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(Arrays.asList(
                "0175362285"));
        for (Map.Entry<String, BarcodedTube> stringBarcodedTubeEntry : mapBarcodeToTube.entrySet()) {
            BarcodedTube barcodedTube = stringBarcodedTubeEntry.getValue();
            if (barcodedTube == null) {
                throw new RuntimeException("Failed to find tube " + stringBarcodedTubeEntry.getKey());
            }
            System.out.println("Updating volume in " + barcodedTube.getLabel());
            barcodedTube.setVolume(new BigDecimal("131.3"));
        }
        barcodedTubeDao.flush();
    }


    @Test(enabled = false)
    public void gplim3106FixupVolumes() {
        userBean.loginOSUser();
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(Arrays.asList(
                "0175362322",
                "0175362261",
                "0175362291",
                "0175362304",
                "0175362254",
                "0175362302",
                "0175362332",
                "0175362266",
                "0175362256",
                "0175362249",
                "0175362320",
                "0175362281",
                "0175362316",
                "0175362293",
                "0175362279",
                "0175362308",
                "0175362257",
                "0175362309",
                "0175362298",
                "0175362271",
                "0175362314",
                "0175362295",
                "0175362306",
                "0175362334",
                "0175362258",
                "0175362268",
                "0175362286",
                "0175362319",
                "0175362335",
                "0175362312",
                "0175362297",
                "0175362274",
                "0175362326",
                "0175362289",
                "0175362310",
                "0175362246",
                "0175362255",
                "0175362273",
                "0175362270",
                "0175362294",
                "0175362305",
                "0175362265",
                "0175362260",
                "0175362267",
                "0175362243",
                "0175362247",
                "0175362282",
                "0175362292",
                "0175362245",
                "0175362330",
                "0175362325",
                "0175362290",
                "0175362315",
                "0175362301",
                "0175362285",
                "0175362303",
                "0175362283",
                "0175362280",
                "0175358893",
                "0175362327",
                "0175362263",
                "0175362277",
                "0175362264",
                "0175362272",
                "0175362262",
                "0175362288",
                "0175362328",
                "0175362248",
                "0175362311",
                "0175362296",
                "0175362318",
                "0175362284",
                "0175362241",
                "0175362331",
                "0175362329",
                "0175362278",
                "0175362269",
                "0175362244",
                "0175362333",
                "0175362287",
                "0175362317",
                "0175362307",
                "0175362313",
                "0175362242",
                "0175362259"));
        for (Map.Entry<String, BarcodedTube> stringBarcodedTubeEntry : mapBarcodeToTube.entrySet()) {
            BarcodedTube barcodedTube = stringBarcodedTubeEntry.getValue();
            if (barcodedTube == null) {
                throw new RuntimeException("Failed to find tube " + stringBarcodedTubeEntry.getKey());
            }
            System.out.println("Updating volume in " + barcodedTube.getLabel());
            barcodedTube.setVolume(barcodedTube.getVolume().subtract(new BigDecimal("2.50")));
        }
        barcodedTubeDao.flush();
    }


    @Test(enabled = false)
    public void gplim3139FixupVolumes() {
        userBean.loginOSUser();
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(Arrays.asList(
                "0173519367","0173519410","0173519344","0173519391","0173519387","0173519377","0173519390","0173519385"
        ));
        BigDecimal expectedVolume = new BigDecimal("41");
        BigDecimal correctVolume = new BigDecimal("36");
        for (String barcode : mapBarcodeToTube.keySet()) {
            BarcodedTube barcodedTube = mapBarcodeToTube.get(barcode);
            if (barcodedTube == null) {
                throw new RuntimeException("Failed to find tube " + barcode);
            }
            if (barcodedTube.getVolume().compareTo(expectedVolume) == 0) {
                System.out.println("Updating volume in " + barcodedTube.getLabel() +
                                   " from " + barcodedTube.getVolume().toString() +
                                   " to " + correctVolume.toString());
                barcodedTube.setVolume(correctVolume);
            } else {
                throw new RuntimeException("tube " + barcode + " has unexpected volume " + barcodedTube.getVolume());
            }
        }
        barcodedTubeDao.flush();
    }

    @Test(enabled = false)
    public void gplim3140FixupVolumes() {
        userBean.loginOSUser();
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(Arrays.asList(
                "0175358893", "0175362241", "0175362242", "0175362243", "0175362244", "0175362245", "0175362246",
                "0175362247", "0175362248", "0175362249", "0175362254", "0175362255", "0175362256", "0175362257",
                "0175362258", "0175362259", "0175362260", "0175362261", "0175362262", "0175362263", "0175362264",
                "0175362265", "0175362266", "0175362267", "0175362268", "0175362269", "0175362270", "0175362271",
                "0175362272", "0175362273", "0175362274", "0175362277", "0175362278", "0175362279", "0175362280",
                "0175362281", "0175362282", "0175362283", "0175362284", "0175362285", "0175362286", "0175362287",
                "0175362288", "0175362289", "0175362290", "0175362291", "0175362292", "0175362293", "0175362294",
                "0175362295", "0175362296", "0175362297", "0175362298", "0175362301", "0175362302", "0175362303",
                "0175362304", "0175362305", "0175362306", "0175362307", "0175362308", "0175362309", "0175362310",
                "0175362311", "0175362312", "0175362313", "0175362314", "0175362315", "0175362316", "0175362317",
                "0175362318", "0175362319", "0175362320", "0175362322", "0175362325", "0175362326", "0175362327",
                "0175362328", "0175362329", "0175362330", "0175362331", "0175362332", "0175362333", "0175362334",
                "0175362335"
        ));
        BigDecimal diffVolume = new BigDecimal("2.5");
        for (String barcode : mapBarcodeToTube.keySet()) {
            BarcodedTube barcodedTube = mapBarcodeToTube.get(barcode);
            if (barcodedTube == null) {
                throw new RuntimeException("Failed to find tube " + barcode);
            }
            System.out.println(barcodedTube.getLabel() + " has volume " + barcodedTube.getVolume());
            barcodedTube.setVolume(barcodedTube.getVolume().add(diffVolume));
            System.out.println("Updated to " + barcodedTube.getVolume());
        }
        barcodedTubeDao.flush();
    }

    @Test(enabled = false)
    public void gplim3154relabelTubes() {
        userBean.loginOSUser();
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(Arrays.asList(
                "0175596971", "0175596970", "0175596969", "0175596968", "0175596967", "0175596966", "0175596965",
                "0175596964", "0175596963", "0175596962", "0175596961", "0175596960", "0175596972", "0175596973",
                "0175596974", "0175596975", "0175596976", "0175596977", "0175596978", "0175596979", "0175596980",
                "0175596981", "0175596982", "0175596983", "0175596995", "0175596994", "0175596993", "0175596992",
                "0175596991", "0175596990", "0175596989", "0175596988", "0175596987", "0175596986", "0175596985",
                "0175596984", "0175596996", "0175596997", "0175596998", "0175596999", "0175597000", "0175597001",
                "0175597002", "0175597003", "0175597004", "0175597005", "0175597006", "0175597007", "0175597019",
                "0175597018", "0175597017", "0175597016", "0175597015", "0175597014", "0175597013", "0175597012",
                "0175597011", "0175597010", "0175597009", "0175597008", "0175597020", "0175597021", "0175597022",
                "0175597023", "0175597024", "0175597025", "0175597026", "0175597027", "0175597028", "0175597029",
                "0175597030", "0175597031", "0175597043", "0175597042", "0175597041", "0175597040", "0175597039",
                "0175597038", "0175597037", "0175597036", "0175597035", "0175597034", "0175597033", "0175597032",
                "0175597044", "0175597045", "0175597046", "0175597047", "0175597048", "0175597049", "0175597050",
                "0175597051", "0175597052", "0175597053", "0175597054", "0175597055"));
        for (String barcode : mapBarcodeToTube.keySet()) {
            System.out.println("Relabeling tube " + barcode);
            BarcodedTube tube = mapBarcodeToTube.get(barcode);
            if (tube == null) {
                throw new RuntimeException("cannot find tube " + barcode);
            }
            tube.setLabel(barcode + "gplim3154");
        }
        barcodedTubeDao.flush();
    }

    @Test(enabled = false)
    public void qual525FixupVolumes() {
        userBean.loginOSUser();
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(Arrays.asList(
                "0175568232",
                "0175568229"
        ));
        for (Map.Entry<String, BarcodedTube> stringBarcodedTubeEntry : mapBarcodeToTube.entrySet()) {
            if (stringBarcodedTubeEntry.getValue() == null) {
                throw new RuntimeException("Failed to find tube " + stringBarcodedTubeEntry.getKey());
            }
            System.out.println("Setting " + stringBarcodedTubeEntry.getKey() + " to 65");
            stringBarcodedTubeEntry.getValue().setVolume(new BigDecimal("65.00"));
        }
        barcodedTubeDao.persist(new FixupCommentary("QUAL-525 set volumes to 65"));
        barcodedTubeDao.flush();
    }

    private static class BarcodeVolume {
        private final String barcode;
        private final BigDecimal volume;

        BarcodeVolume(String barcode, BigDecimal volume) {
            this.barcode = barcode;
            this.volume = volume;
        }

        public String getBarcode() {
            return barcode;
        }

        public BigDecimal getVolume() {
            return volume;
        }
    }

    @Test(enabled = false)
    public void gplim3269FixupVolumes() {
        userBean.loginOSUser();
        BarcodeVolume[] barcodeVolumes = {
                new BarcodeVolume("0175349826", new BigDecimal("35.00")),
                new BarcodeVolume("0175359203", new BigDecimal("32.00")),
                new BarcodeVolume("0175349846", new BigDecimal("37.00")),
                new BarcodeVolume("0175349790", new BigDecimal("18.00")),
                new BarcodeVolume("0175349772", new BigDecimal("45.00")),
                new BarcodeVolume("0175349817", new BigDecimal("28.00")),
                new BarcodeVolume("0175349839", new BigDecimal("36.00"))
        };
        for (BarcodeVolume barcodeVolume : barcodeVolumes) {
            BarcodedTube barcodedTube = barcodedTubeDao.findByBarcode(barcodeVolume.getBarcode());
            if (barcodedTube == null) {
                throw new RuntimeException("Failed to find tube " + barcodeVolume.getBarcode());
            }
            System.out.println("Updating " + barcodeVolume.getBarcode() + " to " + barcodeVolume.getVolume());
            barcodedTube.setVolume(barcodeVolume.getVolume());
        }
        barcodedTubeDao.persist(new FixupCommentary("GPLIM-3269 volume fixup"));
        barcodedTubeDao.flush();
    }

    @Test(enabled = false)
    public void gplim3247FixupVolumes() {
        userBean.loginOSUser();
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(Arrays.asList(
                "0175569248",
                "0175569269",
                "0175569227",
                "0175569290"
        ));
        for (Map.Entry<String, BarcodedTube> stringBarcodedTubeEntry : mapBarcodeToTube.entrySet()) {
            if (stringBarcodedTubeEntry.getValue() == null) {
                throw new RuntimeException("Failed to find tube " + stringBarcodedTubeEntry.getKey());
            }
            System.out.println("Setting " + stringBarcodedTubeEntry.getKey() + " to 0");
            stringBarcodedTubeEntry.getValue().setVolume(new BigDecimal("0.00"));
        }
        barcodedTubeDao.persist(new FixupCommentary("GPLIM-3247 set volumes to 0"));
        barcodedTubeDao.flush();
    }

    @Test(enabled = false)
    public void gplim3257FixupVolumes() {
        userBean.loginOSUser();
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(Arrays.asList(
                "0175569240", "0175569239", "0175569306", "0175547419", "0175569302"
        ));
        BigDecimal correctVolume = BigDecimal.ZERO;
        for (String barcode : mapBarcodeToTube.keySet()) {
            BarcodedTube barcodedTube = mapBarcodeToTube.get(barcode);
            if (barcodedTube == null) {
                throw new RuntimeException("Failed to find tube " + barcode);
            }
            System.out.println("Updating volume for tube " + barcodedTube.getLabel() +
                    " from " + barcodedTube.getVolume().toString() +
                    " to " + correctVolume.toString());
            barcodedTube.setVolume(correctVolume);
        }
        barcodedTubeDao.persist(new FixupCommentary("GPLIM-3257 fixup manually adjusted tube volumes."));
        barcodedTubeDao.flush();
    }

    @Test(enabled = false)
    public void gplim3376FixupFlowcellLabel() {
        userBean.loginOSUser();
        // Change lab_vessel 1946416 label HGKJCADXX
        LabVessel flowcell = labVesselDao.findById(LabVessel.class, 1946416L);
        Assert.assertNotNull(flowcell);
        flowcell.setLabel("HGKJCADXX");
        System.out.println("Updated flowcell " + flowcell.getLabVesselId() + " label to " + flowcell.getLabel());
        labVesselDao.persist(new FixupCommentary("GPLIM-3376 fixup flowcell label."));
        labVesselDao.flush();
    }

    @Test(enabled = false)
    public void qual623FixupVolumes() {
        userBean.loginOSUser();
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(Arrays.asList(
                "0175568063",
                "0175568040",
                "0175568039",
                "0175568016",
                "0175568015",
                "0175567992",
                "0175567991",
                "0175567968"));
        for (Map.Entry<String, BarcodedTube> stringBarcodedTubeEntry : mapBarcodeToTube.entrySet()) {
            BarcodedTube barcodedTube = stringBarcodedTubeEntry.getValue();
            if (barcodedTube == null) {
                throw new RuntimeException("Failed to find tube " + stringBarcodedTubeEntry.getKey());
            }
            System.out.println(barcodedTube.getLabel() + " has volume " + barcodedTube.getVolume());
            barcodedTube.setVolume(new BigDecimal("65.00"));
            System.out.println("Updated to " + barcodedTube.getVolume());
        }
        barcodedTubeDao.persist(new FixupCommentary("QUAL-623 update volumes"));
        barcodedTubeDao.flush();
    }

    @Test(enabled = false)
    public void gplim3525UpdateVolume() {
        userBean.loginOSUser();
        String barcode = "0129040052";
        BarcodedTube barcodedTube = barcodedTubeDao.findByBarcode(barcode);
        if (barcodedTube == null) {
            throw new RuntimeException("Failed to find tube " + barcode);
        }
        BigDecimal newVolume = new BigDecimal("157");
        System.out.println(
                "Updating volume of " + barcodedTube.getLabel() + " from " + barcodedTube.getVolume() + " to "
                + newVolume);
        barcodedTube.setVolume(newVolume);
        barcodedTubeDao.persist(new FixupCommentary("GPLIM-3525 manually set volume for tube missing initial tare"));
        barcodedTubeDao.flush();
    }

    @Test(enabled = false)
    public void fixupSupport1011_2() {
        userBean.loginOSUser();

        // Rack CO-15323029 has two tube formations. Keep the one that is lab_vessel_id 2197589
        // and remove the one that is lab_vessel_id 2212665.
        RackOfTubes rackToDisassociate = (RackOfTubes) labVesselDao.findByIdentifier("CO-15323029");
        TubeFormation tubeFormation = labVesselDao.findById(TubeFormation.class, 2212665L);

        Assert.assertNotNull(rackToDisassociate);
        Assert.assertNotNull(tubeFormation);
        Assert.assertTrue(tubeFormation.getRacksOfTubes().contains(rackToDisassociate));
        System.out.println("Removing tube formation " + tubeFormation.getLabel() + " id " +
                           tubeFormation.getLabVesselId() + " from rack " + rackToDisassociate.getLabel());
        tubeFormation.getRacksOfTubes().remove(rackToDisassociate);
        labVesselDao.persist(new FixupCommentary("SUPPORT-1011 fixup incorrect rack contents due to label swap"));
        labVesselDao.flush();
    }

    @Test(enabled = false)
    public void fixupSupport1011_3() {
        try {
            userBean.loginOSUser();
            utx.begin();
            // Invalidates the plate barcodes by appending an X.
            String[] plateBarcodes = {"000010676169", "000010720769"};
            for (String plateBarcode : plateBarcodes) {
                StaticPlate plate = staticPlateDao.findByBarcode(plateBarcode);
                System.out.println("Rename plate " + plateBarcode + " to " + plateBarcode + "X");
                plate.setLabel(plateBarcode + "X");
            }
            staticPlateDao.persist(new FixupCommentary(
                    "SUPPORT-1011 invalidate barcodes of incorrect pico plates."));
            staticPlateDao.flush();
            utx.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupSupport1011_3a() {
        try {
            userBean.loginOSUser();
            utx.begin();
            // Removes vessel transfers from incorrect tube formation to white and black pico plates.
            Collection<Long> eventIds = Arrays.asList(new Long[]{993964L, 993965L, 993966L, 993968L});
            for (LabEvent labEvent : labVesselDao.findListByList(LabEvent.class, LabEvent_.labEventId, eventIds)) {
                System.out.println("Deleting " + labEvent.getLabEventType() + " " + labEvent.getLabEventId());
                labEvent.getReagents().clear();
                labEvent.getSectionTransfers().clear();
                labEvent.getVesselToSectionTransfers().clear();
                labVesselDao.remove(labEvent);
            }
            labVesselDao.persist(new FixupCommentary(
                    "SUPPORT-1011 delete incorrect pico transfer because of bad rack tube formation"));
            labVesselDao.flush();
            utx.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test(enabled = false)
    public void fixupGplim3807() {
        userBean.loginOSUser();

        LabVessel labVessel = labVesselDao.findByIdentifier("ms2031375-50v2");
        System.out.println("Changing " + labVessel.getLabel() + " to upper case");
        labVessel.setLabel(labVessel.getLabel().toUpperCase());

        labVessel = labVesselDao.findByIdentifier("ms2031414-50v2");
        System.out.println("Changing " + labVessel.getLabel() + " to upper case");
        labVessel.setLabel(labVessel.getLabel().toUpperCase());

        labVesselDao.persist(new FixupCommentary("GPLIM-3807 change barcodes to uppercase"));
        labVesselDao.flush();
    }
    /**
     * This test reads its parameters from a file, testdata/FixupVolumes.txt, so it can be used for other similar fixups,
     * without writing a new test.  Example contents of the file are:
     * SUPPORT-1289
     * 0185769113 50.00
     * 0185769029 50.00
     */
    @Test(enabled = false)
    public void support1289UpdateVolume() {
        try {
            userBean.loginOSUser();
            List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("FixupVolumes.txt"));
            String jiraTicket = lines.get(0);
            for (int i = 1; i < lines.size(); i++) {
                String[] fields = WHITESPACE_PATTERN.split(lines.get(i));
                if (fields.length != 2) {
                    throw new RuntimeException("Expected two white-space separated fields in " + lines.get(i));
                }
                String barcode = fields[0];
                BigDecimal newVolume = new BigDecimal(fields[1]);
                BarcodedTube barcodedTube = barcodedTubeDao.findByBarcode(barcode);
                if (barcodedTube == null) {
                    throw new RuntimeException("Failed to find tube " + barcode);
                }
                System.out.println("Updating volume of " + barcodedTube.getLabel() + " from " +
                        barcodedTube.getVolume() + " to " + newVolume);
                barcodedTube.setVolume(newVolume);
            }
            barcodedTubeDao.persist(new FixupCommentary(jiraTicket + " update volumes"));
            barcodedTubeDao.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Delete fingerprinting controls that were uploaded without leading zero. */
    @Test(enabled = false)
    public void fixupGplim3953() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(Arrays.asList(
                "183491135",
                "183491112",
                "183491111",
                "183491088",
                "183491087",
                "183491064",
                "183491063",
                "183491040",
                "183491134",
                "183491113",
                "183491110",
                "183491089",
                "183491086",
                "183491065",
                "183491062",
                "183491041",
                "183491133",
                "183491114",
                "183491109",
                "183491090",
                "183491085",
                "183491066",
                "183491061",
                "183491042",
                "183491132",
                "183491115",
                "183491108",
                "183491091",
                "183491084",
                "183491067",
                "183491060",
                "183491043",
                "183491131",
                "183491116",
                "183491107",
                "183491092",
                "183491083",
                "183491068",
                "183491059",
                "183491044",
                "183491130",
                "183491117",
                "183491106",
                "183491093",
                "183491082",
                "183491069",
                "183491058",
                "183491045"));

        for (Map.Entry<String, BarcodedTube> barcodeTubeEntry : mapBarcodeToTube.entrySet()) {
            BarcodedTube barcodedTube = barcodeTubeEntry.getValue();
            if (barcodedTube == null) {
                throw new RuntimeException("Failed to find " + barcodeTubeEntry.getKey());
            }
            barcodedTube.getReagentContents().clear();
            System.out.println("Deleting " + barcodedTube.getLabel());
            labVesselDao.remove(barcodedTube);
        }

        labVesselDao.persist(new FixupCommentary("GPLIM-3953 delete fingerprinting controls with no leading zero"));
        labVesselDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim3994() throws Exception {
        userBean.loginOSUser();
        utx.begin();

/*   *** Find unassigned positive controls
     select lv.label, ms.sample_key,
             m.value, to_char(lv.created_on, 'mm/dd/yyyy') as sample_tube_created
        from mercury_sample ms
           , mercury_sample_metadata msm
           , metadata m
           , lab_vessel lv
           , lab_vessel_mercury_samples lvms
           , batch_starting_vessels bsv
       where m.key        = 'PATIENT_ID'
         and m.value      = 'NA12878'
         and msm.metadata = m.metadata_id
         and ms.mercury_sample_id = msm.mercury_sample
         and lvms.mercury_samples = ms.mercury_sample_id
         and lv.lab_vessel_id     = lvms.lab_vessel
         and bsv.lab_vessel(+)    = lv.lab_vessel_id
         and bsv.lab_vessel is null
       order by lv.created_on asc
       ***         */

        List<BarcodedTube> controlTubes = barcodedTubeDao.findListByBarcodes(Arrays.asList(
                "0175336694", "0175336665", "0175336641", "0175336622", "0175336646",
                "0175361649", "0175567677", "0175567678", "0175567674", "0175567661",
                "0175567654", "0175567627", "0175567638", "0175567662", "0175567613",
                "0175567607", "0175567589", "0175567584", "0175567603", "0175567633",
                "0175567606", "0175567588", "0175567637", "0175567585", "0175567658",
                "0175567653", "0175567609", "0175567602", "0175567626", "0175567650",
                "0175567649", "0175567673", "0175567657", "0175568047", "0175568048",
                "0175568055", "0175568031", "0175568061", "0175568029", "0175568034",
                "0175568035", "0175568059", "0175568033", "0175568057", "0175568044",
                "0175568032", "0175568056", "0175568028", "0175568342", "0175568281",
                "0175568325", "0175568292", "0175568313", "0175568335", "0175568305",
                "0175568295", "0175568287", "0175568328", "0175568337", "0175568306",
                "0175568312", "0175568349", "0175568340", "0175568284", "0175568299",
                "0175568334", "0175568345", "0175568289", "0175568323", "1124999980",
                "1124999981", "1124999982", "1124999983", "1124999985", "1124999986",
                "1124999987", "1124999988", "1124999989", "1125000003", "1125000002",
                "1125000001", "1125000000", "1124999999", "1124999998", "1124999997",
                "1124999996", "1124999995", "1124999994", "1125000004", "1125000005",
                "1125000006", "1125000007", "1125000008", "1125000009", "1125000010",
                "1125000011", "1125000012", "1124998772", "1125000027", "1125000026",
                "1125000025", "1125000024", "1125000023", "1125000022", "1125000021",
                "1125000020", "1125000019", "1125000018", "1125000016", "1125000028",
                "1125000029", "1125000030", "1125000031", "1125000032", "1125000033",
                "1125000034", "1125000035", "1125000036", "1125000037", "1125000038",
                "1125000039", "1125000051", "1125000050", "1125000049", "1125000048",
                "1125000047", "1125000046", "1125000045", "1125000044", "1125000043",
                "1125000042", "1125000041", "1124998791", "1125000052", "1125000053",
                "1125000054", "1125000055", "1125000056", "1125000057", "1125000058",
                "1125000059", "1125000060", "1125000061", "1125000062", "1125000063",
                "1125000075", "1125000074", "1125000073", "1125000072", "1125000071",
                "1125000070", "1125000069", "1125000068", "1125000067", "1125000066"));

        for( LabVessel controlTube : controlTubes ){
            // Restrict logic to the existence of a ShearingAliquot transfer directly from the control vessel
            for( LabEvent xferFrom : controlTube.getTransfersFrom() ) {
                if( xferFrom.getLabEventType() == LabEventType.SHEARING_ALIQUOT ) {
                    // Additionally restrict to an event with a single LCSET
                    Set<LabBatch> lcsets = xferFrom.getComputedLcSets();
                    if( lcsets.size() == 1 ) {
                        LabBatch lcset = lcsets.iterator().next();
                        controlTube.addNonReworkLabBatch(lcset);
                        System.out.println("Tube: " + controlTube.getLabel() + ", LCSET: " + lcset.getBatchName() );
                    }
                    break;
                }
            }
        }

        FixupCommentary fixupCommentary = new FixupCommentary("GPLIM-3994 - Assign controls to applicable LCSETs");
        barcodedTubeDao.persist(fixupCommentary);
        barcodedTubeDao.flush();

        utx.commit();

    }

    @Test(enabled = false)
    public void fixupGplim4165() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        // Changes two tube positions in the destination container. Its label was obtained by Transfer Visualizer.
        String tubeFormationLabel = "0ff71eadca9d4ee16abb37a987df13c1";
        TubeFormation tubeFormation = tubeFormationDao.findByDigest(tubeFormationLabel);
        Assert.assertNotNull(tubeFormation);
        VesselContainer<?> vesselContainer = tubeFormation.getContainerRole();
        Assert.assertNotNull(vesselContainer);

        // Manipulating the position-vessel map alone only works for section transfers, so verify.
        Assert.assertEquals(tubeFormation.getTransfersTo().size(), 1);
        Assert.assertEquals(tubeFormation.getTransfersTo().iterator().next().getCherryPickTransfers().size(), 0);
        Assert.assertEquals(tubeFormation.getTransfersTo().iterator().next().getSectionTransfers().size(), 1);

        Map<VesselPosition, BarcodedTube> mapPositionToVessel =
                (Map<VesselPosition, BarcodedTube>) vesselContainer.getMapPositionToVessel();

        Assert.assertEquals(mapPositionToVessel.get(VesselPosition.A12).getLabel(), "0201127659");
        changePosition(mapPositionToVessel, VesselPosition.A12, VesselPosition.A11);

        Assert.assertEquals(mapPositionToVessel.get(VesselPosition.C12).getLabel(), "0201127638");
        changePosition(mapPositionToVessel, VesselPosition.C12, VesselPosition.C11);

        tubeFormation.setLabel(TubeFormation.makeDigest(mapPositionToVessel));

        FixupCommentary fixupCommentary = new FixupCommentary("GPLIM-4165 Fixup tube positions.");
        barcodedTubeDao.persist(fixupCommentary);
        barcodedTubeDao.flush();

        utx.commit();

    }

    @Test(enabled = false)
    public void fixupGplim4202() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<BarcodedTube> tubes = barcodedTubeDao.findListByBarcodes(Arrays.asList(
                "1125642961",
                "1125642962",
                "1125795498",
                "1125795497"));

        for (LabVessel labVessel : tubes) {
            labVessel.setReceptacleWeight(new BigDecimal(".62"));
            System.out.println("Setting tube initial tare weight: " + labVessel.getLabel() + " to .62");
        }

        FixupCommentary fixupCommentary = new FixupCommentary("GPLIM-4202 - Assign missing tare values to default");
        barcodedTubeDao.persist(fixupCommentary);
        barcodedTubeDao.flush();

        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim4249() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<BarcodedTube> tubes = barcodedTubeDao.findListByBarcodes(Arrays.asList(
                "1125641737",
                "1125641736",
                "1125641811",
                "1125641735",
                "1125642482",
                "1125641738",
                "1125641810",
                "1125641759",
                "1125641807",
                "1125641761",
                "1125641762",
                "1125641760",
                "1125641809",
                "1125641808",
                "1125641786",
                "1125641785",
                "1125641784",
                "1125641783"));

        for (LabVessel labVessel : tubes) {
            labVessel.setReceptacleWeight(new BigDecimal(".62"));
            System.out.println("Setting tube initial tare weight: " + labVessel.getLabel() + " to .62");
        }

        FixupCommentary fixupCommentary = new FixupCommentary("GPLIM-4249 - Assign missing tare values to default");
        barcodedTubeDao.persist(fixupCommentary);
        barcodedTubeDao.flush();

        utx.commit();
    }

    /**
     * Reads container barcodes and plate names from mercury/src/test/resources/testdata/FixupPlateNames.txt.
     * It's read as a csv file and rows should have two fields.
     * The first row should have the fixup commentary reason:
     * GPLIM-4276,set plate names from file
     * Subsequent rows should have plateLabel and plateName:
     * CO-29561374,MultiPDO_0910_GSA14
     */
    @Test(enabled = false)
    public void fixupGplim4276() throws Exception {
        /*
        Use this BSP query to fill the file
        SELECT DISTINCT
            'CO-'||br.RECEPTACLE_ID,
            br.RECEPTACLE_NAME
        FROM
            bsp.bsp_export_job bej
            INNER JOIN bsp.bsp_receptacle br
                ON   br.receptacle_id = bej.receptacle_id
        WHERE
            bej.export_type = 'MERCURY'
        ORDER BY
          1;
         */
        userBean.loginOSUser();
        utx.begin();

        InputStream csvInputStream = VarioskanParserTest.getTestResource("FixupPlateNames.txt");
        CSVReader reader = new CSVReader(new InputStreamReader(csvInputStream));
        String [] nextLine;
        // Gets the reason from the first line.
        nextLine = reader.readNext();
        Assert.assertNotNull(nextLine);
        String reason = StringUtils.join(nextLine, " ");
        // Subsequent lines have data.
        while ((nextLine = reader.readNext()) != null) {
            LabVessel labVessel = labVesselDao.findByIdentifier(nextLine[0]);
            if (labVessel == null) {
                System.out.println(nextLine[0] + " not found");
            } else {
                System.out.println("updating " + nextLine[0]);
                labVessel.setName(nextLine[1]);
            }
        }
        FixupCommentary fixupCommentary = new FixupCommentary(reason);
        barcodedTubeDao.persist(fixupCommentary);
        barcodedTubeDao.flush();

        utx.commit();
    }
    @Test(enabled = false)
    public void fixupGplim4301() throws Exception {
        // the source tubeformation for the ShearingTransfer doesn't seem to have been used in any other transfer,
        // so it can be altered
        userBean.loginOSUser();
        utx.begin();
        BarcodedTube barcodedTube = barcodedTubeDao.findByBarcode("0206882951");
        boolean found = false;
        for (VesselContainer<?> vesselContainer : barcodedTube.getVesselContainers()) {
            for (LabEvent labEvent : vesselContainer.getTransfersFrom()) {
                if (labEvent.getLabEventType() == LabEventType.SHEARING_TRANSFER) {
                    found = true;
                    TubeFormation tubeFormation = (TubeFormation) vesselContainer.getEmbedder();
                    Map<VesselPosition, BarcodedTube> mapPositionToVessel =
                            (Map<VesselPosition, BarcodedTube>) vesselContainer.getMapPositionToVessel();
                    BarcodedTube barcodedTubeAtG3 = mapPositionToVessel.get(VesselPosition.G03);
                    Assert.assertEquals("0206882951", barcodedTubeAtG3.getLabel());
                    System.out.println("Moving tube " + barcodedTubeAtG3.getLabVesselId() + " from G03 to B11");
                    changePosition(mapPositionToVessel, VesselPosition.G03, VesselPosition.B11);
                    tubeFormation.setLabel(TubeFormation.makeDigest(mapPositionToVessel));
                }
            }
        }
        if (!found) {
            throw new RuntimeException("Failed to find tube formation for shearing transfer");
        }
        FixupCommentary fixupCommentary = new FixupCommentary("GPLIM-4301 - Move sample from G03 to B11");
        barcodedTubeDao.persist(fixupCommentary);
        barcodedTubeDao.flush();

        utx.commit();
    }

    @Test(enabled = false)
    public void fixupBsp3119FiupTubeLabels() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        Map<String, String> oldBarcodeToNewBarcode = new HashMap<String, String>() {{
            put("1140121300", "1140121258");
            put("1140121306", "1140121296");
            put("1140121320", "1140121300");
            put("1140121321", "1140121306");
            put("1140121280", "1140121320");
            put("1140121322", "1140121321");
            put("1140121258", "1140121280");
            put("1140121296", "1140121322");
        }};

        String tempSuffix = "_TEMP";
        List<String> tempBarcodes = new ArrayList<>();
        List<LabVessel> labVessels = labVesselDao.findByListIdentifiers(new ArrayList<>(oldBarcodeToNewBarcode.keySet()));
        for (LabVessel labVessel: labVessels) {
            String oldBarcode = labVessel.getLabel();
            String newBarcode = oldBarcode + tempSuffix;
            tempBarcodes.add(newBarcode);
            labVessel.setLabel(newBarcode);
            System.out.println(
                    "Changing tube " + labVessel.getLabVesselId() + " label from " + oldBarcode + " to " + newBarcode);
        }

        FixupCommentary fixupCommentary =
                new FixupCommentary("BSP-3119 Change tube labels to temp to avoid unique constaint.");
        labBatchDao.persist(fixupCommentary);
        labBatchDao.flush();

        labVessels = labVesselDao.findByListIdentifiers(tempBarcodes);
        for (LabVessel labVessel: labVessels) {
            String oldBarcode = labVessel.getLabel().replaceAll(tempSuffix, "");
            String newBarcode = oldBarcodeToNewBarcode.get(oldBarcode);
            labVessel.setLabel(newBarcode);
            System.out.println(
                    "Changing tube " + labVessel.getLabVesselId() + " label from " + oldBarcode + " to " + newBarcode);
        }

        fixupCommentary = new FixupCommentary("BSP-3119 Change tube labels to correct name.");
        labBatchDao.persist(fixupCommentary);
        labBatchDao.flush();

        utx.commit();
    }

    @Test(enabled = false)
    public void fixupSupport2709() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<BarcodedTube> tubes = barcodedTubeDao.findListByBarcodes(Arrays.asList(
                "1125668423",
                "1125668470",
                "1125668491",
                "1125668483",
                "1125664356",
                "1125665028",
                "1125664990",
                "1125665004",
                "1125665161",
                "1125665157",
                "1125665166",
                "1125665212"));

        for (LabVessel labVessel : tubes) {
            labVessel.setReceptacleWeight(new BigDecimal(".62"));
            System.out.println("Setting tube initial tare weight: " + labVessel.getLabel() + " to .62");
        }

        FixupCommentary fixupCommentary = new FixupCommentary("SUPPORT-2709 - Assign missing tare values to default");
        barcodedTubeDao.persist(fixupCommentary);
        barcodedTubeDao.flush();

        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim5001() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<StaticPlate> plates = staticPlateDao.findListByList(StaticPlate.class, StaticPlate_.label, Arrays.asList(
                "000001827023",
                "000001811023", "000001828323", "000001808923", "000001806223", "000001800423", "000001824523",
                "000001801023", "000001812523", "000001829823", "000001806023", "000001804123", "000001819223",
                "000001802123", "000001814423-GPLIM-3164", "000001816023"));

        for (StaticPlate plate : plates) {
            plate.setPlateType(StaticPlate.PlateType.IndexedAdapterPlate96);
        }

        FixupCommentary fixupCommentary = new FixupCommentary("GPLIM-5001 - Assign missing plate types to static plates");
        barcodedTubeDao.persist(fixupCommentary);
        barcodedTubeDao.flush();

        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim5136() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        CriteriaBuilder builder = labVesselDao.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<LabVessel> query = builder.createQuery(LabVessel.class);
        Root<LabVessel> zeroConcRoot = query.from(LabVessel.class);
        Join<LabVessel, MercurySample> labVessels = zeroConcRoot.join(LabVessel_.mercurySamples);
        BigDecimal zeroDecimal = BigDecimal.valueOf(0);
        query.select(zeroConcRoot)
            .where(builder.and(builder.equal(zeroConcRoot.get(LabVessel_.concentration), zeroDecimal),
                               builder.equal(labVessels.get(MercurySample_.metadataSource), MercurySample.MetadataSource.BSP)
            ));
        List<LabVessel> labVesselList  = labVesselDao.getEntityManager().createQuery(query).getResultList();
        for (LabVessel labVessel: labVesselList) {
            System.out.println("Setting tube concentration: " + labVessel.getLabel() + " to null");
            labVessel.setConcentration(null);
        }
        FixupCommentary fixupCommentary =
                new FixupCommentary("GPLIM-5136 Set all 0 concentrations to null where BSP is metadatasource");
        labVesselDao.persist(fixupCommentary);
        labVesselDao.flush();

        utx.commit();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/AlterSampleName.txt,
     * so it can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * SUPPORT-3871 change name of incorrectly accessioned sample and vessel
     * SM-G811M A1119993
     * SM-9T6OH A9920002
     */
    @Test(enabled = false)
    public void fixupSupport3871ChangeSampleName() throws Exception {
        userBean.loginOSUser();

        List<String> sampleUpdateLines = IOUtils.readLines(VarioskanParserTest.getTestResource("AlterSampleName.txt"));

        for(int i = 1; i < sampleUpdateLines.size(); i++) {
            String[] fields = LabVesselFixupTest.WHITESPACE_PATTERN.split(sampleUpdateLines.get(i));
            if(fields.length != 2) {
                throw new RuntimeException("Expected two white-space separated fields in " + sampleUpdateLines.get(i));
            }
               LabVessel vessel = labVesselDao.findByIdentifier(fields[1]);

            Assert.assertNotNull(vessel, fields[1] + " not found");
            final String replacementVesselLabel = fields[1] + "_bad_vessel";
            System.out.println("Changing " + vessel.getLabel() + " to " + replacementVesselLabel);
            vessel.setLabel(replacementVesselLabel);
        }

        labVesselDao.persist(new FixupCommentary(sampleUpdateLines.get(0)));
        labVesselDao.flush();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/UpdateVesselBarcode.txt,
     * so it can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * SUPPORT-4118 change vessel barcode
     * 5600 FB04985689
     */
    @Test(enabled = false)
    public void fixupSupport4118UpdateVesselBarcode() throws Exception {
        userBean.loginOSUser();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("UpdateVesselBarcode.txt"));

        Map<String, String> mapOldToNew = new LinkedHashMap<>();
        for(int i = 1; i < lines.size(); i++) {
            String[] fields = WHITESPACE_PATTERN.split(lines.get(i));
            if (fields.length != 2) {
                throw new RuntimeException("Expected two white-space separated fields in " + lines.get(i));
            }
            mapOldToNew.put(fields[0], fields[1]);
        }
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(new ArrayList<>(mapOldToNew.keySet()));
        for (Map.Entry<String, LabVessel> barcodeVesselEntry : mapBarcodeToVessel.entrySet()) {
            LabVessel labVessel = barcodeVesselEntry.getValue();
            String barcode = barcodeVesselEntry.getKey();
            Assert.assertNotNull(labVessel, barcode + " not found");
            String newBarcode = mapOldToNew.get(barcode);
            System.out.println("Changing " + labVessel.getLabel() + " to " + newBarcode);
            labVessel.setLabel(newBarcode);
        }

        labVesselDao.persist(new FixupCommentary(lines.get(0)));
        labVesselDao.flush();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/AddBaitToVessel.txt,
     * so it can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * GPLIM-5657 add missing reagent design
     * 5600 FB04985689
     */
    @Test(enabled = false)
    public void fupxGplim5657AddBait() throws Exception {
        userBean.loginOSUser();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("AddBaitToVessel.txt"));

        Map<String, ReagentDesign> mapTubeToReagentDesign = new LinkedHashMap<>();
        for(int i = 1; i < lines.size(); i++) {
            String[] fields = WHITESPACE_PATTERN.split(lines.get(i));
            if (fields.length != 2) {
                throw new RuntimeException("Expected two white-space separated fields in " + lines.get(i));
            }
            ReagentDesign reagent = reagentDesignDao.findByBusinessKey(fields[1]);
            Assert.assertNotNull(reagent, fields[1] + " not found");
            Assert.assertEquals(reagent.getReagentType(), ReagentDesign.ReagentType.BAIT, "Expect only bait designs");
            mapTubeToReagentDesign.put(fields[0], reagent);
        }
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(new ArrayList<>(mapTubeToReagentDesign.keySet()));
        for (Map.Entry<String, LabVessel> barcodeVesselEntry : mapBarcodeToVessel.entrySet()) {
            LabVessel labVessel = barcodeVesselEntry.getValue();
            String barcode = barcodeVesselEntry.getKey();
            Assert.assertNotNull(labVessel, barcode + " not found");

            // Ensure Bait isn't already associated to tube
            for (SampleInstanceV2 sampleInstanceV2: labVessel.getSampleInstancesV2()) {
                for (ReagentDesign reagentDesign : sampleInstanceV2.getReagentsDesigns()) {
                    if (reagentDesign.getReagentType() == ReagentDesign.ReagentType.BAIT) {
                        throw new RuntimeException("Tube already has bait associated: " + barcode);
                    }
                }
                for (Reagent reagent : sampleInstanceV2.getReagents()) {
                    if (OrmUtil.proxySafeIsInstance(reagent, DesignedReagent.class)) {
                        DesignedReagent designedReagent = OrmUtil.proxySafeCast(reagent, DesignedReagent.class);
                        ReagentDesign.ReagentType reagentType = designedReagent.getReagentDesign().getReagentType();
                        if (reagentType == ReagentDesign.ReagentType.BAIT) {
                            throw new RuntimeException("Tube already has bait associated: " + barcode);
                        }
                    }
                }
            }
            ReagentDesign baitDesign = mapTubeToReagentDesign.get(barcode);
            System.out.println("Adding " + baitDesign.getDesignName() + " to " + labVessel.getLabel());
            DesignedReagent reagent = new DesignedReagent(baitDesign);
            labVessel.addReagent(reagent);
        }

        labVesselDao.persist(new FixupCommentary(lines.get(0)));
        labVesselDao.flush();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/UpdateFlowcellType.txt,
     * so it can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * GPLIM-6422 changed fc type from Nova S1 to MiSeq
     * CDR7F NovaSeqS1Flowcell MiSeqFlowcell
     */
    @Test(enabled = false)
    public void fixupGplim6422UpdateFCType() throws Exception {
        userBean.loginOSUser();

        List<String> flowcellUpdateLines = IOUtils.readLines(VarioskanParserTest.getTestResource("UpdateFlowcellType.txt"));

        for(int i = 1; i < flowcellUpdateLines.size(); i++) {
            String[] fields = LabVesselFixupTest.WHITESPACE_PATTERN.split(flowcellUpdateLines.get(i));
            if(fields.length != 3) {
                throw new RuntimeException("Expected three white-space separated fields in " + flowcellUpdateLines.get(i));
            }
            IlluminaFlowcell illuminaFlowcell = illuminaFlowcellDao.findByBarcode(fields[0]);
            Assert.assertNotNull(illuminaFlowcell);

            IlluminaFlowcell.FlowcellType oldFcType = IlluminaFlowcell.FlowcellType.valueOf(fields[1]);
            Assert.assertEquals(illuminaFlowcell.getFlowcellType(), oldFcType);

            IlluminaFlowcell.FlowcellType newFcType = IlluminaFlowcell.FlowcellType.valueOf(fields[2]);

            System.out.println("Changing " + illuminaFlowcell.getLabel() + " FC Type from " + oldFcType.getDisplayName()
                                                                         + " to " + newFcType.getDisplayName());
            illuminaFlowcell.setFlowcellType(newFcType);
        }

        labVesselDao.persist(new FixupCommentary(flowcellUpdateLines.get(0)));
        labVesselDao.flush();
    }

    @Test(enabled = false)
    public void undoIndexedPlateFactoryTest() throws Exception {
        userBean.loginOSUser();
        long rev = 3532152L;
        String plateBarcode = "20190710090153_DualIndexPlate";

        String query = "select r.reagent_id, mis.molecular_indexing_scheme_id from reagent r " +
                "join molecular_indexing_scheme_aud mis " +
                "   on r.molecular_indexing_scheme = mis.molecular_indexing_scheme_id " +
                "where not exists (select 1 from reagent_aud ra " +
                "  where ra.reagent_id = r.reagent_id and ra.rev != mis.rev) " +
                "and not exists (select 1 from sample_instance_entity sie " +
                "  where sie.molecular_indexing_scheme = mis.molecular_indexing_scheme_id) " +
                "and mis.rev = " + rev;

        List<Long> reagentIds = new ArrayList<>();
        List<Long> misIds = new ArrayList<>();
        ((List<Object[]>)labVesselDao.getEntityManager().createNativeQuery(query).getResultList()).
                forEach(item -> {
                    reagentIds.add(((BigDecimal)item[0]).longValueExact());
                    misIds.add(((BigDecimal)item[1]).longValueExact());
                });

        utx.begin();
        StaticPlate plate = staticPlateDao.findByBarcode(plateBarcode);
        Assert.assertNotNull(plate);
        Collection<PlateWell> wells = plate.getContainerRole().getContainedVessels();
        for (PlateWell well : wells) {
            well.getReagentContents().clear();
        }

        System.out.println("Removing reagents " + StringUtils.join(reagentIds, " "));
        labVesselDao.findListByList(Reagent.class, Reagent_.reagentId, reagentIds).
                forEach(reagent -> {
                    reagent.getMetadata().clear();
                    labVesselDao.remove(reagent);
                });

        System.out.println("Removing molecular index schemes " + StringUtils.join(misIds, " "));
        labVesselDao.findListByList(MolecularIndexingScheme.class,
                MolecularIndexingScheme_.molecularIndexingSchemeId, misIds).
                forEach(molecularIndexingScheme ->  {
                    labVesselDao.remove(molecularIndexingScheme);
                });

        plate.getContainerRole().getMapPositionToVessel().clear();

        System.out.println("Removing " + wells.size() + " wells in plate " + plate.getLabel());
        for (PlateWell well : wells) {
            labVesselDao.remove(well);
        }

        System.out.println("Removing static plate " + plate.getLabel());
        labVesselDao.remove(plate);

        labVesselDao.persist(new FixupCommentary("Undo accidental run of IndexedPlateFactoryTest"));
        labVesselDao.flush();
        utx.commit();
    }

}
