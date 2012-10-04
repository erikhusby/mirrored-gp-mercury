package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import java.io.Serializable;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/1/12
 * Time: 12:56 PM
 */
public interface PMBSequencingService extends Serializable {

    // Sequencing people
    List<Person> getPlatformPeople();


}
