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
import org.broadinstitute.gpinformatics.mercury.boundary.sample.ExternalLibrarySampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryMapped;
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

    public static final String UPLOAD_SAMPLES = "uploadSamples";
    public static final String POOLED = "pooled";
    public static final String NON_POOLED = "non-pooled";
    public static final String MULTI_ORG = "multi-org";
    private static final String SESSION_LIST_PAGE = "/sample/externalLibraryUpload.jsp";
    private boolean overWriteFlag;
    public static final int externalLibraryRowOffset = 25;
    public static final int ezPassRowOffset = 28;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @Validate(required = true, on = UPLOAD_SAMPLES)
    private ExternalLibraryUploadActionBean.SpreadsheetType spreadsheetType;

    @Inject
    private ExternalLibrarySampleInstanceEjb externalLibrarySampleInstanceEjb;

    @Validate(required = true, on = UPLOAD_SAMPLES)
    private FileBean samplesSpreadsheet;


    /**
     * Entry point for initial upload of spreadsheet.
     */
    @HandlesEvent(UPLOAD_SAMPLES)
    public Resolution uploadTubes() {

        switch (spreadsheetType) {
            case EZ_PASS:
                uploadEzPassLibraries(false);
                break;
            case EZ_PASS_KIOSK:
                uploadEzPassLibraries(true);
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

            PoiSpreadsheetParser.processSingleWorksheet(samplesSpreadsheet.getInputStream(), vesselSpreadsheetProcessor);
            externalLibrarySampleInstanceEjb.verifyPooledTubes(vesselSpreadsheetProcessor, messageCollection, overWriteFlag);

            addMessages(messageCollection);

        } catch (InvalidFormatException | IOException | ValidationException e) {
            addValidationError("samplesSpreadsheet", e.getMessage());
        }
    }


    /**
     * EZ Pass samples
     */
    public void uploadEzPassLibraries(Boolean isKiosk) {
        try {

            MessageCollection messageCollection = new MessageCollection();

            ExternalLibraryProcessorEzPass spreadSheetProcessor = new ExternalLibraryProcessorEzPass("Sheet1");

            spreadSheetProcessor.setHeaderRowIndex(ezPassRowOffset);
            PoiSpreadsheetParser.processSingleWorksheet(samplesSpreadsheet.getInputStream(), spreadSheetProcessor);
            //TODO: Not finished
            //externalLibrarySampleInstanceEjb.verifyExternalLibraryEZPass(spreadSheetProcessor, messageCollection, overWriteFlag, isKiosk);

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

            ExternalLibraryProcessorPooledMultiOrganism spreadSheetProcessor = new ExternalLibraryProcessorPooledMultiOrganism("Sheet1");

            spreadSheetProcessor.setHeaderRowIndex(externalLibraryRowOffset);
            PoiSpreadsheetParser.processSingleWorksheet(samplesSpreadsheet.getInputStream(), spreadSheetProcessor);
            ExternalLibraryMapped externalLibraryMapped = new ExternalLibraryMapped();
            externalLibraryMapped.mapPooledMultiOrg(spreadSheetProcessor);
            externalLibrarySampleInstanceEjb.verifyExternalLibrary(externalLibraryMapped, messageCollection, overWriteFlag, MULTI_ORG);

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

            ExternalLibraryProcessorPooled spreadSheetProcessor = new ExternalLibraryProcessorPooled("Sheet1");

            spreadSheetProcessor.setHeaderRowIndex(externalLibraryRowOffset);
            PoiSpreadsheetParser.processSingleWorksheet(samplesSpreadsheet.getInputStream(), spreadSheetProcessor);
            ExternalLibraryMapped externalLibraryMapped = new ExternalLibraryMapped();
            externalLibraryMapped.mapPooled(spreadSheetProcessor);
            externalLibrarySampleInstanceEjb.verifyExternalLibrary(externalLibraryMapped, messageCollection, overWriteFlag, POOLED);

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

            ExternalLibraryProcessorNonPooled spreadSheetProcessor = new ExternalLibraryProcessorNonPooled("Sheet1");

            spreadSheetProcessor.setHeaderRowIndex(externalLibraryRowOffset);
            PoiSpreadsheetParser.processSingleWorksheet(samplesSpreadsheet.getInputStream(), spreadSheetProcessor);

            ExternalLibraryMapped externalLibraryMapped = new ExternalLibraryMapped();
            externalLibraryMapped.mapNonPooled(spreadSheetProcessor);
            externalLibrarySampleInstanceEjb.verifyExternalLibrary(externalLibraryMapped, messageCollection, overWriteFlag, NON_POOLED);

            addMessages(messageCollection);

        } catch (InvalidFormatException | IOException | ValidationException e) {
            addValidationError("samplesSpreadsheet", e.getMessage());
        }

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

    public void setSpreadsheetType(ExternalLibraryUploadActionBean.SpreadsheetType spreadsheetType) {
        this.spreadsheetType = spreadsheetType;
    }

    public void setSamplesSpreadsheet(FileBean spreadsheet) {
        this.samplesSpreadsheet = spreadsheet;
    }

    public void setOverWriteFlag(boolean overWriteFlag) {
        this.overWriteFlag = overWriteFlag;
    }

}
