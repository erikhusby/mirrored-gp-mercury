package org.broadinstitute.pmbridge.infrastructure.squid;

import org.broad.squid.services.TopicService.SquidPerson;
import org.broad.squid.services.TopicService.SquidPersonList;
import org.broad.squid.services.TopicService.SquidTopicPortype;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.List;

import static org.broadinstitute.pmbridge.TestGroups.DATABASE_FREE;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/3/12
 * Time: 2:55 PM
 */
public class SequencingServiceTest {

    @BeforeMethod
    public void setUp() throws Exception {

    }

    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test(groups = DATABASE_FREE)
    public void testGetPlatformPeople() throws Exception {

        SquidTopicPortype mockSquidTopicPortype = EasyMock.createMock(SquidTopicPortype.class);
        // create a new service with squidport mocked out.
        SequencingServiceImpl sequencingService = new SequencingServiceImpl(mockSquidTopicPortype);
        SquidPersonList squidPersonList = new SquidPersonList();
        List<SquidPerson> aList = squidPersonList.getSquidPerson();
        SquidPerson squidPerson = new SquidPerson();
        squidPerson.setLogin("tester");
        squidPerson.setFirstName("Jon");
        squidPerson.setLastName("Tester");
        squidPerson.setPersonID(new BigInteger("100"));
        aList.add(squidPerson);
        EasyMock.expect(mockSquidTopicPortype.getGreeting()).andReturn("UnitTest Greeting").anyTimes();
        EasyMock.expect(mockSquidTopicPortype.getBroadPIList()).andReturn(squidPersonList).once();
        EasyMock.replay(mockSquidTopicPortype);

        List<Person> people = sequencingService.getPlatformPeople();
        Assert.assertNotNull(people);
        Assert.assertEquals(people.size(), 1);
        Assert.assertEquals( people.get(0).getUsername(), "tester");
        Assert.assertEquals( people.get(0).getLastName(), "Tester");
        Assert.assertEquals(people.get(0).getPersonId().compareTo("100"), 0);
        Assert.assertEquals( people.get(0).getRoleType(), RoleType.BROAD_SCIENTIST );

    }

//    @Test
//    public void testGetOrganisms() throws Exception {
//
//    }
//
//    @Test
//    public void testGetBaitSets() throws Exception {
//
//    }
//
//    @Test
//    public void testGetReferenceSequences() throws Exception {
//
//    }
//
//    @Test
//    public void testGetRequestSummariesByCreator() throws Exception {
//
//    }
//
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
