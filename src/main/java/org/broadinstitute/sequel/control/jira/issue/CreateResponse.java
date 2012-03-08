package org.broadinstitute.sequel.control.jira.issue;


import java.io.Serializable;

public class CreateResponse implements Serializable {
    
    private String id;
    
    private String key;
    
    private String self;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
