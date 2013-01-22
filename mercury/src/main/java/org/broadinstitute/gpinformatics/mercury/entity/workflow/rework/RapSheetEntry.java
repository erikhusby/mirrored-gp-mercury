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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Audited
@Table(schema = "mercury")
public abstract class RapSheetEntry {
    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_SAMPLE_RAP_SHEET", schema = "mercury", sequenceName = "SEQ_SAMPLE_RAP_SHEET")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAMPLE_RAP_SHEET")
    private Long rapSheetEntryId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @NotNull
    protected LabEvent labEvent;

    private String comment;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private LabVessel labVessel;

    @Temporal(TemporalType.DATE)
    private Date logDate;

    @PrePersist
    private void prePersist() {
        logDate = new Date();
    }

    @ManyToOne
    @NotNull
    private RapSheet rapSheet;


    public RapSheetEntry() {
    }

    public RapSheetEntry(RapSheet rapSheet, String comment, LabVessel labVessel) {
        this.comment = comment;
        this.rapSheet = rapSheet;
        this.labVessel = labVessel;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getLogDate() {
        return logDate;
    }

    public LabVessel getLabVessel() {
        return labVessel;
    }

    public void setLabVessel(LabVessel labVessel) {
        this.labVessel = labVessel;
    }

    public RapSheet getRapSheet() {
        return rapSheet;
    }

    public void setRapSheet(RapSheet rapSheet) {
        this.rapSheet = rapSheet;
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }

    public void setLabEvent(LabEvent labEvent) {
        this.labEvent = labEvent;
    }
}
