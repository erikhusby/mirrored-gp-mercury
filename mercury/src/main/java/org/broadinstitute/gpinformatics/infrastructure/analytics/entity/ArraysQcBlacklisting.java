package org.broadinstitute.gpinformatics.infrastructure.analytics.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

/**
 * Entity for array metrics blacklisting, ETL'd from cloud database by Analytics team. <br/>
 * Fetch this separately as an orphan because it's not linked as a child of ArraysQc by ID
 *   and JPA (despite a lot of research and trial) won't associate with chipWellBarcode.
 */
@Entity
@Table(schema = "ANALYTICS")
@IdClass(ArraysQcBlacklisting.ArrayBlacklistingPK.class)
public class ArraysQcBlacklisting implements Serializable {

    @Id
    private String chipWellBarcode;

    @Id
    private String blacklistReason;

    @Temporal(TemporalType.TIMESTAMP)
    private Date blacklistedOn;

    private String blacklistedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date whitelistedOn;

    private String whitelistedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedAt;

    private String notes;

    public String getChipWellBarcode() {
        return chipWellBarcode;
    }

    public String getBlacklistReason() {
        return blacklistReason;
    }

    public Date getBlacklistedOn() {
        return blacklistedOn;
    }

    public String getBlacklistedBy() {
        return blacklistedBy;
    }

    public Date getWhitelistedOn() {
        return whitelistedOn;
    }

    public String getWhitelistedBy() {
        return whitelistedBy;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public String getNotes() {
        return notes;
    }

    @Embeddable
    public static class ArrayBlacklistingPK implements Serializable {

        @Column(insertable = false, updatable = false)
        private String chipWellBarcode;
        @Column(insertable = false, updatable = false)
        private String blacklistReason;

        public String getChipWellBarcode() {
            return chipWellBarcode;
        }

        public void setChipWellBarcode(String chipWellBarcode) {
            this.chipWellBarcode = chipWellBarcode;
        }

        public String getBlacklistReason() {
            return blacklistReason;
        }

        public void setBlacklistReason(String blacklistReason) {
            this.blacklistReason = blacklistReason;
        }

        @Override
        public int hashCode() {
            return chipWellBarcode.hashCode() + (blacklistReason.hashCode() << 1);
        }

        @Override
        public boolean equals(Object that) {
            return (this == that)
                   || ((that instanceof ArrayBlacklistingPK)
                       && this.chipWellBarcode.equals(((ArrayBlacklistingPK) that).getChipWellBarcode())
                       && this.blacklistReason.equals(((ArrayBlacklistingPK) that).getBlacklistReason()));
        }
    }
}