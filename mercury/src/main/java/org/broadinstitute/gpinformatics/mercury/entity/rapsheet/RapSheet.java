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

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Basically, a RapSheet is a log for a sample. Any sample can have a RapSheet and for each event you are logging
 * you add a new entry. The possible entries are currently either a RapSheetEntry or a ReworkEntry. Both types
 * keep track the usual things such as date/time as well as the location of the sample in a tube/plate/rack/flowcell
 * or whatever it is in, and what event brought you there.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class RapSheet {
    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_RAP_SHEET", schema = "mercury", sequenceName = "SEQ_RAP_SHEET")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RAP_SHEET")
    private Long rapSheetId;

    @Column(nullable = false)
    @OneToMany(mappedBy = "rapSheet", cascade = CascadeType.ALL )
    private List<MercurySample> samples=new ArrayList<MercurySample>();

    @Column(nullable = false)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "rapSheet")
    private List<RapSheetEntry> rapSheetEntries = new ArrayList<RapSheetEntry>();

    public RapSheet() {
    }
    public RapSheet(MercurySample sample) {
        setSample(sample);
    }

    public void addRework(ReworkEntry... entries) {
        for (ReworkEntry reworkEntry : entries) {
            rapSheetEntries.add(reworkEntry);
            reworkEntry.setRapSheet(this);
        }
    }

    public void setSample(MercurySample sample) {
        samples.clear();
        samples.add(sample);
    }

    public void setSamples(List<MercurySample> samples) {
        if (samples.size() > 1) {
            throw new IllegalStateException("Only one sample allowed here.");
        }
        this.samples = samples;
    }

    public MercurySample getSample() {
        return getSamples().iterator().next();
    }

    private List<MercurySample> getSamples() {
        return samples;
    }

    public List<RapSheetEntry> getRapSheetEntries() {
        return rapSheetEntries;
    }

    public void setRapSheetEntries(List<RapSheetEntry> reworkEntries) {
        Collections.sort(reworkEntries, RapSheetEntry.BY_DATE_ASC);
        this.rapSheetEntries.clear();
        this.rapSheetEntries.addAll(reworkEntries);
    }

    public ReworkEntry getCurrentReworkEntry(){
        for (ReworkEntry rapSheetEntry : getReworkEntries()) {
            return rapSheetEntry;
        }
        return null;
    }

    /**
     * Get the active rework.
     * Active Rework is Rework which has not been put in a bucket.
     * Once it is added to a bucket, it becomes inactive.
     * @return Active Rework or null;
     */
    public ReworkEntry getActiveRework() {
        for (ReworkEntry rapSheetEntry : getReworkEntries()) {
            if (rapSheetEntry.isActiveRework()) {
                return rapSheetEntry;
            }
        }
        return null;
    }

    public Collection<ReworkEntry> getReworkEntries() {
        Collection<ReworkEntry> entries = new ArrayList<ReworkEntry>();
        for (RapSheetEntry rapSheetEntry : getRapSheetEntries()) {
            if (rapSheetEntry instanceof ReworkEntry) {
                entries.add((ReworkEntry) rapSheetEntry);
            }
        }
        return entries;
    }


    public void activateRework() {
        getCurrentReworkEntry().setActiveRework(true);
    }


    /**
     * Get all the active rework and make it inactive.
     *
     * Active Rework is Rework which has not been put in a bucket.
     * Once it is added to a bucket, it becomes inactive.
     *
     * If there is no ReworkEntries, it will do nothing.
     */
    public void deactivateRework() {
        if (getActiveRework() != null) {
            getActiveRework().setActiveRework(false);
        }
    }
}
