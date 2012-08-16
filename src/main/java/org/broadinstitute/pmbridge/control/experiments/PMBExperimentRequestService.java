package org.broadinstitute.pmbridge.control.experiments;

import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.broadinstitute.pmbridge.infrastructure.UserNotFoundException;
import org.broadinstitute.pmbridge.infrastructure.gap.GenotypingService;
import org.broadinstitute.pmbridge.infrastructure.squid.SequencingService;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 7/19/12
 * Time: 5:13 PM
 */
@Default
public class PMBExperimentRequestService implements ExperimentRequestService {

    @Inject
    SequencingService sequencingService;

    @Inject
    private GenotypingService genotypingService;

    @Override
    public List<ExperimentRequestSummary> findExperimentSummaries(final String username) throws UserNotFoundException {
        List<ExperimentRequestSummary> summaryList = new ArrayList<ExperimentRequestSummary>();

        // Get experiments from Sequencing
        List<ExperimentRequestSummary> seqSummaryList = sequencingService.getRequestSummariesByCreator(
                new Person(username, RoleType.PROGRAM_PM));
        summaryList.addAll(seqSummaryList);

        // Get experiments from Gap
        List<ExperimentRequestSummary> gapSummaryList = genotypingService.getRequestSummariesByCreator(
                new Person(username, RoleType.PROGRAM_PM));
        summaryList.addAll(gapSummaryList);

        return summaryList;
    }

    @Override
    public ExperimentRequest getPlatformRequest(final ExperimentRequestSummary experimentRequestSummary) {

        ExperimentRequest experimentRequest = null;

        //TODO under construction !!!
        if ((experimentRequestSummary != null ) &&
            (experimentRequestSummary.getTitle() != null) &&
            (experimentRequestSummary.getTitle().name != null) ) {

            if ( experimentRequestSummary.getExperimentId().value.startsWith("PASS")  ) {
                experimentRequest = sequencingService.getPlatformRequest(experimentRequestSummary);
            }

            if ( experimentRequestSummary.getExperimentId().value.startsWith("GXP")  ) {
                experimentRequest = genotypingService.getPlatformRequest(experimentRequestSummary);
            }

        }
        return experimentRequest;
    }

}
