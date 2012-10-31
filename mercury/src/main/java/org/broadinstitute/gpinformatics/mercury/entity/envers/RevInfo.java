package org.broadinstitute.gpinformatics.mercury.entity.envers;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

/**
 * The entity that Envers uses to store information about each revision
 */
@Entity
@RevisionEntity(EnversRevisionListener.class)
@Table(schema = "mercury")
public class RevInfo implements Serializable { /* extends DefaultRevisionEntity or DefaultTrackingModifiedEntitiesRevisionEntity*/
    @Id
    @SequenceGenerator(name = "SEQ_REV_INFO", schema = "mercury", sequenceName = "SEQ_REV_INFO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REV_INFO")
    @RevisionNumber
    private Long revInfoId;

    @RevisionTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date revDate;

    private String username;

    public Long getRevInfoId() {
        return revInfoId;
    }

    public void setRevInfoId(Long revInfoId) {
        this.revInfoId = revInfoId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getRevDate() {
        return revDate;
    }

    public void setRevDate(Date revDate) {
        this.revDate = revDate;
    }
}