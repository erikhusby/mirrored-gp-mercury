package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentId;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentType;
import org.broadinstitute.gpinformatics.athena.entity.experiments.seq.*;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.*;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.*;

import static org.testng.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/3/12
 * Time: 2:55 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SequencingServiceTest {

    public static final String SHOULD_HAVE_THROWN_EXCEPTION = "Should have thrown exception ";
    private SquidTopicPortype mockSquidTopicPortype = null;
    private PMBSequencingServiceImpl sequencingService = null;

    @BeforeMethod
    public void setUp() throws Exception {

        //Create and configure the mock
        mockSquidTopicPortype = EasyMock.createMock(SquidTopicPortype.class);
        EasyMock.expect(mockSquidTopicPortype.getGreeting()).andReturn("UnitTest Greeting").anyTimes();
        sequencingService = new PMBSequencingServiceImpl(mockSquidTopicPortype);

    }

    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetPlatformPeople() throws Exception {

        SquidPersonList squidPersonList = new SquidPersonList();
        List<SquidPerson> aList = squidPersonList.getSquidPerson();
        SquidPerson squidPerson = new SquidPerson();
        squidPerson.setLogin("tester");
        squidPerson.setFirstName("Jon");
        squidPerson.setLastName("Tester");
        squidPerson.setPersonID(new BigInteger("100"));
        aList.add(squidPerson);
        aList.add(null);
        aList.add(new SquidPerson());
        SquidPerson numberLessPerson = new SquidPerson();
        numberLessPerson.setLogin("tester");
        numberLessPerson.setFirstName("Jon");
        numberLessPerson.setLastName("Tester");
        aList.add(numberLessPerson);

        //Configure the mock to respond as needed
        EasyMock.expect(mockSquidTopicPortype.getBroadPIList()).andReturn(squidPersonList).once();
        EasyMock.replay(mockSquidTopicPortype);

        // Test the get Platform people method.
        List<Person> people = sequencingService.getPlatformPeople();
        Assert.assertNotNull(people);
        Assert.assertEquals(people.size(), 1);
        assertEquals(people.get(0).getFirstName(), "tester");
        assertEquals(people.get(0).getLastName(), "Tester");
        assertEquals(people.get(0).getLogin().compareTo("100"), 0);

    }

    @Test
    public void testGetOrganisms() throws Exception {

        List<OrganismName> organismNames = null;
        OrganismListResult organismListResult = new OrganismListResult();
        List<Organism> organismList = organismListResult.getOrganismList();
        Organism organism = new Organism();
        organism.setCommonName("velociraptor");
        organism.setSpecies("XLR8");
        organism.setGenus("Dino");
        organism.setId(48L);
        organismList.add(organism);
        organismList.add(null);
        organismList.add(new Organism());

        //Configure the mock to respond as needed
        EasyMock.expect(mockSquidTopicPortype.getOrganisms()).andReturn(organismListResult).once();
        EasyMock.replay(mockSquidTopicPortype);

        // Test the getOrganisms method.
        organismNames = sequencingService.getOrganisms();
        Assert.assertNotNull(organismNames);
        Assert.assertEquals(organismNames.size(), 1);
        assertEquals(organismNames.get(0).getCommonName(), "velociraptor");
        assertEquals(organismNames.get(0).getId(), 48L);

    }

    @Test
    public void testGetBaitSets() throws Exception {

        List<BaitSetName> baitSetNames = null;
        BaitSetListResult baitSetListResult = new BaitSetListResult();
        List<BaitSet> baitSetList = baitSetListResult.getBaitSetList();
        BaitSet baitSet = new BaitSet();
        baitSet.setDesignName("aBaitSet");
        baitSet.setId(12L);
        baitSetList.add(baitSet);
        BaitSet badBaitSet = new BaitSet();
        badBaitSet.setDesignName("x");
        baitSetList.add(badBaitSet);
        baitSetList.add(null);

        //Configure the mock to respond as needed
        EasyMock.expect(mockSquidTopicPortype.getBaitSets()).andReturn(baitSetListResult).once();
        EasyMock.replay(mockSquidTopicPortype);

        // Test the getBaitSets method.
        baitSetNames = sequencingService.getBaitSets();
        Assert.assertNotNull(baitSetNames);
        Assert.assertEquals(baitSetNames.size(), 1);
        assertEquals(baitSetNames.get(0).name, "aBaitSet");
        assertEquals(baitSetNames.get(0).getId(), 12L);

    }

    @Test
    public void testGetReferenceSequences() throws Exception {

        List<ReferenceSequenceName> referenceSequenceNames = null;
        ReferenceSequenceListResult referenceSequenceListResult = new ReferenceSequenceListResult();
        List<ReferenceSequence> referenceSequenceList = referenceSequenceListResult.getReferenceSequenceList();

        ReferenceSequence referenceSequence = new ReferenceSequence();
        referenceSequence.setAlias("aReferenceSequence");
        referenceSequence.setActive(true);
        referenceSequence.setId(99L);
        referenceSequenceList.add(referenceSequence);

        ReferenceSequence badReferenceSequence = new ReferenceSequence();
        badReferenceSequence.setAlias("badRef");
        badReferenceSequence.setId(66L);
        badReferenceSequence.setActive(false);
        referenceSequenceList.add(badReferenceSequence);

        referenceSequenceList.add(null);

        //Configure the mock to respond as needed
        EasyMock.expect(mockSquidTopicPortype.getReferenceSequences()).andReturn(referenceSequenceListResult).once();
        EasyMock.replay(mockSquidTopicPortype);

        // Test the getReferenceSequences method.
        referenceSequenceNames = sequencingService.getReferenceSequences();
        Assert.assertNotNull(referenceSequenceNames);
        Assert.assertEquals(referenceSequenceNames.size(), 1);
        assertEquals(referenceSequenceNames.get(0).name, "aReferenceSequence");
        assertEquals(referenceSequenceNames.get(0).getId(), 99L);

    }


    @Test
    public void testGetRequestSummariesByCreator() throws Exception {

        // Negative Tests
        List<ExperimentRequestSummary> aList0 = null;
        // Call the getRequestSummariesByCreator with a null user
        try {
            aList0 = sequencingService.getRequestSummariesByCreator(null);
            fail("should have thrown exception.");
        } catch (Exception exp) {
            assertTrue(exp instanceof IllegalArgumentException);
            assertTrue(exp.getMessage().contains("without a valid username"));
        }
        assertNull(aList0);
        // Call the getRequestSummariesByCreator with a user with no username
        try {
            aList0 = sequencingService.getRequestSummariesByCreator(new Person(""));
            fail("should have thrown exception.");
        } catch (Exception exp) {
            assertTrue(exp instanceof IllegalArgumentException);
            assertTrue(exp.getMessage().contains("without a valid username"));
        }
        assertNull(aList0);


        //Positive Tests
        SummarizedPassListResult summarizedPassListResult = new SummarizedPassListResult();
        List<SummarizedPass> summarizedPassList = summarizedPassListResult.getSummarizedPassList();
        Calendar today = Calendar.getInstance();

        {
            SummarizedPass summarizedPass = new SummarizedPass();
            summarizedPass.setCreatedDate(today);
            summarizedPass.setPassNumber("Pass-0101");
            summarizedPass.setResearchProject("101");
            summarizedPass.setStatus(PassStatus.NEW);
            summarizedPass.setType(PassType.WG);
            summarizedPass.setTitle("WGS");
            summarizedPass.setVersion(1);
            summarizedPassList.add(summarizedPass);
        }
        {
            SummarizedPass summarizedPass = new SummarizedPass();
            summarizedPass.setCreatedDate(today);
            summarizedPass.setPassNumber("Pass-0202");
            summarizedPass.setResearchProject("101");
            summarizedPass.setStatus(PassStatus.APPROVED);
            summarizedPass.setType(PassType.DIRECTED);
            summarizedPass.setTitle("HybridSelection");
            summarizedPass.setVersion(2);
            summarizedPassList.add(summarizedPass);
        }
        {
            SummarizedPass summarizedPass = new SummarizedPass();
            summarizedPass.setCreatedDate(today);
            summarizedPass.setPassNumber("Pass-0303");
            summarizedPass.setResearchProject("101");
            summarizedPass.setStatus(PassStatus.NEW);
            summarizedPass.setType(PassType.RNASEQ);
            summarizedPass.setTitle("RNASEQ");
            summarizedPass.setVersion(1);
            summarizedPassList.add(summarizedPass);
        }
        summarizedPassList.add(null);
        {
            SummarizedPass summarizedPass = new SummarizedPass();
            summarizedPass.setCreatedDate(today);
            summarizedPassList.add(summarizedPass);
            summarizedPass.setPassNumber("Pass-0606");
            summarizedPass.setTitle("Abandoned Pass");
            summarizedPass.setStatus(PassStatus.ABANDONED);
        }

        //Configure the mock to respond as needed
        EasyMock.expect(mockSquidTopicPortype.searchPassesByCreator((String) EasyMock.anyObject())).andReturn(summarizedPassListResult).once();
        EasyMock.replay(mockSquidTopicPortype);

        List<ExperimentRequestSummary> aList = sequencingService.getRequestSummariesByCreator(new Person("athena"));

        assertNotNull(aList);
        Assert.assertEquals(aList.size(), 4);
        List<ExperimentType> expectedExperimentTypes = new ArrayList<ExperimentType>();
        expectedExperimentTypes.add(ExperimentType.WholeGenomeSequencing);
        expectedExperimentTypes.add(ExperimentType.HybridSelection);
        expectedExperimentTypes.add(ExperimentType.RNASeq);


        for (ExperimentRequestSummary experimentRequestSummary : aList) {
            assertNotNull(experimentRequestSummary);
            assertEquals(experimentRequestSummary.getCreation().date, today.getTime());
            assertNotNull(experimentRequestSummary.getStatus());
            assertNotNull(experimentRequestSummary.getResearchProjectID());
            assertNotNull(experimentRequestSummary.getTitle());
            assertNotNull(experimentRequestSummary.getExperimentId());
            assertTrue(expectedExperimentTypes.contains(experimentRequestSummary.getExperimentType()));
        }
    }


    @Test
    public void testGetPlatformRequest() throws Exception {

        // Negative Tests
        SeqExperimentRequest seqExperimentRequest = null;
        // Call the getPlatformRequest without a null summary
        try {
            seqExperimentRequest = sequencingService.getPlatformRequest(null);
            fail("should have thrown exception.");
        } catch (Exception exp) {
            assertTrue(exp instanceof IllegalArgumentException);
            assertTrue(exp.getMessage().contains("without a remote sequencing experiment Id"));
        }
        assertNull(seqExperimentRequest);

        // Call the getPlatformRequest without a exp req Id
        try {
            seqExperimentRequest = sequencingService.getPlatformRequest(new ExperimentRequestSummary("An Experiment Title", null, new Date(), null));
            fail(SHOULD_HAVE_THROWN_EXCEPTION + IllegalArgumentException.class.getSimpleName());
        } catch (Exception exp) {
            assertTrue(exp instanceof IllegalArgumentException);
            assertTrue(exp.getMessage().equals(ExperimentRequestSummary.BLANK_CREATOR_EXCEPTION.getMessage()));
        }
        assertNull(seqExperimentRequest);

        // Positive Tests
        AbstractPass aPass = null;
        Calendar today = Calendar.getInstance();
        aPass = new WholeGenomePass();
        ProjectInformation projectInformation = new ProjectInformation();
        projectInformation.setAnalysisContacts("setAnalysisContacts");
        projectInformation.setCommonName("setCommonName");
        projectInformation.setControlledAccess(false);
        projectInformation.setDateCreated(today);
        projectInformation.setDiseaseName("setDiseaseName");
        projectInformation.setExperimentGoals("setExperimentGoals");
        projectInformation.setIrb("IRB1");
        projectInformation.setOrganismID(1L);
        projectInformation.setPassNumber("Pass-1234");

        projectInformation.setProgramProjectManagers("Bashful,Sneezey,Happy");
        SquidPersonList squidPlatformPeople = extractSquidPeopleFromUsernameList("Bashful,Grumpy,Doc");
        projectInformation.setPlatformProjectManagers(squidPlatformPeople);
        projectInformation.setSequencingTechnology(SequencingTechnology.ILLUMINA);
        SquidPersonList squidScientists = extractSquidPeopleFromUsernameList("Snow,White");
        projectInformation.setSponsoringScientists(squidScientists);
        projectInformation.setTitle("Walk in the woods experiment.");
        aPass.setProjectInformation(projectInformation);

        //Configure the mock to respond as needed
        EasyMock.expect(mockSquidTopicPortype.loadPassByNumber((String) EasyMock.anyObject())).andReturn(aPass).once();
        EasyMock.replay(mockSquidTopicPortype);

        ExperimentRequestSummary experimentRequestSummary = new ExperimentRequestSummary("An Experiment Title", new Person("athena"),
                new Date(), ExperimentType.WholeGenomeSequencing);
        experimentRequestSummary.setExperimentId(new ExperimentId(projectInformation.getPassNumber()));


        seqExperimentRequest = sequencingService.getPlatformRequest(experimentRequestSummary);
        Assert.assertNotNull(seqExperimentRequest);
        Assert.assertTrue(seqExperimentRequest instanceof WholeGenomeExperiment);
        WholeGenomeExperiment wholeGenomeExperiment = (WholeGenomeExperiment) seqExperimentRequest;
        assertEquals(wholeGenomeExperiment.getPlatformProjectManagers().size(), 3);
//        for (Person person :  wholeGenomeExperiment.getProgramProjectManagers(). ) {
//
//        }

        Assert.assertTrue(wholeGenomeExperiment.getProgramProjectManagers().contains(new Person("Bashful")));
        Assert.assertTrue(wholeGenomeExperiment.getProgramProjectManagers().contains(new Person("Sneezey")));
        Assert.assertTrue(wholeGenomeExperiment.getProgramProjectManagers().contains(new Person("Happy")));

    }
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


    private SquidPersonList extractSquidPeopleFromUsernameList(final String peopleStr) {
        SquidPersonList squidPersonList = new SquidPersonList();
        List<SquidPerson> personList = squidPersonList.getSquidPerson();
        if (StringUtils.isNotBlank(peopleStr)) {
            String[] userNames = peopleStr.split(",");
            List<String> nameList = Arrays.asList(userNames);
            int idCounter = 0;
            for (String name : nameList) {
                if (StringUtils.isNotBlank(name)) {
                    idCounter++;
                    SquidPerson person = new SquidPerson();
                    person.setLogin(name);
                    person.setPersonID(new BigInteger("" + idCounter));
                    personList.add(person);
                }
            }
        }
        return squidPersonList;
    }

}
