package org.broadinstitute.gpinformatics.athena.control.experiments;

import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequest;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.athena.entity.person.Person;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.squid.PMBSequencingService;
import org.broadinstitute.gpinformatics.infrastructure.UserNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.infrastructure.gap.GenotypingService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 7/19/12
 * Time: 5:13 PM
 */
@Impl
public class PMBExperimentRequestServiceImpl implements PMBExperimentRequestService {

    @Inject
    PMBSequencingService sequencingService;

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
