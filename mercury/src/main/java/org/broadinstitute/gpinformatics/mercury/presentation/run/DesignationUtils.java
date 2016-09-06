package org.broadinstitute.gpinformatics.mercury.presentation.run;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Methods called by designation action beans.
 */
public class DesignationUtils {
    public static final String CLINICAL = "Clinical";
    public static final String RESEARCH = "Research";
    public static final String MIXED = "Clinical and Research";
    public static final EnumSet<FlowcellDesignation.Status> MODIFIABLE_STATUSES = EnumSet.noneOf(
            FlowcellDesignation.Status.class);
    public static final EnumSet<FlowcellDesignation.Status> TARGETABLE_STATUSES = EnumSet.noneOf(
            FlowcellDesignation.Status.class);

    static {
        for (FlowcellDesignation.Status status : FlowcellDesignation.Status.values()) {
            if (status.isModifiable()) {
                MODIFIABLE_STATUSES.add(status);
            }
            if (status.isTargetable()) {
                TARGETABLE_STATUSES.add(status);
            }
        }
    }


    public DesignationUtils() {}

    public DesignationUtils(Caller caller) {
        this.caller = caller;
    }

    private Caller caller;

    /** The calling class must implement this interface. */
    public interface Caller {
        public DesignationDto getMultiEdit();
        public void setMultiEdit(DesignationDto dto);
        public List<DesignationDto> getDtos();
    }

    /**
     * Changes the selected dto rows, and persists them if their status is persistable.
     */
    public void applyMultiEdit(EnumSet<FlowcellDesignation.Status> persistableStatuses,
                               FlowcellDesignationEjb designationTubeEjb) {
        DesignationDto multiEdit = caller.getMultiEdit();

        for (Iterator<DesignationDto> iter = caller.getDtos().iterator(); iter.hasNext(); ) {
            DesignationDto dto = iter.next();
            if (dto.isSelected()) {
                if (multiEdit.getStatus() != null) {
                    // Abandoning an unsaved dto just removes it from the list and not persisted.
                    if (dto.getStatus() == FlowcellDesignation.Status.UNSAVED
                        && multiEdit.getStatus() == FlowcellDesignation.Status.ABANDONED) {
                        iter.remove();
                        continue;
                    } else {
                        dto.setStatus(multiEdit.getStatus());
                    }
                }
                if (multiEdit.getPriority() != null) {
                    dto.setPriority(multiEdit.getPriority());
                }
                if (multiEdit.getSequencerModel() != null) {
                    dto.setSequencerModel(multiEdit.getSequencerModel());
                }
                if (multiEdit.getNumberLanes() != null) {
                    dto.setNumberLanes(multiEdit.getNumberLanes());
                }
                if (multiEdit.getLoadingConc() != null) {
                    dto.setLoadingConc(multiEdit.getLoadingConc());
                }
                if (multiEdit.getReadLength() != null) {
                    dto.setReadLength(multiEdit.getReadLength());
                }
                if (multiEdit.getIndexType() != null) {
                    dto.setIndexType(multiEdit.getIndexType());
                }
                if (multiEdit.getNumberCycles() != null) {
                    dto.setNumberCycles(multiEdit.getNumberCycles());
                }
                if (multiEdit.getPoolTest() != null) {
                    dto.setPoolTest(multiEdit.getPoolTest());
                }
            }
        }
        caller.setMultiEdit(new DesignationDto());
        updateDesignationsAndDtos(caller.getDtos(), persistableStatuses, designationTubeEjb);
    }

    /**
     * Persists existing and new designations from the dtos, and updates the dtos' entity ids.
     * @param dtos The dtos. Persists those that have isSelected = true.
     * @param persistableStatuses The dto status that permits persisting it.
     * @param designationTubeEjb
     */
    public void updateDesignationsAndDtos(Collection<DesignationDto> dtos,
                                          EnumSet<FlowcellDesignation.Status> persistableStatuses,
                                          FlowcellDesignationEjb designationTubeEjb) {
        for (Map.Entry<DesignationDto, FlowcellDesignation> dtoAndTube :
                designationTubeEjb.update(dtos, persistableStatuses).entrySet()) {
            // After Hibernate flushes new entities the dto can get the updated designation id.
            DesignationDto dto = dtoAndTube.getKey();
            FlowcellDesignation designation = dtoAndTube.getValue();
            dto.setDesignationId(designation.getDesignationId());
            dto.setSelected(false);
        }
    }

    /** Makes dtos from existing designations. */
    public void makeDtosFromDesignations(Collection<FlowcellDesignation> flowcellDesignations) {
        for (FlowcellDesignation designation : flowcellDesignations) {

            LabBatch lcset = designation.getLcset();
            Set<String> productNames = new HashSet<>();
            for (Product product : designation.getProducts()) {
                productNames.add(product.getProductName());
            }

            DesignationDto designationDto = new DesignationDto(designation.getLoadingTube(),
                    Collections.singletonList(designation.getLoadingTubeEvent()),
                    lcset.getJiraTicket().getBrowserUrl(), lcset, Collections.<String>emptySet(), productNames, "",
                    // MIXED cannot be persisted, only CLINICAL or RESEARCH.
                    designation.isClinical() ? CLINICAL : RESEARCH,
                    designation.getNumberSamples(), designation.getSequencerModel(), designation.getIndexType(),
                    designation.getNumberCycles(), designation.getNumberLanes(), designation.getReadLength(),
                    designation.getLoadingConc(), designation.isPoolTest(), designation.getCreatedOn(),
                    designation.getStatus());

            designationDto.setDesignationId(designation.getDesignationId());
            designationDto.setPriority(designation.getPriority());

            caller.getDtos().add(designationDto);
        }
    }

    /** Returns the primary and additional lcsets. */
    public Pair<LabBatch, List<String>> sortLcsets(Collection<LabBatch> lcsets) {
        List<String> lcsetNames = new ArrayList<>();
        for (LabBatch lcset : lcsets) {
            lcsetNames.add(lcset.getBatchName());
        }
        Collections.sort(lcsetNames);
        // Puts the url on the latest lcset when there are multiple.
        String primaryLcsetName = lcsetNames.remove(lcsetNames.size() - 1);
        LabBatch primaryLcset = null;
        for (LabBatch lcset : lcsets) {
            if (primaryLcsetName.equals(lcset.getBatchName())) {
                primaryLcset = lcset;
            }
        }
        return Pair.of(primaryLcset, lcsetNames);
    }


    public FlowcellDesignation.Priority[] getPriorityValues() {
        return FlowcellDesignation.Priority.values();
    }

    public IlluminaFlowcell.FlowcellType[] getFlowcellValues() {
        return IlluminaFlowcell.FlowcellType.values();
    }

    public FlowcellDesignation.IndexType[] getIndexTypes() {
        return FlowcellDesignation.IndexType.values();
    }

    /** Returns the status values for designations that the user can edit, i.e. not the completed ones. */
    public List<FlowcellDesignation.Status> getStatusValues() {
        List<FlowcellDesignation.Status> statusValues = new ArrayList<>();
        for (FlowcellDesignation.Status status : FlowcellDesignation.Status.values()) {
            if (status.isTargetable()) {
                statusValues.add(status);
            }
        }
        return statusValues;
    }

    public Caller getCaller() {
        return caller;
    }

    public void setCaller(Caller caller) {
        this.caller = caller;
    }

    public static EnumSet<FlowcellDesignation.Status> getModifiableStatuses() {
        return MODIFIABLE_STATUSES;
    }
}