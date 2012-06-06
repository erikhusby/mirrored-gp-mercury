package org.broadinstitute.sequel.presentation.pass;

import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.boundary.SummarizedPass;
import org.broadinstitute.sequel.boundary.pass.PassSOAPService;
import org.broadinstitute.sequel.presentation.AbstractJsfBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;


@ManagedBean
@SessionScoped
public class PassDashboard extends AbstractJsfBean {

    @Inject
    private PassSOAPService service;

    @Inject
    private Log log;


    private List<SummarizedPass> passes = null;


    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    // how much to skip when parsing pass numbers
    private final int PASS_NUMBER_OFFSET = "PASS-".length();



    public List<SummarizedPass> getPassList() {
        if (passes == null) {
            log.info("Loading all PASSes...");
            passes = service.searchPasses().getSummarizedPassList();
        }
        return passes;
    }


    public void setSelectedPass(SummarizedPass pass) {

    }


    public int sortByCalendar(Object cal1, Object cal2) {
        return ((Calendar) cal1).compareTo((Calendar) cal2);
    }


    public int sortByPassNumber(Object passNumber1, Object passNumber2) {

        if (passNumber1 == null)
            passNumber1 = "PASS-0";

        if (passNumber2 == null)
            passNumber2 = "PASS-0";


        Integer int1 = Integer.valueOf(((String) passNumber1).substring(PASS_NUMBER_OFFSET));
        Integer int2 = Integer.valueOf(((String) passNumber2).substring(PASS_NUMBER_OFFSET));

        return int1.compareTo(int2);
    }


    public String format(Calendar calendar) {
        return dateFormat.format(calendar.getTime());
    }

}
