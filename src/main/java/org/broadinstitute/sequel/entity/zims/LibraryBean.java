package org.broadinstitute.sequel.entity.zims;

import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A library DTO for Zamboni.  Copied from
 * LIMQuery.thrift from squidThriftService.
 */

public class LibraryBean {

    private String library;

    private String project;

    private String initiative;

    private Long workRequest;

    private MolecularIndexingSchemeBean indexingScheme;
    
    private Boolean hasIndexingRead;

    private String expectedInsertSize;
    
    private String analysisType;

    private String referenceSequence;

    private String referenceSequenceVersion;

    /** the name the collaborator has given to the sample */
    private String collaboratorSampleId;

    private Boolean doAggregation;

    /** the name of the collaborator */
    private String collaborator;

    private String organism;

    private String species;

    private String strain;

    private String sampleLSID;

    private String tissueType;

    private String expectedPlasmid;
    
    private String aligner;

    private String rrbsSizeRange;

    private String restrictionEnzyme;

    private String cellLine;

    private String bait;

    /** obfuscated name of the individual (person) from whence this sample was taken */
    private String individual;

    private Double labMeasuredInsertSize;

    private Boolean isPositiveControl;

    private Boolean isNegativeControl;

    private String weirdness;

    private Double preCircularizationDnaSize;

    private Boolean isPartOfDevExperiment;

    private DevExperimentDataBean devExperimentData;

    private Collection<String> gssrBarcodes = new ArrayList<String>();

    private String gssrSampleType;

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
        this.labMeasuredInsertSize = ThriftConversionUtil.zeroAsNull(labMeasuredInsertSize);
        isPositiveControl = positiveControl;
        isNegativeControl = negativeControl;
        this.weirdness = weirdness;
        this.preCircularizationDnaSize = ThriftConversionUtil.zeroAsNull(preCircularizationDnaSize);
        isPartOfDevExperiment = partOfDevExperiment;
        this.devExperimentData = new DevExperimentDataBean(devExperimentData);
        this.gssrBarcodes = gssrBarcodes;
        this.gssrSampleType = gssrSampleType;
        this.targetLaneCoverage = targetLaneCoverage;
        this.doAggregation = doAggregation;
    }


    public Double getPreCircularizationSize() {
        return preCircularizationDnaSize;
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

    public Boolean isPartOfDevExperiment() {
        return isNegativeControl;
    }

    public Boolean doAggregation() {
        return doAggregation;
    }
}
