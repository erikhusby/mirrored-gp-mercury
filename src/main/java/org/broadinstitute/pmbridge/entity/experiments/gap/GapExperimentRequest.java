package org.broadinstitute.pmbridge.entity.experiments.gap;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.broadinstitute.pmbridge.entity.bsp.BSPSample;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.common.QuoteId;
import org.broadinstitute.pmbridge.entity.experiments.AbstractExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentId;
import org.broadinstitute.pmbridge.entity.experiments.PlatformId;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.infrastructure.gap.ExperimentPlan;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 5:02 PM
 */
public class GapExperimentRequest extends AbstractExperimentRequest {

    private QuoteId bspQuoteId;
    private QuoteId gapQuoteId;
    private Collection<Person> analysts;

    private ExperimentPlan experimentPlan;

    public GapExperimentRequest(Person creator, ExperimentId id, Name title, PlatformId platformId,
                                Collection<Person> platformProjectManagers, Collection<Person> programProjectManagers,
                                Collection<BSPSample> samples, QuoteId bspQuoteId, QuoteId gapQuoteId) {
        super(creator, id, title, platformId, platformProjectManagers, programProjectManagers, samples);
        this.bspQuoteId = bspQuoteId;
        this.gapQuoteId = gapQuoteId;
    }

    public ExperimentPlan getExperimentPlan() {
        return experimentPlan;
    }

    public void setExperimentPlan(ExperimentPlan experimentPlan) {
        this.experimentPlan = experimentPlan;
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
