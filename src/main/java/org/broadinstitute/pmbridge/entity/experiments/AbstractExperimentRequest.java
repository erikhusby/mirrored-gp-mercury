package org.broadinstitute.pmbridge.entity.experiments;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.broadinstitute.pmbridge.entity.bsp.BSPSample;
import org.broadinstitute.pmbridge.entity.person.Person;

import java.util.Collection;

/**
 * Abstract base class for experiment request
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 5:05 PM
 */
public abstract class AbstractExperimentRequest implements ExperimentRequest {


    private final ExperimentRequestSummary experimentRequestSummary;
    private Collection<Person> platformProjectManagers;
    private Collection<Person> programProjectManagers; // Also defined at the RP level this data would/could override/supplement the values from the RP level
    private Collection<BSPSample> samples;

    protected AbstractExperimentRequest(ExperimentRequestSummary experimentRequestSummary) {
        this.experimentRequestSummary = experimentRequestSummary;
    }

    protected AbstractExperimentRequest(ExperimentRequestSummary experimentRequestSummary,
                                        Collection<Person> platformProjectManagers,
                                        Collection<Person> programProjectManagers,
                                        Collection<BSPSample> samples) {
        this.experimentRequestSummary = experimentRequestSummary;
        this.platformProjectManagers = platformProjectManagers;
        this.programProjectManagers = programProjectManagers;
        this.samples = samples;
    }

    //Getters
    public ExperimentRequestSummary getExperimentRequestSummary() {
        return experimentRequestSummary;
    }

    public Collection<Person> getPlatformProjectManagers() {
        return platformProjectManagers;
    }

    public Collection<Person> getProgramProjectManagers() {
        return programProjectManagers;
    }

    public Collection<BSPSample> getSamples() {
        return samples;
    }

    public LocalId getLocalId() {
        return this.experimentRequestSummary.getLocalId();
    }

    public RemoteId getRemoteId() {
        return this.experimentRequestSummary.getRemoteId();
    }

    public void setRemoteId(RemoteId remoteId) {
        this.experimentRequestSummary.setRemoteId( remoteId );
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
     }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }


}
