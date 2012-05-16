package org.broadinstitute.pmbridge.infrastructure.squid;

import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.experiments.seq.BaitSetName;
import org.broadinstitute.pmbridge.entity.experiments.seq.OrganismName;
import org.broadinstitute.pmbridge.entity.experiments.seq.ReferenceSequenceName;
import org.broadinstitute.pmbridge.entity.experiments.seq.SeqExperimentRequest;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.infrastructure.SubmissionException;
import org.broadinstitute.pmbridge.infrastructure.ValidationException;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/1/12
 * Time: 12:56 PM
 */
public interface SequencingService {

    // Sequencing people
    List<Person> getPlatformPeople();

    // Organisms
    List<OrganismName> getOrganisms();

    //BaitSets
    List<BaitSetName> getBaitSets();

    //reference sets
    List<ReferenceSequenceName> getReferenceSequences();

    //Passes
    List<ExperimentRequestSummary> getRequestSummariesByCreator(Person programMgr);

    //load pass
    SeqExperimentRequest getPlatformRequest(ExperimentRequestSummary experimentRequestSummary);

    //validate pass
    void validatePlatformRequest(SeqExperimentRequest seqExperimentRequest) throws ValidationException;

    //submit pass
    SeqExperimentRequest submitRequestToPlatform(SeqExperimentRequest seqExperimentRequest) throws ValidationException, SubmissionException;

}
