package org.broadinstitute.gpinformatics.infrastructure.jira.issue.comment;


import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;

import java.io.Serializable;

public class AddCommentRequest implements Serializable {

    private String body;

    private Visibility visibility;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }
    
    
    public static AddCommentRequest create(String body, Visibility.Type type, Visibility.Value value) {

        AddCommentRequest ret = new AddCommentRequest();

        ret.setBody(body);

        Visibility visibility = Visibility.create(type, value);

        ret.setVisibility(visibility);

        return ret;
    }
    
    
    public static AddCommentRequest create(String body) {
        
        AddCommentRequest ret = new AddCommentRequest();
        
        ret.setBody(body);
        return ret;
    }
}
