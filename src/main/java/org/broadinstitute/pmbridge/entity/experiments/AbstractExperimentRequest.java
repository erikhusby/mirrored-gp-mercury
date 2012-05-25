package org.broadinstitute.pmbridge.entity.experiments;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.broadinstitute.pmbridge.entity.bsp.BSPSample;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.person.Person;

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
    private Set<Person> platformProjectManagers;
    private Set<Person> programProjectManagers; // Also defined at the RP level this data would/could override/supplement the values from the RP level
    private Set<BSPSample> samples;

    protected AbstractExperimentRequest(ExperimentRequestSummary experimentRequestSummary) {
        this.experimentRequestSummary = experimentRequestSummary;
    }

//    protected AbstractExperimentRequest(ExperimentRequestSummary experimentRequestSummary,
//                                        Set<Person> platformProjectManagers,
//                                        Set<Person> programProjectManagers,
//                                        Set<BSPSample> samples) {
//        this.experimentRequestSummary = experimentRequestSummary;
//        this.platformProjectManagers = platformProjectManagers;
//        this.programProjectManagers = programProjectManagers;
//        this.samples = samples;
//    }

    //Getters
    public ExperimentRequestSummary getExperimentRequestSummary() {
        return experimentRequestSummary;
    }

    public Set<Person> getPlatformProjectManagers() {
        return platformProjectManagers;
    }

    public Set<Person> getProgramProjectManagers() {
        return programProjectManagers;
    }

    public Set<BSPSample> getSamples() {
        return samples;
    }

    public Name getTitle() {
        return getExperimentRequestSummary().getTitle();
    }

    public abstract void setTitle(final Name title ) ;

    public LocalId getLocalId() {
        return this.experimentRequestSummary.getLocalId();
    }

    public RemoteId getRemoteId() {
        return this.experimentRequestSummary.getRemoteId();
    }

    public void setRemoteId(RemoteId remoteId) {
        this.experimentRequestSummary.setRemoteId( remoteId );
    }

    public Name getExperimentStatus() {
        return this.experimentRequestSummary.getStatus();
    }

    public void setSamples(final Set<BSPSample> samples) {
        this.samples = samples;
    }

    public void setPlatformProjectManagers(final Set<Person> platformProjectManagers) {
        this.platformProjectManagers = platformProjectManagers;
    }

    public void setProgramProjectManagers(final Set<Person> programProjectManagers) {
        this.programProjectManagers = programProjectManagers;
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
