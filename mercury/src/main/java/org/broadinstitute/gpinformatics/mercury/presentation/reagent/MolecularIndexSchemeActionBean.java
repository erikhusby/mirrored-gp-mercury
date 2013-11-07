package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.control.reagent.MolecularIndexingSchemeParser;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Stripes action bean for uploading a text file of molecular index schemes.
 */
@UrlBinding("/reagent/molindscheme.action")
public class MolecularIndexSchemeActionBean extends CoreActionBean {

    public static final String UPLOAD_ACTION = "upload";

    public static final String MOL_IND_SCHEME_PAGE = "/reagent/mol_ind_scheme_upload.jsp";

    private FileBean schemesTextFile;

    private List<MolecularIndexingScheme> molecularIndexingSchemes = new ArrayList<>();

    @Inject
    private MolecularIndexingSchemeParser molecularIndexingSchemeParser;

    @DefaultHandler
    @HandlesEvent(UPLOAD_ACTION)
    public Resolution upload() {
        if (schemesTextFile != null) {
            try {
                molecularIndexingSchemes = molecularIndexingSchemeParser.parse(schemesTextFile.getInputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new ForwardResolution(MOL_IND_SCHEME_PAGE);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setSchemesTextFile(FileBean schemesTextFile) {
        this.schemesTextFile = schemesTextFile;
    }

    public List<MolecularIndexingScheme> getMolecularIndexingSchemes() {
        return molecularIndexingSchemes;
    }
}
