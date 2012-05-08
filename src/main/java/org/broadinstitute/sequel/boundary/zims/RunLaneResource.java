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
import java.util.*;


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

    public LibrariesBean getLibraries(LIMQueries.Client thriftClient,
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
            final TZamboniRun tRun = thriftClient.fetchSingleLane(runName,new Short(chamber).shortValue());
            final Map<String,BSPSampleDTO> lsidToBSPSample = fetchAllBSPDataAtOnce(tRun);

            if (tRun == null) {
                throw new RuntimeException("Could not load run " + runName);
            }
            else {
                for (TZamboniLane tZamboniLane : tRun.getLanes()) {
                    for (TZamboniLibrary zamboniLibrary : tZamboniLane.getLibraries()) {
                        String organism = null;
                        BSPSampleDTO bspDTO = lsidToBSPSample.get(zamboniLibrary.getLsid());

                        if (bspDTO == null) {
                            organism = zamboniLibrary.getOrganism();
                        }
                        else {
                            organism = bspDTO.getOrganism();
                        }
                       
                        
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
                                zamboniLibrary.getDevExperimentData(),
                                zamboniLibrary.getGssrBarcode(),
                                zamboniLibrary.getGssrBarcodes(),
                                zamboniLibrary.getGssrSampleType(),
                                zamboniLibrary.getTargetLaneCoverage());
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
     * Fetches all BSP data for the run in one shot,
     * returning a Map from the sample LSID to the
     * {@link BSPSampleDTO}.
     * @param run
     * @return
     */
    private Map<String,BSPSampleDTO> fetchAllBSPDataAtOnce(TZamboniRun run) {
        final Set<String> sampleLsids = new HashSet<String>();
        final Set<String> sampleNames = new HashSet<String>();
        for (TZamboniLane zamboniLane : run.getLanes()) {
            for (TZamboniLibrary zamboniLibrary : zamboniLane.getLibraries()) {
                if (isBspSample(zamboniLibrary)) {
                    sampleLsids.add(zamboniLibrary.getLsid());
                }
            }
        }
        for (Map.Entry<String,String> lsIdToBareId: bspSearchService.lsidsToBareIds(sampleLsids).entrySet()) {
            if (lsIdToBareId.getValue() == null) {
                throw new RuntimeException("Could not map lsid " + lsIdToBareId.getKey() + " to a bsp id.");
            }
            else {
                sampleNames.add(lsIdToBareId.getValue());
            }
        }
        return bspDataFetcher.fetchSamplesFromBSP(sampleNames);
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
