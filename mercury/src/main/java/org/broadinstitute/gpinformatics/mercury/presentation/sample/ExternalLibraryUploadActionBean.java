package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorEzPass;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorNonPooled;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorPooled;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorPooledMultiOrganism;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;

@UrlBinding(value = "/sample/ExternalLibraryUpload.action")
public class ExternalLibraryUploadActionBean extends CoreActionBean {

    public static final String EZPASS_KIOSK = "ezpassKiosk";
    public static final String UPLOAD_SAMPLES = "uploadSamples";
    public static final String POOLED = "pooled";
    public static final String NON_POOLED = "non-pooled";
    public static final String MULTI_ORG = "multi-org";
    private static final String SESSION_LIST_PAGE = "/sample/externalLibraryUpload.jsp";
    private boolean overWriteFlag;

    @Validate(required = true, on = UPLOAD_SAMPLES)
    private ExternalLibraryUploadActionBean.SpreadsheetType spreadsheetType;

    @Inject
    private SampleInstanceEjb sampleInstanceEjb;

    @Validate(required = true, on = UPLOAD_SAMPLES)
    private FileBean samplesSpreadsheet;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    /**
     * Entry point for initial upload of spreadsheet.
     */
    @HandlesEvent(UPLOAD_SAMPLES)
    public Resolution uploadTubes() {
        switch (spreadsheetType) {
        case EZ_PASS:
            uploadEzPassLibraries();
            break;
        case EZ_PASS_KIOSK:
            uploadEzPassLibraries();
            break;
        case EXTERNAL_LIBRARY_POOLED_TUBES:
            uploadPooledLibrary();
            break;
        case EXTERNAL_LIBRARY_NON_POOLED_TUBES:
            uploadNonPooledLibrary();
            break;
        case EXTERNAL_LIBRARY_MULTIPLE_ORGANISM:
            uploadMultipleOrganismLibrary();
            break;
        case POOLED_TUBES:
            uploadPooledTubes();
            break;
        }
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    /**
     * Pooled tube samples
     */
    public void uploadPooledTubes() {
        try {
            MessageCollection messageCollection = new MessageCollection();
            VesselPooledTubesProcessor vesselSpreadsheetProcessor = new VesselPooledTubesProcessor("Sheet1");
            messageCollection.addErrors(PoiSpreadsheetParser.processSingleWorksheet(
                    samplesSpreadsheet.getInputStream(), vesselSpreadsheetProcessor));
            sampleInstanceEjb.verifyAndPersistPooledTubeSpreadsheet(vesselSpreadsheetProcessor,
                    messageCollection, overWriteFlag);
            addMessages(messageCollection);
        } catch (InvalidFormatException | IOException | ValidationException e) {
            addValidationError("samplesSpreadsheet", e.getMessage());
        }
    }

    /**
     * EZ Pass samples
     */
    public void uploadEzPassLibraries() {
        try {
            MessageCollection messageCollection = new MessageCollection();
            ExternalLibraryProcessorEzPass processor = new ExternalLibraryProcessorEzPass("Sheet1");
            messageCollection.addErrors(PoiSpreadsheetParser.processSingleWorksheet(
                    samplesSpreadsheet.getInputStream(), processor));
            sampleInstanceEjb.verifyAndPersistExternalLibrary(processor, messageCollection,
                    overWriteFlag);
            addMessages(messageCollection);
        } catch (InvalidFormatException | IOException | ValidationException e) {
            addValidationError("samplesSpreadsheet", e.getMessage());
        }
    }

    /**
     * External library multi organism
     */
    public void uploadMultipleOrganismLibrary() {
        try {
            MessageCollection messageCollection = new MessageCollection();
            ExternalLibraryProcessorPooledMultiOrganism processor =
                    new ExternalLibraryProcessorPooledMultiOrganism("Sheet1");
            messageCollection.addErrors(PoiSpreadsheetParser.processSingleWorksheet(
                    samplesSpreadsheet.getInputStream(), processor));
            sampleInstanceEjb.verifyAndPersistExternalLibrary(processor, messageCollection,
                    overWriteFlag);
            addMessages(messageCollection);
        } catch (InvalidFormatException | IOException | ValidationException e) {
            addValidationError("samplesSpreadsheet", e.getMessage());
        }
    }

    /**
     * External library pooled samples
     */
    public void uploadPooledLibrary() {
        try {
            MessageCollection messageCollection = new MessageCollection();
            ExternalLibraryProcessorPooled processor = new ExternalLibraryProcessorPooled("Sheet1");
            messageCollection.addErrors(PoiSpreadsheetParser.processSingleWorksheet(
                    samplesSpreadsheet.getInputStream(), processor));
            sampleInstanceEjb.verifyAndPersistExternalLibrary(processor, messageCollection,
                    overWriteFlag);
            addMessages(messageCollection);
        } catch (InvalidFormatException | IOException | ValidationException e) {
            addValidationError("samplesSpreadsheet", e.getMessage());
        }
    }

    /**
     * External library NON pooled samples
     */
    public void uploadNonPooledLibrary() {
        try {
            MessageCollection messageCollection = new MessageCollection();
            ExternalLibraryProcessorNonPooled processor = new ExternalLibraryProcessorNonPooled("Sheet1");
            messageCollection.addErrors(PoiSpreadsheetParser.processSingleWorksheet(
                    samplesSpreadsheet.getInputStream(), processor));
            sampleInstanceEjb.verifyAndPersistExternalLibrary(processor, messageCollection,
                    overWriteFlag);
            addMessages(messageCollection);
        } catch (InvalidFormatException | IOException | ValidationException e) {
            addValidationError("samplesSpreadsheet", e.getMessage());
        }
    }

    public void setSamplesSpreadsheet(FileBean spreadsheet) {
        this.samplesSpreadsheet = spreadsheet;
    }

    public SpreadsheetType getSpreadsheetType() {
        return spreadsheetType;
    }

    public void setSpreadsheetType(
            SpreadsheetType spreadsheetType) {
        this.spreadsheetType = spreadsheetType;
    }

    public void setOverWriteFlag(boolean overWriteFlag) {
        this.overWriteFlag = overWriteFlag;
    }

    public boolean isOverWriteFlag() {
        return overWriteFlag;
    }

    @UrlBinding(value = "/sample/ExternalLibraryUpload.action")
    public enum SpreadsheetType {
        EZ_PASS("EZ Pass"),
        EZ_PASS_KIOSK("EZ Pass Kiosk"),
        EXTERNAL_LIBRARY_POOLED_TUBES("External Pooled Tubes"),
        EXTERNAL_LIBRARY_NON_POOLED_TUBES("External Non Pooled Tubes"),
        EXTERNAL_LIBRARY_MULTIPLE_ORGANISM("External Multiple Organisim"),
        POOLED_TUBES("Pooled Tubes");

        private String displayName;

        SpreadsheetType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

}
