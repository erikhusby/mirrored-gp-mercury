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
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGetExportedSamplesFromAliquots;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.FingerprintDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.SnpListDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.FpGenotype;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;


import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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

    @Inject
    private ProductOrderDao productOrderDao;


    private String sampleId;
    private String participantId;
    private String pdoId;
    private boolean showLayout = false;
    private List<Fingerprint> fingerprints = new ArrayList<>();
    private Collection<MercurySample> mercurySamples;
    private Workbook workbook;


    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent("search")
    public Resolution search() throws IOException {
        showLayout = true;

        if (sampleId == null && pdoId == null && participantId == null) {
            addGlobalValidationError("You must input a valid search term.");
            return new ForwardResolution(VIEW_PAGE);
        } else {
            displayFingerprints();
        }

        return new ForwardResolution(VIEW_PAGE);

    }

    @HandlesEvent("download")
    public Resolution download() throws IOException {

        displayFingerprints();

        workbook = makeSpreadsheet(fingerprints);

        String filename = FormatDate(new Date()) + "_FP_REPORT_" + ".xls";

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
        } else if (StringUtils.isNotBlank(pdoId)) {
            ProductOrder productOrder = null;
            productOrder = productOrderDao.findByBusinessKey(pdoId);
            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                mapIdToMercurySample.put(productOrderSample.getSampleKey(), productOrderSample.getMercurySample());
            }

        } else if (StringUtils.isNotBlank(participantId)) {

        }

        findFingerints(mapIdToMercurySample);

        Collections.sort(fingerprints);
    }

    private void findFingerints(Map<String, MercurySample> mapIdToMercurySample) {
        Map<String, String> mapSmidToQueriedLsid = new HashMap<>();
        List<String> lsids = new ArrayList<>();
        for (String id : mapIdToMercurySample.keySet()) {
            String lsid = BSPSampleSearchServiceStub.LSID_PREFIX + id.substring(3);
            lsids.add(lsid);
            mapSmidToQueriedLsid.put(id, lsid);
        }

        // Make list of samples for which to query BSP
        List<String> bspLsids = new ArrayList<>();
        Map<String, String> mapSmidToFpLsid =
                FingerprintResource.getSmidToFpLsidMap(mapSmidToQueriedLsid, mapIdToMercurySample, bspLsids);

        //Query BSP for FP aliquots for given (sequencing) aliquots and map queried LSIDs to MercurySamples that may have fingerprints
        Multimap<String, MercurySample> mapLsidToFpSamples =
                FingerprintResource.getMercurySampleMultimap(lsids, mapIdToMercurySample, bspLsids, mapSmidToFpLsid,
                        bspGetExportedSamplesFromAliquots, mercurySampleDao);

        //
        mercurySamples = mapLsidToFpSamples.values();
        for (MercurySample mercurySample : mercurySamples) {
            fingerprints.addAll(mercurySample.getFingerprints());
        }

        //
        Map<String, SampleData> mapIdToData =
                sampleDataFetcher.fetchSampleDataForSamples(mercurySamples,
                        BSPSampleSearchColumn.PARTICIPANT_ID, BSPSampleSearchColumn.ROOT_SAMPLE);
        for (MercurySample mercurySample : mercurySamples) {
            mercurySample.setSampleData(mapIdToData.get(mercurySample.getSampleKey()));
        }


    }

    public Workbook makeSpreadsheet(List<Fingerprint> fingerprints) {
        Map<String, Object[][]> sheets = new HashMap<>();


        int numFingerprintCells = 0;
        for (Fingerprint fingerprint : fingerprints) {
            numFingerprintCells++;
        }
        String[][] fingerprintCells = new String[numFingerprintCells + 1][];

        int rowIndex = 0;

        fingerprintCells[rowIndex] =
                new String[]{"Participant Id", "Root Sample", "Fingerprint Aliquot", "Date", "Platform", "Pass/Fail",
                        "Genotypes", "LOD Score"};
        ++rowIndex;

        for (Fingerprint fingerprint : fingerprints) {
            String genotype = "";
            for (FpGenotype geno : fingerprint.getFpGenotypesOrdered() ) {
                if (geno != null) {
                    genotype = genotype + geno.getGenotype();
                }
            }
            fingerprintCells[rowIndex] = new String[]{fingerprint.getMercurySample().getSampleData().getPatientId(),
                    fingerprint.getMercurySample().getSampleData().getRootSample(),
                    fingerprint.getMercurySample().getSampleKey(),
                    FormatDate(fingerprint.getDateGenerated()), fingerprint.getPlatform().name(),
                    fingerprint.getDisposition().name(), genotype};
            ++rowIndex;
        }

        String[] sheetNames = {"Fingerprints"};

        sheets.put(sheetNames[0], fingerprintCells);

        return SpreadsheetCreator.createSpreadsheet(sheets);
    }


    public String FormatDate(Date date) {
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
