package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Holds lane level information about a run.
 */
@Entity
@Audited
public class IlluminaSequencingRunChamber extends SequencingRunChamber {

    @ManyToOne
    private IlluminaSequencingRun illuminaSequencingRun;
    private int laneNumber;
    private String actualReadStructure;

    public IlluminaSequencingRunChamber(
            IlluminaSequencingRun illuminaSequencingRun, int laneNumber) {
        this.illuminaSequencingRun = illuminaSequencingRun;
        this.laneNumber = laneNumber;
    }

    /**
     * For JPA.
     */
    protected IlluminaSequencingRunChamber() {
    }

    public IlluminaSequencingRun getIlluminaSequencingRun() {
        return illuminaSequencingRun;
    }

    public int getLaneNumber() {
        return laneNumber;
    }

    public String getActualReadStructure() {
        return actualReadStructure;
    }

    public void setActualReadStructure(String actualReadStructure) {
        this.actualReadStructure = actualReadStructure;
    }
}
