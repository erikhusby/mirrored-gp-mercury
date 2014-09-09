package org.broadinstitute.gpinformatics.mercury.boundary.zims;


import clover.org.apache.commons.lang.StringUtils;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniLane;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniLibrary;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRead;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPLSIDUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.zims.SquidThriftLibraryConverter;
import org.broadinstitute.gpinformatics.mercury.control.zims.ThriftLibraryConverter;
import org.broadinstitute.gpinformatics.mercury.control.zims.ZimsIlluminaRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;

import javax.annotation.Nonnull;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Web service for fetching run data for Zamboni.
 *
 * The flip side to this regards registering runs.  This is found in
 * {@link org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunResource}.  it may be prudent sometime in
 * the near future to join these two, or at least have thim in the same package.
 *
 */
@Path("/IlluminaRun")
@Stateless
public class IlluminaRunResource implements Serializable {
    private static final long serialVersionUID = -4933761044216626763L;

    private static final Log LOG = LogFactory.getLog(IlluminaRunResource.class);

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private ThriftService thriftService;

    @Inject
    private ProductOrderDao pdoDao;

    @Inject
    private ZimsIlluminaRunFactory zimsIlluminaRunFactory;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Inject
    private SystemRouter systemRouter;

    public IlluminaRunResource() {
    }

    public IlluminaRunResource(ThriftService thriftService,
            SampleDataFetcher sampleDataFetcher,
            IlluminaSequencingRunDao illuminaSequencingRunDao) {
        this.thriftService = thriftService;
        this.sampleDataFetcher = sampleDataFetcher;
        this.illuminaSequencingRunDao = illuminaSequencingRunDao;
    }

    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public ZimsIlluminaRun getRun(
            @QueryParam("runName") String runName) {
        ZimsIlluminaRun runBean = new ZimsIlluminaRun();
        if (runName == null) {
            runBean.setError("runName cannot be null");
        } else {
            IlluminaSequencingRun illuminaSequencingRun = illuminaSequencingRunDao.findByRunName(runName);
            if (illuminaSequencingRun == null) {
                runBean = callThrift(runName);
            } else {
                SystemRouter.System systemOfRecordForVessel = systemRouter.getSystemOfRecordForVessel(
                        illuminaSequencingRun.getSampleCartridge().getLabel());
                switch (systemOfRecordForVessel) {
                case MERCURY:
                    runBean = zimsIlluminaRunFactory.makeZimsIlluminaRun(illuminaSequencingRun);
                    break;
                case SQUID:
                    runBean = callThrift(runName);
                    break;
                default:
                    throw new RuntimeException("Failed to route " + runName);
                }
            }
        }

        return runBean;
    }

    private ZimsIlluminaRun callThrift(String runName) {
        ZimsIlluminaRun runBean = new ZimsIlluminaRun();
        try {
            runBean = getRun(thriftService, runName);
        } catch (Throwable t) {
            String message = "Failed while running pipeline query for run " + runName;
            LOG.error(message, t);
            runBean.setError(message + ": " + t.getMessage());
        }
        return runBean;
    }

    /**
     * Given a thrift run and a precomputed map of lsids
     * to BSP DTOs, create the run object.  Package protected
     * for testing.
     *
     * @param thriftRun from Thrift
     * @param lsidToBSPSample DTOs
     * @return DTO
     */
    ZimsIlluminaRun getRun(@Nonnull TZamboniRun thriftRun,
                           Map<String, BSPSampleDTO> lsidToBSPSample,
                           ThriftLibraryConverter thriftLibConverter,
                           ProductOrderDao pdoDao) {
        if (thriftRun == null) {
            throw new NullPointerException("thriftRun cannot be null");
        }

        ZimsIlluminaRun runBean = ZimsIlluminaRun.makeZimsIlluminaRun(thriftRun);

        for (TZamboniRead thriftZamboniRead : thriftRun.getReads()) {
            runBean.addRead(thriftZamboniRead);
        }

        for (TZamboniLane tZamboniLane : thriftRun.getLanes()) {
            List<LibraryBean> libraries = new ArrayList<>(96);
            for (TZamboniLibrary zamboniLibrary : tZamboniLane.getLibraries()) {
                BSPSampleDTO bspDTO = lsidToBSPSample.get(zamboniLibrary.getLsid());
                ProductOrder pdo = null;
                if (zamboniLibrary.getPdoKey() != null) {
                    pdo = pdoDao.findByBusinessKey(zamboniLibrary.getPdoKey());
                }
                libraries.add(thriftLibConverter.convertLibrary(zamboniLibrary, bspDTO, pdo));
            }
            runBean.addLane(new ZimsIlluminaChamber(tZamboniLane.getLaneNumber(), libraries, tZamboniLane.getPrimer(),
                    tZamboniLane.getSequencedLibraryName(), tZamboniLane.getSequencedLibraryCreationDate(),
                    tZamboniLane.getLoadingConcentration(), tZamboniLane.getActualReadStructure()));
        }

        return runBean;
    }

    /**
     * Always returns a non-null {@link ZimsIlluminaRun).
     *
     * @param thriftService
     * @param runName
     * @return
     */
    ZimsIlluminaRun getRun(ThriftService thriftService, @Nonnull String runName) {
        ZimsIlluminaRun runBean = new ZimsIlluminaRun();
        TZamboniRun tRun = thriftService.fetchRun(runName);
        if (tRun != null) {
            Map<String, BSPSampleDTO> lsidToBSPSample = fetchAllBSPDataAtOnce(tRun);
            runBean = getRun(tRun, lsidToBSPSample, new SquidThriftLibraryConverter(), pdoDao);
        } else {
            setErrorNoRun(runName, runBean);
        }
        return runBean;
    }

    private void setErrorNoRun(@Nonnull String runName, @Nonnull ZimsIlluminaRun runBean) {
        runBean.setError("Run " + runName + " doesn't appear to have been registered yet.  Please try again later " +
                         "or contact the mercury team if the problem persists.");
    }

    /**
     * Fetches all BSP data for the run in one shot, returning a Map from the LSID to the {@link BSPSampleDTO}.
     *
     * @param run from Thrift
     * @return map lsid to DTO
     */
    private Map<String, BSPSampleDTO> fetchAllBSPDataAtOnce(TZamboniRun run) {
        Set<String> sampleLsids = new HashSet<>();
        for (TZamboniLane zamboniLane : run.getLanes()) {
            for (TZamboniLibrary zamboniLibrary : zamboniLane.getLibraries()) {
                if (isBspSample(zamboniLibrary)) {
                    sampleLsids.add(zamboniLibrary.getLsid());
                }
            }
        }

        Set<String> sampleNames = new HashSet<>();
        for (Map.Entry<String, String> lsIdToBareId : BSPLSIDUtil.lsidsToBareIds(sampleLsids).entrySet()) {
            if (lsIdToBareId.getValue() == null) {
                throw new RuntimeException("Could not map lsid " + lsIdToBareId.getKey() + " to a bsp id.");
            } else {
                sampleNames.add(lsIdToBareId.getValue());
            }
        }
        Map<String, BSPSampleDTO> sampleToBspDto = sampleDataFetcher.fetchSamplesFromBSP(sampleNames);

        Map<String, BSPSampleDTO> lsidToBspDto = new HashMap<>();
        for (Map.Entry<String, BSPSampleDTO> bspSampleDTOEntry : sampleToBspDto.entrySet()) {
            BSPSampleDTO bspDto = bspSampleDTOEntry.getValue();
            // Make sure we get something out of BSP.  If we don't, consider it a
            // catastrophe, especially for the pipeline.
            String sampleName = bspSampleDTOEntry.getKey();
            if (bspDto == null) {
                throw new BSPLookupException("BSP returned no data for " + sampleName);
            }
            if (bspDto.getSampleLsid() == null || StringUtils.isBlank(bspDto.getSampleLsid())) {
                throw new BSPLookupException("BSP returned no LSID for " + sampleName);
            }
            if (bspDto.getSampleId() == null || StringUtils.isBlank(bspDto.getSampleId())) {
                throw new BSPLookupException("BSP returned no sample id for " + sampleName);
            }
            lsidToBspDto.put(bspDto.getSampleLsid(), bspDto);
        }

        if (sampleNames.size() != sampleToBspDto.size()) {
            throw new BSPLookupException("BSP search for " + sampleNames.size() + " samples returned " + sampleToBspDto.size() + " results!");
        }

        return lsidToBspDto;
    }

    /**
     * Based on the LSID, is this {@link TZamboniLibrary} derived
     * from a BSP sample?
     *
     * @param zamboniLibrary from Thrift
     * @return true if LSID indicates BSP
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

    /**
     * Returns a ZIMS DTO, based on chain of custody in Mercury (rather than Squid)
     * @param runName registered run
     * @return DTO based on Mercury chain of custody
     */
    @GET
    @Path("/queryMercury")
    @Produces(MediaType.APPLICATION_JSON)
    public ZimsIlluminaRun getMercuryRun(
            @QueryParam("runName") String runName) {
        ZimsIlluminaRun runBean = new ZimsIlluminaRun();
        if (runName == null) {
            runBean.setError("runName cannot be null");
        } else {
            try {
                IlluminaSequencingRun illuminaSequencingRun = illuminaSequencingRunDao.findByRunName(runName);
                if (illuminaSequencingRun == null) {
                    setErrorNoRun(runName, runBean);
                } else {
                    runBean = zimsIlluminaRunFactory.makeZimsIlluminaRun(illuminaSequencingRun);
                }
            } catch (Throwable t) {
                String message = "Failed while running pipeline query for run " + runName;
                LOG.error(message, t);
                runBean.setError(message + ": " + t.getMessage());
            }
        }

        return runBean;
    }

    /**
     * Returns a ZIMS DTO, based on chain of custody in Squid
     * @param runName registered run
     * @return DTO based on Squid chain of custody
     */
    @GET
    @Path("/querySquid")
    @Produces(MediaType.APPLICATION_JSON)
    public ZimsIlluminaRun getRunSquid(
            @QueryParam("runName") String runName) {
        ZimsIlluminaRun runBean = new ZimsIlluminaRun();
        if (runName == null) {
            runBean.setError("runName cannot be null");
        } else {
            try {
                runBean = getRun(thriftService, runName);
            } catch (Throwable t) {
                String message = "Failed while running pipeline query for run " + runName;
                LOG.error(message, t);
                runBean.setError(message + ": " + t.getMessage());
            }
        }

        return runBean;
    }
}
