package org.broadinstitute.sequel.boundary.zims;


import org.broadinstitute.sequel.control.dao.run.RunChamberDAO;
import org.broadinstitute.sequel.entity.run.RunChamber;
import org.broadinstitute.sequel.entity.run.SequencingRun;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.zims.LibraryBean;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;

@Path("/RunLane")
public class RunLane {

    @Inject
    private RunChamberDAO runChamberDAO;

    @GET
    @Path("/query")
    @Produces("application/json")
    public List<LibraryBean> getLibraries(
            @QueryParam("runName") String runName,
            @QueryParam("chamber") String chamber)
    {
        if (runName == null) {
            throw new NullPointerException("runName cannot be null");
        }
        if (chamber == null) {
            throw new NullPointerException("chamber cannot be null");
        }

        RunChamber runChamber = runChamberDAO.findByRunNameAndChamber(runName, chamber);

        if (runChamber == null) {
            throw new RuntimeException("No run chamber found for run " + runName + " and chamber " + chamber);
        }

        throw new RuntimeException("I haven't been written yet");


    }
}
