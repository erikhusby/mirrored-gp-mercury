package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixup production Lab Vessel entities
 */
public class LabVesselFixupTest extends Arquillian {

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    TubeFormationDao tubeFormationDao;

    @Inject
    RackOfTubesDao rackOfTubesDao;

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

        List<String> tubeList = new ArrayList<String>();
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

        List<String> rackList = new ArrayList<String>();
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
}
