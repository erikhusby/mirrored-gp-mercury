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
import org.primefaces.event.UnselectEvent;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
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
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd yyyy 'at' hh:mm aa");
    ListDataModel<BSPSample> sampleListDataModel;


    public ExpReqSummaryList getExpReqSummaryList() {

        List<ExperimentRequestSummary> summaryList = sequencingService.getRequestSummariesByCreator(new Person("namrata", RoleType.PROGRAM_PM));

        expReqSummaryList.setWrappedData(summaryList);

        return expReqSummaryList;
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
        return ((Long) id1).compareTo((Long) id2);
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
    }

    public ListDataModel<BSPSample> getSampleListDataModel() {
        return sampleListDataModel;
    }

    public void setSampleListDataModel(final ListDataModel<BSPSample> sampleListDataModel) {
        this.sampleListDataModel = sampleListDataModel;
    }


}
