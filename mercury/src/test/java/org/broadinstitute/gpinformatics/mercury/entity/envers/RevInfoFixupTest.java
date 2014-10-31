package org.broadinstitute.gpinformatics.mercury.entity.envers;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixup production RevInfo entities
 */
@Test(groups = TestGroups.FIXUP)
public class RevInfoFixupTest extends Arquillian {

    @Inject
    private AuditReaderDao auditReaderDao;

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction userTransaction;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = true)
    public void gplim3140FixupUsername() throws Exception {
        // The audit username for data fixup GPLIM-3140 should be "epolk".
        // This fix only affects rev_info for an existing fixup, so there is no userbean or fixupCommentary.
        long revInfoId = 355651L;
        userTransaction.begin();
        RevInfo revInfo = auditReaderDao.getEntityManager().find(RevInfo.class, revInfoId);
        if (revInfo == null) {
            throw new RuntimeException("cannot find " + revInfoId);
        }
        revInfo.setUsername("epolk");
        System.out.println("Updated " + revInfo.getRevInfoId() + " to " + revInfo.getUsername());
        auditReaderDao.getEntityManager().persist(revInfo);
        userTransaction.commit();
    }

}
