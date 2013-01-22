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

package org.broadinstitute.gpinformatics.mercury.entity.rework;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Entity
@Audited
@Table(schema = "mercury")
public class LabVesselComment {
    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_LV_COMMENT", schema = "mercury", sequenceName = "SEQ_LV_COMMENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LV_COMMENT")
    private Long labVesselCommentId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @NotNull
    protected LabEvent labEvent;

    private String comment;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private LabVessel labVessel;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RapSheetEntry> rapSheetEntries;

    @Temporal(TemporalType.DATE)
    private Date logDate;

    public LabVesselComment() {
    }

    public LabVesselComment(LabEvent labEvent, LabVessel labVessel, String comment, List<RapSheetEntry> rapSheetEntries) {
        this.labEvent = labEvent;
        this.comment = comment;
        this.labVessel = labVessel;
        this.rapSheetEntries = rapSheetEntries;
    }

        @PrePersist
    private void prePersist() {
        logDate = new Date();
    }

    public Date getLogDate() {
        return logDate;
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }

    public void setLabEvent(LabEvent labEvent) {
        this.labEvent = labEvent;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LabVessel getLabVessel() {
        return labVessel;
    }

    public void setLabVessel(LabVessel labVessel) {
        this.labVessel = labVessel;
    }

    public List<RapSheetEntry> getRapSheetEntries() {
        return rapSheetEntries;
    }

    public void setRapSheetEntries(List<RapSheetEntry> rapSheetEntries) {
        this.rapSheetEntries = rapSheetEntries;
    }
}
