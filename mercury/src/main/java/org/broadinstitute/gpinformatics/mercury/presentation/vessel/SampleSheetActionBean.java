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
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.SampleSheetFactory;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stripes action bean that allows user to download as samplesheet.csv file for arrays.
 */
@UrlBinding(value = "/vessel/SampleSheet.action")
public class SampleSheetActionBean extends CoreActionBean {
    private static final String CREATE_PAGE = "/vessel/sample_sheet.jsp";
    private static final String DOWNLOAD_ACTION = "download";

    /** Extract barcode and position from e.g. 3999595020_R12C02 */
    private static final Pattern BARCODE_PATTERN = Pattern.compile("(\\d*)_(R\\d*C\\d*)");

    /** POSTed from form. */
    private String pdoBusinessKeys;

    /** POSTed from form. */
    private String chipWellBarcodes;

    @Inject
    private SampleSheetFactory sampleSheetFactory;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private LabVesselDao labVesselDao;

    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(CREATE_PAGE);
    }

    @HandlesEvent(DOWNLOAD_ACTION)
    public Resolution download() {
        final List<Pair<LabVessel, VesselPosition>> vesselPositionPairs = new ArrayList<>();

        String[] pdoKeys = pdoBusinessKeys.trim().split("\\s+");
        ResearchProject researchProject = null;
        for (String pdoKey : pdoKeys) {
            ProductOrder productOrder = productOrderDao.findByBusinessKey(pdoKey);
            vesselPositionPairs.addAll(sampleSheetFactory.loadByPdo(productOrder));
            // todo jmt error if multiple research projects
            researchProject = productOrder.getResearchProject();
        }

        String[] chipWellBarcodeArray = chipWellBarcodes.trim().split("\\s+");
        if (chipWellBarcodeArray.length > 0) {
            ArrayList<String> barcodes = new ArrayList<>();
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
                }
            }
        }

        if (hasErrors()) {
            return new ForwardResolution(CREATE_PAGE);
        } else {
            final ResearchProject finalResearchProject = researchProject;
            return new StreamingResolution("text/plain") {
                @Override
                public void stream(HttpServletResponse response) throws Exception {
                    ServletOutputStream out = response.getOutputStream();
                    sampleSheetFactory.write(new PrintStream(out), vesselPositionPairs, finalResearchProject);
                }
            }.setFilename("Samplesheet.csv");
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
}
