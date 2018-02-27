package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryBarcodeUpdate;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorEzPass;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorNonPooled;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorPooledMultiOrganism;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

import static org.broadinstitute.gpinformatics.mercury.presentation.sample.SampleVesselActionBean.UPLOAD_SAMPLES;

@UrlBinding(value = "/sample/ExternalLibraryUpload.action")
public class ExternalLibraryUploadActionBean extends CoreActionBean {
    private static final String SESSION_LIST_PAGE = "/sample/externalLibraryUpload.jsp";
    private boolean overWriteFlag;

    /** The types of spreadsheet that can be uploaded. */
    public enum SpreadsheetType {
        PooledTubes("Pooled Tubes", new VesselPooledTubesProcessor(null)),
        EzPassLibraries("EZ Pass Libraries", new ExternalLibraryProcessorEzPass(null)),
        PooledMultiOrganismLibraries("Pooled or Multi-Organism Libraries",
                new ExternalLibraryProcessorPooledMultiOrganism(null)),
        NonPooledLibraries("Non-pooled Libraries", new ExternalLibraryProcessorNonPooled(null)),
        BarcodeUpdates("Tube Barcode Updates", new ExternalLibraryBarcodeUpdate(null));

        private String displayName;
        private TableProcessor processor;

        SpreadsheetType(String displayName, TableProcessor processor) {
            this.displayName = displayName;
            this.processor = processor;
        }

        public String getDisplayName() {
            return displayName;
        }

        public TableProcessor getProcessor() {
            return processor;
        }
    }

    @Validate(required = true, on = UPLOAD_SAMPLES)
    private SpreadsheetType spreadsheetType;

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
        MessageCollection messageCollection = new MessageCollection();
        InputStream inputStream = null;
        try {
            inputStream = samplesSpreadsheet.getInputStream();
        } catch (IOException e) {
            addMessage("Cannot upload spreadsheet: " + e);
        }
        sampleInstanceEjb.doExternalUpload(inputStream, overWriteFlag, spreadsheetType.getProcessor(),
                messageCollection);
        addMessages(messageCollection);
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    public void setSamplesSpreadsheet(FileBean spreadsheet) {
        this.samplesSpreadsheet = spreadsheet;
    }

    public void setOverWriteFlag(boolean overWriteFlag) {
        this.overWriteFlag = overWriteFlag;
    }

    public boolean isOverWriteFlag() {
        return overWriteFlag;
    }

    public SpreadsheetType getSpreadsheetType() {
        return spreadsheetType;
    }
}
