package org.broadinstitute.pmbridge.infrastructure.squid;

import junit.framework.Assert;
import org.broadinstitute.pmbridge.DeploymentBuilder;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.experiments.seq.BaitSetName;
import org.broadinstitute.pmbridge.entity.experiments.seq.OrganismName;
import org.broadinstitute.pmbridge.entity.experiments.seq.ReferenceSequenceName;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.pmbridge.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.assertNotNull;


/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/3/12
 * Time: 2:55 PM
 */
public class SequencingServiceExternalTest extends Arquillian {

    @Inject
    private SequencingService sequencingService;

    @Deployment
    public static WebArchive buildBridgeWar() {
        WebArchive war = DeploymentBuilder.buildBridgeWar();
        return war;
    }

    @Test(groups = {EXTERNAL_INTEGRATION})
    public void testGetPlatformPeople() throws Exception {

        Assert.fail("should not pass");
        List<Person> aList = sequencingService.getPlatformPeople();
        assertNotNull(aList);
        assertNotNull(aList.get(0));

    }

    @Test(groups = {EXTERNAL_INTEGRATION})
    public void testGetOrganisms() throws Exception {

        List<OrganismName> aList = sequencingService.getOrganisms();
        assertNotNull(aList);
        OrganismName organismName = aList.get(0);
        assertNotNull(organismName);
        assertNotNull(organismName.getCommonName());

    }

    public void testGetBaitSets() throws Exception {
        List<BaitSetName> aList = sequencingService.getBaitSets();
        assertNotNull(aList);
        BaitSetName baitSetName = aList.get(0);
        assertNotNull(baitSetName.name);
    }

    public void testGetReferenceSequences() throws Exception {
        List<ReferenceSequenceName> aList = sequencingService.getReferenceSequences();
        assertNotNull(aList);
        ReferenceSequenceName referenceSequenceName = aList.get(0);
        assertNotNull(referenceSequenceName.name);
        assertNotNull(referenceSequenceName.getId());
    }

    public void testGetRequestSummariesByCreator() throws Exception {
        List<ExperimentRequestSummary> aList = sequencingService.getRequestSummariesByCreator(new Person("pmbridge", RoleType.PROGRAM_PM));
        assertNotNull(aList);
        ExperimentRequestSummary experimentRequestSummary = aList.get(0);
        assertNotNull(experimentRequestSummary.getTitle());
        assertNotNull(experimentRequestSummary.getRemoteId().value);
        assertNotNull(experimentRequestSummary.getStatus().name);
    }

//    @Test
//    public void testGetPlatformRequest() throws Exception {
//
//    }
//
//    @Test
//    public void testValidatePlatformRequest() throws Exception {
//
//    }
//
//    @Test
//    public void testSubmitRequestToPlatform() throws Exception {
//
//    }
}
