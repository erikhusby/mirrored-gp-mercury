package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import clover.org.apache.commons.lang3.tuple.ImmutablePair;
import clover.org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGetExportedSamplesFromAliquots;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintCallsBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintsBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.RsIdsBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.FingerprintDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.SnpListDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.FpGenotype;
import org.broadinstitute.gpinformatics.mercury.entity.run.Snp;
import org.broadinstitute.gpinformatics.mercury.entity.run.SnpList;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Comparator.reverseOrder;
import static org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintResource.getSmIdFromLsid;

@UrlBinding(value = FingerprintReportActionBean.ACTIONBEAN_URL_BININDING)
public class FingerprintReportActionBean extends CoreActionBean {

    public static final String ACTIONBEAN_URL_BININDING = "/sample/fingerprint_report.action";
    private static final String VIEW_PAGE = "/sample/fingerprint_report.jsp";

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private SnpListDao snpListDao;

    @Inject
    private BSPGetExportedSamplesFromAliquots bspGetExportedSamplesFromAliquots;

    @Inject
    private FingerprintDao fingerprintDao;

    @Inject
    private UserBean userBean;

    @Inject
    private SampleDataFetcher sampleDataFetcher;


    private String sampleId;
    private String participantId;
    private String pdoId;
    private boolean showLayout = false;
    private List<Fingerprint> fingerprints = new ArrayList<>();
    private Collection<MercurySample> mercurySamples;

    @NotNull
    public static String getSmIdFromLsid(String lsid) {
        return "SM-" + lsid.substring(lsid.lastIndexOf(':') + 1);
    }

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent("search")
    public Resolution search() {
        showLayout = true;

        findFingerprints(Arrays.asList(sampleId.split("\\s+")));
        //sampleDataFetcher.fetchSampleDataForSamples(mercurySamples);

        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * The lsids from the pipeline are sequencing aliquots, so we need to find the associated fingerprinting aliquot(s).
     * Prior to this code being deployed to production, all Fluidigm fingerprints are backfilled to their associated
     * MercurySamples.  However, the BSP transfers that made these plates are not in Mercury, so we have to call
     * getExportedSamplesFromAliquots.
     */

    private void findFingerprints(List<String> sampleIds) {

        // Extract SM-IDs from LSIDs
        Map<String, String> mapSmidToQueriedLsid = new HashMap<>();
        List<String> lsids = new ArrayList<>();
        for (String id : sampleIds) {
            String lsid = BSPSampleSearchServiceStub.LSID_PREFIX + id.substring(3);
            lsids.add(lsid);
            mapSmidToQueriedLsid.put(id, lsid);
        }


        // Make list of samples for which to query BSP
        Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(sampleIds);
        List<String> bspLsids = new ArrayList<>();
        Map<String, String> mapSmidToFpLsid = new HashMap<>();
        for (Map.Entry<String, MercurySample> idMercurySampleEntry : mapIdToMercurySample.entrySet()) {
            MercurySample mercurySample = idMercurySampleEntry.getValue();
            if (mercurySample == null || mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                bspLsids.add(mapSmidToQueriedLsid.get(idMercurySampleEntry.getKey()));
            } else {
                mapSmidToFpLsid
                        .put(idMercurySampleEntry.getKey(), mapSmidToQueriedLsid.get(idMercurySampleEntry.getKey()));
            }
        }


        // Query BSP for FP aliquots for given (sequencing) aliquots
        Multimap<String, BSPGetExportedSamplesFromAliquots.ExportedSample> mapLsidToExportedSample =
                ArrayListMultimap.create();
        if (!bspLsids.isEmpty()) {
            List<BSPGetExportedSamplesFromAliquots.ExportedSample> samplesExportedFromBsp =
                    bspGetExportedSamplesFromAliquots
                            .getExportedSamplesFromAliquots(bspLsids, IsExported.ExternalSystem.GAP);
            if (samplesExportedFromBsp.size() > 100) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                samplesExportedFromBsp.sort((o1, o2) -> LocalDate.parse(o2.getExportDate(), formatter).
                        compareTo(LocalDate.parse(o1.getExportDate(), formatter)));
                samplesExportedFromBsp = samplesExportedFromBsp.subList(0, 100);
            }
            Set<String> bspSampleIds = new HashSet<>();
            for (BSPGetExportedSamplesFromAliquots.ExportedSample exportedSample : samplesExportedFromBsp) {
                mapLsidToExportedSample.put(exportedSample.getLsid(), exportedSample);
                String exportedLsid = exportedSample.getExportedLsid();
                String smId = getSmIdFromLsid(exportedLsid);
                bspSampleIds.add(smId);
                mapSmidToFpLsid.put(smId, exportedLsid);
            }
            mapIdToMercurySample.putAll(mercurySampleDao.findMapIdToMercurySample(bspSampleIds));
        }

        // Map queried LSIDs to MercurySamples that may have fingerprints
        Multimap<String, MercurySample> mapLsidToFpSamples = HashMultimap.create();
        for (String lsid : lsids) {
            MercurySample mercurySample = mapIdToMercurySample.get(getSmIdFromLsid(lsid));
            if (mercurySample == null) {
                throw new ResourceException("Sample not found: " + lsid, Response.Status.BAD_REQUEST);
            }
            mapLsidToFpSamples.put(lsid, mercurySample);
            for (BSPGetExportedSamplesFromAliquots.ExportedSample exportedSample : mapLsidToExportedSample.get(lsid)) {
                mapLsidToFpSamples
                        .put(lsid, mapIdToMercurySample.get(getSmIdFromLsid(exportedSample.getExportedLsid())));
            }
        }
        mercurySamples = mapLsidToFpSamples.values();
        for (MercurySample mercurySample : mercurySamples) {
            for (Fingerprint fingerprint : mercurySample.getFingerprints()) {
                fingerprints.add(fingerprint);
            }

        }

        Map<String, SampleData> mapIdToData = sampleDataFetcher.fetchSampleDataForSamples(mercurySamples, BSPSampleSearchColumn.PARTICIPANT_ID,
                BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, BSPSampleSearchColumn.ROOT_SAMPLE);
        for (MercurySample mercurySample : mercurySamples) {
            mercurySample.setSampleData(mapIdToData.get(mercurySample.getSampleKey()));
        }

    }

    public String FormatDate (Date date){
       return DateUtils.getDate(date);
    }



    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getPdoId() {
        return pdoId;
    }

    public void setPdoId(String pdoId) {
        this.pdoId = pdoId;
    }

    public boolean isShowLayout() {
        return showLayout;
    }

    public void setShowLayout(boolean showLayout) {
        this.showLayout = showLayout;
    }

    public List<Fingerprint> getFingerprints() {
        return fingerprints;
    }




}
