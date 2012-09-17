package org.broadinstitute.sequel.infrastructure.jira.issue.comment;


import org.broadinstitute.sequel.infrastructure.jira.issue.Visibility;

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
    
    
    public static AddCommentRequest create(String key, String body, Visibility.Type type, Visibility.Value value) {

        AddCommentRequest ret = new AddCommentRequest();

        ret.setBody(body);

        Visibility visibility = Visibility.create(type, value);

        ret.setVisibility(visibility);

        return ret;
    }
    
    
    public static AddCommentRequest create(String key, String body) {
        
        AddCommentRequest ret = new AddCommentRequest();
        
        ret.setBody(body);
        return ret;
    }
}
