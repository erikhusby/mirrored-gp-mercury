package org.broadinstitute.gpinformatics.mercury.boundary.zims;


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
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.mercury.control.zims.SquidThriftLibraryConverter;
import org.broadinstitute.gpinformatics.mercury.control.zims.ThriftLibraryConverter;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;

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
 * The flip side to this regards registering runs.  This is found in
 * {@link org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunResource}.  it may be prudent sometime in
 * the near future to join these two, or at least have thim in the same package.
 *
 */
@Path("/IlluminaRun")
@Stateless
public class IlluminaRunResource implements Serializable {

    private static final Log LOG = LogFactory.getLog(IlluminaRunResource.class);

    @Inject
    BSPSampleDataFetcher bspDataFetcher;

    @Inject
    ThriftService thriftService;

    @Inject
    ProductOrderDao pdoDao;

    public IlluminaRunResource() {
    }

    public IlluminaRunResource(ThriftService thriftService,
                               BSPSampleDataFetcher bspFetcher) {
        this.thriftService = thriftService;
        this.bspDataFetcher = bspFetcher;
    }

    @GET
    @Path("/query")
    @Produces({MediaType.APPLICATION_JSON})
    public ZimsIlluminaRun getRun(
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

    /**
     * Given a thrift run and a precomputed map of lsids
     * to BSP DTOs, create the run object.  Package protected
     * for testing.
     * @param tRun
     * @param lsidToBSPSample
     * @return
     */
    ZimsIlluminaRun getRun(final TZamboniRun tRun,
                           Map<String, BSPSampleDTO> lsidToBSPSample,
                           ThriftLibraryConverter thriftLibConverter,
                           ProductOrderDao pdoDao) {
        if (tRun == null) {
            throw new NullPointerException("tRun cannot be null");
        }

        final ZimsIlluminaRun runBean = new ZimsIlluminaRun(tRun.getRunName(),
                tRun.getRunBarcode(),
                tRun.getFlowcellBarcode(),
                tRun.getSequencer(),
                tRun.getSequencerModel(),
                tRun.getRunDate(),
                tRun.isPairedRun(),
                tRun.getActualReadStructure(),
                tRun.getImagedAreaPerLaneMM2());

        for (TZamboniRead tZamboniRead : tRun.getReads()) {
            runBean.addRead(tZamboniRead);
        }
        for (TZamboniLane tZamboniLane : tRun.getLanes()) {
            final List<LibraryBean> libraries = new ArrayList<LibraryBean>(96);
            for (TZamboniLibrary zamboniLibrary : tZamboniLane.getLibraries()) {
                BSPSampleDTO bspDTO = lsidToBSPSample.get(zamboniLibrary.getLsid());
                ProductOrder pdo = null;
                if (zamboniLibrary.getPdoKey() != null) {
                    pdo = pdoDao.findByBusinessKey(zamboniLibrary.getPdoKey());
                }
                libraries.add(thriftLibConverter.convertLibrary(zamboniLibrary, bspDTO, pdo));
            }
            runBean.addLane(new ZimsIlluminaChamber(tZamboniLane.getLaneNumber(), libraries, tZamboniLane.getPrimer(), tZamboniLane.getSequencedLibraryName()));
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
    ZimsIlluminaRun getRun(ThriftService thriftService,
                           String runName) {
        ZimsIlluminaRun runBean = new ZimsIlluminaRun();
        final TZamboniRun tRun = thriftService.fetchRun(runName);
        if (tRun != null) {
            Map<String, BSPSampleDTO> lsidToBSPSample = fetchAllBSPDataAtOnce(tRun);
            runBean = getRun(tRun, lsidToBSPSample, new SquidThriftLibraryConverter(), pdoDao);
        } else {
            runBean.setError("Run " + runName + " doesn't appear to have been registered yet.  Please try again later or contact the mercury team if the problem persists.");
        }
        return runBean;
    }

    /**
     * Fetches all BSP data for the run in one shot,
     * returning a Map from the {@link org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO#getSampleLsid()} to the
     * {@link BSPSampleDTO}.
     *
     * @param run
     * @return
     */
    private Map<String, BSPSampleDTO> fetchAllBSPDataAtOnce(TZamboniRun run) {
        Set<String> sampleLsids = new HashSet<String>();
        Set<String> sampleNames = new HashSet<String>();
        Map<String, BSPSampleDTO> lsidToBspDto = new HashMap<String, BSPSampleDTO>();
        Map<String, BSPSampleDTO> sampleToBspDto = new HashMap<String, BSPSampleDTO>();
        for (TZamboniLane zamboniLane : run.getLanes()) {
            for (TZamboniLibrary zamboniLibrary : zamboniLane.getLibraries()) {
                if (isBspSample(zamboniLibrary)) {
                    sampleLsids.add(zamboniLibrary.getLsid());
                }
            }
        }
        for (Map.Entry<String, String> lsIdToBareId : BSPLSIDUtil.lsidsToBareIds(sampleLsids).entrySet()) {
            if (lsIdToBareId.getValue() == null) {
                throw new RuntimeException("Could not map lsid " + lsIdToBareId.getKey() + " to a bsp id.");
            } else {
                sampleNames.add(lsIdToBareId.getValue());
            }
        }
        if (!sampleNames.isEmpty()) {
            sampleToBspDto = bspDataFetcher.fetchSamplesFromBSP(sampleNames);
        }
        for (BSPSampleDTO bspSampleDTO : sampleToBspDto.values()) {
            if (bspSampleDTO.getSampleLsid() != null) {
                lsidToBspDto.put(bspSampleDTO.getSampleLsid(), bspSampleDTO);
            }
        }

        return lsidToBspDto;
    }

    /**
     * Based on the LSID, is this {@link TZamboniLibrary} derived
     * from a BSP sample?
     *
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
