package org.broadinstitute.gpinformatics.athena.entity.work;

import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the message containing all information for work complete
 */
@Entity
@Audited
@Table(name= "WORK_COMPLETE_MESSAGE", schema = "athena")
public class WorkCompleteMessage {
    public enum REQUIRED_NAMES {
        PDO_NAME, SAMPLE_NAME, SAMPLE_INDEX, COMPLETED_TIME
    }

    WorkCompleteMessage() {
    }

    public WorkCompleteMessage(
        @Nonnull String pdoName, @Nonnull String sampleName, int sampleIndex, @Nonnull Date completedDate,
        @Nonnull Map<String, Object> dataMap) {

        this.pdoName = pdoName;
        this.sampleName = sampleName;
        this.sampleIndex = sampleIndex;
        this.completedDate = completedDate;

        data = new HashMap<String, MessageDataValue>();
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

    @Column(name = "SAMPLE_NAME", length = 255, nullable = false)
    @Nonnull
    private String sampleName;

    // If the same sample exists multiple times in the PDO, this will let us know which was meant
    @Column(name = "SAMPLE_INDEX", nullable = false)
    private int sampleIndex;

    @Column(name = "COMPLETED_DATE", nullable = false)
    @Nonnull
    private Date completedDate;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.LAZY)
    @MapKeyColumn(name="KEY")
    @JoinColumn(name = "WORK_COMPLETE_MESSAGE", nullable = false)
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
    public String getSampleName() {
        return sampleName;
    }

    public Integer getSampleIndex() {
        return sampleIndex;
    }

    @Nonnull
    public Date getCompletedDate() {
        return completedDate;
    }

    @Nonnull
    public Map<String, MessageDataValue> getData() {
        return data;
    }

    public Date getProcessDate() {
        return processDate;
    }

    public void setProcessDate(Date processDate) {
        this.processDate = processDate;
    }
}
