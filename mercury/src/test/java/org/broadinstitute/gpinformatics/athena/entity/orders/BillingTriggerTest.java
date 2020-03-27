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

import org.broadinstitute.gpinformatics.athena.entity.project.BillingTrigger;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.Collections;

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
            equalTo(productOrder.getResearchProject().getBillingTriggers()));
    }

    public void testBillingTriggerOverridden() throws Exception {
        ProductOrder productOrder = new ProductOrder();
        productOrder.setResearchProject(new ResearchProject());
        productOrder.setBillingTriggers(Collections.singleton(BillingTrigger.ADDONS_ON_RECEIPT));
        assertThat(productOrder.getBillingTriggerOrDefault(),
            hasItems(BillingTrigger.ADDONS_ON_RECEIPT));
    }

    public void testBillingTriggerDefaultToRP() throws Exception {
        ProductOrder productOrder = new ProductOrder();
        ResearchProject researchProject = new ResearchProject();
        researchProject.setBillingTriggers(Collections.singleton(BillingTrigger.ADDONS_ON_RECEIPT));
        productOrder.setResearchProject(researchProject);
        assertThat(productOrder.getBillingTriggerOrDefault(),
            equalTo(productOrder.getResearchProject().getBillingTriggers()));
    }

    public void testBillingTriggerDifferentThenRP() throws Exception {
        ProductOrder productOrder = new ProductOrder();
        productOrder.setBillingTriggers(Collections.singleton(BillingTrigger.NONE));
        ResearchProject researchProject = new ResearchProject();
        researchProject.setBillingTriggers(Collections.singleton(BillingTrigger.ADDONS_ON_RECEIPT));
        productOrder.setResearchProject(researchProject);
        assertThat(productOrder.getBillingTriggerOrDefault(), contains(BillingTrigger.NONE));
        assertThat(productOrder.getBillingTriggerOrDefault(),
            not(containsInAnyOrder(productOrder.getResearchProject().getBillingTriggers())));
    }

    public void testBillingTriggerDefaultNoRP() throws Exception {
        ProductOrder productOrder = new ProductOrder();
        assertThat(productOrder.getBillingTriggerOrDefault(), contains(BillingTrigger.NONE));
    }
}
