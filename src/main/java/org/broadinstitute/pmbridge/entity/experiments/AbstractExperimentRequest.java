package org.broadinstitute.pmbridge.entity.experiments;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.broadinstitute.pmbridge.entity.bsp.BSPSample;
import org.broadinstitute.pmbridge.entity.common.ChangeEvent;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.person.Person;

import java.util.Collection;
import java.util.Date;

/**
 * Abstract base class for experiment request
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 5:05 PM
 */
public abstract class AbstractExperimentRequest implements ExperimentRequest {

    public final ExperimentId id;
    public final Name title;
    public final ChangeEvent creation;
    public final PlatformId platformId;
    private Collection<Person> platformProjectManagers;
    private Collection<Person> programProjectManagers; // Also defined at the RP level this data would/could override/supplement the values from the RP level
    private Collection<BSPSample> samples;
    private ChangeEvent modification;

    //TODO hmc should ID be generated ?
    protected AbstractExperimentRequest(Person creator, ExperimentId id, Name title, PlatformId platformId,
                                        Collection<Person> platformProjectManagers,
                                        Collection<Person> programProjectManagers,
                                        Collection<BSPSample> samples) {
        this.id = id;
        this.platformId = platformId;
        this.title = title;
        this.platformProjectManagers = platformProjectManagers;
        this.programProjectManagers = programProjectManagers;
        this.samples = samples;
        this.creation = new ChangeEvent( creator );
        this.modification = new ChangeEvent(new Date(this.creation.date.getTime()), creator);

    }

    //Getters
    public ExperimentId getId() {
        return id;
    }

    public ChangeEvent getCreation() {
        return creation;
    }

    public PlatformId getPlatformId() {
        return platformId;
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

    public ChangeEvent getModification() {
        return modification;
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
