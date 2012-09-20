package org.broadinstitute.gpinformatics.athena.infrastructure.squid;

import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.athena.entity.experiments.seq.BaitSetName;
import org.broadinstitute.gpinformatics.athena.entity.experiments.seq.OrganismName;
import org.broadinstitute.gpinformatics.athena.entity.experiments.seq.ReferenceSequenceName;
import org.broadinstitute.gpinformatics.athena.entity.experiments.seq.SeqExperimentRequest;
import org.broadinstitute.gpinformatics.athena.entity.person.Person;
import org.broadinstitute.gpinformatics.infrastructure.SubmissionException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;

import javax.enterprise.inject.Alternative;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/11/12
 * Time: 1:54 PM
 */
@Alternative
public class MockSequencingServiceImpl implements SequencingService {

    @Override
    public List<Person> getPlatformPeople() {
        throw new IllegalArgumentException("Not implemented yet.");
        
    }

    @Override
    public List<OrganismName> getOrganisms() {
        throw new IllegalArgumentException("Not implemented yet.");
    }

    @Override
    public List<BaitSetName> getBaitSets() {
        throw new IllegalArgumentException("Not implemented yet.");
    }

    @Override
    public List<ReferenceSequenceName> getReferenceSequences() {
        throw new IllegalArgumentException("Not implemented yet.");
    }

    @Override
    public List<ExperimentRequestSummary> getRequestSummariesByCreator(Person programMgr) {
        throw new IllegalArgumentException("Not implemented yet.");
    }

    @Override
    public SeqExperimentRequest getPlatformRequest(ExperimentRequestSummary experimentRequestSummary) {
        throw new IllegalArgumentException("Not implemented yet.");
    }

    @Override
    public void validatePlatformRequest(SeqExperimentRequest seqExperimentRequest) throws ValidationException {
        throw new IllegalArgumentException("Not implemented yet.");
    }

    @Override
    public SeqExperimentRequest submitRequestToPlatform(final Person programMgr, final SeqExperimentRequest seqExperimentRequest) throws ValidationException, SubmissionException {
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
