package org.broadinstitute.sequel.boundary.zims;


import edu.mit.broad.prodinfo.thrift.lims.*;
import org.apache.commons.collections15.map.LRUMap;
import org.apache.thrift.TException;
import org.broadinstitute.sequel.control.dao.run.RunChamberDAO;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.sequel.entity.zims.LibraryBean;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.sequel.infrastructure.bsp.BSPLSIDUtil;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.sequel.infrastructure.jmx.ZimsCacheControl;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftService;

import javax.annotation.concurrent.GuardedBy;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.*;


/**
 * Web service for fetching run data for Zamboni.
 *
 */
@Path("/IlluminaRun")
@Stateless
public class IlluminaRunResource implements Serializable {

    @Inject
    private RunChamberDAO runChamberDAO;

    @Inject
    BSPSampleDataFetcher bspDataFetcher;

    @Inject
    BSPSampleSearchService bspSearchService;

    @Inject
    ThriftService thriftService;

    @Inject
    ZimsCacheControl cacheControl;

    private static Object cacheSemaphore = Boolean.TRUE;

    @GuardedBy("cacheSemaphore")
    private static LRUMap<String,ZimsIlluminaRun> runCache;

    public IlluminaRunResource() {
        initCache();
    }

    public IlluminaRunResource(ThriftService thriftService,
                               ZimsCacheControl cacheControl) {
        this.thriftService = thriftService;
        this.cacheControl = cacheControl;
        initCache();
    }

    private void initCache() {
        synchronized (cacheSemaphore) {
            if (runCache == null) {
                runCache = new LRUMap<String, ZimsIlluminaRun>();
            }
        }
    }

    @GET
    @Path("/query")
    @Produces({MediaType.APPLICATION_JSON})
    public ZimsIlluminaRun getRun(
            @QueryParam("runName") String runName)
    {
        if (runName == null) {
            throw new NullPointerException("runName cannot be null");
        }

        updateCache();
        ZimsIlluminaRun runBean = getRunFromCache(runName);

        if (runBean == null) {
            runBean = getRun(thriftService,runName);
            if (runBean != null) {
                runCache.put(runName,runBean);
            }
        }

        return runBean;
    }

    /**
     * Given a thrift run and a precomputed map of lsids
     * to BSP DTOs, create the run object.  Package protected
     * for testing.
     * @param tRun
     * @param lsidToBSPSample
     * @return
     */
    ZimsIlluminaRun getRun(final TZamboniRun tRun,Map<String,BSPSampleDTO> lsidToBSPSample) {
        if (tRun == null) {
            throw new NullPointerException("tRun cannot be null");
        }

        final ZimsIlluminaRun runBean = new ZimsIlluminaRun(tRun.getRunName(),
                tRun.getRunBarcode(),
                tRun.getFlowcellBarcode(),
                tRun.getSequencer(),
                tRun.getSequencerModel(),
                tRun.getRunDate(),
                tRun.getFirstCycle(),
                tRun.getFirstCycleReadLength(),
                tRun.getLastCycle(),
                tRun.getMolBarcodeCycle(),
                tRun.getMolBarcodeLength(),
                tRun.isPairedRun());

        for (TZamboniRead tZamboniRead : tRun.getReads()) {
            runBean.addRead(tZamboniRead);
        }
        for (TZamboniLane tZamboniLane : tRun.getLanes()) {
            final List<LibraryBean> libraries = new ArrayList<LibraryBean>(96);
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
                        zamboniLibrary.getTargetLaneCoverage(),
                        zamboniLibrary.aggregate);
                libraries.add(libBean);
            }
            runBean.addLane(new ZimsIlluminaChamber(tZamboniLane.getLaneNumber(), libraries, tZamboniLane.getPrimer()));
        }
        return runBean;
    }

    private ZimsIlluminaRun getRunFromCache(String runName) {
        synchronized (cacheSemaphore) {
            ZimsIlluminaRun run = null;
            if (runCache.containsKey(runName)) {
                run = runCache.get(runName);
            }
            return run;
        }
    }

    /**
     * Updates the cache.  If someone changed the cache size through
     * JConsole, we resize the cache.  If the cache was invalidated through
     * JConsole, we clear and reset it.
     */
    private void updateCache() {
        synchronized (cacheSemaphore) {
            if (cacheControl.wasInvalidated()) {
                cacheControl.reset();
                runCache.clear();
            }
            if (runCache.maxSize() != cacheControl.getMaximumCacheSize()) {
                LRUMap<String,ZimsIlluminaRun> newCache = new LRUMap<String, ZimsIlluminaRun>(cacheControl.getMaximumCacheSize());
                newCache.putAll(runCache);
                runCache = newCache;
            }
        }
    }

    ZimsIlluminaRun getRun(ThriftService thriftService,
                           String runName) {
        ZimsIlluminaRun runBean = null;
        try {
            final TZamboniRun tRun = thriftService.fetchRun(runName);
            if (tRun == null) {
                throw new RuntimeException("Could not load run " + runName);
            }
            else {
                final Map<String,BSPSampleDTO> lsidToBSPSample = fetchAllBSPDataAtOnce(tRun);
                runBean = getRun(tRun,lsidToBSPSample);
            }
        }
        catch(TZIMSException e) {
            throw new RuntimeException("Failed to fetch run " + runName,e);
        }
        catch(TException e) {
            throw new RuntimeException("Failed to fetch run " + runName,e);
        }

        return runBean;
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
        Map<String,BSPSampleDTO> sampleToBspDto = new HashMap<String, BSPSampleDTO>();
        for (TZamboniLane zamboniLane : run.getLanes()) {
            for (TZamboniLibrary zamboniLibrary : zamboniLane.getLibraries()) {
                if (isBspSample(zamboniLibrary)) {
                    sampleLsids.add(zamboniLibrary.getLsid());
                }
            }
        }
        for (Map.Entry<String,String> lsIdToBareId: BSPLSIDUtil.lsidsToBareIds(sampleLsids).entrySet()) {
            if (lsIdToBareId.getValue() == null) {
                throw new RuntimeException("Could not map lsid " + lsIdToBareId.getKey() + " to a bsp id.");
            }
            else {
                sampleNames.add(lsIdToBareId.getValue());
            }
        }
        if (!sampleNames.isEmpty()) {
            sampleToBspDto = bspDataFetcher.fetchSamplesFromBSP(sampleNames);
        }
        return sampleToBspDto;
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
