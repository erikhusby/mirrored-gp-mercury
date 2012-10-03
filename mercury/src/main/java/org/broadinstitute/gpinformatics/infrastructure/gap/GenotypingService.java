package org.broadinstitute.gpinformatics.infrastructure.gap;

import org.broadinstitute.gpinformatics.infrastructure.UserNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/22/12
 * Time: 12:26 PM
 */
public interface GenotypingService {


    // Get a list of experiment request summaries
    public List<ExperimentRequestSummary> getRequestSummariesByCreator(final Person creator) throws UserNotFoundException;

    // Get all supported platforms and products
    public Platforms getPlatforms();

}
