package org.broadinstitute.gpinformatics.mercury.control.run;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.FingerprintDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class ConcordanceCalculatorTest extends Arquillian {

    private static final Date DATE_GENERATED = new GregorianCalendar(2019, Calendar.FEBRUARY, 5, 12, 27, 06).getTime();
    private static final String SAMPLE_KEY = "SM-IFXLP";

    @Inject
    private FingerprintDao fingerprintDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private ControlDao controlDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testNa12878() {
        MercurySample mercurySample1 = mercurySampleDao.findBySampleKey(SAMPLE_KEY);
        Fingerprint fingerprint1 = fingerprintDao.findBySampleAndDateGenerated(mercurySample1, DATE_GENERATED);

        MercurySample mercurySample2 = mercurySampleDao.findBySampleKey("SM-IGBAA");
        Fingerprint fingerprint2 = fingerprintDao.findBySampleAndDateGenerated(mercurySample2,
                new GregorianCalendar(2019, Calendar.FEBRUARY, 7, 12, 25, 01).getTime());

        ConcordanceCalculator concordanceCalculator = new ConcordanceCalculator();
        double lodScore = concordanceCalculator.calculateLodScore(fingerprint1, mercurySample1.getSampleKey(),
                fingerprint2, mercurySample2.getSampleKey());
        Assert.assertTrue(lodScore > 20.0);
        concordanceCalculator.done();
    }

    @Test
    public void testConcordance() {
        ConcordanceCalculator concordanceCalculator = new ConcordanceCalculator();
        MercurySample mercurySample1 = mercurySampleDao.findBySampleKey(SAMPLE_KEY);
        Fingerprint fingerprint1 = fingerprintDao.findBySampleAndDateGenerated(mercurySample1, DATE_GENERATED);
        double lodScore = concordanceCalculator.calculateHapMapConcordance(fingerprint1, mercurySample1.getSampleKey(),
                controlDao.findByCollaboratorParticipantId("NA12878"));
        Assert.assertTrue(lodScore > 20.0);
        concordanceCalculator.done();
    }
}