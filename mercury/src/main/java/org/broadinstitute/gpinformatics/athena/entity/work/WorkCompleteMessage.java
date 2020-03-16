package org.broadinstitute.gpinformatics.athena.entity.work;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This contains information for work complete messages from pipelines.
 */
@Entity
@Audited
@Table(name= "WORK_COMPLETE_MESSAGE", schema = "athena")
public class WorkCompleteMessage  implements Serializable {
    private static final long serialVersionUID = 9210151556903945304L;

    public enum Properties {
        PDO_NAME, ALIQUOT_ID, COMPLETED_TIME, PF_READS, PF_ALIGNED_GB, PF_READS_ALIGNED_IN_PAIRS,
        PCT_TARGET_BASES_20X, PCT_TARGET_BASES_100X,PART_NUMBER,USER_ID, FORCE_AUTOBILL
    }

    protected WorkCompleteMessage() {
    }

    public WorkCompleteMessage(
        @Nonnull String pdoName, @Nonnull String aliquotId, @Nullable String partNumber, @Nullable Long userId,
        @Nonnull Date completedDate, @Nonnull Map<String, Object> dataMap) {
        this.pdoName = pdoName;
        this.aliquotId = aliquotId;
        this.partNumber = partNumber;
        this.userId = userId;
        this.completedDate = completedDate;

        data = new HashMap<>();
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            data.put(entry.getKey(), new MessageDataValue(entry.getKey(), entry.getValue().toString()));
        }
    }

    @Id
    @SequenceGenerator(name = "SEQ_WORK_COMPLETE", schema = "athena", sequenceName = "SEQ_WORK_COMPLETE", allocationSize = 5)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_WORK_COMPLETE")
    private Long workCompleteMessageId;

    @Column(name = "PDO_NAME", length = 255, nullable = false)
    @Nonnull
    private String pdoName;

    @Column(name = "ALIQUOT_ID", length = 255, nullable = false)
    @Nonnull
    private String aliquotId;

    @Column(name = "COMPLETED_DATE", nullable = false)
    @Nonnull
    private Date completedDate;

    @Column(name = "PART_NUMBER", nullable = true)
    @Nullable
    private String partNumber;

    @Column(name = "USER_ID", nullable = true)
    @Nullable
    private Long userId;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.LAZY)
    @MapKeyColumn(name="KEY")
    @JoinColumn(name = "WORK_COMPLETE_MESSAGE", nullable = false)
    @AuditJoinTable(name = "WORK_COMPLETE_MESSAGE_JOIN_AUD")
    @Nonnull
    private Map<String, MessageDataValue> data;

    /** If not null, the date when this message was processed.  If null, the message hasn't yet been processed. */
    @Column(name = "PROCESS_DATE")
    private Date processDate;

    @Nonnull
    public String getPdoName() {
        return pdoName;
    }

    @Nonnull
    public String getAliquotId() {
        return aliquotId;
    }

    @Nonnull
    public Date getCompletedDate() {
        return completedDate;
    }

    @Nonnull
    public Map<String, MessageDataValue> getData() {
        return data;
    }

    @SuppressWarnings("unused")
    public Date getProcessDate() {
        return processDate;
    }

    public void setProcessDate(Date processDate) {
        this.processDate = processDate;
    }

    public BigInteger getPfReads() {
        return getBigIntegerPropertyValue(Properties.PF_READS);
    }

    public BigInteger getAlignedGb() {
        return getBigIntegerPropertyValue(Properties.PF_ALIGNED_GB);
    }

    public BigInteger getPfReadsAlignedInPairs() {
        return getBigIntegerPropertyValue(Properties.PF_READS_ALIGNED_IN_PAIRS);
    }

    public Double getPercentCoverageAt20X() {
        return getDoublePropertyValue(Properties.PCT_TARGET_BASES_20X);
    }

    public Double getPercentCoverageAt100X() {
        return getDoublePropertyValue(Properties.PCT_TARGET_BASES_100X);
    }

    @Nullable
    public String getPartNumber() {
        return partNumber;
    }

    @Nullable
    public Long getUserId() {
        return userId;
    }

    private BigInteger getBigIntegerPropertyValue(Properties property) {
        MessageDataValue messageDataValue = data.get(property.name());
        if (messageDataValue != null) {
            String value = messageDataValue.getValue();
            if (!StringUtils.isEmpty(value)) {
                return new BigInteger(value);
            }
        }
        return null;
    }

    private Double getDoublePropertyValue(Properties property) {
        MessageDataValue messageDataValue = data.get(property.name());
        if (messageDataValue != null) {
            String value = messageDataValue.getValue();
            if (!StringUtils.isEmpty(value)) {
                return Double.parseDouble(value);
            }
        }
        return null;
    }
}
