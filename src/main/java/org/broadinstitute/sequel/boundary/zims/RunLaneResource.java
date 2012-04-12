package org.broadinstitute.sequel.boundary.zims;


import edu.mit.broad.prodinfo.thrift.lims.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.broadinstitute.sequel.control.dao.run.RunChamberDAO;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.run.RunChamber;
import org.broadinstitute.sequel.entity.run.SequencingRun;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.zims.LibraryBean;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftConfiguration;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.List;

@Path("/RunLane")
public class RunLaneResource {

    @Inject
    private RunChamberDAO runChamberDAO;
    
    @Inject
    BSPSampleDataFetcher bspDataFetcher;

    @Inject
    ThriftConfiguration thriftConfiguration;
    
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
        final List<LibraryBean> libraries = new ArrayList<LibraryBean>(96);

        TTransport transport = new TSocket(thriftConfiguration.getHost(), thriftConfiguration.getPort());
        TProtocol protocol = new TBinaryProtocol(transport);
        LIMQueries.Client client = new LIMQueries.Client(protocol);
        try {
            transport.open();
        }
        catch(TTransportException e) {
            throw new RuntimeException("Could not open transport for " + thriftConfiguration.getHost() + ":" + thriftConfiguration.getPort(),e);
        }
        try {
            TZamboniRun tRun = client.fetchSingleLane(runName,new Short(chamber).shortValue());

            if (tRun == null) {
                throw new RuntimeException("Could not load run " + runName);
            }
            else {
                for (TZamboniLane tZamboniLane : tRun.getLanes()) {
                    for (TZamboniLibrary tZamboniLibrary : tZamboniLane.getLibraries()) {
                        libraries.add(new LibraryBean(tZamboniLibrary.getProject(),
                                tZamboniLibrary.getOrganism(),
                                tZamboniLibrary.getWorkRequestId()));
                    }
                }
            }
        }
        catch(TZIMSException e) {
            throw new RuntimeException("Failed to fetch run " + runName + " lane " + chamber,e);
        }
        catch(TException e) {
            throw new RuntimeException("Failed to fetch run " + runName + " lane " + chamber,e);
        }
        finally {
            if (transport != null) {
                transport.close();
            }
        }
        return libraries;
    }

}
