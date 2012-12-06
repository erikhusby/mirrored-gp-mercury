package org.broadinstitute.gpinformatics.mercury.boundary.zims;


import edu.mit.broad.prodinfo.thrift.lims.*;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.zims.SquidThriftLibraryConverter;
import org.broadinstitute.gpinformatics.mercury.control.zims.ThriftLibraryConverter;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPLSIDUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;

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
            @QueryParam("runName") String runName)
    {
        if (runName == null) {
            throw new NullPointerException("runName cannot be null");
        }

       ZimsIlluminaRun runBean = getRun(thriftService,runName);

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
                           Map<String,BSPSampleDTO> lsidToBSPSample,
                           ThriftLibraryConverter thriftLibConverter) {
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
                BSPSampleDTO bspDTO = lsidToBSPSample.get(zamboniLibrary.getLsid());
                ProductOrder pdo = null;
                if (zamboniLibrary.getPdoKey() != null) {
                    pdo = pdoDao.findSingle(ProductOrder.class,ProductOrder_.jiraTicketKey,zamboniLibrary.getPdoKey());
                }
                libraries.add(thriftLibConverter.convertLibrary(zamboniLibrary,bspDTO,pdo));
            }
            //TODO SGM:  pull lane library name from tZamboniLane
            runBean.addLane(new ZimsIlluminaChamber(tZamboniLane.getLaneNumber(), libraries, tZamboniLane.getPrimer(), tZamboniLane.getSequencedLibraryName()));
        }
        return runBean;
    }

    ZimsIlluminaRun getRun(ThriftService thriftService,
                           String runName) {
        ZimsIlluminaRun runBean = null;
        final TZamboniRun tRun = thriftService.fetchRun(runName);
        if (tRun == null) {
            throw new RuntimeException("Could not load run " + runName);
        }
        else {
            final Map<String,BSPSampleDTO> lsidToBSPSample = fetchAllBSPDataAtOnce(tRun);
            runBean = getRun(tRun,lsidToBSPSample,new SquidThriftLibraryConverter());
        }

        return runBean;
    }

    /**
     * Fetches all BSP data for the run in one shot,
     * returning a Map from the {@link org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO#getSampleLsid()} to the
     * {@link BSPSampleDTO}.
     * @param run
     * @return
     */
    private Map<String,BSPSampleDTO> fetchAllBSPDataAtOnce(TZamboniRun run) {
        Set<String> sampleLsids = new HashSet<String>();
        Set<String> sampleNames = new HashSet<String>();
        Map<String,BSPSampleDTO> lsidToBspDto = new HashMap<String, BSPSampleDTO>();
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
        for (BSPSampleDTO bspSampleDTO : sampleToBspDto.values()) {
            if (bspSampleDTO.getSampleLsid() != null) {
                lsidToBspDto.put(bspSampleDTO.getSampleLsid(),bspSampleDTO);
            }
        }

        return lsidToBspDto;
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
