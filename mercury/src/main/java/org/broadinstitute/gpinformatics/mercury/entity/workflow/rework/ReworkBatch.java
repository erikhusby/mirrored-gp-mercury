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

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.List;

@Entity
@Audited
@Table(schema = "mercury")
public class ReworkBatch {
    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_REWORK_BATCH", schema = "mercury", sequenceName = "SEQ_REWORK_BATCH")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REWORK_BATCH")
    private Long reworkBatchId;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private List<MercurySample> reworkedSamples;

    @Enumerated
    private ReworkReason reason;

    @Enumerated
    private ReworkLevel reworkLevel;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabEvent labEvent;

    private String comments;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private RapSheet rapSheet;

    public ReworkBatch() {
    }

    @SuppressWarnings("UnusedDeclaration")
    public ReworkBatch(List<MercurySample> reworkedSamples, ReworkReason reason,
                       ReworkLevel level, LabEvent labEvent, String comments) {
        this.reworkedSamples = reworkedSamples;
        this.reason = reason;
        this.reworkLevel = level;
        this.labEvent = labEvent;
        this.comments = comments;
    }

    @PrePersist
    private void findLabEvent(){
        rapSheet =new RapSheet();
    }

    public List<MercurySample> getReworkedSamples() {
        return reworkedSamples;
    }

    public void setReworkedSamples(List<MercurySample> reworkedSamples) {
        this.reworkedSamples = reworkedSamples;
    }

    public ReworkReason getReason() {
        return reason;
    }

    public void setReason(ReworkReason reason) {
        this.reason = reason;
    }

    public ReworkLevel getReworkLevel() {
        return reworkLevel;
    }

    public void setReworkLevel(ReworkLevel reworkLevel) {
        this.reworkLevel = reworkLevel;
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }

    public void setLabEvent(LabEvent labEvent) {
        this.labEvent = labEvent;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public RapSheet getRapSheet() {
        return rapSheet;
    }

    public void setRapSheet(RapSheet rapSheet) {
        this.rapSheet = rapSheet;
    }


    public static enum ReworkLevel {
        ONE_SAMPLE_HOLD_REST_BATCH("Type 1", "Rework one sample and hold up the rest of the batch."),
        ONE_SAMPLE_RELEASE_REST_BATCH("Type 2", "Rework one sample let the rest of the batch proceed "),
        ENTIRE_BATCH("Type 3", "Rework all samples in the batch.");

        private String value;
        private String description;

        private ReworkLevel(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }
    }
    public static enum ReworkReason {
        MACHINE_ERROR("Machine Error");

        private String value;

        private ReworkReason(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
