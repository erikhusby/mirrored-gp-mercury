package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
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
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new EnumMap<>(VesselPosition.class);

        TwoDBarcodedTube twoDBarcodedTube = new TwoDBarcodedTube("0114382737");
        twoDBarcodedTube.addSample(new MercurySample("SM-2AY8L"));
        mapPositionToTube.put(VesselPosition.A01, twoDBarcodedTube);

        TwoDBarcodedTube twoDBarcodedTube1 = new TwoDBarcodedTube("SM-22GXJ");
        twoDBarcodedTube.addSample(new MercurySample("SM-22GXJ"));
        mapPositionToTube.put(VesselPosition.A02, twoDBarcodedTube1);

        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        tubeFormation.addRackOfTubes(new RackOfTubes("CO-5971666", RackOfTubes.RackType.Matrix96));

        labVesselDao.persist(tubeFormation);

        mapPositionToTube.clear();
        TwoDBarcodedTube twoDBarcodedTube2 = new TwoDBarcodedTube("SM-29FPE");
        twoDBarcodedTube2.addSample(new MercurySample("SM-29FPE"));
        mapPositionToTube.put(VesselPosition.A01, twoDBarcodedTube2);

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
}
