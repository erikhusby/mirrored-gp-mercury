package org.broadinstitute.sequel.infrastructure.jira.issue;


import java.io.Serializable;

public class CreateIssueResponse implements Serializable {
    
    private String id;

    private String key;
    
    private String self;

    public CreateIssueResponse(String id,String key) {
        this.id = id;
        this.key = key;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTicketName() {
        return key;
    }
    
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }


    @Override
    public String toString() {
        return "CreateResponse{" +
                "id='" + id + '\'' +
                ", key='" + key + '\'' +
                ", self='" + self + '\'' +
                '}';
    }
}
