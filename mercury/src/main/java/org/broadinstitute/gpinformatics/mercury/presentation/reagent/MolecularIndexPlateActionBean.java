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
import java.util.ArrayList;
import java.util.List;
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

    private IndexedPlateFactory.TechnologiesAndParsers technologyAndParserType;

    @DefaultHandler
    @HandlesEvent(UPLOAD_ACTION)
    public Resolution upload() {
        if (platesFile != null) {
            try {
                Map<String, StaticPlate> mapBarcodeToPlate = indexedPlateFactory.parseAndPersist(
                        technologyAndParserType,
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

    public void setTechnologyAndParserType(
            IndexedPlateFactory.TechnologiesAndParsers technologyAndParserType) {
        this.technologyAndParserType = technologyAndParserType;
    }

    public IndexedPlateFactory.TechnologiesAndParsers[] getTechnologiesAndParsers() {
        return IndexedPlateFactory.TechnologiesAndParsers.values();
    }

    public List<IndexedPlateFactory.TechnologiesAndParsers> getActiveTechnologiesAndParsers() {
        List<IndexedPlateFactory.TechnologiesAndParsers> activeParsers = new ArrayList<>();
        IndexedPlateFactory.TechnologiesAndParsers[] values = IndexedPlateFactory.TechnologiesAndParsers.values();
        for (IndexedPlateFactory.TechnologiesAndParsers technologiesAndParser: values) {
            if (technologiesAndParser.isActive()) {
                activeParsers.add(technologiesAndParser);
            }
        }
        return activeParsers;
    }
}
