package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import clover.org.apache.commons.lang3.ArrayUtils;
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
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.run.ConcordanceCalculator;
import org.broadinstitute.gpinformatics.mercury.control.run.FingerprintEjb;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.FpGenotype;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;


import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


@UrlBinding(value = FingerprintReportActionBean.ACTIONBEAN_URL_BININDING)
public class FingerprintReportActionBean extends CoreActionBean {

    public static final String ACTIONBEAN_URL_BININDING = "/sample/fingerprint_report.action";
    private static final String VIEW_PAGE = "/sample/fingerprint_report.jsp";

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private FingerprintEjb fingerprintEjb;

    @Inject
    private ConcordanceCalculator concordanceCalculator;


    private String sampleId;
    private String participantId;
    private String pdoId;
    private boolean showLayout = false;
    private List<Fingerprint> fingerprints = new ArrayList<>();
    private Map<String, String> mapLodScoreToFingerprint = new HashMap<>();
    private Map<String, MercurySample> mapSmidToMercurySample = new HashMap<>();



    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    /** Validate search input and returns page with sample details and genotype */
    @HandlesEvent("search")
    public Resolution search() {

        if (sampleId == null && pdoId == null && participantId == null) {
            addGlobalValidationError("You must input a valid search term.");
        } else if (StringUtils.isNotBlank(sampleId)
                   && mercurySampleDao.findBySampleKeys(Arrays.asList(sampleId.split("\\s+"))).size() == 0) {
            addGlobalValidationError("There were no matching Sample Ids for " + "'" + sampleId + "'.");
        } else if (StringUtils.isNotBlank(pdoId) && productOrderDao.findByBusinessKey(pdoId) == null) {
            addGlobalValidationError("There were no matching items for " + "'" + pdoId + "'.");
        } else if (StringUtils.isNotBlank(participantId) && fingerprintEjb.getPtIdMercurySamples(mapSmidToMercurySample,
                participantId, mercurySampleDao).size() == 0) {
            addGlobalValidationError("There were no matching items for " + "'" + participantId + "'.");
        } else {
            showLayout = true;
            searchFingerprints();
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    /** Download .xlsx file containing sample details with genotype broken down into snps by rsid */
    @HandlesEvent("downloadReport")
    public Resolution downloadReport() throws IOException {

        searchFingerprints();
        Workbook workbook = makeSpreadsheet(fingerprints);

        String filename = "";
        if (sampleId != null) {
            filename = sampleId.substring(0, 8) + "_" + formatDate(new Date()) + "_FP_REPORT" + ".xlsx";
        } else if (pdoId != null) {
            filename = pdoId + "_" + formatDate(new Date()) + "_FP_REPORT" + ".xlsx";
        } else if (participantId != null) {
            filename = participantId.substring(0, 8) + "_" + formatDate(new Date()) + "_FP_REPORT" + ".xlsx";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        StreamingResolution stream = new StreamingResolution(StreamCreatedSpreadsheetUtil.XLS_MIME_TYPE,
                new ByteArrayInputStream(out.toByteArray()));
        stream.setFilename(filename);

        return stream;
    }

    /** Use search input to retrieve fingerprints for sm-ids */
    private void searchFingerprints() {
        if (StringUtils.isNotBlank(sampleId)) {
            mapSmidToMercurySample =
                    mercurySampleDao.findMapIdToMercurySample(Arrays.asList(sampleId.split("\\s+")));
        } else if (StringUtils.isNotBlank(pdoId)) {
            ProductOrder productOrder;
            productOrder = productOrderDao.findByBusinessKey(pdoId);
            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                mapSmidToMercurySample.put(productOrderSample.getSampleKey(), productOrderSample.getMercurySample());
            }
        } else {
            mapSmidToMercurySample =
                    fingerprintEjb
                            .getPtIdMercurySamples(mapSmidToMercurySample, participantId, mercurySampleDao);
        }

        fingerprints = fingerprintEjb.findFingerprints(mapSmidToMercurySample);
        fingerprints.sort(new Fingerprint.OrderFpPtidRootSamp());
    }

    /** Create spreadsheet using list of fingerprints
     * @param fingerprints list of sorted fingerprints
     * @return spreadsheet with fingerprint sample details and genotype broken down into snps by rsid
     */
    private Workbook makeSpreadsheet(List<Fingerprint> fingerprints) {
        Map<String, Object[][]> sheets = new HashMap<>();
        Set<String> rsIds = new LinkedHashSet<>();

        int numFingerprintCells = fingerprints.size() + 1;
        int rowIndex = 0;

        String[][] fingerprintCells = new String[numFingerprintCells][];

        for (Fingerprint fingerprint : fingerprints) {
            for (FpGenotype geno : fingerprint.getFpGenotypesOrdered()) {
                if (geno != null && fingerprint.getPlatform() == Fingerprint.Platform.FLUIDIGM) {
                    String rsId = geno.getSnp().getRsId();
                    rsIds.add(rsId);
                }
            }
        }

        fingerprintCells[rowIndex] =
                new String[]{"Participant Id", "Root Sample", "Fingerprint Aliquot", "Date", "Platform", "Pass/Fail",
                        "LOD Score"};
        String[] rsIdArray = rsIds.toArray(new String[0]);

        fingerprintCells[rowIndex] = ArrayUtils.addAll(fingerprintCells[rowIndex], rsIdArray);

        rowIndex++;

        startConcCalc();
        List<String> rsIdsList = new ArrayList<>(rsIds);
        for (Fingerprint fingerprint : fingerprints) {
            String[] snps = new String[rsIdsList.size()];
            String lodScoreStr = findLodScore(fingerprint);
            for (FpGenotype geno : fingerprint.getFpGenotypesOrdered()) {
                if (geno != null) {
                    String snp = geno.getGenotype();
                    String rsId = geno.getSnp().getRsId();
                    if (rsIds.contains(rsId)) {
                        int indexOf = rsIdsList.indexOf(rsId);
                        snps[indexOf] = snp;
                    }
                }
            }

            fingerprintCells[rowIndex] =
                    new String[]{fingerprint.getMercurySample().getSampleData().getPatientId(),
                            fingerprint.getMercurySample().getSampleData().getRootSample(),
                            fingerprint.getMercurySample().getSampleKey(),
                            formatDate(fingerprint.getDateGenerated()), fingerprint.getPlatform().name(),
                            fingerprint.getDisposition().name(), lodScoreStr};

            fingerprintCells[rowIndex] = ArrayUtils.addAll(fingerprintCells[rowIndex], snps);
            ++rowIndex;
        }
        endConcCalc();

        String[] sheetNames = {"Fingerprints"};
        sheets.put(sheetNames[0], fingerprintCells);

        return SpreadsheetCreator.createSpreadsheet(sheets);
    }

    /** Calculate lod score using oldest fluidigm fingerprint if available
     * @param fingerprint used to determine anchor fp and compare against to find lod score
     * @return "N/A" if fail, "Anchor FP" if oldest fluidigm fingerprint(general array if no fluidigm),
     *         calculated lod score compared to Anchor FP
     */
    public String findLodScore(Fingerprint fingerprint) {
        Optional<Fingerprint> oldFluidigmFp = fingerprints.stream()
                .filter(fp -> fp.getMercurySample().getSampleData().getPatientId()
                        .equals(fingerprint.getMercurySample().getSampleData().getPatientId()))
                .filter(fp -> fp.getPlatform() == Fingerprint.Platform.FLUIDIGM
                              && fp.getDisposition() == Fingerprint.Disposition.PASS)
                .min(Comparator.comparing(Fingerprint::getDateGenerated));

        if (!oldFluidigmFp.isPresent()) {
            oldFluidigmFp = fingerprints.stream()
                    .filter(fp -> fp.getMercurySample().getSampleData().getPatientId()
                            .equals(fingerprint.getMercurySample().getSampleData().getPatientId()))
                    .filter(fp -> fp.getPlatform() == Fingerprint.Platform.GENERAL_ARRAY
                                  && fp.getDisposition() == Fingerprint.Disposition.PASS)
                    .min(Comparator.comparing(Fingerprint::getDateGenerated));
        }

        DecimalFormat df = new DecimalFormat("##.####");
        df.setRoundingMode(RoundingMode.HALF_UP);
        String lodScoreStr = "N/A";

        if (oldFluidigmFp.isPresent() && oldFluidigmFp.get().getMercurySample().getSampleKey()
                .equals(fingerprint.getMercurySample().getSampleKey())
            && fingerprint.getDisposition() == Fingerprint.Disposition.PASS) {
            lodScoreStr = "Anchor FP";
        } else if (oldFluidigmFp.isPresent() && oldFluidigmFp.get().getGender() != null
                   && oldFluidigmFp.get().getDisposition() == Fingerprint.Disposition.PASS
                   && fingerprint.getDisposition() == Fingerprint.Disposition.PASS
                   && fingerprint.getGender() != null) {
            double lodScore = concordanceCalculator.calculateLodScore(fingerprint, oldFluidigmFp.get());
            lodScoreStr = String.valueOf(df.format(lodScore));
        }
        return lodScoreStr;
    }

    public void startConcCalc(){
        ConcordanceCalculator concordanceCalculator = new ConcordanceCalculator();
    }

    public void endConcCalc(){
        concordanceCalculator.done();
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

    public String getPdoId() {
        return pdoId;
    }

    public void setPdoId(String pdoId) {
        this.pdoId = pdoId.toUpperCase();
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
}
