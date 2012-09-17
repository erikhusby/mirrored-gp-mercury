package org.broadinstitute.pmbridge.control.experiments;

import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.infrastructure.UserNotFoundException;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 7/19/12
 * Time: 5:50 PM
 */
public interface ExperimentRequestService {

    List<ExperimentRequestSummary> findExperimentSummaries(String username) throws UserNotFoundException;

    ExperimentRequest getPlatformRequest(ExperimentRequestSummary experimentRequestSummary);

}
