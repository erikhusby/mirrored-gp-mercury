package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import java.util.Date;

public class Worklog {

    Author author;
    Author updateAuthor;
    String comment;

    Date started;
    Date created;
    Date updated;

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public Author getUpdateAuthor() {
        return updateAuthor;
    }

    public void setUpdateAuthor(Author updateAuthor) {
        this.updateAuthor = updateAuthor;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }
}
