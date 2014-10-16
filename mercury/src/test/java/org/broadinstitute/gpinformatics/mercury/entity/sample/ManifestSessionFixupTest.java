package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixup test to assist in all deviations Manifest session related
 */
@Test(groups = TestGroups.FIXUP)
public class ManifestSessionFixupTest extends Arquillian {

    @Inject
    private ManifestSessionDao manifestSessionDao;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void fixupGPLIM3031CleanLingeringSessions() {

        userBean.loginOSUser();

        List<ManifestSession> allOpenSessions = manifestSessionDao.findOpenSessions();

        for (ManifestSession openSession : allOpenSessions) {
            for (ManifestRecord nonQuarantinedManifestRecord : openSession.getNonQuarantinedRecords()) {
                nonQuarantinedManifestRecord.setStatus(ManifestRecord.Status.ACCESSIONED);
            }
            openSession.setStatus(ManifestSession.SessionStatus.COMPLETED);
        }
        manifestSessionDao.flush();

        List<ManifestSession> allClosedSessions = manifestSessionDao.findSessionsEligibleForTubeTransfer();

        for (ManifestSession closedSession : allClosedSessions) {
            for (ManifestRecord manifestRecord : closedSession.getNonQuarantinedRecords()) {
                manifestRecord.setStatus(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE);
            }

        }
        manifestSessionDao.flush();
    }
}
