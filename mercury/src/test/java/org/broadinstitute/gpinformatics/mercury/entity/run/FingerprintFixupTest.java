package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.FingerprintDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.FIXUP)
public class FingerprintFixupTest extends Arquillian {

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction utx;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private FingerprintDao fingerprintDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * Run to delete fingerprint when consent is withdrawn.  This test reads its input from a file,
     * mercury/src/test/resources/testdata/DeleteFingerprints.txt, so it can be re-used.  The format is as follows:
     * SUPPORT-4812 consent withdrawal
     * broadinstitute.org:bsp.prod.sample:GJMGD
     * broadinstitute.org:bsp.prod.sample:GHRIH
     */
    @Test(enabled = false)
    public void support4812DeleteFingerprint()
            throws IOException, HeuristicRollbackException, RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
        userBean.loginOSUser();
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("DeleteFingerprints.txt"));
        utx.begin();
        for (int i = 1; i < lines.size(); i++) {
            String lsid = lines.get(i);
            MercurySample mercurySample = mercurySampleDao.findBySampleKey("SM-" + lsid.substring(lsid.lastIndexOf(':') + 1));
            if (mercurySample == null) {
                throw new RuntimeException("Failed to find " + lsid);
            }
            for (Fingerprint fingerprint : mercurySample.getFingerprints()) {
                System.out.println("Deleting " + fingerprint.getMercurySample().getSampleKey() + " " + fingerprint.getDateGenerated());
            }
            mercurySample.getFingerprints().clear();
        }
        fingerprintDao.persist(new FixupCommentary(lines.get(0)));
        fingerprintDao.flush();
        utx.commit();
    }

    /**
     * Run to set fingerprint to ignore (or back to pass if previous ignore was a mistake), so the pipeline doesn't
     * include it in LOD score calculation.  This test reads its input from a file,
     * mercury/src/test/resources/testdata/FingerprintDispositions.txt, so it can be re-used.
     * The format is as follows:
     * SUPPORT-5802 ignore Infinium fingerprint
     * broadinstitute.org:bsp.prod.sample:JHN3Q,2019-09-25 21.23.00,IGNORE
     * broadinstitute.org:bsp.prod.sample:JHN7I,2019-09-25 21.19.00,PASS
     */
    @Test(enabled = false)
    public void gplim6662FingerprintDisposition() throws IOException, ParseException {
        userBean.loginOSUser();
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("FingerprintDispositions.txt"));
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] fields = line.split(",");
            if (fields.length != 3) {
                throw new RuntimeException("Expected two comma separated fields in " + line);
            }
            String lsid = fields[0];
            MercurySample mercurySample = mercurySampleDao.findBySampleKey("SM-" + lsid.substring(lsid.lastIndexOf(':') + 1));
            if (mercurySample == null) {
                throw new RuntimeException("Failed to find " + lsid);
            }
            Date date = ArchetypeAttribute.dateFormat.parse(fields[1]);
            List<Fingerprint> fingerprints = mercurySample.getFingerprints().stream().filter(
                    fingerprint -> fingerprint.getDateGenerated().getTime() == date.getTime()).collect(Collectors.toList());
            Assert.assertEquals(fingerprints.size(), 1);
            Fingerprint fingerprint = fingerprints.get(0);
            Fingerprint.Disposition disposition = Fingerprint.Disposition.valueOf(fields[2]);
            System.out.println("Setting " + fingerprint.getMercurySample().getSampleKey() + " to " + disposition);
            fingerprint.setDisposition(disposition);
        }
        fingerprintDao.persist(new FixupCommentary(lines.get(0)));
    }
}