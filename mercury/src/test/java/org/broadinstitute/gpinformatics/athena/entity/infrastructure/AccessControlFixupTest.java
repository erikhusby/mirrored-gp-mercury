package org.broadinstitute.gpinformatics.athena.entity.infrastructure;

import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.infrastructure.SAPAccessControlDao;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.FIXUP)
public class AccessControlFixupTest extends Arquillian {

    @Inject
    private UserBean userBean;

    @Inject
    SAPAccessControlEjb controller;

    @Inject
    SAPAccessControlDao accessControlDao;

    // When you run this on prod, change to PROD and prod.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void GPLIM4652ChangeToNewControlSetup() throws Exception {

        userBean.loginOSUser();

        SAPAccessControl accessControl = controller.getCurrentControlDefinitions();

        Set<String> currentRestrictions = accessControl.getDisabledFeatures();
        Set<AccessItem> newAccessItems = new HashSet<>();

        for (String currentRestriction : currentRestrictions) {
            newAccessItems.add(new AccessItem(currentRestriction));
        }

        accessControl.setDisabledItems(newAccessItems);

        controller.setDefinitionItems(accessControl.getAccessStatus(), currentRestrictions);

        accessControlDao.persist(new FixupCommentary("GPLIM-4652 moving old access restrictions to a new structure"));

    }
}
