package org.broadinstitute.pmbridge.entity.experiments;

import org.broadinstitute.pmbridge.entity.bsp.BSPSample;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;

import java.util.Collection;

/**
 * Interface for experiment requests in PMBridge outlining
 * some of the expected functionality that the concrete class
 * are expected to support.
 * <p/>
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 5:05 PM
 */
public interface ExperimentRequest {


    ExperimentRequestSummary getExperimentRequestSummary();

    Collection<Person> getPlatformProjectManagers();

    Collection<Person> getProgramProjectManagers();

    Collection<BSPSample> getSamples();

    ExperimentId getRemoteId();

    void setRemoteId(ExperimentId remoteId);

    /**
     * Method to save a local copy of the experiment request
     * without officially submitting it to the platform.
     *
     * @return
     */
    ExperimentRequest cloneRequest();

    /**
     * Method to generate a excel copy of the experiment request
     *
     * @return
     */
    ExperimentRequest exportToExcel();

    /**
     * Method to return the Status of the experiment request.
     *
     * @return
     */
    Name getExperimentStatus();

    /**
     * Method to associated the experiment request
     * with a research project.
     *
     * @param researchProject
     */
    void associateWithResearchProject(ResearchProject researchProject);


    /**
     * Indication of the Type of the Experiment
     *
     * @return
     */
    ExperimentType getExperimentType();
}
