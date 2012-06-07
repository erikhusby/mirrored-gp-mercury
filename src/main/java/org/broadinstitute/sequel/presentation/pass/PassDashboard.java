package org.broadinstitute.sequel.presentation.pass;

import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.boundary.PassStatus;
import org.broadinstitute.sequel.boundary.SummarizedPass;
import org.broadinstitute.sequel.boundary.pass.PassSOAPService;
import org.broadinstitute.sequel.presentation.AbstractJsfBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
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


    private boolean onlyMine;

    // TODO mlc build Squid WS to return this stuff.  The methods to do this already exist, they just need WS wrappers
    // TODO actually not needed urgently since we can filter on the JEE "client" of the Squid WS
    private boolean onlyActive;


    public List<SummarizedPass> getPassList() {

        if (passes == null) {

            log.info("Loading PASSes with onlyMine = " + onlyMine + ", onlyActive = " + onlyActive);

            if ( onlyMine )
                // TODO mlc figure out how to get the logged in user
                passes = service.searchPassesByCreator("mcovarr").getSummarizedPassList();

            else
                passes = service.searchPasses().getSummarizedPassList();


            // cheating and doing ACTIVE filtering on the JEE "client" of the Squid WS since Squid does not offer a
            // WS that will calculate this for us
            if ( onlyActive ) {

                final Iterator<SummarizedPass> iterator = passes.iterator();

                while (iterator.hasNext()) {

                    final PassStatus status = iterator.next().getStatus();

                    if ( status == PassStatus.COMPLETE || status == PassStatus.ABANDONED )

                        iterator.remove();
                }
            }

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

    public boolean isOnlyMine() {
        return onlyMine;
    }

    public void setOnlyMine(boolean onlyMine) {
        this.onlyMine = onlyMine;
    }

    public boolean isOnlyActive() {
        return onlyActive;
    }

    public void setOnlyActive(boolean onlyActive) {
        this.onlyActive = onlyActive;
    }


    private void issueSummarizedPassReload() {
        passes = null;

    }


    public void onOnlyMine(AjaxBehaviorEvent event) {
        onlyMine = !onlyMine;
        issueSummarizedPassReload();
    }


    public void onOnlyActive(AjaxBehaviorEvent event) {
        onlyActive = !onlyActive;
        issueSummarizedPassReload();
    }

}
