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
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGetExportedSamplesFromAliquots;
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
import org.h2.value.Value;
import org.jetbrains.annotations.NotNull;
import org.xmlsoap.schemas.soap.encoding.Int;


import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;


@UrlBinding(value = FingerprintReportActionBean.ACTIONBEAN_URL_BININDING)
public class FingerprintReportActionBean extends CoreActionBean {

    public static final String ACTIONBEAN_URL_BININDING = "/sample/fingerprint_report.action";
    private static final String VIEW_PAGE = "/sample/fingerprint_report.jsp";

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private BSPGetExportedSamplesFromAliquots bspGetExportedSamplesFromAliquots;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private FingerprintEjb fingerprintEjb;


    private String sampleId;
    private String participantId;
    private String pdoId;
    private boolean showLayout = false;
    private List<Fingerprint> fingerprints = new ArrayList<>();
    private Map<String, String> mapLodScoreToFingerprint = new HashMap<>();


    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent("search")
    public Resolution search() {

        if (sampleId == null && pdoId == null && participantId == null) {
            addGlobalValidationError("You must input a valid search term.");
            return new ForwardResolution(VIEW_PAGE);
        } else if (StringUtils.isNotBlank(sampleId)
                   && mercurySampleDao.findBySampleKeys(Arrays.asList(sampleId.toUpperCase().split("\\s+"))) == null) {
            addGlobalValidationError("There were no matching Sample Ids for " + "'" + sampleId + "'.");
        } else if (StringUtils.isNotBlank(pdoId) && productOrderDao.findByBusinessKey(pdoId.toUpperCase()) == null) {
            addGlobalValidationError("There were no matching items for " + "'" + pdoId + "'.");
//              TODO validate PT-ID
//            }else if(StringUtils.isNotBlank(participantId) && ){
//                addGlobalValidationError("There were no matching items for " + "'" + participantId + "'.");
        } else {
            showLayout = true;
            displayFingerprints();
        }

        return new ForwardResolution(VIEW_PAGE);

    }

    @HandlesEvent("downloadReport")
    public Resolution downloadRepor() throws IOException {

        displayFingerprints();

        Workbook workbook = makeSpreadsheet(fingerprints);

        String filename = "";
        if (sampleId != null) {
            filename = sampleId.substring(0, 8) + "_" + formatDate(new Date()) + "_FP_REPORT" + ".xlsx";
        } else if (pdoId != null) {
            filename = pdoId + "_" + formatDate(new Date()) + "_FP_REPORT" + ".xlsx";
        } else if (participantId != null) {
            filename = participantId + "_" + formatDate(new Date()) + "_FP_REPORT" + ".xlsx";
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
                    mercurySampleDao.findMapIdToMercurySample(Arrays.asList(sampleId.toUpperCase().split("\\s+")));
        } else if (StringUtils.isNotBlank(pdoId)) {
            ProductOrder productOrder = null;
            productOrder = productOrderDao.findByBusinessKey(pdoId.toUpperCase());
            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                mapIdToMercurySample.put(productOrderSample.getSampleKey(), productOrderSample.getMercurySample());
            }
//            TODO search by PT-ID
        } else if (StringUtils.isNotBlank(participantId)) {

        }

        fingerprints = fingerprintEjb.findFingerints(mapIdToMercurySample);

        Collections.sort(fingerprints);
    }


//    public Workbook makeSpreadsheet(List<Fingerprint> fingerprints) {
//        Map<String, Object[][]> sheets = new HashMap<>();
//
//
//        int numFingerprintCells = fingerprints.size() +1;
//
//
//        String[][] fingerprintCells = new String[numFingerprintCells][];
//        String[][] genoArray = new String[numFingerprintCells][1000];
//
//        int rowIndex = 0;
//        int colIndex = 0;
//
//        fingerprintCells[rowIndex] =
//                new String[]{"Participant Id", "Root Sample", "Fingerprint Aliquot", "Date", "Platform", "Pass/Fail",
//                        "LOD Score", "Genotypes"};
//        ++rowIndex;
//
//
//        for (Fingerprint fingerprint : fingerprints) {
//            //Build genotype string
//            String genotype = "";
//                for (FpGenotype geno : fingerprint.getFpGenotypesOrdered()) {
//                    if (geno != null) {
//                        genotype = genotype + geno.getGenotype();
//                        genoArray[rowIndex][colIndex] = geno.getGenotype();
//                        colIndex++;
//                    }
//                }
//
//
//            String lodScoreStr = findLodScore(fingerprint);
//            genoArray=Arrays.stream(genoArray)
//                    .filter(Objects::nonNull)
//                    .toArray(String[][]::new);
//
//
//            fingerprintCells[rowIndex] =
//                    new String[]{fingerprint.getMercurySample().getSampleData().getPatientId(),
//                            fingerprint.getMercurySample().getSampleData().getRootSample(),
//                            fingerprint.getMercurySample().getSampleKey(),
//                            formatDate(fingerprint.getDateGenerated()), fingerprint.getPlatform().name(),
//                            fingerprint.getDisposition().name(), lodScoreStr, /*genotype,*/ Arrays.toString(genoArray[rowIndex])};
//            ++rowIndex;
//
//        }
//
//        String[] sheetNames = {"Fingerprints"};
//
//        sheets.put(sheetNames[0], fingerprintCells);
//
//        return SpreadsheetCreator.createSpreadsheet(sheets);
//    }


    public Workbook makeSpreadsheet(List<Fingerprint> fingerprints) {
        Map<String, Object[][]> sheets = new HashMap<>();
        Map<Integer, String> mapRsidToColumn = new HashMap<>();
        Map<String, String> mapSnpToColumn = new HashMap<>();

        int numFingerprintCells = fingerprints.size() + 1;
        int rowIndex = 0;
        int rsIdIndex = 0;

        String[][] fingerprintCells = new String[numFingerprintCells][];

        for (Fingerprint fingerprint : fingerprints) {

            for (FpGenotype geno : fingerprint.getFpGenotypesOrdered()) {
                if (geno != null) {
                    String rsId = geno.getSnp().getRsId();
                    mapRsidToColumn.put(rsIdIndex, rsId);
                    ++rsIdIndex;
                }
            }
        }

        Map<Integer, String> sortLabelsByRsId = mapRsidToColumn.entrySet().stream()
                .sorted(Map.Entry.<Integer, String>comparingByValue())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

        fingerprintCells[rowIndex] =
                new String[]{"Participant Id", "Root Sample", "Fingerprint Aliquot", "Date", "Platform", "Pass/Fail",
                        "LOD Score"};
        Set<String> sortedRsidSet = new LinkedHashSet<>(sortLabelsByRsId.values());
        Object[] array1 = sortedRsidSet.toArray();
        String[] sortedRsidArray = (String[]) sortedRsidSet.toArray(new String[0]);

        fingerprintCells[rowIndex] = ArrayUtils.addAll(fingerprintCells[rowIndex], sortedRsidArray);

        rowIndex++;

        for (Fingerprint fingerprint : fingerprints) {
            String lodScoreStr = findLodScore(fingerprint);
            for (FpGenotype geno : fingerprint.getFpGenotypesOrdered()) {
                if (geno != null) {
                    String snp = geno.getGenotype();
                    String rsId = geno.getSnp().getRsId();
                    mapSnpToColumn.put(rsId, snp);
                    ++rsIdIndex;
                }
            }

            Map<String, String> sortedByRsId = mapSnpToColumn.entrySet().stream()
                    .sorted(Map.Entry.<String, String>comparingByKey())
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

            fingerprintCells[rowIndex] =
                    new String[]{fingerprint.getMercurySample().getSampleData().getPatientId(),
                            fingerprint.getMercurySample().getSampleData().getRootSample(),
                            fingerprint.getMercurySample().getSampleKey(),
                            formatDate(fingerprint.getDateGenerated()), fingerprint.getPlatform().name(),
                            fingerprint.getDisposition().name(), lodScoreStr};

            List<String> sortedSnpList = new ArrayList<>(sortedByRsId.values());
            Object[] array3 = sortedSnpList.toArray();
            String[] sortedSnpArray = (String[]) sortedSnpList.toArray(new String[0]);

            fingerprintCells[rowIndex] = ArrayUtils.addAll(fingerprintCells[rowIndex], sortedSnpArray);
            ++rowIndex;
        }

        String[] sheetNames = {"Fingerprints"};
        sheets.put(sheetNames[0], fingerprintCells);

        return SpreadsheetCreator.createSpreadsheet(sheets);
    }


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

        ConcordanceCalculator concordanceCalculator = new ConcordanceCalculator();
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

    public Map<String, String> getMapLodScoreToFingerprint() {
        return mapLodScoreToFingerprint;
    }
}
