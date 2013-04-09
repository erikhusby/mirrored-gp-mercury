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
import javax.validation.constraints.NotNull;

@Entity
@Audited
@Table(schema = "mercury")
public abstract class RapSheetEntry {
    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_RAP_SHEET_ENTRY", schema = "mercury", sequenceName = "SEQ_RAP_SHEET_ENTRY")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RAP_SHEET_ENTRY")
    private Long rapSheetEntryId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    private RapSheet rapSheet;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private LabVesselComment labVesselComment;

    // this should not cause n+1 select performance issue if it is LAZY and mandatory
    @OneToOne(optional = false, fetch = FetchType.LAZY,cascade = {CascadeType.ALL})
    @NotNull
    private LabVesselPosition labVesselPosition;

    public RapSheetEntry() {
    }

    public RapSheetEntry(LabVesselPosition labVesselPosition, LabVesselComment labVesselComment) {
        this.labVesselPosition = labVesselPosition;
        this.labVesselComment=labVesselComment;
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
}
