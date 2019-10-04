package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import com.google.common.collect.Multimap;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGetExportedSamplesFromAliquots;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintResource;
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
import java.util.Collection;
import java.util.Collections;
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
    private BSPGetExportedSamplesFromAliquots bspGetExportedSamplesFromAliquots;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private FingerprintEjb fingerprintEjb;



    private String sampleId;
    private String participantId;
    private boolean showLayout = false;
    private List<Fingerprint> fingerprints = new ArrayList<>();
    private Map<String, String> mapLodScoreToFingerprint = new HashMap<>();
    private Set<String> platforms;


    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent("search")
    public Resolution search() {

//        if (sampleId == null && participantId == null) {
//            addGlobalValidationError("You must input a valid search term.");
//            return new ForwardResolution(VIEW_PAGE);
//        } else
        if (StringUtils.isNotBlank(sampleId)
            && mercurySampleDao.findBySampleKeys(Arrays.asList(sampleId.split("\\s+"))) == null) {
            addGlobalValidationError("There were no matching Sample Ids for " + "'" + sampleId + "'.");
//              TODO validate PT-ID
//            }else if(StringUtils.isNotBlank(participantId) && ){
//                addGlobalValidationError("There were no matching items for " + "'" + participantId + "'.");
        } else {
            showLayout = true;
            displayFingerprints();
        }

        return new ForwardResolution(VIEW_PAGE);

    }

    @HandlesEvent("downloadMatrix")
    public Resolution downloadMatrix() throws IOException {

        displayFingerprints();

//TODO update with enum
        Workbook workbook = fingerprintEjb.makeMatrix(fingerprints, platforms);

        String filename = "";
        if (sampleId != null) {
            filename = sampleId.substring(0, 8) + "_" + formatDate(new Date()) + "_FP_MATRIX" + ".xls";
        } else if (participantId != null) {
            filename = participantId + "_" + formatDate(new Date()) + "_FP_MATRIX" + ".xls";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        StreamingResolution stream = new StreamingResolution(StreamCreatedSpreadsheetUtil.XLS_MIME_TYPE,
                new ByteArrayInputStream(out.toByteArray()));
        stream.setFilename(filename);

        return stream;
    }


    public void displayFingerprints() {
        Map<String, MercurySample> mapIdToMercurySample = new HashMap<>();
        if (StringUtils.isNotBlank(sampleId)) {
            mapIdToMercurySample =
                    mercurySampleDao.findMapIdToMercurySample(Arrays.asList(sampleId.split("\\s+")));
//            TODO search by PT-ID
        } else if (StringUtils.isNotBlank(participantId)) {

        }

       fingerprints = fingerprintEjb.findFingerints(mapIdToMercurySample);

        Collections.sort(fingerprints);
    }



    public String formatDate(Date date) {
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

    public boolean isShowLayout() {
        return showLayout;
    }

    public void setShowLayout(boolean showLayout) {
        this.showLayout = showLayout;
    }

    public List<Fingerprint> getFingerprints() {
        return fingerprints;
    }

    public Map<String, String> getMapLodScoreToFingerprint() {
        return mapLodScoreToFingerprint;
    }

    public Set<String> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(Set<String> platforms) {
        this.platforms = platforms;
    }
}
