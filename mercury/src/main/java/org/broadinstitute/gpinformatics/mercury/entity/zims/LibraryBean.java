package org.broadinstitute.gpinformatics.mercury.entity.zims;

import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A library DTO for Zamboni.  Copied from
 * LIMQuery.thrift from squidThriftService.
 */

public class LibraryBean {

    // todo add bspEthnicity and bspRace

    // todo: 1 delete unneeded fields, 2. merge bsp and gssr, 3. provide getInformaticsSource() = BSP/GSSR

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
    private Collection<String> gssrBarcodes = new ArrayList<String>(); // Squid only

    @JsonProperty("customAmpliconSetNames")
    private Collection<String> customAmpliconSetNames = new ArrayList<String>(); // Squid only

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
    private boolean isGssrSample;

    @JsonProperty
    private String population;

    @JsonProperty
    private String race;

    @JsonProperty
    private String productPartNumber;

    public LibraryBean() {}

    /**
     * Sets gssr parameters and then overrides them
     * with BSP values.  Useful for testing.
     * @param gssrLsid
     * @param bspSampleDTO
     */
    LibraryBean(String gssrLsid,
                       String gssrMaterialType,
                       String gssrCollaboratorSampleId,
                       String gssrOrganism,
                       String gssrSpecies,
                       String gssrStrain,
                       String gssrIndividual,
                       BSPSampleDTO bspSampleDTO) {
        this.sampleLSID = gssrLsid;
        this.materialType = gssrMaterialType;
        this.collaboratorSampleId = gssrCollaboratorSampleId;
        this.species = gssrOrganism + ":" + gssrSpecies + ":" + gssrStrain;
        this.collaboratorParticipantId = gssrIndividual;
        overrideSampleFieldsFromBSP(bspSampleDTO);
    }

    /**
     * Sample-ish fields here are supplied for GSSR.
     * @param library
     * @param project
     * @param initiative
     * @param workRequest
     * @param indexingScheme
     * @param hasIndexingRead
     * @param expectedInsertSize
     * @param analysisType
     * @param referenceSequence
     * @param referenceSequenceVersion
     * @param collaboratorSampleId
     * @param organism
     * @param species
     * @param strain
     * @param sampleLSID
     * @param aligner
     * @param rrbsSizeRange
     * @param restrictionEnzyme
     * @param bait
     * @param individual
     * @param labMeasuredInsertSize
     * @param positiveControl
     * @param negativeControl
     * @param devExperimentData
     * @param gssrBarcodes
     * @param gssrSampleType
     * @param doAggregation
     * @param customAmpliconSetNames
     * @param productOrder
     * @param lcSet
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
                       ProductOrder productOrder, String lcSet, BSPSampleDTO bspSampleDTO) {
        this(sampleLSID,gssrSampleType,collaboratorSampleId,organism,species,strain,individual,bspSampleDTO);
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
            this.productOrderKey = productOrder.getBusinessKey();
            this.productOrderTitle = productOrder.getTitle();
            ResearchProject mercuryProject = productOrder.getResearchProject();
            if (mercuryProject != null) {
                this.researchProjectId = mercuryProject.getBusinessKey();
                this.researchProjectName = mercuryProject.getTitle();
            }
            Product product = productOrder.getProduct();
            if (product != null) {
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
     * @param bspSampleDTO
     */
    private final void overrideSampleFieldsFromBSP(BSPSampleDTO bspSampleDTO) {
        if (bspSampleDTO != null) {
            this.species = bspSampleDTO.getOrganism();
            this.primaryDisease = bspSampleDTO.getPrimaryDisease();
            this.sampleType = bspSampleDTO.getSampleType();
            this.rootSample = bspSampleDTO.getRootSample();
            this.sampleLSID = bspSampleDTO.getSampleLsid();
            this.sampleId = bspSampleDTO.getSampleId();
            this.gender = bspSampleDTO.getGender();
            // todo arz pop/ethnicity,
            this.collection = bspSampleDTO.getCollection();
            this.collaboratorSampleId = bspSampleDTO.getCollaboratorsSampleName();
            this.materialType = bspSampleDTO.getMaterialType();
            this.participantId = bspSampleDTO.getPatientId();
            this.population = bspSampleDTO.getEthnicity();
            this.race = bspSampleDTO.getRace();
            this.collaboratorParticipantId = bspSampleDTO.getCollaboratorParticipantId();
            isGssrSample = false;
        }
        else {
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
}
