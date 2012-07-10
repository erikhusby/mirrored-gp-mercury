package org.broadinstitute.pmbridge.presentation.summary;

import org.apache.commons.logging.Log;
import org.broadinstitute.pmbridge.entity.bsp.BSPSample;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.broadinstitute.pmbridge.infrastructure.squid.SequencingService;
import org.broadinstitute.pmbridge.presentation.AbstractJsfBean;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.UnselectEvent;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 6/28/12
 * Time: 1:08 PM
 */
@ManagedBean
@SessionScoped
public class SummaryTableBean extends AbstractJsfBean {


    @Inject
    Log log;

    @Inject
    SequencingService sequencingService;

    private ExpReqSummaryList expReqSummaryList = new ExpReqSummaryList();
    private ExperimentRequestSummary selectedExperimentRequestSummary = null;
    private ExperimentRequest selectedExperimentRequest;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy 'at' hh:mm aa");
    ListDataModel<BSPSample> sampleListDataModel;

    private boolean rebuild = true;

    //TODO this needs to be removed !!
    private String username = "mccrory";

    private int tabIndex = 1;
    //TODO - remove this.
    private final int PASS_NUMBER_OFFSET = 5;

    Boolean experimentSelected = false;

    public ExpReqSummaryList getExpReqSummaryList() {

        if (rebuild && (getUsername() != null)) {

            List<ExperimentRequestSummary> summaryList = sequencingService.getRequestSummariesByCreator(
                    new Person(getUsername(), RoleType.PROGRAM_PM));

            expReqSummaryList.setWrappedData(summaryList);
            setRebuild(false);

        } else {
            System.out.println("Not updated.");
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

    public void setUsername(final String username) {
        this.username = username;
        setRebuild(true);
        onRowUnselect(null);
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(final int tabIndex) {
        this.tabIndex = tabIndex;
    }


    private void setRebuild(final boolean rebuild) {
        this.rebuild = rebuild;
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
        this.selectedExperimentRequestSummary = (ExperimentRequestSummary) event.getObject();

        this.selectedExperimentRequest = sequencingService.getPlatformRequest(selectedExperimentRequestSummary);

        //TODO
        //sampleListDataModel.setWrappedData(selectedExperimentRequest.getSamples());
    }


    public void onRowUnselect(UnselectEvent event) {
        this.selectedExperimentRequestSummary = null;
        this.selectedExperimentRequest = null;
    }

    public ListDataModel<BSPSample> getSampleListDataModel() {
        return sampleListDataModel;
    }

    public void setSampleListDataModel(final ListDataModel<BSPSample> sampleListDataModel) {
        this.sampleListDataModel = sampleListDataModel;
    }

    public ExperimentRequest getSelectedExperimentRequest() {
        return selectedExperimentRequest;
    }

    public Boolean getExperimentSelected() {
        return (this.selectedExperimentRequest != null);
    }
}
