package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
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
            IlluminaSequencingRun illuminaSequencingRun, VesselPosition vesselPosition) {
        this(illuminaSequencingRun, Integer.parseInt(vesselPosition.name().replace("LANE", "")));
    }

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

    public VesselPosition getLanePosition() {
        return VesselPosition.getByName("LANE" + getLaneNumber());
    }

    public void addState(State state) {
        states.add(state);
        state.addSequencingRunChamber(this);
    }

    public <T extends State> Optional<T> getMostRecentCompleteStateOfType(Class<T> clazz) {
        return getStates().stream()
                .filter(state -> OrmUtil.proxySafeIsInstance(state, clazz) && state.isComplete())
                .map(state -> OrmUtil.proxySafeCast(state, clazz))
                .max(Comparator.comparing(State::getEndTime));
    }

    public <T extends State> Optional<T> getRecentStateWithSample(Class<T> clazz, MercurySample mercurySample) {
        return getStates().stream()
                .filter(state -> OrmUtil.proxySafeIsInstance(state, clazz) && state.isComplete() && state.getMercurySamples().contains(mercurySample))
                .map(state -> OrmUtil.proxySafeCast(state, clazz))
                .max(Comparator.comparing(State::getEndTime));
    }

}
