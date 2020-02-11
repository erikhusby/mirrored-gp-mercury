package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.reagent.ControlReagentFactory;
import org.broadinstitute.gpinformatics.mercury.control.reagent.UniqueMolecularIdentifierReagentFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.IndexedPlateFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Stripes Action bean for uploading vessels of reagents.
 */
@UrlBinding("/reagent/reagent.action")
public class ReagentActionBean extends CoreActionBean {

    public enum ReagentFormat {
        CONTROLS("Control tubes"),
        UMI("Unique Molecular Identifiers"),
        INDEXES("Molecular Indexes");
        private String displayName;

        ReagentFormat(String displayName) {

            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static final String UPLOAD_ACTION = "upload";

    public static final String REAGENT_UPLOAD_PAGE = "/reagent/reagent_upload.jsp";
    public static final String REAGENT_UPLOAD_PAGE2 = "/reagent/reagent_upload2.jsp";
    private static final String DELIMITER = " ";

    @Validate(required = true, on = UPLOAD_ACTION)
    private FileBean reagentsFile;
    private ReagentFormat reagentFormat;
    private List<String> tubesAndIndexNames = new ArrayList<>();
    private boolean hasWarnings;

    @Inject
    private ControlReagentFactory controlReagentFactory;

    @Inject
    private UniqueMolecularIdentifierReagentFactory umiReagentFactory;

    @Inject
    private IndexedPlateFactory indexedPlateFactory;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(REAGENT_UPLOAD_PAGE);
    }

    @HandlesEvent(UPLOAD_ACTION)
    public Resolution upload() {
        try {
            MessageCollection messageCollection = new MessageCollection();
            int uploadCount;
            switch (reagentFormat) {
            case CONTROLS:
                List<BarcodedTube> barcodedTubes = controlReagentFactory.buildTubesFromSpreadsheet(
                        reagentsFile.getInputStream(), messageCollection);
                uploadCount = barcodedTubes.size();
                break;
            case UMI:
                List<LabVessel> staticPlates =
                        umiReagentFactory.buildUMIFromSpreadsheet(reagentsFile.getInputStream(), messageCollection);
                if (!messageCollection.hasErrors() && staticPlates != null) {
                    uploadCount = staticPlates.size();
                } else {
                    uploadCount = 0;
                }
                break;
            case INDEXES:
                // A header must be present but it is ignored. Data rows consist of barcode, mis name.
                List<List<String>> rows = indexedPlateFactory.parseSpreadsheet(reagentsFile.getFileName(),
                        reagentsFile.getInputStream(), 2, messageCollection);
                if (!messageCollection.hasErrors()) {
                    indexedPlateFactory.validateTubesAndIndexNames(rows, messageCollection);
                }
                addMessages(messageCollection);
                if (messageCollection.hasErrors()) {
                    return new ForwardResolution(REAGENT_UPLOAD_PAGE);
                } else {
                    hasWarnings = messageCollection.hasWarnings();
                    // Populates the list of pairs so the jsp can return it for the save action.
                    rows.subList(1, rows.size()).
                            forEach(row -> tubesAndIndexNames.add(row.get(0) + DELIMITER + row.get(1)));
                    return new ForwardResolution(REAGENT_UPLOAD_PAGE2);
                }
            default:
                throw new RuntimeException("Unexpected reagent format " + reagentFormat);
            }
            addMessages(messageCollection);
            if (!messageCollection.hasErrors()) {
                addMessage("Uploaded {0} reagents.", uploadCount);
            }
            return new ForwardResolution(REAGENT_UPLOAD_PAGE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution reagentTubeSave() {
        MessageCollection messageCollection = new MessageCollection();
        List<String> tubeBarcodes = new ArrayList<>();
        List<String> indexNames = new ArrayList<>();
        tubesAndIndexNames.forEach(tubeAndIndexName -> {
            String[] splitString = tubeAndIndexName.split(DELIMITER);
            tubeBarcodes.add(splitString[0]);
            indexNames.add(splitString[1]);
        });

        indexedPlateFactory.saveTubesAndIndexNames(tubeBarcodes, indexNames, messageCollection);
        addMessages(messageCollection);
        return new ForwardResolution(REAGENT_UPLOAD_PAGE);
    }

    public ReagentFormat getReagentFormat() {
        return reagentFormat;
    }

    public void setReagentFormat(ReagentFormat reagentFormat) {
        this.reagentFormat = reagentFormat;
    }

    public void setReagentsFile(FileBean reagentsFile) {
        this.reagentsFile = reagentsFile;
    }

    public String formatTubeAndIndexName(String tubeBarcode, String indexName) {
        return tubeBarcode + " " + indexName;
    }

    public String[] parseTubeAndIndexName(String tubeAndIndexName) {
        return tubeAndIndexName.split(" ");
    }

    public List<String> getTubesAndIndexNames() {
        return tubesAndIndexNames;
    }

    public void setTubesAndIndexNames(List<String> tubesAndIndexNames) {
        this.tubesAndIndexNames = tubesAndIndexNames;
    }

    public boolean isHasWarnings() {
        return hasWarnings;
    }

    public void setHasWarnings(boolean hasWarnings) {
        this.hasWarnings = hasWarnings;
    }
}
