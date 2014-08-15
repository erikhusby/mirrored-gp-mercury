package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.control.vessel.IndexedPlateFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

/**
 * A Stripes action bean to allow upload of files describing plates of molecular indexes.
 */
@UrlBinding("/reagent/molindplate.action")
public class MolecularIndexPlateActionBean extends CoreActionBean {

    public static final String UPLOAD_ACTION = "upload";

    public static final String MOL_IND_PLATE_PAGE = "/reagent/mol_ind_plate_upload.jsp";

    @Inject
    private IndexedPlateFactory indexedPlateFactory;

    private FileBean platesFile;

    @DefaultHandler
    @HandlesEvent(UPLOAD_ACTION)
    public Resolution upload() {
        if (platesFile != null) {
            try {
                // todo jmt support other index types.
                Map<String, StaticPlate> mapBarcodeToPlate = indexedPlateFactory.parseAndPersist(
                        IndexedPlateFactory.TechnologiesAndParsers.ILLUMINA_SINGLE,
                        platesFile.getInputStream());
                addMessage("Uploaded " + mapBarcodeToPlate.size() + " plates");
            } catch (Exception e) {
                addGlobalValidationError(e.getMessage());
            }
        }
        return new ForwardResolution(MOL_IND_PLATE_PAGE);
    }

    public void setPlatesFile(FileBean platesFile) {
        this.platesFile = platesFile;
    }
}
