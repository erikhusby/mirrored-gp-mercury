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
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
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
    @OneToMany(mappedBy = "rapSheet", cascade = CascadeType.ALL, fetch = FetchType.LAZY )
    private List<MercurySample> samples=new ArrayList<MercurySample>();

    @NotNull
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY,mappedBy = "rapSheet")
    private List<RapSheetEntry> rapSheets = new ArrayList<RapSheetEntry>();

    public RapSheet() {
    }
    public RapSheet(MercurySample sample) {
        setSample(sample);
    }

    public void addRework(ReworkEntry... entries) {
        for (ReworkEntry reworkEntry : entries) {
            rapSheets.add(reworkEntry);
            reworkEntry.setRapSheet(this);
        }
    }

    @Transient
    /**
     * Sort the reworkEntries
     */
    private boolean reworkEntriesSorted =false;

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

    public List<RapSheetEntry> getRapSheets() {
        if (!reworkEntriesSorted){
            Collections.sort(rapSheets);
            reworkEntriesSorted =true;
        }
        return rapSheets;
    }

    public void setRapSheets(List<RapSheetEntry> reworkEntries) {
        Collections.sort(reworkEntries);
        this.rapSheets = reworkEntries;
    }

    public void startRework() {
        getCurrentReworkEntry().setActiveRework(true);
    }


    public ReworkEntry getCurrentReworkEntry(){
        for (RapSheetEntry rapSheetEntry : getRapSheets()) {
            if (rapSheetEntry instanceof ReworkEntry) {
                return (ReworkEntry) rapSheetEntry;
            }
        }

        return null;
    }

    /**
     * Get the active rework.
     * @return Active Rework or null;
     */
    public ReworkEntry getActiveRework() {
        for (RapSheetEntry rapSheetEntry : getRapSheets()) {
            if (rapSheetEntry instanceof ReworkEntry) {
                if (((ReworkEntry)rapSheetEntry).isActiveRework()) {
                    return (ReworkEntry) rapSheetEntry;
                }
            }
        }
        return null;
    }

    /**
     * Get all the active rework and make it inactive.
     * If there is no ReworkEntries, it will do nothing.
     */
    public void stopRework() {
        if (getActiveRework() != null) {
            getActiveRework().setActiveRework(false);
        }
    }
}
