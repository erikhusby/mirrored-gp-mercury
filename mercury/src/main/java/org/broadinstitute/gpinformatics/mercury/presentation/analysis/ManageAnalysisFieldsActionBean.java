package org.broadinstitute.gpinformatics.mercury.presentation.analysis;

import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.analysis.AnalysisEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AlignerDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.List;

/**
 * Class used to Add/Delete analysis field data, such as aligners, analysis type, bait and ref seq.
 */
@UrlBinding(value = "/view/analysisFields.action")
public class ManageAnalysisFieldsActionBean extends CoreActionBean {

    public static final String MANAGE_ALIGNER_PAGE = "/analysis/manageAligners.jsp";
    public static final String MANAGE_ANALYSIS_TYPE_PAGE = "/analysis/manageAnalysisTypes.jsp";
    public static final String MANAGE_BAIT_PAGE = "/analysis/manageBaits.jsp";
    public static final String MANAGE_REFERENCE_SEQUENCE_PAGE = "/analysis/manageReferenceSequences.jsp";

    @Inject
    AnalysisEjb analysisEjb;
    @Inject
    AlignerDao alignerDao;
    @Inject
    AnalysisTypeDao analysisTypeDao;
    @Inject
    ReagentDesignDao reagentDesignDao;
    @Inject
    ReferenceSequenceDao referenceSequenceDao;

    private List<Aligner> alignerList;
    private List<AnalysisType> analysisTypeList;
    private List<ReagentDesign> reagentDesignList;
    private List<ReferenceSequence> referenceSequenceList;

    @Validate(required = true, on = {"RemoveReferenceSequence", "RemoveAligner"})
    private String businessKey;

    /**
     * The variables below are used for adding a new analysis field.
     */
    @Validate(required = true, on = {"AddReferenceSequence", "AddAligner"})
    private String newName;
    @Validate(required = true, on = {"AddReferenceSequence"})
    private String newVersion;

    @HandlesEvent("showAligner")
    public Resolution showAligner() {
        alignerList = alignerDao.findAll();
        return new ForwardResolution(MANAGE_ALIGNER_PAGE);
    }

    @HandlesEvent("showAnalysisType")
    public Resolution showAnalysisType() {
        analysisTypeList = analysisTypeDao.findAll();
        return new ForwardResolution(MANAGE_ANALYSIS_TYPE_PAGE);
    }

    @HandlesEvent("showBait")
    public Resolution showBait() {
        return new ForwardResolution(MANAGE_BAIT_PAGE);
    }

    @HandlesEvent("showReferenceSequence")
    public Resolution showReferenceSequence() {
        referenceSequenceList = referenceSequenceDao.findAll();
        return new ForwardResolution(MANAGE_REFERENCE_SEQUENCE_PAGE);
    }


    public List<Aligner> getAlignerList() {
        return alignerList;
    }

    public List<AnalysisType> getAnalysisType() {
        return analysisTypeList;
    }

    public List<ReferenceSequence> getReferenceSequenceList() {
        return referenceSequenceList;
    }


    /**
     * Method used to add a new aligner analysis field.
     */
    @HandlesEvent("AddAligner")
    public Resolution addAligner() {

        analysisEjb.addAligner(newName);

        return showAligner();
    }

    @HandlesEvent("RemoveAligner")
    public Resolution removeAligner() {

        analysisEjb.removeAligners(businessKey);

        return showAligner();
    }

    /**
     * Method used to add a new reference sequence analysis field.
     */
    @HandlesEvent("AddReferenceSequence")
    public Resolution addReferenceSequence() {

        if (NumberUtils.isNumber(newVersion)) {
            analysisEjb.addReferenceSequence(newName, newVersion);
        } else {
            addGlobalValidationError(MessageFormat.format("Invalid version provided ''{0}''.", newVersion));
        }

        return showReferenceSequence();
    }

    @HandlesEvent("RemoveReferenceSequence")
    public Resolution removeReferenceSequence() {

        analysisEjb.removeReferenceSequences(businessKey);

        return showReferenceSequence();
    }


    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }
}
