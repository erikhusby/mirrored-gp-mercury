package org.broadinstitute.pmbridge.infrastructure.gap;

import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.experiments.gap.GapExperimentRequest;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.infrastructure.SubmissionException;
import org.broadinstitute.pmbridge.infrastructure.UserNotFoundException;
import org.broadinstitute.pmbridge.infrastructure.ValidationException;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/22/12
 * Time: 12:26 PM
 */
public interface GenotypingService {

    //get a expriment request
    public GapExperimentRequest getPlatformRequest(final ExperimentRequestSummary experimentRequestSummary);

    //submit a expriment request
    public GapExperimentRequest saveExperimentRequest(final Person programMgr, final GapExperimentRequest gapExperimentRequest) throws ValidationException,
            SubmissionException;

        //submit a expriment request
    public GapExperimentRequest submitExperimentRequest(final Person programMgr, final GapExperimentRequest gapExperimentRequest) throws ValidationException,
            SubmissionException;

    // Get a list of experiment request summaries
    public List<ExperimentRequestSummary> getRequestSummariesByCreator(final Person creator) throws UserNotFoundException;

    // Get all supported platforms and products
    public Platforms getPlatforms();

}
