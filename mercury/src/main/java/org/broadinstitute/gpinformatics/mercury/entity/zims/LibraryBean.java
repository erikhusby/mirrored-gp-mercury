package org.broadinstitute.gpinformatics.mercury.entity.zims;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * A library DTO for Zamboni.  Copied from
 * LIMQuery.thrift from squidThriftService.
 */

public class LibraryBean {
    public static final String NO_PDO_SAMPLE = null;
    public static final String CRSP_SOMATIC_TEST_TYPE = "Somatic";
    public static final String MERCURY_LSID_PREFIX = "broadinstitute.org:mercury.prod.sample:";
    public static final String CRSP_LSID_PREFIX = "org.broadinstitute:crsp:";

    @JsonProperty("metadataSource")
    private String metadataSource;

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
    private String expectedInsertSize;

    @JsonProperty("analysisType")
    private String analysisType;

    @JsonProperty("referenceSequence")
    private String referenceSequence;

    @JsonProperty("referenceSequenceVersion")
    private String referenceSequenceVersion;

    @JsonProperty("aggregate")
    private Boolean doAggregation;

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
    private String bait;

    /** obfuscated name of the participantId (person) from whence this sample was taken */
    @JsonProperty("participantId")
    private String participantId;

    @JsonProperty("labMeasuredInsertSize")
    private Double labMeasuredInsertSize; // Squid only (for now)

    @JsonProperty("positiveControl")
    private Boolean isPositiveControl;

    @JsonProperty("negativeControl")
    private Boolean isNegativeControl;

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
    private String sampleId;

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
    private String productOrderSample;

    @JsonProperty
    private String libraryCreationDate;

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

    @JsonProperty("workRequestType")
    private String workRequestType;

    @JsonProperty("workRequestDomain")
    private String workRequestDomain;

    @JsonProperty("regulatoryDesignation")
    private String regulatoryDesignation = ResearchProject.RegulatoryDesignation.RESEARCH_ONLY.name();

    private String stockSample;

    @JsonProperty("testType")
    private String testType;

    @JsonProperty("buickVisit")
    private String buickVisit;  // buick-specific field, not generally applicable to future crsp work

    @JsonProperty("buickCollectionDate")
    private String buickCollectionDate; // buick specific field, not generally applicable to future crsp work

    @JsonProperty("analyzeUmis")
    private boolean analyzeUmis;

    @JsonProperty("submissionsMetadata")
    private List<SubmissionMetadata> submissionMetadata = new ArrayList<>();

    @JsonProperty("aggregationParticle")
    private String aggregationParticle;

    @JsonIgnore
    private boolean impliedSampleName = false;

    public LibraryBean() {}

    /**
     * Sets gssr parameters and then overrides them with BSP values.  Useful for testing.
     *
     * @param gssrLsid The lsid of the gssr sample.
     * @param sampleData The BSP representation of the sample.
     */
    LibraryBean(String gssrLsid, String gssrMaterialType, String gssrCollaboratorSampleId, String gssrOrganism,
                String gssrSpecies, String gssrStrain, String gssrIndividual, SampleData sampleData,
                String labWorkflow, String productOrderSample, String libraryCreationDate) {
        sampleLSID = gssrLsid;
        materialType = gssrMaterialType;
        collaboratorSampleId = gssrCollaboratorSampleId;
        this.labWorkflow = labWorkflow;
        this.productOrderSample = productOrderSample;
        species = gssrOrganism + ":" + gssrSpecies + ":" + gssrStrain;
        collaboratorParticipantId = gssrIndividual;
        this.libraryCreationDate = libraryCreationDate;
        overrideSampleFieldsFromBSP(sampleData);
    }

    public LibraryBean(String library, String initiative, Long workRequest, MolecularIndexingScheme indexingScheme,
                       Boolean hasIndexingRead, String expectedInsertSize, String analysisType,
                       String referenceSequence, String referenceSequenceVersion, String organism, String species,
                       String strain, String aligner, String rrbsSizeRange, String restrictionEnzyme, String bait,
                       double labMeasuredInsertSize, Boolean positiveControl, Boolean negativeControl,
                       TZDevExperimentData devExperimentData, Collection<String> gssrBarcodes, String gssrSampleType,
                       Boolean doAggregation, Collection<String> customAmpliconSetNames, ProductOrder productOrder,
                       String lcSet, SampleData sampleData, String labWorkflow, String libraryCreationDate,
                       String productOrderSample, String metadataSource, String aggregationDataType,
                       JiraService jiraService, List<SubmissionMetadata> submissionMetadata, boolean analyzeUmis,
                       String aggregationParticle, boolean impliedSampleName) {

        // project was always null in the calls here, so don't send it through. Can add back later.
        this(library, null, initiative, workRequest, indexingScheme, hasIndexingRead, expectedInsertSize,
                analysisType, referenceSequence, referenceSequenceVersion, null, organism, species, strain, null,
                aligner, rrbsSizeRange, restrictionEnzyme, bait, null, labMeasuredInsertSize, positiveControl,
                negativeControl, devExperimentData, gssrBarcodes, gssrSampleType, doAggregation, customAmpliconSetNames,
                productOrder, lcSet, sampleData, labWorkflow, productOrderSample, libraryCreationDate, null, null,
                metadataSource, aggregationDataType, jiraService, submissionMetadata, analyzeUmis, aggregationParticle,
                impliedSampleName);
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
     * @param sampleData trumps all other sample-related fields.  Other sample
     *                     related fields (such as inidividual, organism, etc.) are here
     *                     for GSSR samples.  If bspSampleData is non-null, all sample
     *                     information is derived from bspSampleData; otherwise individual
     *                     sample fields are pulled from their constructor counterparts
     * @param productOrderSample the product order sample name (key).
     * @param workRequestType squid work request type name
     * @param workRequestDomain squid work request domain name
     * @param metadataSource BSP or Mercury
     * @param aggregationDataType only for controls
     * @param analyzeUmis are we analyzing the Umi, set in product and overriden in PDO
     * @param impliedSampleName indicates the Mercury sample name should be null in the pipeline output.
     */
    public LibraryBean(String library, String project, String initiative, Long workRequest,
            MolecularIndexingScheme indexingScheme, Boolean hasIndexingRead, String expectedInsertSize,
            String analysisType, String referenceSequence, String referenceSequenceVersion,
            String collaboratorSampleId, String organism, String species, String strain, String sampleLSID,
            String aligner, String rrbsSizeRange, String restrictionEnzyme, String bait, String individual,
            double labMeasuredInsertSize, Boolean positiveControl, Boolean negativeControl,
            TZDevExperimentData devExperimentData, Collection<String> gssrBarcodes, String gssrSampleType,
            Boolean doAggregation, Collection<String> customAmpliconSetNames, ProductOrder productOrder,
            String lcSet, SampleData sampleData, String labWorkflow, String productOrderSample,
            String libraryCreationDate, String workRequestType, String workRequestDomain, String metadataSource,
            String aggregationDataType, JiraService jiraService, List<SubmissionMetadata> submissionMetadata,
            boolean analyzeUmis, String aggregationParticle, boolean impliedSampleName) {

        this(sampleLSID, gssrSampleType, collaboratorSampleId, organism, species, strain, individual, sampleData,
                labWorkflow, productOrderSample, libraryCreationDate);
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
            this.devExperimentData = new DevExperimentDataBean(devExperimentData, jiraService);
        }
        this.gssrBarcodes = gssrBarcodes;
        this.doAggregation = doAggregation;
        this.customAmpliconSetNames = customAmpliconSetNames;
        if (metadataSource != null) {
            this.metadataSource = metadataSource;
        }
        dataType = aggregationDataType;

        if (productOrder != null) {
            this.regulatoryDesignation = productOrder.getRegulatoryDesignationCodeForPipeline();
            productOrderKey = productOrder.getBusinessKey();
            productOrderTitle = productOrder.getTitle();
            ResearchProject mercuryProject = productOrder.getResearchProject();
            if (mercuryProject != null) {
                researchProjectId = mercuryProject.getBusinessKey();
                researchProjectName = mercuryProject.getTitle();
            }

            Product product = productOrder.getProduct();
            if (product != null) {
                if (StringUtils.isBlank(dataType)) {
                    dataType = productOrder.getProduct().getPipelineDataTypeString();
                }
                this.product = product.getProductName();
                ProductFamily family = product.getProductFamily();
                if (family != null) {
                    productFamily = family.getName();
                }
                productPartNumber = product.getPartNumber();
            }

            // Tries to return the actual pdo sample name in case the lab silently substituted an aliquot
            // when it started the seq plating request.
            List<String> pdoSampleNames = ProductOrderSample.getSampleNames(productOrder.getSamples());
            if (!pdoSampleNames.contains(productOrderSample)) {
                if (pdoSampleNames.contains(stockSample)) {
                    this.productOrderSample = stockSample;
                } else if (pdoSampleNames.contains(rootSample)) {
                    this.productOrderSample = rootSample;
                }
            }
        }
        this.lcSet = lcSet;
        this.workRequestType = workRequestType;
        this.workRequestDomain = workRequestDomain;
        this.submissionMetadata = submissionMetadata;
        this.analyzeUmis = analyzeUmis;
        this.aggregationParticle = aggregationParticle;
        this.impliedSampleName = impliedSampleName;
        if (impliedSampleName) {
            sampleId = null;
        }
    }

    /**
     * Set various sample related fields to whatever the {@link BspSampleData} says.  In other words, ignore any
     * GSSR parameters and overwrite them with what BSP says.
     *
     * @param sampleData BSP data for sample
     */
    private void overrideSampleFieldsFromBSP(SampleData sampleData) {
        if (sampleData != null) {
            // We force all empty fields to null, because this is the format that the web service client (the
            // Picard pipeline) expects.  The raw results from BSP provide the empty string for missing data,
            // so there is no way to tell missing data from empty data.
            species = StringUtils.trimToNull(sampleData.getOrganism());
            primaryDisease = StringUtils.trimToNull(sampleData.getPrimaryDisease());
            String sampleType = StringUtils.trimToNull(sampleData.getSampleType());
            // The pipeline API understands only Tumor and Normal
            if (sampleType != null) {
                if (sampleType.equals("Primary") || sampleType.equals("Secondary")) {
                    sampleType = "Tumor";
                }
            }
            this.sampleType = sampleType;
            rootSample = StringUtils.trimToNull(sampleData.getRootSample());
            stockSample = StringUtils.trimToNull(sampleData.getStockSample());
            sampleId = StringUtils.trimToNull(sampleData.getSampleId());
            gender = StringUtils.trimToNull(sampleData.getGender());
            // todo arz pop/ethnicity,
            collection = StringUtils.trimToNull(sampleData.getCollection());
            String trimCollabSampleName = StringUtils.trimToNull(sampleData.getCollaboratorsSampleName());
            collaboratorSampleId = trimCollabSampleName == null ? sampleId : trimCollabSampleName;
            String localSampleLSID = StringUtils.trimToNull(sampleData.getSampleLsid());
            if (localSampleLSID == null && trimCollabSampleName != null) {
                localSampleLSID = MERCURY_LSID_PREFIX + sampleId;
            }
            sampleLSID = localSampleLSID;
            materialType = StringUtils.trimToNull(sampleData.getMaterialType());
            participantId = StringUtils.trimToNull(sampleData.getPatientId());
            population = StringUtils.trimToNull(sampleData.getEthnicity());
            race = StringUtils.trimToNull(sampleData.getRace());

            collaboratorParticipantId = StringUtils.trimToNull(sampleData.getCollaboratorParticipantId());
            isGssrSample = false;
            metadataSource = MercurySample.BSP_METADATA_SOURCE;
        } else {
            isGssrSample = true;
            metadataSource = MercurySample.GSSR_METADATA_SOURCE;
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

    public void setCollaboratorSampleId(String collaboratorSampleId) {
        this.collaboratorSampleId = collaboratorSampleId;
    }

    public String getLsid() {
        return sampleLSID;
    }

    public void setLsid(String sampleLSID) {
        this.sampleLSID = sampleLSID;
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

    public void setResearchProjectId(String researchProjectId) {
        this.researchProjectId = researchProjectId;
    }

    public String getResearchProjectName() {
        return researchProjectName;
    }

    public void setResearchProjectName(String researchProjectName) {
        this.researchProjectName = researchProjectName;
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

    public void setProductPartNumber(String productPartNumber) {
        this.productPartNumber = productPartNumber;
    }

    public String getRootSample() {
        return rootSample;
    }

    public void setRootSample(String rootSample) {
        this.rootSample = rootSample;
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

    public void setCollaboratorParticipantId(String collaboratorParticipantId) {
        this.collaboratorParticipantId = collaboratorParticipantId;
    }


    public String getRace() {
        return race;
    }

    public String getProductOrderSample() {
        return productOrderSample;
    }

    public String getLibraryCreationDate() {
        return libraryCreationDate;
    }

    public String getWorkRequestType() {
        return workRequestType;
    }

    public String getWorkRequestDomain() {
        return workRequestDomain;
    }

    public String getRegulatoryDesignation() {
        return regulatoryDesignation;
    }

    public void setRegulatoryDesignation(String regulatoryDesignation) {
        this.regulatoryDesignation = regulatoryDesignation;
    }

    public String getMetadataSource() {
        return metadataSource;
    }

    public static final Comparator<LibraryBean> BY_SAMPLE_ID_LIBRARY = new Comparator<LibraryBean> () {
        @Override
        public int compare(LibraryBean libraryBean1, LibraryBean libraryBean2) {
            return new CompareToBuilder().
                    append(libraryBean1.getSampleId(), libraryBean2.getSampleId()).
                    append(libraryBean1.getLibrary(), libraryBean2.getLibrary()).
                    toComparison();
        }
    };

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public String getTestType() {
        return testType;
    }

    public void setBuickVisit(String buickVisit) {
        this.buickVisit = buickVisit;
    }

    public String getBuickVisit() {
        return buickVisit;
    }

    public void setBuickCollectionDate(String buickCollectionDate) {
        this.buickCollectionDate = buickCollectionDate;
    }

    public String getBuickCollectionDate() {
        return buickCollectionDate;
    }

    public boolean isAnalyzeUmis() {
        return analyzeUmis;
    }

    public List<SubmissionMetadata> getSubmissionMetadata() {
        return submissionMetadata;
    }

    public String getAggregationParticle() {
        return aggregationParticle;
    }

    public boolean isImpliedSampleName() {
        return impliedSampleName;
    }
}
