package org.broadinstitute.gpinformatics.infrastructure.jira.issue.comment;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddCommentResponse implements Serializable {
    
    @Override
    public String toString() {
        return "AddCommentResponse{}";
    }
}
