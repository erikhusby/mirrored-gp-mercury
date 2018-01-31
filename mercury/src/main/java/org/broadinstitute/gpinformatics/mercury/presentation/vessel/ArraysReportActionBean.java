package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.ArraysSummaryFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.SampleSheetFactory;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stripes action bean that allows user to download a Samplesheet.csv or Summary.txt file for arrays.
 */
@UrlBinding(value = "/vessel/ArraysReport.action")
public class ArraysReportActionBean extends CoreActionBean {
    private static final String CREATE_PAGE = "/vessel/arrays_report.jsp";
    private static final String DOWNLOAD_ACTION = "download";

    /** Extract barcode and position from e.g. 3999595020_R12C02 */
    private static final Pattern BARCODE_PATTERN = Pattern.compile("(\\d*)_(R\\d*C\\d*)");

    /** POSTed from form. */
    private ArraysReportActionBean.Report report;

    /** POSTed from form. */
    private boolean includeScannerName;

    /** POSTed from form. */
    private String pdoBusinessKeys;

    /** POSTed from form. */
    private String chipWellBarcodes;

    @Inject
    private SampleSheetFactory sampleSheetFactory;

    @Inject
    private ArraysSummaryFactory arraysSummaryFactory;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private LabVesselDao labVesselDao;

    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(CREATE_PAGE);
    }

    public enum Report {
        SAMPLE_SHEET("Samplesheet.csv"),
        SUMMARY("Summary.txt");

        private final String displayName;

        Report(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @HandlesEvent(DOWNLOAD_ACTION)
    public Resolution download() {
        final List<Pair<LabVessel, VesselPosition>> vesselPositionPairs = new ArrayList<>();

        ProductOrder firstProductOrder = null;
        if (pdoBusinessKeys != null) {
            String[] pdoKeys = pdoBusinessKeys.trim().split("\\s+");
            for (String pdoKey : pdoKeys) {
                ProductOrder productOrder = productOrderDao.findByBusinessKey(pdoKey);
                if( productOrder == null ) {
                    addMessage( "PDO " + pdoKey + " not found");
                    continue;
                }
                vesselPositionPairs.addAll(SampleSheetFactory.loadByPdo(productOrder));
                if (firstProductOrder == null) {
                    firstProductOrder = productOrder;
                }
            }
        }

        if (chipWellBarcodes != null) {
            String[] chipWellBarcodeArray = chipWellBarcodes.trim().split("\\s+");
            if (chipWellBarcodeArray.length > 0) {
                List<String> barcodes = new ArrayList<>();
                for (String chipWellBarcode : chipWellBarcodeArray) {
                    Matcher matcher = BARCODE_PATTERN.matcher(chipWellBarcode);
                    if (matcher.matches()) {
                        String chipBarcode = matcher.group(1);
                        barcodes.add(chipBarcode);
                    } else {
                        addValidationError("chipWellBarcodes", "Barcode " + chipWellBarcode +
                                " is not of expected format");
                    }
                }
                Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);

                for (String chipWellBarcode : chipWellBarcodeArray) {
                    Matcher matcher = BARCODE_PATTERN.matcher(chipWellBarcode);
                    if (matcher.matches()) {
                        String chipBarcode = matcher.group(1);
                        VesselPosition vesselPosition = null;
                        try {
                            vesselPosition = VesselPosition.valueOf(matcher.group(2));
                        } catch (IllegalArgumentException e) {
                            addValidationError("chipWellBarcodes", "Barcode " + chipWellBarcode +
                                    " has incorrect position");
                        }
                        LabVessel labVessel = mapBarcodeToVessel.get(chipBarcode);
                        if (labVessel == null) {
                            addValidationError("chipWellBarcodes", "Barcode " + chipWellBarcode +
                                    " not found");
                        }
                        vesselPositionPairs.add(new ImmutablePair<>(labVessel, vesselPosition));
                        if (firstProductOrder == null && !hasErrors()) {
                            Set<SampleInstanceV2> sampleInstances =
                                    labVessel.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition);
                            ProductOrderSample productOrderSample = null;
                            if( !sampleInstances.isEmpty() ) {
                                productOrderSample = sampleInstances.iterator().next().getProductOrderSampleForSingleBucket();
                            }
                            if (productOrderSample != null) {
                                firstProductOrder = productOrderSample.getProductOrder();
                            }
                        }
                    }
                }
            }
        }
        if (vesselPositionPairs.isEmpty()) {
            addGlobalValidationError("No chips found.");
        }
        if ( pdoBusinessKeys != null && firstProductOrder == null && !hasErrors()) {
            addGlobalValidationError("No product orders found.");
        }

        if (hasErrors()) {
            return new ForwardResolution(CREATE_PAGE);
        } else {
            final ProductOrder finalFirstProductOrder = firstProductOrder;
            return new StreamingResolution("text/plain") {
                @Override
                public void stream(HttpServletResponse response) throws Exception {
                    ServletOutputStream out = response.getOutputStream();
                    switch (report) {
                        case SAMPLE_SHEET:
                            sampleSheetFactory.write(new PrintStream(out), vesselPositionPairs, finalFirstProductOrder);
                            break;
                        case SUMMARY:
                            arraysSummaryFactory.write(new PrintStream(out), vesselPositionPairs,
                                    finalFirstProductOrder, includeScannerName);
                            break;
                        default:
                            throw new RuntimeException("Unexpected report " + report);
                    }
                }
            }.setFilename(report.getDisplayName());
        }
    }

    @SuppressWarnings("unused")
    public String getPdoBusinessKeys() {
        return pdoBusinessKeys;
    }

    @SuppressWarnings("unused")
    public void setPdoBusinessKeys(String pdoBusinessKeys) {
        this.pdoBusinessKeys = pdoBusinessKeys;
    }

    @SuppressWarnings("unused")
    public String getChipWellBarcodes() {
        return chipWellBarcodes;
    }

    @SuppressWarnings("unused")
    public void setChipWellBarcodes(String chipWellBarcodes) {
        this.chipWellBarcodes = chipWellBarcodes;
    }

    public ArraysReportActionBean.Report getReport() {
        return report;
    }

    public void setReport(ArraysReportActionBean.Report report) {
        this.report = report;
    }

    @SuppressWarnings("unused")
    public void setIncludeScannerName(boolean includeScannerName) {
        this.includeScannerName = includeScannerName;
    }
}
