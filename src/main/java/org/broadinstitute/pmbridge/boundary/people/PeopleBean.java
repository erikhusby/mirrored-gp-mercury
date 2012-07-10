package org.broadinstitute.pmbridge.boundary.people;

import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.infrastructure.squid.SequencingService;
import org.broadinstitute.pmbridge.presentation.AbstractJsfBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 7/4/12
 * Time: 3:19 PM
 */

@ManagedBean
@RequestScoped
public class PeopleBean extends AbstractJsfBean {

    @Inject
    private SequencingService sequencingService;

//    public List<Person> getSeqPlatformPeople() {
//        List<Person> result = sequencingService.getPlatformPeople();
//
//        Collections.sort(result, new Comparator<Person>() {
//                    @Override
//                    public int compare(Person w1, Person w2) {
//                        return w1.getLastName().compareTo(w2.getLastName());
//                    }
//                });
//        return result;
//    }


    public Map<String, Object> getSeqPlatformPeople() {
        List<Person> list = sequencingService.getPlatformPeople();

        Collections.sort(list, new Comparator<Person>() {
            @Override
            public int compare(Person w1, Person w2) {
                return w1.getLastName().compareTo(w2.getLastName());
            }
        });

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Person p : list) {
            result.put(p.getLastName() + ", " + p.getFirstName(), p.getUsername());
        }

        return result;
    }

}
