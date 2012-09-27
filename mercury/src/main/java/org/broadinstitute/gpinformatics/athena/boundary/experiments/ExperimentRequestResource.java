package org.broadinstitute.gpinformatics.athena.boundary.experiments;


import org.broadinstitute.gpinformatics.athena.control.experiments.PMBExperimentRequestService;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentType;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 7/19/12
 * Time: 5:03 PM
 */
public class ExperimentRequestResource {

    @Inject
    private PMBExperimentRequestService experimentRequestService;

    @GET
    @Path("{username}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<ExperimentRequestSummary> findExperimentSummaries(@PathParam("username") String username) {

        List<ExperimentRequestSummary> summaryList = new ArrayList<ExperimentRequestSummary>();

        //TODO under construction !!  Need to get the summaries from the platforms.
        //TODO For now new up one experiment summary  !!
        ExperimentRequestSummary experimentRequestSummary = new ExperimentRequestSummary(
                "An Experiment Title", new Person("someone"),
                new Date(),
                ExperimentType.Genotyping
        );

        summaryList.add( experimentRequestSummary );

        //TODO uncomment and test the following line
//    summaryList = experimentRequestService.findExperimentSummaries( username );

        return summaryList;

    }

}
