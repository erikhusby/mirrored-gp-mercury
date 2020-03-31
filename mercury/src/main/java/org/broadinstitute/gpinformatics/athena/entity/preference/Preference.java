/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.preference;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.util.Date;

/**
 * This class handles the storage of preference data. It is based on a similar class created by Mike Dinsmore
 * for GAP. Any problems with logic or comments are his fault.
 */
@Entity
@Table(name = "PREF_PREFERENCE", schema = "athena")
public class Preference {

    @Id
    @SequenceGenerator(name = "SEQ_PREFERENCE", schema = "athena", sequenceName = "SEQ_PREFERENCE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PREFERENCE")
    private Long preferenceId;

    /** The user the preference is associated with */
    @Column(name = "USER_ID")
    private Long associatedUser;

    /** The type of the preference */
    @Enumerated(EnumType.STRING)
    @Column(name = "PREFERENCE_TYPE")
    private PreferenceType preferenceType;

    @Column(name = "OBJECT1_ID")
    private Long object1Id;

    @Column(name = "OBJECT2_ID")
    private Long object2Id;

    /** The data in a string clob. Using xml for many of these. */
    @Lob
    @Column(name = "DATA")
    private String data;

    /** BSP 'User' for the person who created this. */
    @Column(name = "CREATED_BY")
    private Long createdBy;

    /** When the object was created */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_DATE")
    private Date createdDate;

    /** When the object was last modified */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "MODIFIED_DATE")
    private Date modifiedDate;

    /** BSP 'User' for the person who modified this. */
    @Column(name = "MODIFIED_BY")
    private Long modifiedBy;

    protected Preference() {}

    public Preference(
        long createdBy, long object1Id, @Nullable Long object2Id, @Nonnull PreferenceType preferenceType, @Nonnull String data) {

        this(preferenceType, data);
        this.object1Id = object1Id;
        this.object2Id = object2Id;
        this.createdBy = createdBy;
        this.modifiedBy = createdBy;
    }

    public Preference(@Nonnull Long associatedUser, @Nonnull PreferenceType preferenceType, @Nonnull String data) {
        this(preferenceType, data);
        this.associatedUser = associatedUser;
        createdBy = associatedUser;
        modifiedBy = associatedUser;
    }

    public Preference(@Nonnull PreferenceType preferenceType, @Nonnull String data) {
        this.associatedUser = null;
        this.object1Id = null;
        this.object2Id= null;

        this.preferenceType = preferenceType;
        this.data = data;

        // For now we don't allow for users creating preferences for other users.
        createdDate = new Date();
        modifiedDate = createdDate;
    }

    public Long getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(Long preferenceId) {
        this.preferenceId = preferenceId;
    }

    public Long getAssociatedUser() {
        return associatedUser;
    }

    public void setAssociatedUser(Long associatedUser) {
        this.associatedUser = associatedUser;
    }

    public PreferenceType getPreferenceType() {
        return preferenceType;
    }

    public void setPreferenceType(PreferenceType preferenceType) {
        this.preferenceType = preferenceType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Transient
    private PreferenceDefinition preferenceDefinition;
    public PreferenceDefinition getPreferenceDefinition() throws Exception {
        if ((preferenceDefinition == null) || (preferenceDefinition.getDefinitionValue() == null)) {
            preferenceDefinition = new PreferenceDefinition(preferenceType, getData());
        }

        return preferenceDefinition;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(Long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    /**
     * @return the object1Id
     */
    public Long getObject1Id() {
        return object1Id;
    }

    /**
     * @param object1Id the object1Id to set
     */
    public void setObject1Id(Long object1Id) {
        this.object1Id = object1Id;
    }

    /**
     * @return the object2Id
     */
    public Long getObject2Id() {
        return object2Id;
    }

    /**
     * @param object2Id the object2Id to set
     */
    public void setObject2Id(Long object2Id) {
        this.object2Id = object2Id;
    }

    public void markModified(@Nonnull String data) {
        this.data = data;
        setModifiedDate(new Date());
    }

    public void update(long object1Id, @Nullable Long object2Id, @Nonnull String data) {
        this.object1Id = object1Id;
        this.object2Id = object2Id;
        this.data = data;
        this.modifiedDate = new Date();
    }

    public boolean isSame(@Nonnull String data) {
        return data.equals(this.data);
    }

    public boolean isSame(long object1Id, @Nullable Long object2Id, @Nonnull String data) {
        return (this.object1Id == object1Id) &&
               (((this.object2Id == null) && (object2Id == null)) ||
                 (this.object2Id != null) && this.object2Id.equals(object2Id)) &&
               data.equals(this.data);
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (!(other instanceof Preference)) {
            return false;
        }

        Preference castOther = (Preference) other;
        return new EqualsBuilder().append(getPreferenceId(), castOther.getPreferenceId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getPreferenceId()).toHashCode();
    }
}
