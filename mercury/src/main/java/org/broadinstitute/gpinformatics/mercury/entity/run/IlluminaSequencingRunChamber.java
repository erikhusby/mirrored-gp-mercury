package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
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

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, mappedBy = "runChamber")
    private Set<DemultiplexState> states = new HashSet<>();

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

    public Set<DemultiplexState> getStates() {
        return states;
    }

    public void setStates(Set<DemultiplexState> states) {
        this.states = states;
    }
}
