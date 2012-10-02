package org.broadinstitute.gpinformatics.infrastructure.squid;

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
}
