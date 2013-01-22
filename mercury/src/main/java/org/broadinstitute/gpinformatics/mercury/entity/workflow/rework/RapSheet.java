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

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Audited
@Table(schema = "mercury")
public class RapSheet {
    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_SAMPLE_RAP_SHEET", schema = "mercury", sequenceName = "SEQ_SAMPLE_RAP_SHEET")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAMPLE_RAP_SHEET")
    private Long rapSheetId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @NotNull
    private MercurySample sample;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "rapSheet")
    private List<RapSheetEntry> rapSheetEntries;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "rapSheet")
    private List<ReworkEntry> reworkEntries;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<LabVesselComment> labVesselComment;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVesselPosition labVesselPosition;

    public RapSheet() {
    }

    public RapSheet(RapSheetEntry... rapSheetEntry) {
        rapSheetEntries = new ArrayList<RapSheetEntry>(rapSheetEntry.length);
        Collections.addAll(rapSheetEntries, rapSheetEntry);
    }

    public RapSheet(MercurySample sample) {
        this.sample = sample;
    }

    public void addEntry(RapSheetEntry rapSheetEntry) {
        getRapSheetEntries().add(rapSheetEntry);
        rapSheetEntry.setRapSheet(this);
    }

    public void addRework(ReworkReason reworkReason, ReworkLevel reworkLevel, LabEventType reworkStep) {
        final RapSheetEntry rapSheetEntry = new ReworkEntry(reworkReason, reworkLevel, reworkStep);
        getRapSheetEntries().add(rapSheetEntry);
        rapSheetEntry.setRapSheet(this);
    }

    public MercurySample getSample() {
        return sample;
    }

    public void setSample(MercurySample sample) {
        this.sample = sample;
    }

    public List<RapSheetEntry> getRapSheetEntries() {
        if (rapSheetEntries == null) {
            rapSheetEntries = new ArrayList<RapSheetEntry>();
        }
        return rapSheetEntries;
    }

    public void setRapSheetEntries(List<RapSheetEntry> entries) {
        this.rapSheetEntries = entries;
    }

    public List<LabVesselComment> getLabVesselComment() {
        return labVesselComment;
    }

    public void setLabVesselComment(List<LabVesselComment> labVesselComment) {
        this.labVesselComment = labVesselComment;
    }

    public List<ReworkEntry> getReworkEntries() {
        return reworkEntries;
    }

    public void setReworkEntries(List<ReworkEntry> reworkEntries) {
        this.reworkEntries = reworkEntries;
    }

    public LabVesselPosition getLabVesselPosition() {
        return labVesselPosition;
    }

    public void setLabVesselPosition(LabVesselPosition labVesselPosition) {
        this.labVesselPosition = labVesselPosition;
    }
}
