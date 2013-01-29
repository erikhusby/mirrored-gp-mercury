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

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Audited
@Table(schema = "mercury")
public class RapSheet {
    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_RAP_SHEET", schema = "mercury", sequenceName = "SEQ_RAP_SHEET")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RAP_SHEET")
    private Long rapSheetId;

    @NotNull
    @OneToMany(mappedBy = "rapSheet", cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    private List<MercurySample> samples;

    @NotNull
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "rapSheet")
    private List<RapSheetEntry> rapSheetEntries;

    public RapSheet() {
    }

    public RapSheet(RapSheetEntry... rapSheetEntry) {
        rapSheetEntries = new ArrayList<RapSheetEntry>(rapSheetEntry.length);
        Collections.addAll(rapSheetEntries, rapSheetEntry);
    }

    public RapSheet(MercurySample sample) {
        setSample(sample);
    }

    public void addEntry(RapSheetEntry rapSheetEntry) {
        rapSheetEntries = new ArrayList<RapSheetEntry>();
        rapSheetEntries.add(rapSheetEntry);
        rapSheetEntry.setRapSheet(this);
    }

    public void addRework(ReworkReason reworkReason, ReworkLevel reworkLevel, LabEventType reworkStep) {
        final RapSheetEntry rapSheetEntry = new ReworkEntry(reworkReason, reworkLevel, reworkStep);
        getRapSheetEntries().add(rapSheetEntry);
        rapSheetEntry.setRapSheet(this);
    }

    public void setSample(MercurySample sample) {
        this.samples.clear();
        this.samples.add(sample);
    }

    public void setSamples(List<MercurySample> samples) {
        if (samples.size() > 1) {
            throw new IllegalStateException("Only one sample allowed here.");
        }
        this.samples = samples;
    }

    public MercurySample getSample() {
        return samples.get(0);
    }

    private List<MercurySample> getSamples() {
        return samples;
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
}
