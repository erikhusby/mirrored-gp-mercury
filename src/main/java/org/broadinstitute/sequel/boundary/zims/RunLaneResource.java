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
import org.broadinstitute.sequel.entity.zims.LibrariesBean;
import org.broadinstitute.sequel.entity.zims.LibraryBean;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftConfiguration;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


/**
 * Web service for fetching run data for Zamboni.
 */
@Path("/RunLane")
@Stateless
public class RunLaneResource {

    @Inject
    private RunChamberDAO runChamberDAO;
    
    @Inject
    BSPSampleDataFetcher bspDataFetcher;
    
    @Inject
    BSPSampleSearchService bspSearchService;

    @Inject
    ThriftConfiguration thriftConfiguration;

    public RunLaneResource() {}

    public RunLaneResource(ThriftConfiguration thriftConfiguration) {
        this.thriftConfiguration = thriftConfiguration;
    }

    @GET
    @Path("/query")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    public LibrariesBean getLibraries(
            @QueryParam("runName") String runName,
            @QueryParam("chamber") String chamber)
    {
        if (runName == null) {
            throw new NullPointerException("runName cannot be null");
        }
        if (chamber == null) {
            throw new NullPointerException("chamber cannot be null");
        }

        TTransport transport = new TSocket(thriftConfiguration.getHost(), thriftConfiguration.getPort());
        TProtocol protocol = new TBinaryProtocol(transport);
        LIMQueries.Client client = new LIMQueries.Client(protocol);

        return getLibraries(client,transport,runName,chamber);
    }

    LibrariesBean getLibraries(LIMQueries.Client thriftClient,
                               TTransport thriftTransport,
                               String runName,
                               String chamber) {
        final List<LibraryBean> libraries = new ArrayList<LibraryBean>(96);
        try {
            thriftTransport.open();
        }
        catch(TTransportException e) {
            throw new RuntimeException("Could not open transport for " + thriftConfiguration.getHost() + ":" + thriftConfiguration.getPort(),e);
        }
        try {
            TZamboniRun tRun = thriftClient.fetchSingleLane(runName,new Short(chamber).shortValue());

            if (tRun == null) {
                throw new RuntimeException("Could not load run " + runName);
            }
            else {
                for (TZamboniLane tZamboniLane : tRun.getLanes()) {
                    for (TZamboniLibrary zamboniLibrary : tZamboniLane.getLibraries()) {
                        String organism = getOrganism(zamboniLibrary);
                       
                        
                        LibraryBean libBean = new LibraryBean(zamboniLibrary.getLibrary(),
                                zamboniLibrary.getProject(),
                                zamboniLibrary.getInitiative(),
                                zamboniLibrary.getWorkRequestId(),
                                zamboniLibrary.getMolecularIndexes(),
                                zamboniLibrary.isHasIndexingRead(),
                                zamboniLibrary.getExpectedInsertSize(),
                                zamboniLibrary.getAnalysisType(),
                                zamboniLibrary.getReferenceSequence(),
                                zamboniLibrary.getReferenceSequenceVersion(),
                                zamboniLibrary.getSampleAlias(),
                                zamboniLibrary.getSampleCollaborator(),
                                organism,
                                zamboniLibrary.getSpecies(),
                                zamboniLibrary.getStrain(),
                                zamboniLibrary.getLsid(),
                                zamboniLibrary.getTissueType(),
                                zamboniLibrary.getExpectedPlasmid(),
                                zamboniLibrary.getAligner(),
                                zamboniLibrary.getRrbsSizeRange(),
                                zamboniLibrary.getRestrictionEnzyme(),
                                zamboniLibrary.getCellLine(),
                                zamboniLibrary.getBaitSetName(),
                                zamboniLibrary.getIndividual(),
                                zamboniLibrary.getLabMeasuredInsertSize(),
                                zamboniLibrary.isPositiveControl(),
                                zamboniLibrary.isNegativeControl(),
                                zamboniLibrary.getWeirdness(),
                                zamboniLibrary.getPrecircularizationDnaSize(),
                                zamboniLibrary.isPartOfDevExperiment(),
                                zamboniLibrary.getDevExperimentData());
                        libraries.add(libBean);
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
            if (thriftTransport != null) {
                thriftTransport.close();
            }
        }
        return new LibrariesBean(libraries);
    }

    /**
     * Why are we doing this redundant call?  To get some practice running
     * live against BSP to see how well it works.
     * @param zamboniLibrary
     * @return
     */
    // todo fetch this data from BSP in batch, not one at a time.
    private String getOrganism(TZamboniLibrary zamboniLibrary) {
        String organism = null;
        if (isBspSample(zamboniLibrary)) {
            Collection<String> lsids = new HashSet<String>();
            lsids.add(zamboniLibrary.getLsid());
            String lsid = bspSearchService.lsidsToBareIds(lsids).values().iterator().next();
            BSPSampleDTO bspSample = bspDataFetcher.fetchSingleSampleFromBSP(lsid);
            organism = bspSample.getOrganism();
        }
        else {
            organism = zamboniLibrary.getOrganism();
        }
        return organism;
    }

    /**
     * Based on the LSID, is this {@link TZamboniLibrary} derived
     * from a BSP sample?
     * @param zamboniLibrary
     * @return
     */
    private boolean isBspSample(TZamboniLibrary zamboniLibrary) {
        String lsid = zamboniLibrary.getLsid();
        boolean isBsp = false;
        if (lsid != null) {
            lsid = lsid.toLowerCase();
            if (lsid.startsWith("broad.mit.edu:bsp.prod.sample:") || lsid.startsWith("broadinstitute.org:bsp.prod.sample:")) {
                isBsp = true;
            }
        }
        return isBsp;
    }
}
