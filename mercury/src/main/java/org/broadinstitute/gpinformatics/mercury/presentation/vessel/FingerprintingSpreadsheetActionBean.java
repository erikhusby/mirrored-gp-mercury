/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.vessel;


import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.FingerprintingPlateFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This Action Bean takes the barcode of a static plate designated for GAP fingerprinting
 * and streams back a newly created spreadsheet of the well contents.
 */
@UrlBinding(value = FingerprintingSpreadsheetActionBean.ACTION_BEAN_URL)
public class FingerprintingSpreadsheetActionBean extends CoreActionBean {
    private static final Logger logger = Logger.getLogger(FingerprintingSpreadsheetActionBean.class.getName());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M-d-yy");

    public static final String ACTION_BEAN_URL = "/vessel/FingerprintingSpreadsheet.action";
    private static final String CREATE_PAGE = "/vessel/create_fingerprint_spreadsheet.jsp";
    private static final String SUBMIT_ACTION = "barcodeSubmit";

    private static final List<Integer> ACCEPTABLE_SAMPLE_COUNTS = Arrays.asList(new Integer[]{48, 96});

    private String plateBarcode;
    StaticPlate staticPlate;
    Workbook workbook;

    @Inject
    private StaticPlateDao staticPlateDao;

    private FingerprintingPlateFactory FingerprintingPlateFactory = new FingerprintingPlateFactory();

    void setControlDao(ControlDao controlDao) {
        FingerprintingPlateFactory.setControlDao(controlDao);
    }

    public String getPlateBarcode() {
        return plateBarcode;
    }

    public void setPlateBarcode(String plateBarcode) {
        this.plateBarcode = plateBarcode;
    }

    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(CREATE_PAGE);
    }

    @HandlesEvent(SUBMIT_ACTION)
    public Resolution barcodeSubmit() {
        clearValidationErrors();
        List<FingerprintingPlateFactory.FpSpreadsheetRow> dtos;
        try {

            // Makes a dto for each plate well.
            dtos = FingerprintingPlateFactory.makeSampleDtos(staticPlate);
            if (CollectionUtils.isEmpty(dtos)) {
                addMessage("No samples found.");
                return new ForwardResolution(CREATE_PAGE);
            }

            // Must be a full plate for GAP.
            if (!ACCEPTABLE_SAMPLE_COUNTS.contains(dtos.size())) {
                addGlobalValidationError("Plate has a pico'd sample count of " + dtos.size() + " and it must be " +
                                         StringUtils.join(ACCEPTABLE_SAMPLE_COUNTS, " or "));
//                return new ForwardResolution(CREATE_PAGE);
            }

            // Makes the spreadsheet.
            workbook = FingerprintingPlateFactory.makeSpreadsheet(dtos);

            // Sets the default filename.
            String filename = DATE_FORMAT.format(new Date()) + "_FP_" + plateBarcode + ".xls";

            // Streams the spreadsheet back to user.
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            StreamingResolution stream = new StreamingResolution(StreamCreatedSpreadsheetUtil.XLS_MIME_TYPE,
                    new ByteArrayInputStream(out.toByteArray()));
            stream.setFilename(filename);

            plateBarcode = null;
            staticPlate = null;

            return stream;

        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create spreadsheet.", e);
            addGlobalValidationError("Failed to create spreadsheet.");
            return new ForwardResolution(CREATE_PAGE);

        } catch (Exception e) {
            addGlobalValidationError(e.getMessage());
            return new ForwardResolution(CREATE_PAGE);
        }

    }

    @ValidationMethod(on = SUBMIT_ACTION)
    public void validateNoPlate(ValidationErrors errors) {
        if (StringUtils.isBlank(plateBarcode)) {
            errors.add("barcodeTextbox", new SimpleError("Plate barcode is missing."));
        } else {
            staticPlate = staticPlateDao.findByBarcode(plateBarcode);
            if (staticPlate == null) {
                errors.add(plateBarcode, new SimpleError("Plate not found."));
            }
        }
    }

}
