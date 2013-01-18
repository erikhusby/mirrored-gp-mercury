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

import clover.retrotranslator.edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.poi.util.ArrayUtil;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.ArrayList;
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

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private MercurySample sample;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "rapSheet")
    private List<RapSheetEntry> rapSheetEntries;

    public RapSheet() {
    }

    public RapSheet(RapSheetEntry ... rapSheetEntry) {
        rapSheetEntries=new ArrayList<RapSheetEntry>(rapSheetEntry.length);
        Collections.addAll(rapSheetEntries, rapSheetEntry);
    }

    public RapSheet(MercurySample sample) {
        this.sample = sample;
    }

    public void addEntry(RapSheetEntry entry){
        if (rapSheetEntries == null){
            rapSheetEntries = new ArrayList<RapSheetEntry>();
        }
        rapSheetEntries.add(entry);
        entry.setRapSheet(this);
    }

    public MercurySample getSample() {
        return sample;
    }

    public void setSample(MercurySample sample) {
        this.sample = sample;
    }

    public List<RapSheetEntry> getRapSheetEntries() {
        return rapSheetEntries;
    }

    public void setRapSheetEntries(List<RapSheetEntry> entries) {
        this.rapSheetEntries = entries;
    }

}
