package org.broadinstitute.gpinformatics.infrastructure.jira.issue.comment;


import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Author;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;

import java.io.Serializable;

public class AddCommentResponse implements Serializable {
    
    private String self;

    private String id;
    
    private Author author;
    
    private String body;

    private Author updateAuthor;

    private String created;
    
    private String updated;

    private Visibility visibility;

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Author getUpdateAuthor() {
        return updateAuthor;
    }

    public void setUpdateAuthor(Author updateAuthor) {
        this.updateAuthor = updateAuthor;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }


    @Override
    public String toString() {
        return "AddCommentResponse{" +
                "self='" + self + '\'' +
                ", id='" + id + '\'' +
                ", author=" + author +
                ", body='" + body + '\'' +
                ", updateAuthor=" + updateAuthor +
                ", created='" + created + '\'' +
                ", updated='" + updated + '\'' +
                ", visibility=" + visibility +
                '}';
    }
}
