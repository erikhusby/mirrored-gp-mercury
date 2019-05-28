package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class ControlFixupTest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    @Test
    public void gplim6080ControlFingerprintFixup() {
        Map<String, String> mapCollabIdToFpSample = new HashMap<String, String>() {{
            put("NA12878", "");
            put("NA12891", "");
            put("NA12892", "");
            put("NA18506", "");
            put("NA18563", "");
            put("NA18603", "");
            put("NA18609", "");
            put("NA18612", "");
            put("NA18942", "");
            put("NA18947", "");
            put("NA18951", "");
            put("NA18956", "");
            put("NA18967", "");
            put("NA18969", "");
        }};
    }
}
