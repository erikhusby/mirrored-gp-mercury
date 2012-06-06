package org.broadinstitute.pmbridge.entity.experiments.gap;

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
import org.broadinstitute.pmbridge.infrastructure.quote.Quote;

import java.util.Date;
import java.util.Set;
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
    private ExperimentPlan experimentPlanDTO;
    private Quote bspQuote;
    private Quote gapQuote;
    private Product technologyProduct;

    public GapExperimentRequest(ExperimentRequestSummary experimentRequestSummary) {
        super(experimentRequestSummary);
        setExperimentPlanDTO(new ExperimentPlan());
        getExperimentPlanDTO().setExperimentName(experimentRequestSummary.getTitle().name);
        getExperimentPlanDTO().setPlanningStatus( experimentRequestSummary.getStatus().name );
         //TODO This dates need to be formatted such that GAp an handle it.
//        getExperimentPlanDTO().setProjectStartDate(new Date());
        getExperimentPlanDTO().setResearchProjectId( "" + experimentRequestSummary.getResearchProjectId() );
        getExperimentPlanDTO().setProgramPm( experimentRequestSummary.getCreation().person.getUsername());

    }

    public GapExperimentRequest(ExperimentRequestSummary experimentRequestSummary, ExperimentPlan experimentPlan ) {
        super(experimentRequestSummary);
        setExperimentPlanDTO(experimentPlan);
        // if the remoteId is not yet set on the Summary ( in the case of initial submission ) then set it.
        if ((experimentRequestSummary.getRemoteId() == null ) && (experimentPlan.getId() != null) ) {
            experimentRequestSummary.setRemoteId( new RemoteId(experimentPlan.getId()) );
        }
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

    @Override
    public Set<Person> getPlatformProjectManagers() {

        Set<Person>  platformPeople = EntityUtils.extractPeopleFromUsernameList(getExperimentPlanDTO().getPlatformPm(),
                    RoleType.PLATFORM_PM);
            return platformPeople;
    }

    @Override
    public Set<Person> getProgramProjectManagers() {
            Set<Person> programPeople = EntityUtils.extractPeopleFromUsernameList(getExperimentPlanDTO().getProgramPm(),
                    RoleType.PROGRAM_PM);
        return programPeople;
    }


    @Override
    public void setProgramProjectManagers(final Set<Person> programProjectManagers) {
        String programPmList = "";
        if ( programProjectManagers != null ) {
            programPmList = EntityUtils.flattenSetOfPersonUsernames( programProjectManagers );
        }
        getExperimentPlanDTO().setProgramPm( programPmList );
    }

    public String getSynopsis() {
        return getExperimentPlanDTO().getExperimentDescription();
    }

    public void setSynopsis(final String synopsis) {
        getExperimentPlanDTO().setExperimentDescription( synopsis );
    }

    @Override
    public void setTitle(final Name title) {
        getExperimentPlanDTO().setExperimentName(title.name);
        getExperimentRequestSummary().setTitle( title );
    }


    public String getVersion() {
        String versionStr = "";
        if ( getExperimentPlanDTO().getEffectiveDate() != null ) {
            versionStr = formatDate(getExperimentPlanDTO().getEffectiveDate());
        }
        return versionStr;
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

            //Set irb number and info on the gap experiment.
            if ( researchProject.getIrbNumbers() != null )  {
                String irbNumbersStr = EntityUtils.flattenSetOfStrings(researchProject.getIrbNumbers());
                this.getExperimentPlanDTO().setIrbNumbers(irbNumbersStr);
                this.getExperimentPlanDTO().setIrbEngaged(true);
            } else {
                this.getExperimentPlanDTO().setIrbEngaged(false);
            }
            this.getExperimentPlanDTO().setIrbInfo( researchProject.getIrbNotes() );

            // Set the programPM
            String programPMStr = EntityUtils.flattenSetOfPersonUsernames(this.getProgramProjectManagers());
            this.getExperimentPlanDTO().setProgramPm( programPMStr );

        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof GapExperimentRequest)) return false;
        if (!super.equals(o)) return false;

        final GapExperimentRequest that = (GapExperimentRequest) o;

        if (bspQuote != null ? !bspQuote.equals(that.bspQuote) : that.bspQuote != null) return false;
        if (experimentPlanDTO != null ? !experimentPlanDTO.equals(that.experimentPlanDTO) : that.experimentPlanDTO != null)
            return false;
        if (gapQuote != null ? !gapQuote.equals(that.gapQuote) : that.gapQuote != null) return false;
        if (technologyProduct != null ? !technologyProduct.equals(that.technologyProduct) : that.technologyProduct != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (experimentPlanDTO != null ? experimentPlanDTO.hashCode() : 0);
        result = 31 * result + (bspQuote != null ? bspQuote.hashCode() : 0);
        result = 31 * result + (gapQuote != null ? gapQuote.hashCode() : 0);
        result = 31 * result + (technologyProduct != null ? technologyProduct.hashCode() : 0);
        return result;
    }

    private static String formatDate(Date d) {
        if ( d != null ) {
            return (1900+d.getYear())+"-"+pad(d.getMonth()+1,2)+"-"+pad(d.getDate(),2);
        } else {
            return "";
        }
    }
    private static String pad(int n, int nDigits) {
        StringBuilder sb = new StringBuilder();
        for( int i=(int)Math.log10(n)+1; i<nDigits; i++ ) {
            sb.append("0");
        }
        sb.append(n);
        return sb.toString();
    }
}
