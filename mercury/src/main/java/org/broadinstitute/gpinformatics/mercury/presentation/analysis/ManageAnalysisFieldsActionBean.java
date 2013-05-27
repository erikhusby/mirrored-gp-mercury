package org.broadinstitute.gpinformatics.mercury.presentation.analysis;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class used to Add/Delete analysis field data, such as aligners, analysis types, reagent designs and reference sequences.
 */
@UrlBinding(value = "/view/analysisFields.action")
public class ManageAnalysisFieldsActionBean extends CoreActionBean {

    private static final String MANAGE_ALIGNER_PAGE = "/analysis/manage_aligners.jsp";
    private static final String MANAGE_ANALYSIS_TYPE_PAGE = "/analysis/manage_analysis_types.jsp";
    private static final String MANAGE_REAGENT_DESIGN_PAGE = "/analysis/manage_reagent_designs.jsp";
    private static final String MANAGE_REFERENCE_SEQUENCE_PAGE = "/analysis/manage_reference_sequences.jsp";

    public static final String ANALYSIS_FIELDS_ACTION = "/view/analysisFields.action";

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

    @Validate(required = true,
            on = {"removeAligners", "removeAnalysisTypes", "removeReagentDesigns", "removeReferenceSequences"})
    private List<String> businessKeyList;

    @Validate(required = true, on = {"AddReagentDesign"})
    private ReagentDesign.ReagentType selectedReagentType;

    @Validate(required = true, on = {"addAligner", "addAnalysisType", "addReagentDesign", "addReferenceSequence"})
    private String newName;

    @Validate(required = true, on = {"addReferenceSequence"})
    private String newVersion;

    @After(stages = LifecycleStage.BindingAndValidation, on = {"showAligner", "addAligner", "removeAligners"})
    public void afterAlignerValidations() {
            alignerList = alignerDao.findAll();
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {"showAnalysisType", "addAnalysisType", "removeAnalysisTypes"})
    public void afterAnalysisTypeValidations() {
        analysisTypeList = analysisTypeDao.findAll();
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {"showReferenceSequence", "addReferenceSequence", "removeReferenceSequences"})
    public void afterReferenceSequenceValidation() {
        referenceSequenceList = referenceSequenceDao.findAll();
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {"showReagentDesign", "addReagentDesign", "removeReagentDesigns"})
    public void aferReagentDesignValidation() {
        reagentDesignList = reagentDesignDao.findAll();
    }

    @DefaultHandler
    @HandlesEvent("showAligner")
    public Resolution showAligner() {
        return new ForwardResolution(MANAGE_ALIGNER_PAGE);
    }

    @HandlesEvent("addAligner")
    public Resolution addAligner() {
        analysisEjb.addAligner(newName);
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter("showAligner", "");
    }

    @HandlesEvent("removeAligners")
    public Resolution removeAligners() {
        analysisEjb.removeAligners(getBusinessKeyArray());
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter("showAligner", "");
    }

    @HandlesEvent("showAnalysisType")
    public Resolution showAnalysisType() {
        return new ForwardResolution(MANAGE_ANALYSIS_TYPE_PAGE);
    }

    @HandlesEvent("addAnalysisType")
    public Resolution addAnalysisType() {
        analysisEjb.addAnalysisType(newName);
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter("showAnalysisType", "");
    }

    @HandlesEvent("removeAnalysisTypes")
    public Resolution removeAnalysisTypes() {
        analysisEjb.removeAnalysisTypes(getBusinessKeyArray());
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter("showAnalysisType", "");
    }

    @HandlesEvent("showReferenceSequence")
    public Resolution showReferenceSequence() {
        return new ForwardResolution(MANAGE_REFERENCE_SEQUENCE_PAGE);
    }

    @HandlesEvent("addReferenceSequence")
    public Resolution addReferenceSequence() {
        analysisEjb.addReferenceSequence(newName, newVersion);
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter("showReferenceSequence", "");
    }

    @HandlesEvent("removeReferenceSequences")
    public Resolution removeReferenceSequences() {
        analysisEjb.removeReferenceSequences(getBusinessKeyArray());
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter("showReferenceSequence", "");
    }

    @HandlesEvent("showReagentDesign")
    public Resolution showReagentDesign() {
        return new ForwardResolution(MANAGE_REAGENT_DESIGN_PAGE);
    }

    @HandlesEvent("addReagentDesign")
    public Resolution addReagentDesign() {
        analysisEjb.addReagentDesign(newName, selectedReagentType);
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter("showReagentDesign", "");
    }

    @HandlesEvent("removeReagentDesigns")
    public Resolution removeReagentDesigns() {
        analysisEjb.removeReagentDesigns(getBusinessKeyArray());
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter("showReagentDesign", "");
    }

    /**
     * This is used to return an array of business keys for objects that we want to delete.
     * This is a utility method that will reduce code replication.
     *
     * @return String array of business keys for selected analysis fields.
     */
    public String[] getBusinessKeyArray() {
        return getBusinessKeyList().toArray(new String[getBusinessKeyList().size()]);
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

    public List<String> getBusinessKeyList() {
        if (businessKeyList == null) {
            return Collections.emptyList();
        }

        return businessKeyList;
    }

    public void setBusinessKeyList(List<String> businessKeyList) {
        this.businessKeyList = businessKeyList;
    }

    public List<Aligner> getAlignerList() {
        return alignerList;
    }

    public List<AnalysisType> getAnalysisTypeList() {
        return analysisTypeList;
    }

    public List<ReagentDesign> getReagentDesignList() {
        return reagentDesignList;
    }

    public List<ReferenceSequence> getReferenceSequenceList() {
        return referenceSequenceList;
    }

    public ReagentDesign.ReagentType getSelectedReagentType() {
        return selectedReagentType;
    }

    public void setSelectedReagentType(ReagentDesign.ReagentType selectedReagentType) {
        this.selectedReagentType = selectedReagentType;
    }
}
