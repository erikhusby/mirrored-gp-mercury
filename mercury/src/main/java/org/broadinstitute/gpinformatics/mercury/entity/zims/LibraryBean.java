package org.broadinstitute.gpinformatics.mercury.entity.zims;

import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * A library DTO for Zamboni.  Copied from
 * LIMQuery.thrift from squidThriftService.
 */

public class LibraryBean {
    public static final String NO_WORKFLOW = null;
    public static final String NO_PDO_SAMPLE = null;

    @JsonProperty("library")
    private String library;

    @JsonProperty("project")
    private String project;

    @JsonProperty("initiative")
    private String initiative; // Squid only

    @JsonProperty("workRequestId")
    private Long workRequest; // Squid only

    @JsonProperty("molecularIndexingScheme")
    private MolecularIndexingSchemeBean indexingScheme;

    private Boolean hasIndexingRead;

    @JsonProperty("expectedInsertSize")
    private String expectedInsertSize; // Squid only

    @JsonProperty("analysisType")
    private String analysisType; // Squid only

    @JsonProperty("referenceSequence")
    private String referenceSequence; // Squid only

    @JsonProperty("referenceSequenceVersion")
    private String referenceSequenceVersion; // Squid only

    @JsonProperty("aggregate")
    private Boolean doAggregation; // Squid only

    @JsonProperty("species")
    private String species;

    @JsonProperty("lsid")
    private String sampleLSID;

    @JsonProperty("aligner")
    private String aligner; // Squid only

    @JsonProperty("rrbsSizeRange")
    private String rrbsSizeRange; // Squid only

    @JsonProperty("restrictionEnzyme")
    private String restrictionEnzyme; // Squid only

    @JsonProperty("baitSetName")
    private String bait; // Squid only

    /** obfuscated name of the participantId (person) from whence this sample was taken */
    @JsonProperty("participantId")
    private String participantId;

    @JsonProperty("labMeasuredInsertSize")
    private Double labMeasuredInsertSize; // Squid only (for now)

    @JsonProperty("positiveControl")
    private Boolean isPositiveControl; // Squid only

    @JsonProperty("negativeControl")
    private Boolean isNegativeControl; // Squid only

    @JsonProperty("devExperimentData")
    private DevExperimentDataBean devExperimentData; // Squid only

    @JsonProperty("gssrBarcodes")
    private Collection<String> gssrBarcodes = new ArrayList<>(); // Squid only

    @JsonProperty("customAmpliconSetNames")
    private Collection<String> customAmpliconSetNames = new ArrayList<>(); // Squid only

    @JsonProperty
    private String lcSet;

    @JsonProperty
    private String productOrderTitle;

    @JsonProperty
    private String productOrderKey;

    @JsonProperty
    private String researchProjectName;

    @JsonProperty
    private String researchProjectId;

    @JsonProperty
    private String product;

    @JsonProperty
    private String productFamily;

    @JsonProperty
    private String rootSample;

    @JsonProperty
    private String sampleId; // BSP sample ID

    @JsonProperty
    private String gender;

    @JsonProperty
    private String collection;

    @JsonProperty
    private String primaryDisease;

    @JsonProperty
    private String sampleType; // GSSR?

    @JsonProperty
    private String collaboratorSampleId;

    @JsonProperty
    /** name that the collaborator gave to the participant */
    private String collaboratorParticipantId;

    @JsonProperty
    private String materialType;

    @JsonProperty
    private String pdoSample;

    @JsonProperty("labWorkflow")
    private String labWorkflow;
    /**
     * This is the aggregation data type defined on the product and used by Picard to report the right data.
     */
    @JsonProperty
    private String dataType;

    @JsonProperty
    private boolean isGssrSample;

    @JsonProperty
    private String population;

    @JsonProperty
    private String race;

    @JsonProperty
    private String productPartNumber;

    public LibraryBean() {}

    /**
     * Sets gssr parameters and then overrides them with BSP values.  Useful for testing.
     *
     * @param gssrLsid The lsid of the gssr sample.
     * @param bspSampleDTO The BSP representation of the sample.
     */
    LibraryBean(String gssrLsid,
                       String gssrMaterialType,
                       String gssrCollaboratorSampleId,
                       String gssrOrganism,
                       String gssrSpecies,
                       String gssrStrain,
                       String gssrIndividual,
                       BSPSampleDTO bspSampleDTO, String labWorkflow, String pdoSample) {
        sampleLSID = gssrLsid;
        materialType = gssrMaterialType;
        collaboratorSampleId = gssrCollaboratorSampleId;
        this.labWorkflow = labWorkflow;
        this.pdoSample = pdoSample;
        species = gssrOrganism + ":" + gssrSpecies + ":" + gssrStrain;
        collaboratorParticipantId = gssrIndividual;
        overrideSampleFieldsFromBSP(bspSampleDTO);
    }

    public LibraryBean(String library, String initiative, Long workRequest,
                       MolecularIndexingScheme indexingScheme, Boolean hasIndexingRead, String expectedInsertSize,
                       String analysisType, String referenceSequence, String referenceSequenceVersion,
                       String organism, String species, String strain,
                       String aligner, String rrbsSizeRange, String restrictionEnzyme,
                       String bait, double labMeasuredInsertSize, Boolean positiveControl, Boolean negativeControl,
                       TZDevExperimentData devExperimentData, Collection<String> gssrBarcodes,
                       String gssrSampleType, Boolean doAggregation, Collection<String> customAmpliconSetNames,
                       ProductOrder productOrder, String lcSet, BSPSampleDTO bspSampleDTO, String labWorkflow) {

        // project and pdoSample was always null in the calls here, so why send them through. Can add back later.
        this(library, null, initiative, workRequest, indexingScheme, hasIndexingRead, expectedInsertSize,
                analysisType, referenceSequence, referenceSequenceVersion, null, organism, species, strain, null,
                aligner, rrbsSizeRange, restrictionEnzyme, bait, null, labMeasuredInsertSize, positiveControl,
                negativeControl, devExperimentData, gssrBarcodes, gssrSampleType, doAggregation, customAmpliconSetNames,
                productOrder, lcSet, bspSampleDTO, labWorkflow, null);
    }

    /**
     * Sample-ish fields here are supplied for GSSR.
     *
     * @param library the name of the library.
     * @param project project name.
     * @param initiative initiative.
     * @param workRequest The work request specified.
     * @param indexingScheme The indexing scheme.
     * @param hasIndexingRead Does this have an indexing read.
     * @param expectedInsertSize The size of the expected insert.
     * @param analysisType The analysis type to be performed.
     * @param referenceSequence The name of the reference sequence for analysis.
     * @param referenceSequenceVersion The version of this reference sequence.
     * @param collaboratorSampleId The specified collaborator sample name.
     * @param organism The organism.
     * @param species The species.
     * @param strain The strain/
     * @param sampleLSID The LSID of the sample
     * @param aligner The aligner.
     * @param rrbsSizeRange The size range.
     * @param restrictionEnzyme Restriction Enzyme that was used.
     * @param bait The bait that was used.
     * @param individual The name of the individual.
     * @param labMeasuredInsertSize The size that the lab measured for the insert.
     * @param positiveControl Positive control.
     * @param negativeControl Negative control.
     * @param devExperimentData The data.
     * @param gssrBarcodes barcodes.
     * @param gssrSampleType type of sample.
     * @param doAggregation Are we performing an aggregation?
     * @param customAmpliconSetNames The amplicaon names.
     * @param productOrder The PDO.
     * @param lcSet The LC Set.
     * @param bspSampleDTO trumps all other sample-related fields.  Other sample
*                     related fields (such as inidividual, organism, etc.) are here
*                     for GSSR samples.  If bspSampleDTO is non-null, all sample
*                     information is derived from bspSampleDTO; otherwise individual
*                     sample fields are pulled from their constructor counterparts
     */
    public LibraryBean(String library, String project, String initiative, Long workRequest,
                       MolecularIndexingScheme indexingScheme, Boolean hasIndexingRead, String expectedInsertSize,
                       String analysisType, String referenceSequence, String referenceSequenceVersion, String collaboratorSampleId,
                       String organism, String species, String strain, String sampleLSID,
                       String aligner, String rrbsSizeRange, String restrictionEnzyme,
                       String bait, String individual, double labMeasuredInsertSize, Boolean positiveControl, Boolean negativeControl,
                       TZDevExperimentData devExperimentData, Collection<String> gssrBarcodes,
                       String gssrSampleType, Boolean doAggregation, Collection<String> customAmpliconSetNames,
                       ProductOrder productOrder, String lcSet, BSPSampleDTO bspSampleDTO, String labWorkflow, String pdoSample) {
        this(sampleLSID,gssrSampleType,collaboratorSampleId,organism,species,strain,individual,bspSampleDTO, labWorkflow, pdoSample);
        this.library = library;
        this.project = project;
        this.initiative = initiative;
        this.workRequest = workRequest;
        if (indexingScheme != null) {
            this.indexingScheme = new MolecularIndexingSchemeBean(indexingScheme);
        }
        this.hasIndexingRead = hasIndexingRead;
        this.expectedInsertSize = expectedInsertSize;
        this.analysisType = analysisType;
        this.referenceSequence = referenceSequence;
        this.referenceSequenceVersion = referenceSequenceVersion;
        this.aligner = aligner;
        this.rrbsSizeRange = rrbsSizeRange;
        this.restrictionEnzyme = restrictionEnzyme;
        this.bait = bait;
        this.labMeasuredInsertSize = ThriftConversionUtil.zeroAsNull(labMeasuredInsertSize);
        isPositiveControl = positiveControl;
        isNegativeControl = negativeControl;
        if (devExperimentData != null) {
            this.devExperimentData = new DevExperimentDataBean(devExperimentData);
        }
        this.gssrBarcodes = gssrBarcodes;
        this.doAggregation = doAggregation;
        this.customAmpliconSetNames = customAmpliconSetNames;

        if (productOrder != null) {
            productOrderKey = productOrder.getBusinessKey();
            productOrderTitle = productOrder.getTitle();
            ResearchProject mercuryProject = productOrder.getResearchProject();
            if (mercuryProject != null) {
                researchProjectId = mercuryProject.getBusinessKey();
                researchProjectName = mercuryProject.getTitle();
            }

            Product product = productOrder.getProduct();
            if (product != null) {
                this.dataType = productOrder.getProduct().getAggregationDataType();
                this.product = product.getProductName();
                ProductFamily family = product.getProductFamily();
                if (family != null) {
                    productFamily = family.getName();
                }
                productPartNumber = product.getPartNumber();
            }
        }
        this.lcSet = lcSet;
    }

    /**
     * Set various sample related fields to whatever the
     * {@link BSPSampleDTO} says.  In other words,
     * ignore any GSSR parameters and overwrite them
     * with what BSP says.
     * @param bspSampleDTO BSP data for sample
     */
    private void overrideSampleFieldsFromBSP(BSPSampleDTO bspSampleDTO) {
        if (bspSampleDTO != null) {
            // We force all empty fields to null, because this is the format that the web service client (the
            // Picard pipeline) expects.  The raw results from BSP provide the empty string for missing data,
            // so there is no way to tell missing data from empty data.
            species = StringUtils.trimToNull(bspSampleDTO.getOrganism());
            primaryDisease = StringUtils.trimToNull(bspSampleDTO.getPrimaryDisease());
            sampleType = StringUtils.trimToNull(bspSampleDTO.getSampleType());
            rootSample = StringUtils.trimToNull(bspSampleDTO.getRootSample());
            sampleLSID = StringUtils.trimToNull(bspSampleDTO.getSampleLsid());
            sampleId = StringUtils.trimToNull(bspSampleDTO.getSampleId());
            gender = StringUtils.trimToNull(bspSampleDTO.getGender());
            // todo arz pop/ethnicity,
            collection = StringUtils.trimToNull(bspSampleDTO.getCollection());
            collaboratorSampleId = StringUtils.trimToNull(bspSampleDTO.getCollaboratorsSampleName());
            materialType = StringUtils.trimToNull(bspSampleDTO.getMaterialType());
            participantId = StringUtils.trimToNull(bspSampleDTO.getPatientId());
            population = StringUtils.trimToNull(bspSampleDTO.getEthnicity());
            race = StringUtils.trimToNull(bspSampleDTO.getRace());
            collaboratorParticipantId = StringUtils.trimToNull(bspSampleDTO.getCollaboratorParticipantId());
            isGssrSample = false;
        } else {
            isGssrSample = true;
        }
    }

    public Long getWorkRequestId() {
        return workRequest;
    }

    public String getProject() {
        return project;
    }

    public String getCollaboratorSampleId() {
        return collaboratorSampleId;
    }

    public String getLsid() {
        return sampleLSID;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getLibrary() {
        return library;
    }

    public MolecularIndexingSchemeBean getMolecularIndexingScheme() {
        return indexingScheme;
    }

    public String getAligner() {
        return aligner;
    }

    public Collection<String> getGssrBarcodes() {
        return gssrBarcodes;
    }

    public String getInitiative() {
        return initiative;
    }

    public String getReferenceSequence() {
        return referenceSequence;
    }

    public String getRestrictionEnzyme() {
        return restrictionEnzyme;
    }

    public String getReferenceSequenceVersion() {
        return referenceSequenceVersion;
    }

    public String getRrbsSizeRange() {
        return rrbsSizeRange;
    }

    public String getExpectedInsertSize() {
        return expectedInsertSize;
    }

    public Double getLabMeasuredInsertSize() {
        return labMeasuredInsertSize;
    }

    public String getBaitSetName() {
        return bait;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public String getSpecies() {
        return species;
    }

    public DevExperimentDataBean getDevExperimentData() {
        return devExperimentData;
    }

    public Boolean isNegativeControl() {
        return isNegativeControl;
    }

    public Boolean isPositiveControl() {
        return isPositiveControl;
    }

    public Boolean doAggregation() {
        return doAggregation;
    }

    public Collection<String> getCustomAmpliconSetNames() {
        return customAmpliconSetNames;
    }

    public String getPrimaryDisease() {
        return primaryDisease;
    }

    public String getSampleType() {
        return sampleType;
    }

    public String getProductOrderKey() {
        return productOrderKey;
    }

    public String getProductOrderTitle() {
        return productOrderTitle;
    }

    public String getLcSet() {
        return lcSet;
    }

    public String getResearchProjectId()  {
        return researchProjectId;
    }

    public String getResearchProjectName() {
        return researchProjectName;
    }

    public String getProduct() {
        return product;
    }

    public String getProductFamily() {
        return productFamily;
    }

    public String getProductPartNumber() {
        return productPartNumber;
    }

    public String getRootSample() {
        return rootSample;
    }

    public String getGender() {
        return gender;
    }

    public String getSampleId() {
        return sampleId;
    }

    public String getCollection() {
        return collection;
    }

    public String getMaterialType() {
        return materialType;
    }

    public String getDataType() {
        return dataType;
    }

    public boolean getIsGssrSample() {
        return isGssrSample;
    }

    // todo arz db-free test
    public String getPopulation() {
        return population;
    }

    public String getCollaboratorParticipantId() {
        return collaboratorParticipantId;
    }

    public String getRace() {
        return race;
    }

    public static final Comparator<LibraryBean> BY_SAMPLE_ID = new Comparator<LibraryBean> () {
        @Override
        public int compare(LibraryBean libraryBean1, LibraryBean libraryBean2) {
            return new CompareToBuilder().append(libraryBean1.getSampleId(), libraryBean2.getSampleId()).toComparison();
        }
    };
}
