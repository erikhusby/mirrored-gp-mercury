package org.broadinstitute.gpinformatics.mercury.entity.envers;

import org.hibernate.annotations.Proxy;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * The entity that Envers uses to store information about each revision
 */
@Entity
@RevisionEntity(EnversRevisionListener.class)
@Table(schema = "mercury")
// CrossTypeRevisionChangesReaderImpl.findEntityTypes uses reflection on a proxy for modifiedEntityNames
// Fails miserably and tagging with fetch=FetchType.EAGER also doesn't kill the proxy
@Proxy(lazy=false)
public class RevInfo implements Serializable {
    @Id
    @SequenceGenerator(name = "SEQ_REV_INFO", schema = "mercury", sequenceName = "SEQ_REV_INFO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REV_INFO")
    @RevisionNumber
    private Long revInfoId;

    @RevisionTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date revDate;

    private String username;

    @ElementCollection
    @JoinTable(name = "REVCHANGES", joinColumns = @JoinColumn(name = "REV"))
    @Column(name = "ENTITYNAME")
    @ModifiedEntityNames
    private Set<String> modifiedEntityNames;

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

    public Set<String> getModifiedEntityNames() {
        return modifiedEntityNames;
    }

    public void setModifiedEntityNames(Set<String> modifiedEntityNames) {
        this.modifiedEntityNames = modifiedEntityNames;
    }
}