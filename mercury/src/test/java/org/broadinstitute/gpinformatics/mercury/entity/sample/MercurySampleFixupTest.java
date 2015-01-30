package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.List;

@Test(groups = TestGroups.FIXUP)
public class MercurySampleFixupTest extends Arquillian {

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {

        /*
         * If the need comes to utilize this fixup in production, change the buildMercuryWar parameters accordingly
         */
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    @BeforeMethod(groups = TestGroups.FIXUP)
    public void setUp() throws Exception {
        if (userBean == null) {
            return;
        }
        userBean.loginOSUser();
        utx.begin();
    }

    @AfterMethod(groups = TestGroups.FIXUP)
    public void tearDown() throws Exception {

        if (userBean == null) {
            return;
        }
        userBean.logout();
        utx.commit();
    }

    /**
     * This fixup will remove duplicate samples from the system and reset their existing labVessel relationships to
     * converge to the one remaining sample of which they are a duplicate.
     *
     *
     * IMPORTANT!!!!!   Before Running this, change the mercurySample ManytoMany relationship on LabVessel to be a
     * List!!!!
     *
     *
     *
     * @throws Exception
     */
    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void gplim3005ReassignDuplicateSamples() throws Exception {

        List<MercurySample> duplicateSamples = mercurySampleDao.findDuplicateSamples();

        Multimap<String, MercurySample> duplicateSampleMap = ArrayListMultimap.create();
        Multimap<String, LabVessel> sampleToVesselMap = ArrayListMultimap.create();

        for (MercurySample duplicateSample : duplicateSamples) {
            duplicateSampleMap.put(duplicateSample.getSampleKey(), duplicateSample);
            for (LabVessel labVessel : duplicateSample.labVessel) {
                sampleToVesselMap.put(duplicateSample.getSampleKey(), labVessel);
            }
        }

        for (String sampleName : duplicateSampleMap.keys()) {
            Long sampleIdToKeep = null;
            for (MercurySample duplicateSample : duplicateSampleMap.get(sampleName)) {
                if(sampleIdToKeep == null) {
                    sampleIdToKeep = duplicateSample.getMercurySampleId();
                    for (LabVessel labVessel : sampleToVesselMap.get(sampleName)) {
                        if(!labVessel.getMercurySamples().contains(duplicateSample)) {
                            labVessel.addSample(duplicateSample);
                        }
                    }
                } else {
                    duplicateSample.removeSampleFromVessels(sampleToVesselMap.get(sampleName));
                    mercurySampleDao.remove(duplicateSample);
                }
            }
        }
    }
}
