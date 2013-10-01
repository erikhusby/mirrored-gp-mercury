/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.kits;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class SampleKitEjbTest extends Arquillian {
    private SampleKitEjb sampleKitEjb;

    private JiraService jiraService;

    @org.jboss.arquillian.container.test.api.Deployment
    public static WebArchive getDeployment() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }


    @BeforeMethod()
    public void setUp() throws Exception {
        jiraService = JiraServiceProducer.testInstance();
        sampleKitEjb = new SampleKitEjb(jiraService, Deployment.DEV);
    }

    public void testAllowedValuesResultList() {
        Collection<String> allowedValues = sampleKitEjb.getDeliveryMethods();
        Assert.assertTrue(allowedValues.containsAll(Arrays.asList("FedEx", "Broad Truck", "Local Pickup")));
    }

    public void testGetAllowedValues() throws Exception {
        Collection<String> allowedValues;
        for (SampleKitEjb.JiraField jiraField : SampleKitEjb.JiraField.values()) {
            allowedValues = sampleKitEjb.getAllowedValues(jiraField);
            Assert.assertNotNull(allowedValues);
        }
    }

    public void testCreateKit() throws Exception {
        SampleKitRequestDto sampleKitDto = new SampleKitRequestDto("dryan", Arrays.asList("breilly", "andrew"),
                "Tube - 0.75 mL Matrix", 2, 96, "320", "Broad Truck", "WR-13299", "PDO-2134");
        List<String> kitRequests = sampleKitEjb.createKitRequest(sampleKitDto);
        Assert.assertNotNull(kitRequests);
        Assert.assertEquals(kitRequests.size(), 2);
        for (String kitRequest : kitRequests) {
            Assert.assertTrue(kitRequest.startsWith(CreateFields.ProjectType.SAMPLE_KIT_INITIATION.getKeyPrefix()));
        }

    }
}
