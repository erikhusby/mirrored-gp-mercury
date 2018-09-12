package org.broadinstitute.gpinformatics.mercury.control.sample;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.HeaderValueRow;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntityTsk;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles spreadsheet uploads for "Pooled Tube" external DEV samples.
 */
public class VesselPooledTubesProcessor extends ExternalLibraryProcessor {
    private List<String> baits = new ArrayList<>();
    private List<String> barcodes = new ArrayList<>();
    private List<String> broadParticipantIds = new ArrayList<>();
    private List<String> cats = new ArrayList<>();
    private List<String> collaboratorParticipantIds = new ArrayList<>();
    private List<String> collaboratorSampleIds = new ArrayList<>();
    // conditions is a per-row list of one or more DEV ticket ids.
    private List<List<String>> conditions = new ArrayList<>();
    private List<String> experiments = new ArrayList<>();
    private List<String> fragmentSizes = new ArrayList<>();
    private List<String> libraryNames = new ArrayList<>();
    private List<String> lsids = new ArrayList<>();
    private List<String> molecularBarcodeNames = new ArrayList<>();
    private List<String> organisms = new ArrayList<>();
    private List<String> readLengths = new ArrayList<>();
    private List<Boolean> requiredValuesPresent = new ArrayList<>();
    private List<String> rootSampleNames = new ArrayList<>();
    private List<String> sampleNames = new ArrayList<>();
    private List<String> sexes = new ArrayList<>();
    private List<String> volumes = new ArrayList<>();

    public VesselPooledTubesProcessor() {
        super(null);
        headerValueNames.clear();
    }

    // Only the first four words of header text are used and the rest are ignored.
    // Orderining of headers is only important for generating template spreadsheets in the ActionBean.
    public enum Headers implements ColumnHeader, ColumnHeader.Ignorable {
        TUBE_BARCODE("Tube Barcode", REQUIRED, true),
        LIBRARY_NAME("Library Name", REQUIRED, true),
        BROAD_SAMPLE_ID("Broad Sample Id", REQUIRED, true),
        ROOT_SAMPLE_ID("Root Sample Id", OPTIONAL, true),
        MOLECULAR_INDEXING_SCHEME("Molecular Indexing Scheme", REQUIRED, true),
        BAIT("Bait", OPTIONAL, true),
        CAT("CAT", OPTIONAL, true),
        EXPERIMENT("Experiment", REQUIRED, true),
        CONDITIONS("Conditions", REQUIRED, true),
        COLLABORATOR_SAMPLE_ID("Collaborator Sample Id", OPTIONAL, true),
        COLLABORATOR_PARTICIPANT_ID("Collaborator Participant Id", OPTIONAL, true),
        BROAD_PARTICIPANT_ID("Broad Participant Id", OPTIONAL, true),
        GENDER("Gender", OPTIONAL, true),
        SPECIES("Species", OPTIONAL, true),
        VOLUME("Volume", OPTIONAL, true), // Required on the first row for a tube.
        FRAGMENT_SIZE("Fragment Size", OPTIONAL, true), // Required on the first row for a tube.
        READ_LENGTH("Read Length", OPTIONAL, true),
        LSID("Lsid", OPTIONAL, true),
        ;
        private final String text;
        private boolean isRequired;
        private boolean isString;

        Headers(String text, boolean isRequired, boolean isString) {
            this.text = text;
            this.isRequired = isRequired;
            this.isString = isString;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean isRequiredHeader() {
            return isRequired;
        }

        @Override
        public boolean isRequiredValue() {
            return isRequired;
        }

        @Override
        public boolean isIgnoredValue() {
            return false;
        }

        @Override
        public boolean isOnlyOncePerEntity() {
            return this == VOLUME || this == FRAGMENT_SIZE;
        }

        @Override
        public boolean isDateColumn() {
            return false;
        }

        @Override
        public boolean isStringColumn() {
            return isString;
        }
    }

    @Override
    public HeaderValueRow[] getHeaderValueRows() {
        return new HeaderValueRow[0];
    }

    /** Parses a single spreadsheet row and puts column data in lists that are indexed by rowIndex. */
    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        baits.add(getFromRow(dataRow, Headers.BAIT));
        barcodes.add(getFromRow(dataRow, Headers.TUBE_BARCODE));
        broadParticipantIds.add(getFromRow(dataRow, Headers.BROAD_PARTICIPANT_ID));
        cats.add(getFromRow(dataRow, Headers.CAT));
        collaboratorParticipantIds.add(getFromRow(dataRow, Headers.COLLABORATOR_PARTICIPANT_ID));
        collaboratorSampleIds.add(getFromRow(dataRow, Headers.COLLABORATOR_SAMPLE_ID));
        conditions.add(Arrays.asList(StringUtils.stripAll(getFromRow(dataRow, Headers.CONDITIONS).split(","))));
        experiments.add(getFromRow(dataRow, Headers.EXPERIMENT));
        fragmentSizes.add(getFromRow(dataRow, Headers.FRAGMENT_SIZE));
        libraryNames.add(getFromRow(dataRow, Headers.LIBRARY_NAME));
        lsids.add(getFromRow(dataRow, Headers.LSID));
        molecularBarcodeNames.add(getFromRow(dataRow, Headers.MOLECULAR_INDEXING_SCHEME));
        organisms.add(getFromRow(dataRow, Headers.SPECIES));
        readLengths.add(getFromRow(dataRow, Headers.READ_LENGTH));
        rootSampleNames.add(getFromRow(dataRow, Headers.ROOT_SAMPLE_ID));
        sampleNames.add(getFromRow(dataRow, Headers.BROAD_SAMPLE_ID));
        sexes.add(getFromRow(dataRow, Headers.GENDER));
        volumes.add(getFromRow(dataRow, Headers.VOLUME));

        this.requiredValuesPresent.add(requiredValuesPresent);
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    /**
     * Does self-consistency and other validation checks on the data.
     * Entities fetched for the row data are accessed through maps referenced in the dtos.
     */
    @Override
    public void validateAllRows(List<SampleInstanceEjb.RowDto> dtos, boolean overwrite, MessageCollection messages) {
        // At this point all data is in dtos, and entities referenced by the data are in maps.
        Set<String> uniqueLibraryNames = new HashSet<>();
        Set<String> uniqueTubeAndMis = new HashSet<>();
        Set<String> uniqueSampleAndMis = new HashSet<>();
        // Looks up the Jira tickets identified by Experiment and gets the sub-tasks.
        Multimap<String, String> ticketAndSubtasks = HashMultimap.create();
        dtos.stream().map(SampleInstanceEjb.RowDto::getExperiment).distinct().filter(s -> StringUtils.isNotBlank(s))
                .forEach(experiment -> {
                    JiraIssue jiraIssue = getJiraIssueMap().get(experiment);
                    if (jiraIssue != null) {
                        ticketAndSubtasks.putAll(experiment, CollectionUtils.emptyIfNull(jiraIssue.getSubTaskKeys()));
                    }
                });

        for (SampleInstanceEjb.RowDto dto : dtos) {
            // If the sample appears in multiple spreadsheet rows, the sample metadata values must match the
            // first occurrence, or be blank. The first occurrence must have all of the sample metadata.
            if (StringUtils.isNotBlank(dto.getSampleName())) {
                consistentSampleData(mapSampleNameToFirstRow.get(dto.getSampleName()), dto, messages);
            }

            if (dto.getSampleInstanceEntity() != null && !overwrite) {
                messages.addError(String.format(SampleInstanceEjb.PREXISTING, dto.getRowNumber(),
                        "Library", dto.getLibraryName()));
            }

            if (!uniqueLibraryNames.add(dto.getLibraryName())) {
                messages.addError(String.format(SampleInstanceEjb.DUPLICATE, dto.getRowNumber(), "Library Name"));
            }

            String barcode = dto.getBarcode();
            if (StringUtils.isNotBlank(barcode)) {
                consistentTubeData(mapBarcodeToFirstRow.get(barcode), dto, messages);
            }

            LabVessel tube = getLabVesselMap().get(barcode);
            if (tube != null) {
                if (!overwrite) {
                    messages.addError(String.format(SampleInstanceEjb.PREXISTING, dto.getRowNumber(), "Tube", barcode));
                }
            } else {
                // A new tube barcode character set is restricted.
                if (!StringUtils.containsOnly(barcode, SampleInstanceEjb.RESTRICTED_CHARS)) {
                    messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(), "Tube barcode",
                            "composed of " + SampleInstanceEjb.RESTRICTED_MESSAGE));
                }
            }

            if (StringUtils.isNotBlank(dto.getMisName())) {
                MolecularIndexingScheme molecularIndexingScheme = getMolecularIndexingSchemeMap().get(
                        dto.getMisName());
                if (molecularIndexingScheme == null) {
                    messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                            Headers.MOLECULAR_INDEXING_SCHEME.getText(), "Mercury"));
                }
                // Errors if a tube has duplicate Molecular Index Scheme.
                if (StringUtils.isNotBlank(barcode) && !uniqueTubeAndMis.add(barcode + " " + dto.getMisName())) {
                    messages.addError(String.format(SampleInstanceEjb.DUPLICATE_IN_TUBE, dto.getRowNumber(),
                            Headers.MOLECULAR_INDEXING_SCHEME.getText(),  barcode));
                }

                // Warns if the spreadsheet has duplicate combination of Broad Sample and Molecular Index Scheme
                // (in different tubes). It's not an error as long as the tubes don't get pooled later on, which
                // isn't known at upload time.
                String sampleMis = dto.getSampleName() + " " + dto.getMisName();
                if (!uniqueSampleAndMis.add(sampleMis)) {
                    messages.addWarning(String.format(SampleInstanceEjb.DUPLICATE_S_M, dto.getRowNumber(),
                            dto.getSampleName(), dto.getMisName()));
                }
            }

            // Either bait or cat may be specified, or neither.
            if (StringUtils.isNotBlank(dto.getBait()) && StringUtils.isNotBlank(dto.getCat())) {
                messages.addError(String.format(SampleInstanceEjb.MUST_NOT_HAVE_BOTH, dto.getRowNumber(),
                        Headers.BAIT.getText(), Headers.CAT.getText()));

            } else if (StringUtils.isNotBlank(dto.getBait())) {
                if (dto.getReagent() == null) {
                    messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                            Headers.BAIT.getText(), "Mercury"));
                }
            } else if (StringUtils.isNotBlank(dto.getCat())) {
                if (dto.getReagent() == null) {
                    messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                            Headers.CAT.getText(), "Mercury"));
                }
            }

            errorBspMetadataChanges(dto, overwrite, messages);

            // It's an error if the upload refers to a root that doesn't exist and was not given in the upload.
            if (StringUtils.isNotBlank(dto.getRootSampleName()) &&
                    !getSampleMap().containsKey(dto.getRootSampleName()) &&
                    !mapSampleNameToFirstRow.containsKey(dto.getRootSampleName())) {
                messages.addError(SampleInstanceEjb.NONEXISTENT, dto.getRowNumber(), "Root Sample",
                        dto.getRootSampleName(), "Mercury");
            }

            // Errors invalid experiment or conditions.
            if (StringUtils.isNotBlank(dto.getExperiment())) {
                if (!ticketAndSubtasks.containsKey(dto.getExperiment())) {
                    messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                            Headers.EXPERIMENT.getText(), "JIRA DEV"));
                } else {
                    // Conditions must be listed as sub-tasks on the JIRA ticket.
                    if (!ticketAndSubtasks.get(dto.getExperiment()).containsAll(dto.getConditions())) {
                        messages.addError(String.format(SampleInstanceEjb.UNKNOWN_COND, dto.getRowNumber(),
                                dto.getExperiment()));
                    }
                }
            }
        }
    }

    /**
     * Makes the Samples, LabVessels, and SampleInstanceEntities but does not persist them.
     * New/modified entities are added to the dto shared collection getEntitiesToUpdate().
     * @return the list of SampleInstanceEntity, for testing purposes.
     */
    @Override
    public List<SampleInstanceEntity> makeOrUpdateEntities(List<SampleInstanceEjb.RowDto> dtos) {
        makeTubesAndSamples(dtos);
        List<SampleInstanceEntity> sampleInstanceEntities = makeSampleInstanceEntities(dtos);

        // Add the SampleInstanceEntitiyTasks.
        for (SampleInstanceEjb.RowDto dto : dtos) {
            dto.getSampleInstanceEntity().removeSubTasks();
            int orderOfCreation = 0;
            for (String subTask : dto.getConditions()) {
                SampleInstanceEntityTsk sampleInstanceEntityTsk = new SampleInstanceEntityTsk();
                sampleInstanceEntityTsk.setSubTask(subTask);
                sampleInstanceEntityTsk.setOrderOfCreation(orderOfCreation++);
                dto.getSampleInstanceEntity().addSubTasks(sampleInstanceEntityTsk);
            }
        }
        return sampleInstanceEntities;
    }

    @Override
    public List<String> getSampleNames() {
        return sampleNames;
    }

    @Override
    public List<String> getRootSampleNames() {
        return rootSampleNames;
    }

    @Override
    public List<String> getBaits() {
        return baits;
    }

    @Override
    public List<String> getCats() {
        return cats;
    }

    @Override
    public List<String> getExperiments() {
        return experiments;
    }

    @Override
    public List<List<String>> getConditions() {
        return conditions;
    }

    @Override
    public List<String> getCollaboratorParticipantIds() {
        return collaboratorParticipantIds;
    }

    @Override
    public List<String> getBroadParticipantIds() {
        return broadParticipantIds;
    }

    @Override
    public List<String> getLsids() {
        return lsids;
    }

    @Override
    public List<String> getBarcodes() {
        return barcodes;
    }

    @Override
    public List<String> getCollaboratorSampleIds() {
        return collaboratorSampleIds;
    }

    @Override
    public List<Boolean> getRequiredValuesPresent() {
        return requiredValuesPresent;
    }

    @Override
    public List<String> getReadLengths() {
        return readLengths;
    }

    @Override
    public List<String> getMolecularBarcodeNames() {
        return molecularBarcodeNames;
    }

    @Override
    public List<String> getOrganisms() {
        return organisms;
    }

    @Override
    public List<String> getLibraryNames() {
        return libraryNames;
    }

    @Override
    public List<String> getVolumes() {
        return volumes;
    }

    @Override
    public List<String> getSexes() {
        return sexes;
    }

    @Override
    public boolean supportsSampleKitRequest() {
        return false;
    }

}
