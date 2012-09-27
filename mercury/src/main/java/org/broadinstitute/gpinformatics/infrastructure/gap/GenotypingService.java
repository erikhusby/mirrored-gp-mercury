package org.broadinstitute.gpinformatics.infrastructure.gap;

import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.athena.entity.experiments.gap.GapExperimentRequest;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.infrastructure.SubmissionException;
import org.broadinstitute.gpinformatics.infrastructure.UserNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;

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
