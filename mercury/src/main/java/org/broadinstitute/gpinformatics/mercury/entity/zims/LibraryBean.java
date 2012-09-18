package org.broadinstitute.gpinformatics.mercury.entity.zims;

import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A library DTO for Zamboni.  Copied from
 * LIMQuery.thrift from squidThriftService.
 */

public class LibraryBean {

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

    /** the name of the collaborator */
    @JsonProperty("sampleCollaborator")
    private String collaborator;

    @JsonProperty("organism")
    private String organism;

    @JsonProperty("species")
    private String species;

    @JsonProperty("strain")
    private String strain;

    @JsonProperty("lsid")
    private String sampleLSID;

    @JsonProperty("tissueType")
    private String tissueType;

    @JsonProperty("expectedPlasmid")
    private String expectedPlasmid;

    @JsonProperty("aligner")
    private String aligner;

    @JsonProperty("rrbsSizeRange")
    private String rrbsSizeRange;

    @JsonProperty("restrictionEnzyme")
    private String restrictionEnzyme;

    @JsonProperty("cellLine")
    private String cellLine;

    @JsonProperty("baitSetName")
    private String bait;

    /** obfuscated name of the individual (person) from whence this sample was taken */
    @JsonProperty("individual")
    private String individual;

    @JsonProperty("labMeasuredInsertSize")
    private Double labMeasuredInsertSize;

    @JsonProperty("positiveControl")
    private Boolean isPositiveControl;

    @JsonProperty("negativeControl")
    private Boolean isNegativeControl;

    @JsonProperty("weirdness")
    private String weirdness;

    @JsonProperty("preCircularizationDnaSize")
    private Double preCircularizationDnaSize;

    @JsonProperty("devExperimentData")
    private DevExperimentDataBean devExperimentData;

    @JsonProperty("gssrBarcodes")
    private Collection<String> gssrBarcodes = new ArrayList<String>();

    @JsonProperty("gssrSampleType")
    private String gssrSampleType;

    @JsonProperty("targetLaneCoverage")
    private Short targetLaneCoverage;

    @JsonProperty("customAmpliconSetNames")
    private Collection<String> customAmpliconSetNames = new ArrayList<String>();

    @JsonProperty("fastTrack")
    private Boolean isFastTrack;

    public LibraryBean() {}

    public LibraryBean(String library, String project, String initiative, Long workRequest,
            MolecularIndexingScheme indexingScheme, Boolean hasIndexingRead, String expectedInsertSize,
            String analysisType, String referenceSequence, String referenceSequenceVersion, String collaboratorSampleId,
            String collaborator, String organism, String species, String strain, String sampleLSID, String tissueType,
            String expectedPlasmid, String aligner, String rrbsSizeRange, String restrictionEnzyme, String cellLine,
            String bait, String individual, double labMeasuredInsertSize, Boolean positiveControl, Boolean negativeControl,
            String weirdness, double preCircularizationDnaSize, Boolean partOfDevExperiment,
            TZDevExperimentData devExperimentData, String gssrBarcode, Collection<String> gssrBarcodes,
            String gssrSampleType, Short targetLaneCoverage, Boolean doAggregation, Collection<String> customAmpliconSetNames, Boolean fastTrack) {
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
        this.collaborator = collaborator;
        this.organism = organism;
        this.species = species;
        this.strain = strain;
        this.sampleLSID = sampleLSID;
        this.tissueType = tissueType;
        this.expectedPlasmid = expectedPlasmid;
        this.aligner = aligner;
        this.rrbsSizeRange = rrbsSizeRange;
        this.restrictionEnzyme = restrictionEnzyme;
        this.cellLine = cellLine;
        this.bait = bait;
        this.individual = individual;
        this.labMeasuredInsertSize = ThriftConversionUtil.zeroAsNull(labMeasuredInsertSize);
        isPositiveControl = positiveControl;
        isNegativeControl = negativeControl;
        this.weirdness = weirdness;
        this.preCircularizationDnaSize = ThriftConversionUtil.zeroAsNull(preCircularizationDnaSize);
        if (devExperimentData != null) {
            this.devExperimentData = new DevExperimentDataBean(devExperimentData);
        }
        this.gssrBarcodes = gssrBarcodes;
        this.gssrSampleType = gssrSampleType;
        this.targetLaneCoverage = targetLaneCoverage;
        this.doAggregation = doAggregation;
        this.customAmpliconSetNames = customAmpliconSetNames;
        this.isFastTrack = fastTrack;
    }


    public Double getPreCircularizationDnaSize() {
        return preCircularizationDnaSize;
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

    public String getCellLine() {
        return cellLine;
    }

    public String getIndividual() {
        return individual;
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
    
    public String getSampleCollaborator() {
        return collaborator;
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
    
    public String getTissueType() {
        return tissueType;
    }
    
    public Short getTargetLaneCoverage() {
        return targetLaneCoverage;
    }
    
    public String getStrain() {
        return strain;
    }
    
    public String getWeirdness() {
        return weirdness;
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

    public Boolean getFastTrack() {
        return isFastTrack;
    }
}
