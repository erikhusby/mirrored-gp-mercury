package org.broadinstitute.gpinformatics.athena.entity.experiments;

import org.broadinstitute.gpinformatics.athena.entity.bsp.BSPSample;
import org.broadinstitute.gpinformatics.athena.entity.common.Name;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract base class for experiment request
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 5:05 PM
 */
public abstract class AbstractExperimentRequest implements ExperimentRequest {


    private final ExperimentRequestSummary experimentRequestSummary;
    //    private Set<Person> platformProjectManagers;
    private Set<BSPSample> samples;


    private ExperimentType experimentType;

    protected AbstractExperimentRequest(ExperimentRequestSummary experimentRequestSummary, ExperimentType experimentType) {
        this.experimentRequestSummary = experimentRequestSummary;
//        this.platformProjectManagers = new HashSet<Person>();
        this.samples = new HashSet<BSPSample>();
        this.experimentType = experimentType;
    }

//    protected AbstractExperimentRequest(ExperimentRequestSummary experimentRequestSummary,
//                                        Set<Person> platformProjectManagers,
//                                        Set<BSPSample> samples) {
//        this.experimentRequestSummary = experimentRequestSummary;
//        this.platformProjectManagers = platformProjectManagers;
//        this.samples = samples;
//    }

    //Getters
    public ExperimentRequestSummary getExperimentRequestSummary() {
        return experimentRequestSummary;
    }

    public abstract Set<Person> getPlatformProjectManagers();

    public abstract Set<Person> getProgramProjectManagers();

    public abstract List<BSPSample> getSamples();

    public Name getTitle() {
        return getExperimentRequestSummary().getTitle();
    }

    @Override
    public ExperimentType getExperimentType() {
        return experimentType;
    }

    public abstract void setTitle(final Name title);


    public ExperimentId getRemoteId() {
        return this.experimentRequestSummary.getExperimentId();
    }

    public void setRemoteId(ExperimentId remoteId) {
        this.experimentRequestSummary.setExperimentId(remoteId);
    }

    public Name getExperimentStatus() {
        return this.experimentRequestSummary.getStatus();
    }

    public void setSamples(final Set<BSPSample> samples) {
        this.samples = samples;
    }

    public abstract void setProgramProjectManagers(final Set<Person> programProjectManagers);

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractExperimentRequest)) return false;

        final AbstractExperimentRequest that = (AbstractExperimentRequest) o;

        if (!experimentRequestSummary.equals(that.experimentRequestSummary)) return false;
        if (!samples.equals(that.samples)) return false;
        if (!experimentType.equals(that.experimentType)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = experimentRequestSummary.hashCode();
        result = 31 * result + samples.hashCode();
        result = 31 * result + experimentType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "experimentRequestSummary=" + ((experimentRequestSummary == null) ? "null" : experimentRequestSummary.getTitle()) +
                ", samples=" + ((samples == null) ? "null" : samples.toString()) +
                ", experimentType=" + ((experimentType == null) ? "null" : experimentType.name());
    }
}
