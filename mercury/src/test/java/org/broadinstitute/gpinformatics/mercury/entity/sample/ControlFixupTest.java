package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControlFixupTest extends Arquillian {

    @Inject
    private ControlDao controlDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    @Test(enabled = false)
    public void gplim6080ControlFingerprintFixup() {
        // Plate CO-28400592_04101019
        // http://gapdev.broadinstitute.org:8080/esp/ViewFluidigmPlate.action?packetId=1200050
        Map<String, String> mapCollabIdToFpSample = new HashMap<String, String>() {{
            put("NA12878", "SM-IQYG2");
            put("NA12891", "SM-IQYFS");
            put("NA12892", "SM-IQYFP");
            put("NA18506", "SM-IQYFG");
            put("NA18563", "SM-IQYFV");
            put("NA18603", "SM-IQYFA");
            put("NA18609", "SM-IQYGB");
            put("NA18612", "SM-IQYFY");
            put("NA18942", "SM-IQYFD");
            put("NA18947", "SM-IQYGK");
            put("NA18951", "SM-IQYGE");
            put("NA18956", "SM-IQYFJ");
            put("NA18967", "SM-IQYGI");
            put("NA18969", "SM-IQYFM");
        }};
        Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(
                mapCollabIdToFpSample.values());
        List<Control> controls = controlDao.findAllActiveControlsByType(Control.ControlType.POSITIVE);
        for (Control control : controls) {
            MercurySample mercurySample = mapIdToMercurySample.get(control.getCollaboratorParticipantId());
            Assert.assertNotNull(mercurySample);
            System.out.println("Setting " + control.getCollaboratorParticipantId() + " to " +
                    mercurySample.getSampleKey());
            control.setConcordanceMercurySample(mercurySample);
        }
        controlDao.persist(new FixupCommentary("GPLIM-6080 set fingerprint samples for controls"));
        controlDao.flush();
    }
}
