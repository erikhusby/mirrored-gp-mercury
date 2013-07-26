package org.broadinstitute.gpinformatics.mercury.presentation.analysis;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
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
import java.util.Collections;
import java.util.List;

/**
 * Class used to Add/Delete analysis field data, such as aligners, analysis types, reagent designs and reference sequences.
 */
@UrlBinding(value = "/admin/analysisFields.action")
public class ManageAnalysisFieldsActionBean extends CoreActionBean {

    public static final String ANALYSIS_FIELDS_ACTION = "/admin/analysisFields.action";

    private static final String MANAGE_ALIGNER_PAGE = "/analysis/manage_aligners.jsp";
    private static final String MANAGE_ANALYSIS_TYPE_PAGE = "/analysis/manage_analysis_types.jsp";
    private static final String MANAGE_REAGENT_DESIGN_PAGE = "/analysis/manage_reagent_designs.jsp";
    private static final String MANAGE_REFERENCE_SEQUENCE_PAGE = "/analysis/manage_reference_sequences.jsp";

    public static final String REMOVE_ALIGNERS = "removeAligners";
    public static final String REMOVE_ANALYSIS_TYPES = "removeAnalysisTypes";
    public static final String REMOVE_REAGENT_DESIGNS = "removeReagentDesigns";
    public static final String REMOVE_REFERENCE_SEQUENCES = "removeReferenceSequences";

    public static final String ADD_ALIGNER = "addAligner";
    public static final String ADD_ANALYSIS_TYPE = "addAnalysisType";
    public static final String ADD_REAGENT_DESIGN = "addReagentDesign";
    public static final String ADD_REFERENCE_SEQUENCE = "addReferenceSequence";

    public static final String SHOW_ALIGNER = "showAligner";
    public static final String SHOW_ANALYSIS_TYPE = "showAnalysisType";
    public static final String SHOW_REFERENCE_SEQUENCE = "showReferenceSequence";
    public static final String SHOW_REAGENT_DESIGN = "showReagentDesign";

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
            on = {REMOVE_ALIGNERS, REMOVE_ANALYSIS_TYPES, REMOVE_REAGENT_DESIGNS, REMOVE_REFERENCE_SEQUENCES})
    private List<String> businessKeyList;

    @Validate(required = true, on = {ADD_REAGENT_DESIGN})
    private ReagentDesign.ReagentType selectedReagentType;

    @Validate(required = true, on = {ADD_ALIGNER, ADD_ANALYSIS_TYPE, ADD_REAGENT_DESIGN, ADD_REFERENCE_SEQUENCE})
    private String newName;

    @Validate(required = true, on = {ADD_REFERENCE_SEQUENCE})
    private String newVersion;

    @After(stages = LifecycleStage.BindingAndValidation, on = {SHOW_ALIGNER, ADD_ALIGNER, REMOVE_ALIGNERS})
    public void afterAlignerValidations() {
        alignerList = alignerDao.findAll();
    }

    @After(stages = LifecycleStage.BindingAndValidation,
            on = {SHOW_ANALYSIS_TYPE, ADD_ANALYSIS_TYPE, REMOVE_ANALYSIS_TYPES})
    public void afterAnalysisTypeValidations() {
        analysisTypeList = analysisTypeDao.findAll();
    }

    @After(stages = LifecycleStage.BindingAndValidation,
            on = {SHOW_REFERENCE_SEQUENCE, ADD_REFERENCE_SEQUENCE, REMOVE_REFERENCE_SEQUENCES})
    public void afterReferenceSequenceValidation() {
        referenceSequenceList = referenceSequenceDao.findAll();
    }

    @After(stages = LifecycleStage.BindingAndValidation,
            on = {SHOW_REAGENT_DESIGN, ADD_REAGENT_DESIGN, REMOVE_REAGENT_DESIGNS})
    public void aferReagentDesignValidation() {
        reagentDesignList = reagentDesignDao.findAll();
    }

    @DefaultHandler
    @HandlesEvent(SHOW_ALIGNER)
    public Resolution showAligner() {
        return new ForwardResolution(MANAGE_ALIGNER_PAGE);
    }

    @HandlesEvent(ADD_ALIGNER)
    public Resolution addAligner() {
        analysisEjb.addAligner(newName);
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter(SHOW_ALIGNER, "");
    }

    @HandlesEvent(REMOVE_ALIGNERS)
    public Resolution removeAligners() {
        analysisEjb.removeAligners(getBusinessKeyArray());
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter(SHOW_ALIGNER, "");
    }

    @HandlesEvent("showAnalysisType")
    public Resolution showAnalysisType() {
        return new ForwardResolution(MANAGE_ANALYSIS_TYPE_PAGE);
    }

    @HandlesEvent(ADD_ANALYSIS_TYPE)
    public Resolution addAnalysisType() {
        analysisEjb.addAnalysisType(newName);
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter(SHOW_ANALYSIS_TYPE, "");
    }

    @HandlesEvent(REMOVE_ANALYSIS_TYPES)
    public Resolution removeAnalysisTypes() {
        analysisEjb.removeAnalysisTypes(getBusinessKeyArray());
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter(SHOW_ANALYSIS_TYPE, "");
    }

    @HandlesEvent(SHOW_REFERENCE_SEQUENCE)
    public Resolution showReferenceSequence() {
        return new ForwardResolution(MANAGE_REFERENCE_SEQUENCE_PAGE);
    }

    @HandlesEvent(ADD_REFERENCE_SEQUENCE)
    public Resolution addReferenceSequence() {
        analysisEjb.addReferenceSequence(newName, newVersion);
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter(SHOW_REFERENCE_SEQUENCE, "");
    }

    @HandlesEvent(REMOVE_REFERENCE_SEQUENCES)
    public Resolution removeReferenceSequences() {
        analysisEjb.removeReferenceSequences(getBusinessKeyArray());
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter(SHOW_REFERENCE_SEQUENCE, "");
    }

    @HandlesEvent(SHOW_REAGENT_DESIGN)
    public Resolution showReagentDesign() {
        return new ForwardResolution(MANAGE_REAGENT_DESIGN_PAGE);
    }

    @HandlesEvent(ADD_REAGENT_DESIGN)
    public Resolution addReagentDesign() {
        analysisEjb.addReagentDesign(newName, selectedReagentType);
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter(SHOW_REAGENT_DESIGN, "");
    }

    @HandlesEvent(REMOVE_REAGENT_DESIGNS)
    public Resolution removeReagentDesigns() {
        analysisEjb.removeReagentDesigns(getBusinessKeyArray());
        return new RedirectResolution(ANALYSIS_FIELDS_ACTION).addParameter(SHOW_REAGENT_DESIGN, "");
    }

    /**
     * This is used to return an array of business keys for objects that we want to delete.
     * This is a utility method that will reduce code duplication.
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
