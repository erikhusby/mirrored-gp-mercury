package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.run.FingerprintEjb;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@UrlBinding(value = FingerprintMatrixActionBean.ACTIONBEAN_URL_BININDING)
public class FingerprintMatrixActionBean extends CoreActionBean {

    public static final String ACTIONBEAN_URL_BININDING = "/sample/fingerprint_matrix.action";
    private static final String VIEW_PAGE = "/sample/fingerprint_matrix.jsp";


    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private FingerprintEjb fingerprintEjb;


    private String sampleId;
    private String participantId;
    private List<Fingerprint> fingerprints = new ArrayList<>();
    private Map<String, String> mapLodScoreToFingerprint = new HashMap<>();
    private Set<Fingerprint.Platform> platforms;
    private Map<String, MercurySample> mapSmidToMercurySample = new HashMap<>();
    private List<String> sampleSmidList = new ArrayList<>();
    private Map<String, MercurySample> participantSmidList = new HashMap<>();


    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * Download .xlsx file containing pairwise sample matrix with lod scores
     */
    @HandlesEvent("downloadMatrix")
    public Resolution downloadMatrix() throws IOException {
        int smidLimit = 50;

        if (sampleId == null && participantId == null) {
            addGlobalValidationError("You must input a valid search term.");
            return new ForwardResolution(VIEW_PAGE);
        } else if (StringUtils.isNotBlank(sampleId)
                   && mercurySampleDao.findBySampleKeys(Arrays.asList(sampleId.split("\\s+"))).size() == 0) {
            addGlobalValidationError("There were no matching Sample Ids for " + "'" + sampleId + "'.");
            return new ForwardResolution(VIEW_PAGE);
        } else if (StringUtils.isNotBlank(participantId) && fingerprintEjb.getPtIdMercurySamples(mapSmidToMercurySample,
                participantId, mercurySampleDao).size() == 0) {
            addGlobalValidationError("There were no matching items for " + "'" + participantId + "'.");
            return new ForwardResolution(VIEW_PAGE);
        } else {
            searchFingerprints();
            if (mapSmidToMercurySample.size() > smidLimit) {
                addGlobalValidationError("Over " + smidLimit + " SM-IDs have been selected");
                return new ForwardResolution(VIEW_PAGE);
            }
        }

        Workbook workbook = fingerprintEjb.makeMatrix(fingerprints, platforms);

        String filename = "";
        if (sampleId != null) {
            filename = sampleId.substring(0, 8) + "_" + formatDate(new Date()) + "_FP_MATRIX" + ".xlsx";
        } else if (participantId != null) {
            filename = participantId.substring(0, 8) + "_" + formatDate(new Date()) + "_FP_MATRIX" + ".xlsx";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        StreamingResolution stream = new StreamingResolution(StreamCreatedSpreadsheetUtil.XLS_MIME_TYPE,
                new ByteArrayInputStream(out.toByteArray()));
        stream.setFilename(filename);

        return stream;
    }

    /**
     * Use search input to retrieve fingerprints for sm-ids
     */
    private void searchFingerprints() {
        if (StringUtils.isNotBlank(sampleId)) {
            mapSmidToMercurySample =
                    mercurySampleDao.findMapIdToMercurySample(Arrays.asList(sampleId.split("\\s+")));
        } else if (StringUtils.isNotBlank(participantId)) {
            mapSmidToMercurySample =
                    fingerprintEjb.getPtIdMercurySamples(mapSmidToMercurySample, participantId, mercurySampleDao);
        }

        fingerprints = fingerprintEjb.findFingerprints(mapSmidToMercurySample);
        fingerprints.sort(new Fingerprint.OrderFpPtidRootSamp());
    }


    public String formatDate(Date date) {
        return DateUtils.getDate(date);
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId.toUpperCase();
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId.toUpperCase();
    }

    public List<Fingerprint> getFingerprints() {
        return fingerprints;
    }

    public Map<String, String> getMapLodScoreToFingerprint() {
        return mapLodScoreToFingerprint;
    }

    public Set<Fingerprint.Platform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(Set<Fingerprint.Platform> platforms) {
        this.platforms = platforms;
    }
}
