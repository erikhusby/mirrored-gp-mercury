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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;

public class SubmissionTuple implements Serializable {
    private static final long serialVersionUID = 1262062294730627888L;
    private static Log log = LogFactory.getLog(SubmissionTuple.class);

    @JsonProperty
    private String project;
    @JsonProperty
    private String sampleName;
    @JsonProperty
    private BassFileType fileType;
    @JsonProperty
    private String version;

    /**
     * No-arg constructor needed for JSON deserialization.
     */
    SubmissionTuple() {
    }

    public SubmissionTuple(String project, String sampleName, String version, BassFileType fileType) {
        this.project = project;
        this.sampleName = sampleName;
        this.fileType = fileType;
        this.version = version;
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
                .append(this.version, that.version).isEquals();
    }

    /**
     * JSON Representation of this SubmissionTuple.
     *
     * @return JSON Representation of this SubmissionTuple.
     */
    public String jsonString() {
        ObjectMapper objectMapper = new ObjectMapper();
        String stringValue = null;
        try {
            stringValue = objectMapper.writeValueAsString(this);
        } catch (IOException e) {
            log.info("SubmissionTracker could not be converted to JSON String.", e);
        }
        return stringValue;
    }

    /**
     * @return SubmissionTuple object from given jsonString
     */
    public static SubmissionTuple fromJson(String jsonString) {
        SubmissionTuple submissionTuple = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            submissionTuple = objectMapper.readValue(jsonString, SubmissionTuple.class);
        } catch (IOException e) {
            log.info(String.format("Could not map JSON String [%s] to SubmissionTuple", jsonString), e);
        }
        return submissionTuple;
    }

    @Override
    public String toString() {
        return String.format("{project = %s; sampleName = %s; version = %s; fileType = %s}",
                project, sampleName, version, fileType);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.sampleName).append(project).append(this.fileType).append(this.version)
                .hashCode();
    }
}
