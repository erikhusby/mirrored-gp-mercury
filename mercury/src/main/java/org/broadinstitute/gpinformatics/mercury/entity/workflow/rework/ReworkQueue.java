/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.workflow.rework;

import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.List;

@Entity
@Audited
@Table(schema = "mercury")
public class ReworkQueue {
    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_REWORK_QUEUE", schema = "mercury", sequenceName = "SEQ_REWORK_QUEUE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REWORK_QUEUE")
    Long reworkQueueId;

    public ReworkQueue() {
    }

    @ManyToMany(fetch = FetchType.LAZY)
    List<ReworkBatch> reworkBatchList;

    public ReworkQueue(List<ReworkBatch> reworkBatchList) {
        this.reworkBatchList = reworkBatchList;
    }
}
