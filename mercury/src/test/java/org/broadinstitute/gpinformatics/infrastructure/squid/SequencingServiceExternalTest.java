package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.athena.entity.experiments.seq.BaitSetName;
import org.broadinstitute.gpinformatics.athena.entity.experiments.seq.OrganismName;
import org.broadinstitute.gpinformatics.athena.entity.experiments.seq.ReferenceSequenceName;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertNotNull;


/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/3/12
 * Time: 2:55 PM
 */
@Test(groups = {TestGroups.EXTERNAL_INTEGRATION})
public class SequencingServiceExternalTest  {

    private PMBSequencingService sequencingService;


    @BeforeSuite(alwaysRun = true)
    public void setupSuite() {
        int i = 0;
        sequencingService = PMBSequencingServiceProducer.qaInstance();
    }


    public void testGetPlatformPeople() throws Exception {

        List<Person> aList = sequencingService.getPlatformPeople();
        assertNotNull(aList);
        Person person = aList.get(0);
        assertNotNull(person);

    }

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
        assertNotNull(baitSetName);
    }

    public void testGetReferenceSequences() throws Exception {
        List<ReferenceSequenceName> aList = sequencingService.getReferenceSequences();
        assertNotNull(aList);
        ReferenceSequenceName referenceSequenceName = aList.get(0);
        assertNotNull(referenceSequenceName);
        assertNotNull(referenceSequenceName.getId());
    }

    public void testGetRequestSummariesByCreator() throws Exception {

        List<ExperimentRequestSummary> aList = sequencingService.getRequestSummariesByCreator(new Person("athena"));
        assertNotNull(aList);
        // If there was any data on SQUID for the athena user then check it.
        if (aList.size() > 0) {
            ExperimentRequestSummary experimentRequestSummary = aList.get(0);
            assertNotNull(experimentRequestSummary.getTitle());
            assertNotNull(experimentRequestSummary.getExperimentId().value);
            assertNotNull(experimentRequestSummary.getStatus());
        }

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
