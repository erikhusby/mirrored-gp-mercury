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

package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.products.BillingRequirement;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.BillingTrigger;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.KeyValueMapping;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition_;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.FIXUP)
public class AbcInitialValuesFixupTest extends Arquillian {
    @Inject
    private UserBean userBean;
    @Inject
    private UserTransaction utx;
    @Inject
    private ResearchProjectDao researchProjectDao;
    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;
    @Inject
    private ProductDao productDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void testGPLIM_6698_AoUProductBillingTriggers() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        // TODO: Need to verify, also may not be necessary since it is defined on the "AoU PDO Parameters" page
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

    @Test(enabled = false)
    public void gplim6698_billingTriggerValueMappings() throws Exception {
        utx.begin();
        userBean.loginOSUser();
        List<AttributeArchetype> archetypes = new ArrayList<>();

        Arrays.asList(KeyValueMapping.AOU_PDO_WGS, KeyValueMapping.AOU_PDO_ARRAY).forEach(groupName -> {
            AttributeDefinition attributeDefinition = attributeArchetypeDao
                .findSingle(AttributeDefinition.class, AttributeDefinition_.group, groupName);
            AttributeArchetype archetype = attributeArchetypeDao
                .findKeyValueByKeyAndMappingName(MayoManifestEjb.BILLING_TRIGGERS, groupName);

            if (archetype == null) {
                System.out.println("Adding new " + groupName + " key " + MayoManifestEjb.BILLING_TRIGGERS);
                archetype = new KeyValueMapping(groupName, MayoManifestEjb.BILLING_TRIGGERS,
                    Collections.singleton(attributeDefinition));
                String attributeValue = StringUtils
                    .join(Arrays.asList(BillingTrigger.ADDONS_ON_RECEIPT.name(), BillingTrigger.DATA_REVIEW.name()), ",");
                archetype.addOrSetAttribute("theValue", attributeValue);
            }
            archetypes.add(archetype);
        });
        attributeArchetypeDao.persistAll(archetypes);
        attributeArchetypeDao.persist(new FixupCommentary("GPLIM-6698: All of Us Billing Triggers"));

        utx.commit();
    }
    @Test(enabled = false)
       public void testGPLIM_6698_AoUBillingRequirements() throws Exception {
           userBean.loginOSUser();
           utx.begin();

           List<Product> allOfUsProducts =
               productDao.findByPartNumbers(Arrays.asList("P-WG-0114", "P-CLA-0013", "P-ESH-0079"));

           allOfUsProducts.forEach(product -> {
               if (product.getRequirement() == null) {
                   product.addRequirement(new BillingRequirement("CAN_BILL", Operator.EQUALS, 1.0));
               }
           });

           productDao.persist(new FixupCommentary("GPLIM_6698 set default billing requirements for All Of Us."));

           utx.commit();
       }
}
