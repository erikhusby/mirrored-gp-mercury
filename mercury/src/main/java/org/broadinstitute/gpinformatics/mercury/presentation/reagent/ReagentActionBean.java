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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

/**
 * Stripes Action bean for uploading vessels of reagents.
 */
@UrlBinding("/reagent/reagent.action")
public class ReagentActionBean extends CoreActionBean {

    public enum ReagentFormat {
        CONTROLS("Control tubes"),
        UMI("Unique Molecular Identifiers");
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

    @Validate(required = true, on = UPLOAD_ACTION)
    private FileBean reagentsFile;
    private ReagentFormat reagentFormat;

    @Inject
    private ControlReagentFactory controlReagentFactory;

    @Inject
    private UniqueMolecularIdentifierReagentFactory umiReagentFactory;

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

    public ReagentFormat getReagentFormat() {
        return reagentFormat;
    }

    public void setReagentFormat(ReagentFormat reagentFormat) {
        this.reagentFormat = reagentFormat;
    }

    public void setReagentsFile(FileBean reagentsFile) {
        this.reagentsFile = reagentsFile;
    }
}
