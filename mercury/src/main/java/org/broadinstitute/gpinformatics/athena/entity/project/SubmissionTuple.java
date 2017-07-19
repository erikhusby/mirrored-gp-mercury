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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.submission.FileType;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class SubmissionTuple implements Serializable {
    private static final long serialVersionUID = 1262062294730627888L;
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

    @JsonIgnore
    private final ObjectMapper objectMapper = new ObjectMapper();
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
        SubmissionTuple submissionTuple = null;
        try {
            submissionTuple = objectMapper.readValue(jsonString, SubmissionTuple.class);
            this.project=submissionTuple.project;
            this.sampleName=submissionTuple.sampleName;
            this.version=submissionTuple.version;
            this.fileType = submissionTuple.fileType;
            this.processingLocation= submissionTuple.processingLocation;
        } catch (IOException e) {
            log.info(String.format("Could not map JSON String [%s] to SubmissionTuple", jsonString), e);
        }
    }

    public SubmissionTuple(String project, String sampleName, String version, String processingLocation) {
        this.project = project;
        this.sampleName = sampleName;
        this.fileType = fileType;
        this.version = version;
        this.processingLocation = processingLocation;
        this.jsonValue = jsonValue;
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
                .append(this.version, that.version).isEquals();
    }

    /**
     * @return SubmissionTuple object from given jsonString
     */
    public static Map<String, Collection<SubmissionTuple>> sampleMap(Collection<SubmissionTuple> tuples,
                                                                     SubmissionTuple submissionTuple) {
        Multimap<String, SubmissionTuple> result = HashMultimap.create();
        for (SubmissionTuple tuple : tuples) {
            String sampleName = submissionTuple.getSampleName();
            if (tuple.getSampleName().equals(sampleName) && tuple.getProject().equals(submissionTuple.getProject())){
                result.put(sampleName, tuple);
            }
        }
        return result.asMap();
    }

    public static List<String> samples(List<SubmissionTuple> tuples) {
        List<String> samples = new ArrayList<>();
        for (SubmissionTuple tuple : tuples) {
            samples.add(tuple.getSampleName());
        }
        return samples;
    }

    @Override
    public String toString() {
        if (StringUtils.isBlank(jsonValue)) {
            try {
                jsonValue = objectMapper.writeValueAsString(this);
            } catch (IOException e) {
                log.info("SubmissionTracker could not be converted to JSON String.", e);
            }
        }
        return jsonValue;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.sampleName).append(project).append(this.fileType).append(this.version)
            .append(this.processingLocation).hashCode();
    }

    void setFileType(FileType fileType) {
        this.fileType = fileType;
    }
}
