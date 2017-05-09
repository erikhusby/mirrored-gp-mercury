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
import org.apache.commons.lang3.time.FastDateFormat;
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
import java.util.ArrayList;
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
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("M-d-yy");

    public static final String ACTION_BEAN_URL = "/vessel/FingerprintingSpreadsheet.action";
    private static final String CREATE_PAGE = "/vessel/create_fingerprint_spreadsheet.jsp";
    private static final String SUBMIT_ACTION = "barcodeSubmit";

    private static final Integer MINIMUM_SAMPLE_COUNT = 48;

    private String plateBarcode;
    private StaticPlate staticPlate;
    private Workbook workbook;
    private List<String> errorMessages = new ArrayList<>();

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private ControlDao controlDao;

    @Inject
    private FingerprintingPlateFactory fingerprintingPlateFactory;

    /** Allows unit test to be db free by mocking the control dao. */
    void setControlDao(ControlDao controlDao) {
        fingerprintingPlateFactory = new FingerprintingPlateFactory(controlDao);
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
        if (getContext().getRequest() != null && getContext().getSession() != null) {
            getContext().getMessages().clear();
        }
        errorMessages.clear();

        List<FingerprintingPlateFactory.FpSpreadsheetRow> dtos;
        try {

            // Makes a dto for each plate well.
            dtos = fingerprintingPlateFactory.makeSampleDtos(staticPlate, errorMessages);

            if (getContext().getRequest() != null && getContext().getSession() != null) {
                for (String msg : errorMessages) {
                    addMessage(msg);
                }
            }

            if (CollectionUtils.isEmpty(dtos)) {
                if (getContext().getRequest() != null && getContext().getSession() != null) {
                    addMessage("No samples found.");
                }
                return new ForwardResolution(CREATE_PAGE);
            }

            // Must be a full plate for GAP.
            if (dtos.size() < MINIMUM_SAMPLE_COUNT) {
                addGlobalValidationError("Plate has a pico'd sample count of " + dtos.size() +
                        " and it must be at least " + MINIMUM_SAMPLE_COUNT);
                return new ForwardResolution(CREATE_PAGE);
            }

            // Makes the spreadsheet.
            workbook = fingerprintingPlateFactory.makeSpreadsheet(dtos);

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
            logger.log(Level.WARNING, "Exception thrown when attempting to make fingerprinting spreadsheet.", e);
            addGlobalValidationError(e.getMessage());
            return new ForwardResolution(CREATE_PAGE);
        }

    }

    @ValidationMethod(on = SUBMIT_ACTION)
    public void validateNoPlate(ValidationErrors errors) {
        clearValidationErrors();
        if (StringUtils.isBlank(plateBarcode)) {
            errors.add("barcodeTextbox", new SimpleError("Plate barcode is missing."));
        } else {
            staticPlate = staticPlateDao.findByBarcode(plateBarcode);
            if (staticPlate == null) {
                errors.add(plateBarcode, new SimpleError("Plate not found."));
            }
        }
    }

    public StaticPlate getStaticPlate() {
        return staticPlate;
    }

    public void setStaticPlate(StaticPlate staticPlate) {
        this.staticPlate = staticPlate;
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }
}
