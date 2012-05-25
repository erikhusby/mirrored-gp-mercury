package org.broadinstitute.pmbridge.entity.experiments.gap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.pmbridge.entity.common.EntityUtils;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.experiments.AbstractExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.experiments.RemoteId;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;
import org.broadinstitute.pmbridge.infrastructure.gap.ExperimentPlan;
import org.broadinstitute.pmbridge.infrastructure.gap.Product;
import org.broadinstitute.pmbridge.infrastructure.quote.*;

import java.util.*;
import java.util.regex.Pattern;

/**
 *  Under Construction !!!!
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 5:02 PM
 */
public class GapExperimentRequest extends AbstractExperimentRequest {

    private Log logger = LogFactory.getLog(GapExperimentRequest.class);
    //TODO hmc May have to hide the experimentPlanDTO within and not expose it to clients GapExperimentRequest except for the constructors.
    private ExperimentPlan experimentPlanDTO;
    private Quote bspQuote;
    private Quote gapQuote;
    private Product technologyProduct;

    public GapExperimentRequest(ExperimentRequestSummary experimentRequestSummary) {
        super(experimentRequestSummary);
        setExperimentPlanDTO(new ExperimentPlan());
        getExperimentPlanDTO().setExperimentName(experimentRequestSummary.getTitle().name);
        getExperimentPlanDTO().setPlanningStatus( experimentRequestSummary.getStatus().name );
        //TODO hmc does pmb need to set the start  date ?
//        getExperimentPlanDTO().setProjectStartDate(new Date());
        getExperimentPlanDTO().setResearchProjectId( "" + experimentRequestSummary.getResearchProjectId() );
        getExperimentPlanDTO().setProgramPm( experimentRequestSummary.getCreation().person.getUsername());

    }

    public GapExperimentRequest(ExperimentRequestSummary experimentRequestSummary, ExperimentPlan experimentPlan ) {
        super(experimentRequestSummary);
        setExperimentPlanDTO(experimentPlan);
        if (StringUtils.isNotBlank( experimentPlan.getPlatformPm()) ) {
            Set<Person>  platformPeople = EntityUtils.extractPeopleFromUsernameList(experimentPlan.getPlatformPm(), RoleType.PLATFORM_PM);
            this.setPlatformProjectManagers(platformPeople);
        }
        if (StringUtils.isNotBlank( experimentPlan.getProgramPm()) ) {
            Set<Person> programPeople = EntityUtils.extractPeopleFromUsernameList(experimentPlan.getProgramPm(), RoleType.PROGRAM_PM);
            this.setProgramProjectManagers(programPeople);
        }
        if ((experimentRequestSummary.getRemoteId() == null ) && (experimentPlan.getId() != null) ) {
            experimentRequestSummary.setRemoteId( new RemoteId(experimentPlan.getId()) );
        }
    }


    public static GapExperimentRequest populateResearchProjectFields ( final GapExperimentRequest gapExperimentRequest, final ResearchProject researchProject ) {

        if ( (researchProject != null)  && (gapExperimentRequest != null) ) {
            //Set irb number and info ron the gap experiment.
            if ( researchProject.getIrbNumbers() != null )  {
                String irbNumbersStr = EntityUtils.flattenSetOfStrings(researchProject.getIrbNumbers());
                gapExperimentRequest.getExperimentPlanDTO().setIrbNumbers(irbNumbersStr);
            }
            gapExperimentRequest.getExperimentPlanDTO().setIrbInfo( researchProject.getIrbNotes() );

            // Set the programPM
            String programPMStr = EntityUtils.flattenSetOfPersonUsernames(gapExperimentRequest.getProgramProjectManagers());
            gapExperimentRequest.getExperimentPlanDTO().setProgramPm( programPMStr );

            //TODO hmc - IRB Engaged needs to be set somehow during this workflow.

        }

        return gapExperimentRequest;
    }

    public Name getExperimentStatus() {
        return getExperimentRequestSummary().getStatus();
    }

    public ExperimentPlan getExperimentPlanDTO() {



        return experimentPlanDTO;
    }

    private void setExperimentPlanDTO(final ExperimentPlan experimentPlanDTO) {
        this.experimentPlanDTO = experimentPlanDTO;
    }

    public String getGapGroupName() {
        return getExperimentPlanDTO().getGroupName();
    }

    public void setGapGroupName(final String gapGroupName) {
        getExperimentPlanDTO().setGroupName(gapGroupName);
    }

    public String getGapProjectName() {
        return  getExperimentPlanDTO().getProjectName();
    }
    public void setGapProjectName(final String gapProjectName) {
        getExperimentPlanDTO().setProjectName(gapProjectName);
    }

    public Quote getBspQuote() {
        return bspQuote;
    }
    public void setBspQuote(final Quote quote) {
        this.bspQuote = quote;
        if ( quote != null ) {
            getExperimentPlanDTO().setBspQuoteId( quote.getId() );
        }
    }

    public Quote getGapQuote() {
        return gapQuote;
    }
    public void setGapQuote(final Quote quote) {
        this.gapQuote = quote;
        if ( quote != null ) {
            getExperimentPlanDTO().setGapQuoteId(quote.getId());
        }
    }

    public Product getTechnologyProduct() {
        return technologyProduct;
    }
    public void setTechnologyProduct(final Product technologyProduct) {
        this.technologyProduct = technologyProduct;
        if ( technologyProduct != null ) {
            boolean isNumeric = Pattern.matches("[\\d]+", technologyProduct.getId());
            if ( isNumeric ) {
                int id = Integer.parseInt( technologyProduct.getId() );
                getExperimentPlanDTO().setProductId(id);
            }
        }
    }

    public String getSynopsis() {
        return getExperimentPlanDTO().getExperimentDescription();
    }

    public void setSynopsis(final String synopsis) {
        getExperimentPlanDTO().setExperimentDescription( synopsis );
    }

    @Override
    public void setTitle(final Name title) {
        experimentPlanDTO.setExperimentName(title.name);
        getExperimentRequestSummary().setTitle( title );
    }

    public Date getExpectedKitReceiptDate() {
        return getExperimentPlanDTO().getExpectedKitReceiptDate();
    }

    public void setExpectedKitReceiptDate(final Date expectedKitReceiptDate) {
        getExperimentPlanDTO().setExpectedKitReceiptDate( expectedKitReceiptDate);
    }

    @Override
    public ExperimentRequest cloneRequest() {
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
        if ( researchProject != null ) {
            // Add this experiment to the list referred to by the research Project
            researchProject.addExperimentRequest(this);

            // Update the RP id that is associated with the research project.
            getExperimentPlanDTO().setResearchProjectId("" + researchProject.getId().longValue() );

            //Set irb number and info ron the gap experiment.
            if ( researchProject.getIrbNumbers() != null )  {
                String irbNumbersStr = EntityUtils.flattenSetOfStrings(researchProject.getIrbNumbers());
                this.getExperimentPlanDTO().setIrbNumbers(irbNumbersStr);
            }
            this.getExperimentPlanDTO().setIrbInfo( researchProject.getIrbNotes() );

            // Set the programPM
            String programPMStr = EntityUtils.flattenSetOfPersonUsernames(this.getProgramProjectManagers());
            this.getExperimentPlanDTO().setProgramPm( programPMStr );

            //TODO hmc - IRB Engaged needs to be set somehow during this workflow.

        }
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
