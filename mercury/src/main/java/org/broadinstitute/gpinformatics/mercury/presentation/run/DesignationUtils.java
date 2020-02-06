package org.broadinstitute.gpinformatics.mercury.presentation.run;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.mercury.presentation.workflow.CreateFCTActionBean.CONTROLS;

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
    public static final String UNKNOWN_LCSET_MSG = "Designation (id %d) for tube %s has unknown LCSET.";
    private Caller caller;

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

    public DesignationUtils(Caller caller) {
        this.caller = caller;
    }

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
            if (dto != null && dto.isSelected()) {
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
                if (multiEdit.getPairedEndRead() != null) {
                    dto.setPairedEndRead(multiEdit.getPairedEndRead());
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
    public static void updateDesignationsAndDtos(Collection<DesignationDto> dtos,
                                                 EnumSet<FlowcellDesignation.Status> persistableStatuses,
                                                 FlowcellDesignationEjb designationTubeEjb) {
        List<Pair<DesignationDto, FlowcellDesignation>> pairs = designationTubeEjb.update(dtos, persistableStatuses);
        for (Pair<DesignationDto, FlowcellDesignation> dtoAndTube : pairs) {
            // After Hibernate flushes new entities the dto can get the updated designation id.
            DesignationDto dto = dtoAndTube.getKey();
            FlowcellDesignation designation = dtoAndTube.getValue();
            dto.setDesignationId(designation.getDesignationId());
            dto.setSelected(false);
        }
    }

    /**
     * Makes dtos from existing designations. This is essentially starting from a loading tube
     * since the lcset will have been normalized, except in the case where there was a chosen lcet.
     *
     * @param flowcellDesignations the designations to be made into Dtos.
     * @param messageCollection any messages for the UI are returned in this collection.
     * @return any designations having multiple LCSETs are put in this collection so the user can choose.
     */
    public List<LcsetAssignmentDto> makeDtosFromDesignations(Collection<FlowcellDesignation> flowcellDesignations,
                                                             MessageCollection messageCollection) {
        List<LcsetAssignmentDto> lcsetAssignmentDtos = new ArrayList<>();
        for (FlowcellDesignation designation : flowcellDesignations) {
            Set<SampleInstanceV2> sampleInstances = designation.getStartingTube().getSampleInstancesV2();
            Set<LabBatch> lcsets = sampleInstances.stream().
                    flatMap(sampleInstance -> sampleInstance.getAllWorkflowBatches().stream()).
                    collect(Collectors.toSet());
            // If there is only one lcset, uses it. If there are multiple lcsets and one of those is the
            // lcset that had been chosen by the user, uses it. Otherwise returns an error message.
            LabBatch lcset = lcsets.size() == 1 ? lcsets.iterator().next() :
                    (designation.getChosenLcset() != null && lcsets.contains(designation.getChosenLcset()) ?
                            designation.getChosenLcset() : null);
            if (lcset == null) {
                if (lcsets.size() > 1) {
                    // Returns the ambiguous lcset assignment dtos in case it's a good time to
                    // have the user to choose one.
                    LcsetAssignmentDto assignmentDto = new LcsetAssignmentDto(designation.getStartingTube(), lcsets);
                    assignmentDto.setDesignationId(designation.getDesignationId());
                    lcsetAssignmentDtos.add(assignmentDto);
                }
                messageCollection.addError(String.format(UNKNOWN_LCSET_MSG, designation.getDesignationId(),
                        designation.getStartingTube().getLabel()));
            } else {
                Set<BucketEntry> bucketEntries = new HashSet<>();
                Set<LabVessel> startingVessels = new HashSet<>();

                for (SampleInstanceV2 sampleInstance : sampleInstances) {
                    if (sampleInstance.getAllWorkflowBatches().contains(lcset)) {

                        // Finds starting vessels for this tube in this lcset.
                        for (LabBatchStartingVessel startingVessel : sampleInstance.getAllBatchVessels(
                                lcset.getLabBatchType())) {
                            if (lcset.equals(startingVessel.getLabBatch())) {
                                startingVessels.add(startingVessel.getLabVessel());
                            }
                        }

                        for (BucketEntry bucketEntry : sampleInstance.getAllBucketEntries()) {
                            if (lcset.equals(bucketEntry.getLabBatch())) {
                                bucketEntries.add(bucketEntry);
                            }
                        }
                    }
                }

                // A starting vessel with no bucket entry is a positive control.
                Set<LabVessel> controls = new HashSet<>(startingVessels);
                for (BucketEntry bucketEntry : bucketEntries) {
                    controls.remove(bucketEntry.getLabVessel());
                }

                DesignationDto designationDto = makeDesignationDto(designation.getStartingTube(),
                        lcset, bucketEntries, controls, designation);

                caller.getDtos().add(designationDto);
            }
        }
        return lcsetAssignmentDtos;
    }

    /**
     * Makes a designation dto for UI display. If an existing designation is passed in then its
     * user selected values are used. Normalized lcset and related info is obtained from
     * the collections passed in.
     */
    public static DesignationDto makeDesignationDto(LabVessel loadingTube, LabBatch lcset,
                                                    Collection<BucketEntry> bucketEntries,
                                                    Collection<LabVessel> controlTubes,
                                                    FlowcellDesignation flowcellDesignation) {

        // Populates values from existing flowcell designation if it exists.
        DesignationDto dto = new DesignationDto(flowcellDesignation);
        dto.setTypeAndDate(loadingTube);
        dto.setBarcode(loadingTube.getLabel());
        dto.setLcset(lcset.getBatchName());
        dto.setLcsetUrl(lcset.getJiraTicket().getBrowserUrl());

        int numberSamples = 0;
        Multimap<String, String> productToStartingVessel = HashMultimap.create();
        for (BucketEntry bucketEntry : bucketEntries) {
            Product product = bucketEntry.getProductOrder().getProduct();
            String productName;
            if (product != null) {
                productName = product.getProductName();
                // Sets the default values but doesn't override a possible user setting in existing dto.
                if (dto.getReadLength() == null) {
                    dto.setReadLength(product.getReadLength());
                    // todo emp get read length override when it exists in the ProductOrder.
                }
                if (dto.getPairedEndRead() == null) {
                    dto.setPairedEndRead(product.getPairedEndRead());
                }
                if (dto.getLoadingConc() == null) {
                    dto.setLoadingConc(product.getLoadingConcentration());
                }
                if (dto.getIndexType() == null) {
                    FlowcellDesignation.IndexType indexType = (product.getIndexType() != null) ?
                            product.getIndexType() : FlowcellDesignation.IndexType.DUAL;
                    dto.setIndexType(indexType);
                }
            } else {
                productName = "[" + bucketEntry.getProductOrder().getJiraTicketKey() + "]";
            }
            if (dto.getIndexType() == null) {
                dto.setIndexType(FlowcellDesignation.IndexType.NONE);
            }
            productToStartingVessel.put(productName, bucketEntry.getLabVessel().getLabel());

            // Sets the regulatory designation to Clinical, Research, or mixed.
            if (bucketEntry.getProductOrder().getResearchProject().getRegulatoryDesignation().isClinical()) {
                if (dto.getRegulatoryDesignation() == null) {
                    dto.setRegulatoryDesignation(DesignationUtils.CLINICAL);
                } else if (!dto.getRegulatoryDesignation().equals(DesignationUtils.CLINICAL)) {
                    dto.setRegulatoryDesignation(DesignationUtils.MIXED);
                }
            } else {
                if (dto.getRegulatoryDesignation() == null) {
                    dto.setRegulatoryDesignation(DesignationUtils.RESEARCH);
                } else if (!dto.getRegulatoryDesignation().equals(DesignationUtils.RESEARCH)) {
                    dto.setRegulatoryDesignation(DesignationUtils.MIXED);
                }
            }
            numberSamples += bucketEntry.getLabVessel().getSampleNames().size();
        }

        // Handles any positive control samples that were pooled into this loading tube.
        for (LabVessel tube : controlTubes) {
            productToStartingVessel.put(CONTROLS, tube.getLabel());
            numberSamples += tube.getSampleNames().size();
        }

        dto.setNumberSamples(numberSamples);

        // Each product will have a corresponding list of starting vessels.
        List<String> productNames = new ArrayList<>(productToStartingVessel.keySet());
        Collections.sort(productNames);
        List<String> startingVesselList = new ArrayList<>();
        for (String productName : productNames) {
            List<String> startingVessels = new ArrayList<>(productToStartingVessel.get(productName));
            Collections.sort(startingVessels);
            startingVesselList.add((productName.equals(CONTROLS) ?
                    productName : "Bucketed tubes for " + productName) + ": " +
                                   StringUtils.join(startingVessels, ", "));
        }
        dto.setStartingBatchVessels(StringUtils.join(startingVesselList, "\n"));
        dto.setProductNames(productNames);

        return dto;
    }

    /**
     * Represents a UI row when prompting user to associate an LCSET with a loading tube.
     */
    public static class LcsetAssignmentDto {
        private String barcode;
        private Date tubeDate;
        private List<Pair<String, String>> lcsetNameUrls = new ArrayList<>();
        private String selectedLcsetName = "";
        @Nullable
        private Long designationId;

        public LcsetAssignmentDto() {
        }

        public LcsetAssignmentDto(LabVessel targetTube, Set<LabBatch> lcsets) {
            barcode = targetTube.getLabel();
            tubeDate = targetTube.getCreatedOn();
            for (LabBatch lcset : lcsets) {
                lcsetNameUrls.add(Pair.of(lcset.getBatchName(), lcset.getJiraTicket().getBrowserUrl()));
            }
        }

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public Date getTubeDate() {
            return tubeDate;
        }

        public void setTubeDate(Date tubeDate) {
            this.tubeDate = tubeDate;
        }

        public List<Pair<String, String>> getLcsetNameUrls() {
            return lcsetNameUrls;
        }

        public String getSelectedLcsetName() {
            return selectedLcsetName;
        }

        public void setSelectedLcsetName(String selectedLcsetName) {
            this.selectedLcsetName = selectedLcsetName;
        }

        public Long getDesignationId() {
            return designationId;
        }

        public void setDesignationId(Long designationId) {
            this.designationId = designationId;
        }

        /** Sorts the assignment dtos and their lcsets. */
        public static void sort(List<LcsetAssignmentDto> assignmentDtos) {
            // Sorts the lcset list on each dto.
            for (LcsetAssignmentDto dto : assignmentDtos) {
                Collections.sort(dto.lcsetNameUrls, new Comparator<Pair<String, String>>() {
                    @Override
                    public int compare(Pair<String, String> o1, Pair<String, String> o2) {
                        return o1.getLeft().compareTo(o2.getLeft());
                    }
                });
            }
            // Sorts the dtos by barcode.
            Collections.sort(assignmentDtos, new Comparator<LcsetAssignmentDto>() {
                @Override
                public int compare(LcsetAssignmentDto o1, LcsetAssignmentDto o2) {
                    return o1.getBarcode().compareTo(o2.getBarcode());
                }
            });
        }

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
}