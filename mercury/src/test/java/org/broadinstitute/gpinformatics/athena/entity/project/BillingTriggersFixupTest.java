/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2020 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.FIXUP)
public class BillingTriggersFixupTest extends Arquillian {
    @Inject
    private UserBean userBean;
    @Inject
    private UserTransaction utx;
    @Inject
    private ResearchProjectDao researchProjectDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void testGPLIM_6698_AoUBillingRequirements() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<ResearchProject> allOfUsResearchProjects =
            researchProjectDao.findByJiraTicketKeys(Arrays.asList("RP-2079", "RP-2083"));

        allOfUsResearchProjects.forEach(researchProject -> {
            @SuppressWarnings("serial") Set<BillingTrigger> billingTriggers = new HashSet<BillingTrigger>() {{
                add(BillingTrigger.ADDONS_ON_RECEIPT);
                add(BillingTrigger.DATA_REVIEW);
            }};
            researchProject.setBillingTriggers(billingTriggers);
        });

        researchProjectDao.persist(new FixupCommentary("GPLIM_6698 set default billing triggers for All Of Us."));

        utx.commit();
    }
}
