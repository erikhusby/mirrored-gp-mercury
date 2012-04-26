package org.broadinstitute.sequel.entity.zims;

import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;

import javax.xml.bind.annotation.*;
import java.util.Collection;

/**
 * A library DTO for Zamboni.  Copied from
 * LIMQuery.thrift from squidThriftService.
 */
@XmlRootElement(name = "Library")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class LibraryBean {

    @XmlAttribute(name = "library")
    private String library;

    @XmlAttribute(name = "project")
    private String project;

    @XmlAttribute(name = "initiative")
    private String initiative;

    @XmlAttribute(name = "workRequestId")
    private Long workRequest;

    @XmlElement(name = "MolecularIndexingScheme")
    private MolecularIndexingSchemeBean indexingScheme;
    
    private Boolean hasIndexingRead;
    
    @XmlAttribute(name = "expectedInsertSize")
    private String expectedInsertSize;
    
    @XmlAttribute(name = "analysisType")
    private String analysisType;

    @XmlAttribute(name = "referenceSequence")
    private String referenceSequence;

    @XmlAttribute(name = "referenceSequenceVersion")
    private String referenceSequenceVersion;

    @XmlAttribute(name = "sampleAlias")
    /** the name the collaborator has given to the sample */
    private String collaboratorSampleId;

    @XmlAttribute(name = "sampleCollaborator")
    /** the name of the collaborator */
    private String collaborator;

    @XmlAttribute(name = "organism")
    private String organism;

    @XmlAttribute(name = "species")
    private String species;

    @XmlAttribute(name = "strain")
    private String strain;

    @XmlAttribute(name = "lsid")
    private String sampleLSID;

    @XmlAttribute(name = "tissueType")
    private String tissueType;
    
    private String expectedPlasmid;
    
    @XmlAttribute(name = "aligner")
    private String aligner;

    @XmlAttribute(name = "rrbsSizeRange")
    private String rrbsSizeRange;

    @XmlAttribute(name = "restrictionEnzyme")
    private String restrictionEnzyme;

    @XmlAttribute(name = "cellLine")
    private String cellLine;

    @XmlAttribute(name = "BaitSetName")
    private String bait;

    @XmlAttribute(name = "individual")
    /** obfuscated name of the individual (person) from whence this sample was taken */
    private String individual;

    @XmlAttribute(name = "labMeasuredInsertSize")
    private Double labMeasuredInsertSize;
    
    private Boolean isPositiveControl;
    
    private Boolean isNegativeControl;

    @XmlAttribute(name = "tissueType")
    private String weirdness;
    
    private Double preCircularizationDnaSize;
    
    private Boolean isPartOfDevExperiment;

    private DevExperimentDataBean devExperimentData;

    @XmlAttribute(name = "gssrBarcode")
    private String gssrBarcode;

    @XmlAttribute(name = "gssrBarcodes")
    private Collection<String> gssrBarcodes;

    @XmlAttribute(name = "gssrSampleType")
    private String gssrSampleType;

    @XmlAttribute(name = "targetLaneCoverage")
    private Short targetLaneCoverage;
    
    public LibraryBean() {}

    public LibraryBean(String library, String project, String initiative, Long workRequest, MolecularIndexingScheme indexingScheme, Boolean hasIndexingRead, String expectedInsertSize, String analysisType, String referenceSequence, String referenceSequenceVersion, String collaboratorSampleId, String collaborator, String organism, String species, String strain, String sampleLSID, String tissueType, String expectedPlasmid, String aligner, String rrbsSizeRange, String restrictionEnzyme, String cellLine, String bait, String individual, Double labMeasuredInsertSize, Boolean positiveControl, Boolean negativeControl, String weirdness, Double preCircularizationDnaSize, Boolean partOfDevExperiment, TZDevExperimentData devExperimentData,String gssrBarcode,Collection<String> gssrBarcodes,String gssrSampleType,Short targetLaneCoverage) {
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
        this.labMeasuredInsertSize = labMeasuredInsertSize;
        isPositiveControl = positiveControl;
        isNegativeControl = negativeControl;
        this.weirdness = weirdness;
        setPreCircularizationSize(preCircularizationDnaSize);        
        isPartOfDevExperiment = partOfDevExperiment;
        this.devExperimentData = new DevExperimentDataBean(devExperimentData);
        this.gssrBarcode = gssrBarcode;
        this.gssrBarcodes = gssrBarcodes;
        this.gssrSampleType = gssrSampleType;
        this.targetLaneCoverage = targetLaneCoverage;
    }

    private void setPreCircularizationSize(Double preCircularizationSize) {
        if (preCircularizationSize != null) {
            if (preCircularizationSize.doubleValue() == 0) {
                this.preCircularizationDnaSize = null;
            }
            else {
                this.preCircularizationDnaSize = preCircularizationSize;
            }
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

    public Double getPrecircularizationDnaSize() {
        return preCircularizationDnaSize;
    }

    public MolecularIndexingSchemeBean getIndexingScheme() {
        return indexingScheme;
    }

    public String getAligner() {
        return aligner;
    }

    public String getGssrBarcode() {
        return gssrBarcode;
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
}
