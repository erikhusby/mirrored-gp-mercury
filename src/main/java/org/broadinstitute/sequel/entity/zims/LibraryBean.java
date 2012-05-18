package org.broadinstitute.sequel.entity.zims;

import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.codehaus.jackson.annotate.JsonAutoDetect;

import javax.xml.bind.annotation.*;
import java.util.Collection;

/**
 * A library DTO for Zamboni.  Copied from
 * LIMQuery.thrift from squidThriftService.
 */
@XmlRootElement(name = "Library")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
                getterVisibility = JsonAutoDetect.Visibility.NONE,
                creatorVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class LibraryBean {

    @XmlElement(name = "library")
    private String library;

    @XmlElement(name = "project")
    private String project;

    @XmlElement(name = "initiative")
    private String initiative;

    @XmlElement(name = "workRequestId")
    private Long workRequest;

    @XmlElement(name = "MolecularIndexingScheme")
    private MolecularIndexingSchemeBean indexingScheme;
    
    private Boolean hasIndexingRead;
    
    @XmlElement(name = "expectedInsertSize")
    private String expectedInsertSize;
    
    @XmlElement(name = "analysisType")
    private String analysisType;

    @XmlElement(name = "referenceSequence")
    private String referenceSequence;

    @XmlElement(name = "referenceSequenceVersion")
    private String referenceSequenceVersion;

    @XmlElement(name = "sampleAlias")
    /** the name the collaborator has given to the sample */
    private String collaboratorSampleId;

    @XmlElement(name = "aggregate")
    private Boolean doAggregation;

    @XmlElement(name = "sampleCollaborator")
    /** the name of the collaborator */
    private String collaborator;

    @XmlElement(name = "organism")
    private String organism;

    @XmlElement(name = "species")
    private String species;

    @XmlElement(name = "strain")
    private String strain;

    @XmlElement(name = "lsid")
    private String sampleLSID;

    @XmlElement(name = "tissueType")
    private String tissueType;

    @XmlElement(name = "expectedPlasmid")
    private String expectedPlasmid;
    
    @XmlElement(name = "aligner")
    private String aligner;

    @XmlElement(name = "rrbsSizeRange")
    private String rrbsSizeRange;

    @XmlElement(name = "restrictionEnzyme")
    private String restrictionEnzyme;

    @XmlElement(name = "cellLine")
    private String cellLine;

    @XmlElement(name = "BaitSetName")
    private String bait;

    @XmlElement(name = "individual")
    /** obfuscated name of the individual (person) from whence this sample was taken */
    private String individual;

    private Double labMeasuredInsertSize;

    @XmlElement(name = "isPositiveControl")
    private Boolean isPositiveControl;

    @XmlElement(name = "isNegativeControl")
    private Boolean isNegativeControl;

    @XmlElement(name = "weirdness")
    private String weirdness;

    private Double preCircularizationDnaSize;

    private Boolean isPartOfDevExperiment;

    @XmlElement(name = "devExperimentData")
    private DevExperimentDataBean devExperimentData;

    @XmlElement(name = "gssrBarcodes")
    private Collection<String> gssrBarcodes;

    @XmlElement(name = "gssrSampleType")
    private String gssrSampleType;

    @XmlElement(name = "targetLaneCoverage")
    private Short targetLaneCoverage;
    
    public LibraryBean() {}

    public LibraryBean(String library, String project, String initiative, Long workRequest, MolecularIndexingScheme indexingScheme, Boolean hasIndexingRead, String expectedInsertSize, String analysisType, String referenceSequence, String referenceSequenceVersion, String collaboratorSampleId, String collaborator, String organism, String species, String strain, String sampleLSID, String tissueType, String expectedPlasmid, String aligner, String rrbsSizeRange, String restrictionEnzyme, String cellLine, String bait, String individual, double labMeasuredInsertSize, Boolean positiveControl, Boolean negativeControl, String weirdness, double preCircularizationDnaSize, Boolean partOfDevExperiment, TZDevExperimentData devExperimentData,String gssrBarcode,Collection<String> gssrBarcodes,String gssrSampleType,Short targetLaneCoverage,Boolean doAggregation) {
        this.library = library;
        this.project = project;
        this.initiative = initiative;
        this.workRequest = workRequest;
        this.indexingScheme = new MolecularIndexingSchemeBean(indexingScheme);
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
        setLabMeasuredInsertSize(labMeasuredInsertSize);
        isPositiveControl = positiveControl;
        isNegativeControl = negativeControl;
        this.weirdness = weirdness;
        setPreCircularizationSize(preCircularizationDnaSize);        
        isPartOfDevExperiment = partOfDevExperiment;
        setDevExperimentData(devExperimentData);
        this.gssrBarcodes = gssrBarcodes;
        this.gssrSampleType = gssrSampleType;
        this.targetLaneCoverage = targetLaneCoverage;
        this.doAggregation = doAggregation;
    }

    public void setPreCircularizationSize(Double preCircularizationSize) {
        if (preCircularizationSize != null) {
            this.preCircularizationDnaSize = ThriftConversionUtil.zeroAsNull(preCircularizationSize.doubleValue());
        }
    }

    @XmlElement(name = "preCircularizationDnaSize")
    public Double getPreCircularizationSize() {
        return preCircularizationDnaSize;
    }

    public void setLabMeasuredInsertSize(Double labSize) {
        if (labSize != null) {
            this.labMeasuredInsertSize = ThriftConversionUtil.zeroAsNull(labSize.doubleValue());
        }
    }
    
    public Long getWorkRequest() {
        return workRequest;
    }

    public String getProject() {
        return project;
    }

    public String getOrganism() {
        return organism;
    }

    public String getCollaboratorSampleName() {
        return collaboratorSampleId;
    }

    public String getSampleLSID() {
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

    public MolecularIndexingSchemeBean getIndexingScheme() {
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

    @XmlElement(name = "labMeasuredInsertSize")
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

    public void setDevExperimentData(TZDevExperimentData experimentData) {
        this.devExperimentData = new DevExperimentDataBean(experimentData);
    }

    public Boolean isNegativeControl() {
        return isNegativeControl;
    }

    public Boolean isPositiveControl() {
        return isPositiveControl();
    }

    public Boolean isPartOfDevExperiment() {
        return isPositiveControl();
    }

    public Boolean doAggregation() {
        return doAggregation;
    }
}
