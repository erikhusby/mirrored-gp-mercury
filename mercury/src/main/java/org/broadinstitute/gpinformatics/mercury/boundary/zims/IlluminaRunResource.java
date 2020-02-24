package org.broadinstitute.gpinformatics.mercury.boundary.zims;


import edu.mit.broad.prodinfo.thrift.lims.TZamboniLane;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniLibrary;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRead;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPLSIDUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemOfRecord;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
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
    private BSPSampleDataFetcher sampleDataFetcher;

    @Inject
    private ThriftService thriftService;

    @Inject
    private ProductOrderDao pdoDao;

    @Inject
    private ZimsIlluminaRunFactory zimsIlluminaRunFactory;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Inject
    private SystemOfRecord systemOfRecord;

    public IlluminaRunResource() {
    }

    public IlluminaRunResource(ThriftService thriftService,
            BSPSampleDataFetcher sampleDataFetcher,
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
                SystemOfRecord.System systemOfRecordForVessel = systemOfRecord.getSystemOfRecord(
                        illuminaSequencingRun.getSampleCartridge().getLabel());
                switch (systemOfRecordForVessel) {
                case MERCURY:
                    runBean = zimsIlluminaRunFactory.makeZimsIlluminaRun(illuminaSequencingRun);
                    break;
                case SQUID:
                    runBean = callThrift(runName);
                    break;
                default:
                    throw new RuntimeException("Failed to find system of record for " + runName);
                }
            }
        }

        return runBean;
    }

    @GET
    @Path("/cancel")
    public Response cancelRun(@QueryParam("runName") String runName) {
        IlluminaSequencingRun illuminaSequencingRun = illuminaSequencingRunDao.findByRunName(runName);
        if (illuminaSequencingRun == null) {
            throw new ResourceException(runName + " is not registered.", Response.Status.INTERNAL_SERVER_ERROR);
        }

        illuminaSequencingRun.setCancelled(true);
        illuminaSequencingRunDao.persist(illuminaSequencingRun);
        return Response.status(Response.Status.OK).entity("Run cancelled").type(MediaType.TEXT_PLAIN_TYPE).build();
    }

    @GET
    @Path("/query/runBarcode/{runBarcode}")
    @Produces(MediaType.APPLICATION_JSON)
    public ZimsIlluminaRun getRunByBarcode(@PathParam("runBarcode") String runBarcode) {
        ZimsIlluminaRun runBean = new ZimsIlluminaRun();
        if (runBarcode == null) {
            runBean.setError("runBarcode cannot be null");
        } else {
            Collection<IlluminaSequencingRun> runs = illuminaSequencingRunDao.findByBarcode(runBarcode);
            SystemOfRecord.System systemOfRecordForVessel;
            if (runs.size() == 0) {
                systemOfRecordForVessel = SystemOfRecord.System.SQUID;
            } else if (runs.size() > 1) {
                String runNames = "";
                for (IlluminaSequencingRun run : runs) {
                    runNames += run.getRunName() + " ";
                }
                runBean.setError("Found multiple runNames : " + runNames);
                return runBean;
            } else {
                systemOfRecordForVessel = systemOfRecord.getSystemOfRecordForVessel(
                        runs.iterator().next().getSampleCartridge());
            }

            switch (systemOfRecordForVessel) {
            case MERCURY:
                runBean = zimsIlluminaRunFactory.makeZimsIlluminaRun(runs.iterator().next());
                break;
            case SQUID:
                runBean = new ZimsIlluminaRun();
                try {
                    runBean = getRunByBarcode(thriftService, runBarcode);
                } catch (Throwable t) {
                    String message = "Failed while running pipeline query for run by barcode " + runBarcode;
                    LOG.error(message, t);
                    runBean.setError(message + ": " + t.getMessage());
                }
                break;
            default:
                throw new RuntimeException("Ambiguous system of record for " +
                                           runs.iterator().next().getSampleCartridge().getLabel());
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
     * to BSP SampleData, create the run object.  Package protected
     * for testing.
     *
     * @param thriftRun from Thrift
     * @param lsidToBSPSample DTOs
     * @return DTO
     */
    ZimsIlluminaRun getRun(@Nonnull TZamboniRun thriftRun,
                           Map<String, SampleData> lsidToBSPSample,
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
                SampleData bspSampleData = lsidToBSPSample.get(zamboniLibrary.getLsid());
                ProductOrder pdo = null;
                if (zamboniLibrary.getPdoKey() != null) {
                    pdo = pdoDao.findByBusinessKey(zamboniLibrary.getPdoKey());
                }
                libraries.add(thriftLibConverter.convertLibrary(zamboniLibrary, bspSampleData, pdo));
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
            Map<String, SampleData> lsidToBSPSample = fetchAllBSPDataAtOnce(tRun);
            runBean = getRun(tRun, lsidToBSPSample, new SquidThriftLibraryConverter(), pdoDao);
        } else {
            setErrorNoRun(runName, runBean);
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
    ZimsIlluminaRun getRunByBarcode(ThriftService thriftService, @Nonnull String runBarcode) {
        ZimsIlluminaRun runBean = new ZimsIlluminaRun();
        TZamboniRun tRun = thriftService.fetchRunByBarcode(runBarcode);
        if (tRun != null) {
            Map<String, SampleData> lsidToBSPSample = fetchAllBSPDataAtOnce(tRun);
            runBean = getRun(tRun, lsidToBSPSample, new SquidThriftLibraryConverter(), pdoDao);
        } else {
            setErrorNoRun(runBarcode, runBean);
        }
        return runBean;
    }

    private void setErrorNoRun(@Nonnull String runName, @Nonnull ZimsIlluminaRun runBean) {
        runBean.setError("Run " + runName + " doesn't appear to have been registered yet.  Please try again later " +
                         "or contact the mercury team if the problem persists.");
    }

    /**
     * Fetches all BSP data for the run in one shot, returning a Map from the LSID to the {@link BspSampleData}.
     *
     * @param run from Thrift
     * @return map lsid to DTO
     */
    Map<String, SampleData> fetchAllBSPDataAtOnce(TZamboniRun run) {
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
        Map<String, BspSampleData> sampleDataByName = sampleDataFetcher.fetchSampleData(sampleNames);

        Map<String, SampleData> lsidToSampleData = new HashMap<>();
        for (Map.Entry<String, BspSampleData> bspSampleDataEntry : sampleDataByName.entrySet()) {
            SampleData sampleData = bspSampleDataEntry.getValue();
            // Make sure we get something out of BSP.  If we don't, consider it a
            // catastrophe, especially for the pipeline.
            String sampleName = bspSampleDataEntry.getKey();
            if (sampleData == null) {
                throw new BSPLookupException("BSP returned no data for " + sampleName);
            }
            if (sampleData.getSampleLsid() == null || StringUtils.isBlank(sampleData.getSampleLsid())) {
                throw new BSPLookupException("BSP returned no LSID for " + sampleName);
            }
            if (sampleData.getSampleId() == null || StringUtils.isBlank(sampleData.getSampleId())) {
                throw new BSPLookupException("BSP returned no sample id for " + sampleName);
            }
            lsidToSampleData.put(sampleData.getSampleLsid(), sampleData);
        }

        if (sampleNames.size() != sampleDataByName.size()) {
            throw new BSPLookupException("BSP search for " + sampleNames.size() + " samples returned " + sampleDataByName.size() + " results!");
        }

        return lsidToSampleData;
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
