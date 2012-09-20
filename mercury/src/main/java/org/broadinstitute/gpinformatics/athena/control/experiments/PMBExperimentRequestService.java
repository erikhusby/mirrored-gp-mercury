package org.broadinstitute.gpinformatics.athena.control.experiments;

import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequest;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.infrastructure.UserNotFoundException;

import java.io.Serializable;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 7/19/12
 * Time: 5:50 PM
 */
public interface PMBExperimentRequestService extends Serializable {

    List<ExperimentRequestSummary> findExperimentSummaries(String username) throws UserNotFoundException;

    ExperimentRequest getPlatformRequest(ExperimentRequestSummary experimentRequestSummary);

}
