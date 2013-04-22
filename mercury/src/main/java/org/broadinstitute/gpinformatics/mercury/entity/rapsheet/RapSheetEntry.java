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

package org.broadinstitute.gpinformatics.mercury.entity.rapsheet;

import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Comparator;

/**
 * A RapSheetEntry is basically a log entry for a sample. A Sample can have a RapSheet
 * and each event you are logging gets a new RapSheetEntry.
 */
@Entity
@Audited
@Table(schema = "mercury")
public abstract class RapSheetEntry  {
    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_RAP_SHEET_ENTRY", schema = "mercury", sequenceName = "SEQ_RAP_SHEET_ENTRY")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RAP_SHEET_ENTRY")
    private Long rapSheetEntryId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    private RapSheet rapSheet;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(updatable = true, insertable = true, nullable = false)
    private LabVesselComment labVesselComment;

    // this should not cause n+1 select performance issue if it is LAZY and mandatory
    @OneToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    private LabVesselPosition labVesselPosition;

    public RapSheetEntry() {
    }

    public RapSheetEntry(LabVesselPosition labVesselPosition, LabVesselComment labVesselComment) {
        this.labVesselPosition = labVesselPosition;
        this.labVesselComment = labVesselComment;
        labVesselComment.getRapSheetEntries().add(this);
    }

    public RapSheet getRapSheet() {
        return rapSheet;
    }

    public void setRapSheet(RapSheet rapSheet) {
        this.rapSheet = rapSheet;
    }

    public LabVesselComment getLabVesselComment() {
        return labVesselComment;
    }

    public void setLabVesselComment(LabVesselComment labVesselComment) {
        this.labVesselComment = labVesselComment;
    }

    public LabVesselPosition getLabVesselPosition() {
        return labVesselPosition;
    }

    public void setLabVesselPosition(LabVesselPosition labVesselPosition) {
        this.labVesselPosition = labVesselPosition;
    }

    public static final Comparator<RapSheetEntry> BY_DATE_ASC = new Comparator<RapSheetEntry>() {
        @Override
        public int compare(RapSheetEntry first, RapSheetEntry second) {
            return first.getLabVesselComment().getLogDate().compareTo(second.getLabVesselComment().getLogDate());
        }
    };
}
