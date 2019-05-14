package org.broadinstitute.gpinformatics.mercury.presentation.run;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@UrlBinding("/run/DesignationFct.action")
public class DesignationFctActionBean extends CoreActionBean implements DesignationUtils.Caller {
    private static final String VIEW_PAGE = "/run/designation_fct_create.jsp";
    private static final String TUBE_LCSET_PAGE = "/run/designation_fct_lcset_select.jsp";
    public static final String CREATE_FCT_ACTION = "createFct";
    public static final String SET_MULTIPLE_ACTION = "setMultiple";
    public static final String SUBMIT_TUBE_LCSETS_ACTION = "submitTubeLcsets";
    public static final List<FlowcellDesignation.Status> ONLY_QUEUED = Collections.singletonList(
            FlowcellDesignation.Status.QUEUED);
    private boolean diversifySamplesFlag;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private FlowcellDesignationEjb designationTubeEjb;

    private DesignationDto multiEdit = new DesignationDto();
    private List<DesignationDto> dtos = new ArrayList<>();
    private List<MutablePair<String, String>> createdFcts = new ArrayList<>();
    private DesignationUtils utils = new DesignationUtils(this);
    private List<DesignationUtils.LcsetAssignmentDto> tubeLcsetAssignments = new ArrayList<>();

    private final static EnumSet<FlowcellDesignation.Status> fctTargetableStatuses =
            EnumSet.noneOf(FlowcellDesignation.Status.class);

    static {
        fctTargetableStatuses.addAll(DesignationUtils.TARGETABLE_STATUSES);
        fctTargetableStatuses.add(FlowcellDesignation.Status.IN_FCT);
    }

    /**
     * Displays the page with the designation tubes, if any.
     */
    @HandlesEvent(VIEW_ACTION)
    @DefaultHandler
    public Resolution view() {
        if (CollectionUtils.isEmpty(getDtos())) {
            MessageCollection messageCollection = new MessageCollection();
            tubeLcsetAssignments = utils.makeDtosFromDesignations(designationTubeEjb.existingDesignations(ONLY_QUEUED),
                    messageCollection);
            if (!tubeLcsetAssignments.isEmpty()) {
                return new ForwardResolution(TUBE_LCSET_PAGE);
            }
            addMessages(messageCollection);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * This method is called after the user specifies the desired lcset
     * for each loading tube barcode that had mulitple lcsets.
     */
    @HandlesEvent(SUBMIT_TUBE_LCSETS_ACTION)
    public Resolution submitTubeLcsets() {
        clearValidationErrors();
        List<Pair<Long, String>> designationIdLcsets = new ArrayList<>();
        for (DesignationUtils.LcsetAssignmentDto assignmentDto : tubeLcsetAssignments) {
            if (isNotBlank(assignmentDto.getSelectedLcsetName())) {
                designationIdLcsets.add(
                        Pair.of(assignmentDto.getDesignationId(), assignmentDto.getSelectedLcsetName()));
            }
        }

        // Updates the designations based on user's lcset choice and makes dtos from the updated designations.
        // If an lcset was not selected for a designation it remains in database but does not become a dto here.
        Collection<FlowcellDesignation> updates = designationTubeEjb.updateChosenLcset(designationIdLcsets);
        MessageCollection messageCollection = new MessageCollection();
        utils.makeDtosFromDesignations(updates, messageCollection);
        addMessages(messageCollection);

        return view();
    }

    /**
     * Changes and persists the selected UI rows.
     */
    @HandlesEvent(SET_MULTIPLE_ACTION)
    public Resolution setMultiple() {
        // Permits changing an IN_FCT designation since the only ones being displayed
        // will be one created just seconds ago by this user.
        utils.applyMultiEdit(fctTargetableStatuses, designationTubeEjb);
        multiEdit = new DesignationDto();
        return view();
    }

    /**
     * Creates FCTs from queued designations.
     */
    @HandlesEvent(CREATE_FCT_ACTION)
    public Resolution createFct() {

        List<MutablePair<String, String>> fctUrls = labBatchEjb.makeFcts(dtos, userBean.getLoginUserName(),
                (MessageReporter)this, diversifySamplesFlag);

        createdFcts.addAll(fctUrls);
        Collections.sort(createdFcts, new Comparator<MutablePair<String, String>>() {
            @Override
            public int compare(MutablePair<String, String> o1, MutablePair<String, String> o2) {
                return o1.getLeft().compareTo(o2.getLeft());
            }
        });

        return view();
    }


    @Override
    public List<DesignationDto> getDtos() {
        return dtos;
    }

    public void setDtos(List<DesignationDto> dtos) {
        this.dtos = dtos;
    }

    public List<MutablePair<String, String>> getCreatedFcts() {
        return createdFcts;
    }

    public void setCreatedFcts(List<MutablePair<String, String>> createdFcts) {
        this.createdFcts = createdFcts;
    }

    public DesignationUtils getUtils() {
        return utils;
    }

    @Override
    public DesignationDto getMultiEdit() {
        return multiEdit;
    }

    @Override
    public void setMultiEdit(DesignationDto multiEdit) {
        this.multiEdit = multiEdit;
    }

    public List<DesignationUtils.LcsetAssignmentDto> getTubeLcsetAssignments() {
        return tubeLcsetAssignments;
    }

    public void setTubeLcsetAssignments(
            List<DesignationUtils.LcsetAssignmentDto> tubeLcsetAssignments) {
        this.tubeLcsetAssignments = tubeLcsetAssignments;
    }

    public boolean isDiversifySamplesFlag() {
        return diversifySamplesFlag;
    }

    public void setDiversifySamplesFlag(boolean diversifySamplesFlag) {
        this.diversifySamplesFlag = diversifySamplesFlag;
    }
}
