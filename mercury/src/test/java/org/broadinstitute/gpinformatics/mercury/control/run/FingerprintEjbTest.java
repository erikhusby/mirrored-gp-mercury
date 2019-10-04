package org.broadinstitute.gpinformatics.mercury.control.run;

import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.FingerprintMatrixActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.FingerprintReportActionBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class FingerprintEjbTest extends Arquillian {


    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private FingerprintEjb fingerprintEjb;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testBasics(){
        Map<String, MercurySample> mapIdToMercurySample = new HashMap<>();
        List<Fingerprint> fingerprints;
        List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKeys(Arrays.asList("SM-J6SSU", "SM-HB8GT"));
        Set<String> platforms =new HashSet<>();
        platforms.add("FLUIDIGM");
        fingerprints = fingerprintEjb.findFingerints(mapIdToMercurySample);
        Workbook workbook = fingerprintEjb.makeMatrix(fingerprints,
                platforms);
    }

}