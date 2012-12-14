package org.broadinstitute.gpinformatics.athena.entity.work;

import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * This is the message containing all information for work complete
 */
@Entity
@Audited
@Table(name= "WORK_COMPLETE_MESSAGE", schema = "athena")
public class WorkCompleteMessage {
    public enum REQUIRED_NAMES {
        PDO_NAME, SAMPLE_NAME, SAMPLE_INDEX, ALIQUOT_LSID, COMPLETED_TIME
    }

    WorkCompleteMessage() {
    }

    public WorkCompleteMessage(
        String pdoName, String sampleName, long sampleIndex, String aliquotLsid, Date completedDate, Map<String, Object> dataMap) {

        this.pdoName = pdoName;
        this.collaboratorSampleId = sampleName;
        this.sampleIndex = sampleIndex;
        this.aliquotLsid = aliquotLsid;
        this.completedDate = completedDate;

        createMessageDataValues(dataMap);
    }

    private void createMessageDataValues(Map<String, Object> dataMap) {
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            data.put(entry.getKey(), new MessageDataValue(entry.getKey(), entry.getValue().toString()));
        }
    }

    @Id
    @SequenceGenerator(name = "SEQ_WORK_COMPLETE", schema = "athena", sequenceName = "SEQ_WORK_COMPLETE", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_WORK_COMPLETE")
    private Long workCompleteMessageId;

    @Column(name = "PDO_NAME")
    private String pdoName;

    @Column(name = "COLLABORATOR_SAMPLE_ID")
    private String collaboratorSampleId;

    // If the same sample exists multiple times in the PDO, this will let us know which was meant
    @Column(name = "SAMPLE_INDEX")
    private Long sampleIndex;

    @Column(name = "ALIQUOT_LSID")
    private String aliquotLsid;

    @Column(name = "COMPLETED_DATE")
    private Date completedDate;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, fetch= FetchType.LAZY)
    @MapKeyColumn(name="key")
    private Map<String, MessageDataValue> data;
}
