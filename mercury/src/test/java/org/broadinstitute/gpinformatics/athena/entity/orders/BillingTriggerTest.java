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

package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingTriggerTest {

    public void testBillingTriggerDefault() throws Exception {
        ProductOrder productOrder = new ProductOrder();
        productOrder.setResearchProject(new ResearchProject());
        assertThat(productOrder.getBillingTriggerOrDefault(),
            equalTo(productOrder.getResearchProject().getDefaultBillingTriggers()));
    }

    public void testBillingTriggerOverridden() throws Exception {
        ProductOrder productOrder = new ProductOrder();
        productOrder.setResearchProject(new ResearchProject());
        productOrder.setBillingTriggers(ResearchProject.BillingTrigger.ADDONS_ON_RECEIPT);
        assertThat(productOrder.getBillingTriggerOrDefault(),
            hasItems(ResearchProject.BillingTrigger.ADDONS_ON_RECEIPT));
    }

    public void testBillingTriggerDefaultToRP() throws Exception {
        ProductOrder productOrder = new ProductOrder();
        ResearchProject researchProject = new ResearchProject();
        researchProject.setDefaultBillingTriggers(ResearchProject.BillingTrigger.ADDONS_ON_RECEIPT);
        productOrder.setResearchProject(researchProject);
        assertThat(productOrder.getBillingTriggerOrDefault(),
            equalTo(productOrder.getResearchProject().getDefaultBillingTriggers()));
    }

    public void testBillingTriggerDifferentThenRP() throws Exception {
        ProductOrder productOrder = new ProductOrder();
        productOrder.setBillingTriggers(ResearchProject.BillingTrigger.NONE);
        ResearchProject researchProject = new ResearchProject();
        researchProject.setDefaultBillingTriggers(ResearchProject.BillingTrigger.ADDONS_ON_RECEIPT);
        productOrder.setResearchProject(researchProject);
        assertThat(productOrder.getBillingTriggerOrDefault(), contains(ResearchProject.BillingTrigger.NONE));
        assertThat(productOrder.getBillingTriggerOrDefault(),
            not(containsInAnyOrder(productOrder.getResearchProject().getDefaultBillingTriggers())));
    }

    public void testBillingTriggerDefaultNoRP() throws Exception {
        ProductOrder productOrder = new ProductOrder();
        assertThat(productOrder.getBillingTriggerOrDefault(), contains(ResearchProject.BillingTrigger.NONE));
    }
}
