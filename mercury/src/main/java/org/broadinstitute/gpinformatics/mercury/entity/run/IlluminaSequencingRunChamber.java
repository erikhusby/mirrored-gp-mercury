package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds lane level information about a run.
 */
@Entity
@Audited
public class IlluminaSequencingRunChamber extends SequencingRunChamber {

    @ManyToOne
    @JoinColumn(name = "ILLUMINA_SEQUENCING_RUN")
    private IlluminaSequencingRun illuminaSequencingRun;

    @ManyToMany(cascade = CascadeType.PERSIST, mappedBy = "sequencingRunChambers")
    private Set<State> states = new HashSet<>();

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

    public Set<State> getStates() {
        return states;
    }
}
