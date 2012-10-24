package org.broadinstitute.pmbridge.presentation.projects;

import org.apache.commons.logging.Log;
import org.broadinstitute.pmbridge.control.experiments.ExperimentRequestService;
import org.broadinstitute.pmbridge.entity.bsp.BSPSample;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentType;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.infrastructure.UserNotFoundException;
import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.pmbridge.presentation.AbstractJsfBean;
import org.broadinstitute.pmbridge.presentation.summary.ExpReqSummaryList;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.UnselectEvent;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@ManagedBean
@RequestScoped
public class ProjectListBean extends AbstractJsfBean {

    @Inject
    Log log;

    @Inject
    ExperimentRequestService experimentService;

    //TODO Would like to move/wrap this in a PMB boundary/control specific class
    // The summary table bean does not need to know about the BSPSampleSearchService
    // For example the PMBSampleSearchService could supplement the samples that the
    // BSPSampleSearchService with samples from an RP associated BSP Collection.
    @Inject
    private BSPSampleSearchService bspSampleSearchService;

    private ExpReqSummaryList expReqSummaryList = new ExpReqSummaryList();
    private ExperimentRequestSummary selectedExperimentRequestSummary = null;
    private ExperimentRequest selectedExperimentRequest;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy 'at' hh:mm aa");
    ListDataModel<BSPSample> sampleListDataModel;
    List<BSPSample> samples;

    private boolean rebuild = true;

    //TODO this needs to be removed !!
    private String username = "mccrory";

    private int tabIndex = 1;

    Boolean experimentSelected = false;

    public ExpReqSummaryList getExpReqSummaryList() {

        if (rebuild && (getUsername() != null)) {
            List<ExperimentRequestSummary> summaryList = null;
            try {
                summaryList = experimentService.findExperimentSummaries( getUsername() );
            } catch (UserNotFoundException e) {
                //TODO hmc set message for user that the username was not found
            }
            expReqSummaryList.setWrappedData(summaryList);
            rebuild = false;
        }

        return expReqSummaryList;
    }

//    public Person getUser() {
//        return user;
//    }
//
//    public void setUser(final Person user) {
//        this.user = user;
//        setRebuild(true);
//    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if ( ! this.username.equalsIgnoreCase(username)) {
            selectedExperimentRequestSummary = null;
            selectedExperimentRequest = null;
        }
        this.username = username;
        rebuild = true;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }


    //    public void processValueChange(ValueChangeEvent event)
//            throws AbortProcessingException {
//        if (null != event.getNewValue()) {
//            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("user", event.getNewValue());
//        }
//    }

    public boolean handleTabChange() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        String index = externalContext.getRequestParameterMap().get("tabIndex");
        setTabIndex(Integer.parseInt(index));
        return true;
    }

    public void onTabChange(TabChangeEvent event) {
        FacesMessage msg = new FacesMessage("Tab Changed", "Active Tab: " + event.getTab().getTitle());

        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public ExperimentRequestSummary getSelectedExperimentRequestSummary() {
        return selectedExperimentRequestSummary;
    }

    public void setSelectedExperimentRequestSummary(final ExperimentRequestSummary selectedExperimentRequestSummary) {
        this.selectedExperimentRequestSummary = selectedExperimentRequestSummary;
    }

    public int sortByCalendar(Object cal1, Object cal2) {
        return ((Calendar) cal1).compareTo((Calendar) cal2);
    }

    public int sortByDate(Object date1, Object date2) {
            return ((Date) date1).compareTo((Date) date2);
    }

    public int sortByLong(Object id1, Object id2) {
        Long longId1 = (Long) id1;
        Long longId2 = (Long) id2;

        //TODO
        if ((longId1 == null) || (longId2 == null)) {
            throw new RuntimeException("An ID was null");
        }

        return (longId1).compareTo(longId2);
    }

    public String format(Date date) {
        return dateFormat.format(date);
    }


    public int sortByExperimentId(Object experimentId1, Object experimentId2) {

        if (experimentId1 == null) {
            experimentId1 = " ";
        }
        if (experimentId2 == null) {
            experimentId2 = " ";
        }

        String expId1 = (String) experimentId1;
        String expId2 = (String) experimentId2;

        return expId1.compareToIgnoreCase(expId2);
    }

    public void onRowSelect(SelectEvent event) {
        selectedExperimentRequestSummary = (ExperimentRequestSummary) event.getObject();
        selectedExperimentRequest = experimentService.getPlatformRequest(selectedExperimentRequestSummary);
    }

    public void onRowUnselect(UnselectEvent event) {
        selectedExperimentRequestSummary = null;
        selectedExperimentRequest = null;
    }

    public ListDataModel<BSPSample> getSampleListDataModel() {
        return sampleListDataModel;
    }

    public void setSampleListDataModel(ListDataModel<BSPSample> sampleListDataModel) {
        this.sampleListDataModel = sampleListDataModel;
    }

    public ExperimentRequest getSelectedExperimentRequest() {
        return selectedExperimentRequest;
    }

    public List<BSPSample> getSamples() {

        List<BSPSample> bspSampleList = selectedExperimentRequest.getSamples();

        if (selectedExperimentRequest != null) {

            //TODO - Under construction !!!!
            //TODO This code needs to be moved out of the presentation layer and into the boundary or control layer.

            if (!bspSampleList.isEmpty()) {
                List<String> sampleList = new ArrayList<String>();
                for (BSPSample bspSample: bspSampleList) {
                    sampleList.add(bspSample.getId().value);
                }

                List<String[]>  sampleData = bspSampleSearchService.runSampleSearch(sampleList,
                        BSPSampleSearchColumn.SAMPLE_ID,
                        BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID);

                // TODO transfer bsp sample Meta data to BSPSamplelist

            }

        }

        return bspSampleList;
    }

    public ExperimentType getSelectedExperimentType() {
        return selectedExperimentRequest.getExperimentType();
    }

    public List<Person> getPlatformManagers() {
        List<Person> platformProjectManagers = new ArrayList<Person>();

        if (selectedExperimentRequest != null) {
            for (Person person : selectedExperimentRequest.getPlatformProjectManagers()) {
                platformProjectManagers.add(person);
            }
        }
        return platformProjectManagers;
    }
}
