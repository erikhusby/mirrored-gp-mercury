/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.project;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.submission.FileType;
import org.broadinstitute.gpinformatics.infrastructure.submission.ISubmissionTuple;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


// setting the access order to alphabetical helps the tests pass more reliably.
@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SubmissionTuple implements ISubmissionTuple {
    private static final long serialVersionUID = 1262062294730627888L;
    public static final String PROCESSING_LOCATION_UNKNOWN = null;
    public static final String DATA_TYPE_UNKNOWN = null;
    public static final String VERSION_UNKNOWN = null;
    private static Log log = LogFactory.getLog(SubmissionTuple.class);

    @JsonProperty
    private String project;
    @JsonProperty
    private String mercuryProject;
    @JsonProperty
    private String sampleName;

    // We only support BAM files.
    @JsonProperty
    private FileType fileType = FileType.BAM;
    @JsonProperty
    private String version;
    @JsonProperty
    private String processingLocation;
    @JsonProperty
    private String dataType;

    /**
     * No-arg constructor needed for JSON deserialization.
     */
    SubmissionTuple() {
    }

    /**
     * @return SubmissionTuple object from given jsonString
     */
    @JsonCreator
    public SubmissionTuple(String jsonString) {
        SubmissionTuple tuple = null;
        ObjectMapper objectMapper=new ObjectMapper();
        try {
            tuple = objectMapper.readValue(jsonString, SubmissionTuple.class);
            initSubmissionTuple(tuple.project, tuple.mercuryProject, tuple.sampleName, tuple.version,
                tuple.processingLocation, tuple.dataType);
        } catch (IOException e) {
            log.error(String.format("Could not map JSON String [%s] to SubmissionTuple", jsonString), e);
        }
    }

    public SubmissionTuple(String project, String mercuryProject, String sampleName, String version,
                           String processingLocation,
                           String dataType) {
        initSubmissionTuple(project, mercuryProject, sampleName, version, processingLocation, dataType);
    }

    private void initSubmissionTuple(String project, String mercuryProject, String sampleName, String version,
                                     String processingLocation,
                                     String dataType) {
        this.project = project;
        this.mercuryProject = mercuryProject;
        this.sampleName = sampleName;
        this.version = version;
        this.processingLocation = processingLocation;
        this.dataType = SubmissionLibraryDescriptor.getNormalizedLibraryName(dataType);
    }

    @Override
    public String getProject() {
        return project;
    }

    public String getMercuryProject() {
        return mercuryProject;
    }

    @Override
    public String getSampleName() {
        return sampleName;
    }

    @Override
    public String getVersionString() {
        return version;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public FileType getFileType() {
        return fileType;
    }

    void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    @Override
    public String getProcessingLocation() {
        return processingLocation;
    }

    public static List<String> extractSampleNames(Collection<SubmissionTuple> tuples) {
        List<String> samples = new ArrayList<>();
        for (SubmissionTuple tuple : tuples) {
            samples.add(tuple.getSampleName());
        }
        return samples;
    }

    @Override
    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    @Override
    public String toString() {
        ObjectMapper objectMapper=new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (IOException e) {
            log.info("SubmissionTracker could not be converted to JSON String.", e);
        }
        return null;
    }

    public static boolean hasTuple(List<? extends ISubmissionTuple> tuples, ISubmissionTuple tuple) {
        boolean hasTuple = false;
        for (ISubmissionTuple tupleItem : tuples) {
            if (tuple.equals(tupleItem)) {
                hasTuple = true;
            }
        }
        return hasTuple;
    }

    protected boolean tupleEquals(ISubmissionTuple tuple){
        return this.equals(tuple);
    }

    @Override
    public SubmissionTuple getSubmissionTuple() {
        return this;
    }

    @JsonIgnore
    public boolean isMercuryProject() {
        if (getProject() != null) {
            return getProject().equals(getMercuryProject());
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SubmissionTuple that = (SubmissionTuple) o;

        EqualsBuilder equalsBuilder = new EqualsBuilder()
            .append(project, that.project)
            .append(mercuryProject, that.mercuryProject)
            .append(sampleName, that.sampleName)
            .append(fileType, that.fileType)
            .append(version, that.version)
            .append(processingLocation, that.processingLocation);
        if (isMercuryProject()) {
            equalsBuilder.append(dataType, that.dataType);
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder(17, 37)
            .append(project)
            .append(mercuryProject)
            .append(sampleName)
            .append(fileType)
            .append(version)
            .append(processingLocation);
        if (isMercuryProject()) {
            hashCodeBuilder.append(dataType);
        }
        return hashCodeBuilder.toHashCode();
    }

    public static Map<String, Collection<SubmissionTuple>> byProject(Collection<SubmissionTuple> tuples) {
        Multimap<String, SubmissionTuple> byProject = HashMultimap.create();
        for (SubmissionTuple tuple : tuples) {
            byProject.put(tuple.getProject(), tuple);
        }
        return byProject.asMap();
    }
}
