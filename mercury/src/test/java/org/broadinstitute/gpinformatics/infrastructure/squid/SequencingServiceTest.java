package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.SquidPerson;
import org.broadinstitute.gpinformatics.mercury.boundary.SquidPersonList;
import org.broadinstitute.gpinformatics.mercury.boundary.SquidTopicPortype;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;

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
        assertEquals(people.get(0).getFirstName(), "Jon");
        assertEquals(people.get(0).getLastName(), "Tester");
        assertEquals(people.get(0).getLogin().compareTo("tester"), 0);

    }

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
