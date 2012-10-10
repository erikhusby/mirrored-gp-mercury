package org.broadinstitute.gpinformatics.infrastructure.experiments;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/24/12
 * Time: 1:20 PM
 */
public class EntityUtils {
    private static final Log logger = LogFactory.getLog(EntityUtils.class);

    public static String flattenSetOfStrings(Set<String> stringSet ) {
        return StringUtils.join(stringSet, ", ");
    }

    public static String flattenSetOfPersonUsernames(Set<Person> personSet ) {
        StringBuilder stringBuilder = new StringBuilder();
        if (personSet != null) {
            int i = 0;
            for (Person person :  personSet) {
                if ((person != null) && StringUtils.isNotBlank(person.getLogin())) {
                    if (i > 0) {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(person.getLogin());
                    i++;
                } else {
                    String msg = (person != null ? person.getFirstName() + " " + person.getLastName() : "Null username");
                    logger.error("Person has no username : " + msg );
                }
            }
        }
        return stringBuilder.toString();
    }

    public static Set<Person> extractPeopleFromUsernameList(String peopleStr) {
        Set<Person> personSet  = new HashSet<Person>();
        if (StringUtils.isNotBlank(peopleStr)) {
            String [] userNames = peopleStr.split(",");
            List<String> nameList = Arrays.asList(userNames);
            for (String name : nameList) {
                if (StringUtils.isNotBlank(name)) {
                    Person person = new Person(name);
                    personSet.add(person);
                }
            }
        }
        return personSet;
    }
}
