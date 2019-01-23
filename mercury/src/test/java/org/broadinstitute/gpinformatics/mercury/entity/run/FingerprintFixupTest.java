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
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.util.List;

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
            List<Fingerprint> fingerprints = fingerprintDao.findBySample(mercurySample);
            for (Fingerprint fingerprint : fingerprints) {
                System.out.println("Deleting " + fingerprint.getMercurySample().getSampleKey() + " " + fingerprint.getDateGenerated());
                fingerprintDao.remove(fingerprint);
            }
        }
        fingerprintDao.persist(new FixupCommentary(lines.get(0)));
        fingerprintDao.flush();
        utx.commit();
    }

}