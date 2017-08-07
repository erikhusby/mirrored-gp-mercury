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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.submission.FileType;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.ObjectMapper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


// setting the access order to alphabetical helps the tests pass more reliably.
@JsonPropertyOrder(alphabetic = true)
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SubmissionTuple implements Serializable {
    private static final long serialVersionUID = 1262062294730627888L;
    public static final String PROCESSING_LOCATION_UNKNOWN = null;
    public static final String DATA_TYPE_UNKNOWN = null;
    public static final String VERSION_UNKNOWN = null;
    private static Log log = LogFactory.getLog(SubmissionTuple.class);

    @JsonProperty
    private String project;
    @JsonProperty
    private String sampleName;
    @JsonProperty
    private FileType fileType = FileType.BAM;
    @JsonProperty
    private String version;
    @JsonProperty
    private String processingLocation;
    @JsonProperty
    private String dataType;
    @JsonIgnore
    private ObjectMapper objectMapper=null;
    @JsonIgnore
    private String jsonValue;

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
        try {
            tuple = getObjectMapper().readValue(jsonString, SubmissionTuple.class);
            initSubmissionTuple(tuple.project, tuple.sampleName, tuple.version, tuple.processingLocation, tuple.dataType);
        } catch (IOException e) {
            log.info(String.format("Could not map JSON String [%s] to SubmissionTuple", jsonString), e);
        }
    }

    public SubmissionTuple(String project, String sampleName, String version, String processingLocation,
                           String dataType) {
        initSubmissionTuple(project, sampleName, version, processingLocation, dataType);
    }

    private void initSubmissionTuple(String project, String sampleName, String version, String processingLocation,
                                     String dataType) {
        this.project = project;
        this.sampleName = sampleName;
        this.fileType = fileType;
        this.version = version;
        this.processingLocation = processingLocation;
        this.dataType = dataType;
    }

    public String getProject() {
        return project;
    }

    public String getSampleName() {
        return sampleName;
    }

    public String getVersion() {
        return version;
    }

    public FileType getFileType() {
        return fileType;
    }

    void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper=new ObjectMapper();
        }

        return objectMapper;
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
        return new EqualsBuilder()
                .append(this.sampleName, that.sampleName)
                .append(this.project, that.project)
                .append(this.fileType, that.fileType)
                .append(this.processingLocation, that.processingLocation)
                .append(this.version, that.version)
                .append(this.dataType, that.dataType).isEquals();
    }

    public static List<String> samples(List<SubmissionTuple> tuples) {
        List<String> samples = new ArrayList<>();
        for (SubmissionTuple tuple : tuples) {
            samples.add(tuple.getSampleName());
        }
        return samples;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    @Override
    public String toString() {
        if (StringUtils.isBlank(jsonValue)) {
            try {
                jsonValue = getObjectMapper().writeValueAsString(this);
            } catch (IOException e) {
                log.info("SubmissionTracker could not be converted to JSON String.", e);
            }
        }
        return jsonValue;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.sampleName).append(project).append(this.fileType).append(this.version)
            .append(this.processingLocation).append(this.dataType).hashCode();
    }
}
