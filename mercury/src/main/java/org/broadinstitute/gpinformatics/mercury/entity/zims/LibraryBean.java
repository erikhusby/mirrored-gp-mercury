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
    private String initiative;

    @JsonProperty("workRequestId")
    private Long workRequest;

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

    /** the name the collaborator has given to the sample */
    @JsonProperty("sampleAlias")
    private String collaboratorSampleId;

    @JsonProperty("aggregate")
    private Boolean doAggregation;

    @JsonProperty("organism")
    private String organism;

    @JsonProperty("species")
    private String species;

    @JsonProperty("strain")
    private String strain;

    @JsonProperty("lsid")
    private String sampleLSID;

    @JsonProperty("aligner")
    private String aligner;

    @JsonProperty("rrbsSizeRange")
    private String rrbsSizeRange;

    @JsonProperty("restrictionEnzyme")
    private String restrictionEnzyme;

    @JsonProperty("baitSetName")
    private String bait;

    /** obfuscated name of the participantId (person) from whence this sample was taken */
    @JsonProperty("participantId")
    private String participantId;

    @JsonProperty("labMeasuredInsertSize")
    private Double labMeasuredInsertSize;

    @JsonProperty("positiveControl")
    private Boolean isPositiveControl;

    @JsonProperty("negativeControl")
    private Boolean isNegativeControl;

    @JsonProperty("devExperimentData")
    private DevExperimentDataBean devExperimentData;

    @JsonProperty("gssrBarcodes")
    private Collection<String> gssrBarcodes = new ArrayList<String>();

    @JsonProperty("gssrSampleType")
    private String gssrSampleType;

    @JsonProperty("customAmpliconSetNames")
    private Collection<String> customAmpliconSetNames = new ArrayList<String>();

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
    // todo arz merge with gssr species
    private String bspSpecies;

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
    private String sampleType;

    @JsonProperty
    // todo arz merge with gssr collab sample id
    private String bspCollaboratorSampleId;

    public LibraryBean() {}

    /**
     * Test-only
     * @param gssrLsid
     * @param bspSampleDTO
     */
    public LibraryBean(String gssrLsid,
                       BSPSampleDTO bspSampleDTO) {
        this.sampleLSID = gssrLsid;
        initBSPFields(bspSampleDTO);
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
     * @param partOfDevExperiment
     * @param devExperimentData
     * @param gssrBarcode
     * @param gssrBarcodes
     * @param gssrSampleType
     * @param doAggregation
     * @param customAmpliconSetNames
     * @param productOrder
     * @param lcSet
     * @param bspSampleDTO trumps all other sample-related fields.  Other sample
*                     related fields (such as inidividual, organism, etc.) are here
*                     for GSSR samples.  If bspSampleDTO is non-null, all sample
     */
    public LibraryBean(String library, String project, String initiative, Long workRequest,
                       MolecularIndexingScheme indexingScheme, Boolean hasIndexingRead, String expectedInsertSize,
                       String analysisType, String referenceSequence, String referenceSequenceVersion, String collaboratorSampleId,
                       String organism, String species, String strain, String sampleLSID,
                       String aligner, String rrbsSizeRange, String restrictionEnzyme,
                       String bait, String individual, double labMeasuredInsertSize, Boolean positiveControl, Boolean negativeControl,
                       Boolean partOfDevExperiment,
                       TZDevExperimentData devExperimentData, String gssrBarcode, Collection<String> gssrBarcodes,
                       String gssrSampleType, Boolean doAggregation, Collection<String> customAmpliconSetNames,
                       ProductOrder productOrder, String lcSet, BSPSampleDTO bspSampleDTO) {
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
        this.collaboratorSampleId = collaboratorSampleId;
        this.organism = organism;
        this.species = species;
        this.strain = strain;
        this.sampleLSID = sampleLSID;
        this.aligner = aligner;
        this.rrbsSizeRange = rrbsSizeRange;
        this.restrictionEnzyme = restrictionEnzyme;
        this.bait = bait;
        this.participantId = individual;
        this.labMeasuredInsertSize = ThriftConversionUtil.zeroAsNull(labMeasuredInsertSize);
        isPositiveControl = positiveControl;
        isNegativeControl = negativeControl;
        if (devExperimentData != null) {
            this.devExperimentData = new DevExperimentDataBean(devExperimentData);
        }
        this.gssrBarcodes = gssrBarcodes;
        this.gssrSampleType = gssrSampleType;
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
            }
        }
        this.lcSet = lcSet;
        if (bspSampleDTO != null) {
            initBSPFields(bspSampleDTO);
        }
    }

    private final void initBSPFields(BSPSampleDTO bspSampleDTO) {
        if (bspSampleDTO != null) {
            this.bspSpecies = bspSampleDTO.getOrganism();
            this.primaryDisease = bspSampleDTO.getPrimaryDisease();
            this.sampleType = bspSampleDTO.getSampleType();
            this.rootSample = bspSampleDTO.getRootSample();
            this.sampleLSID = bspSampleDTO.getSampleLsid();
            this.sampleId = bspSampleDTO.getSampleId();
            this.gender = bspSampleDTO.getGender();
            // todo arz pop/ethnicity,
            this.collection = bspSampleDTO.getCollection();
            this.bspCollaboratorSampleId = bspSampleDTO.getCollaboratorsSampleName();
        }
    }

    public Long getWorkRequestId() {
        return workRequest;
    }

    public String getProject() {
        return project;
    }

    public String getOrganism() {
        return organism;
    }

    public String getSampleAlias() {
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
    
    public String getGssrSampleType() {
        return gssrSampleType;
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

    public String getStrain() {
        return strain;
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

    public String getBspSpecies() {
        return bspSpecies;
    }

    public String getBspCollaboratorSampleId() {
        return bspCollaboratorSampleId;
    }

}
