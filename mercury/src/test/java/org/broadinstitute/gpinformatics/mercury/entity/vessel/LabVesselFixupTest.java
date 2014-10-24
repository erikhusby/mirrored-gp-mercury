package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixup production Lab Vessel entities
 */
@Test(groups = TestGroups.FIXUP)
public class LabVesselFixupTest extends Arquillian {

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
            Assert.assertEquals(tube.getContainers().size(), 1, "Wrong number of containers");
            TubeFormation tubeFormation = (TubeFormation) (tube.getContainers().iterator().next().getEmbedder());
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
            Set<BucketEntry> bucketEntries = labVesselOld.getModifiableBucketEntries();
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
        for (VesselContainer<?> vesselContainer : barcodedTube.getContainers()) {
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
        for (VesselContainer<?> vesselContainer : barcodedTube.getContainers()) {
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
        for (VesselContainer<?> container : barcodedTube.getContainers()) {
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
}
