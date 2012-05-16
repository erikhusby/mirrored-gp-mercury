package org.broadinstitute.pmbridge.entity.experiments.gap;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.experiments.AbstractExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;
import org.broadinstitute.pmbridge.infrastructure.gap.ExperimentPlan;

/**
 *  Under Construction !!!!
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 5:02 PM
 */
public class GapExperimentRequest extends AbstractExperimentRequest {

    private Log logger = LogFactory.getLog(GapExperimentRequest.class);
    private ExperimentPlan experimentPlan;

    public GapExperimentRequest(ExperimentRequestSummary experimentRequestSummary) {
        super(experimentRequestSummary);
    }

    public GapExperimentRequest(ExperimentRequestSummary experimentRequestSummary, ExperimentPlan experimentPlan) {
        super(experimentRequestSummary);
        this.experimentPlan = experimentPlan;
    }

//    public GapExperimentRequest(ExperimentRequestSummary experimentRequestSummary,
//                                Collection<Person> platformProjectManagers, Collection<Person> programProjectManagers,
//                                Collection<BSPSample> samples) {
//        super(experimentRequestSummary, platformProjectManagers, programProjectManagers, samples);
//    }

    public ExperimentPlan getExperimentPlan() {
        return experimentPlan;
    }

    public void setExperimentPlan(ExperimentPlan experimentPlan) {
        this.experimentPlan = experimentPlan;
    }

    public Name getExperimentStatus() {
        Name state = ExperimentRequestSummary.DRAFT_STATUS;

        if (  experimentPlan != null ) {
            state = new Name( experimentPlan.getPlanningStatus() );
        }
        return state;
    }


    @Override
    public ExperimentRequest save() {
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ExperimentRequest exportToExcel() {
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void associateWithResearchProject(final ResearchProject researchProject) {
        //To change body of implemented methods use File | Settings | File Templates.
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
