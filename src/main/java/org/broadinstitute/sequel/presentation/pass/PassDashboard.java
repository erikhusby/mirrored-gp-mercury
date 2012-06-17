package org.broadinstitute.sequel.presentation.pass;

import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.boundary.*;
import org.broadinstitute.sequel.control.pass.PassBSPSampleSearchService;
import org.broadinstitute.sequel.control.pass.PassService;
import org.broadinstitute.sequel.presentation.AbstractJsfBean;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;


@ManagedBean
@SessionScoped
public class PassDashboard extends AbstractJsfBean {

    @Inject
    private PassService service;

    @Inject
    private Log log;

    @Inject
    private PassBSPSampleSearchService bspSampleSearchService;

    private boolean rebuild = true;


    private SummarizedPass selectedSummarizedPass = null;


    private AbstractPass selectedPass = null;


    private SummarizedPassDataModel summarizedPassModel = new SummarizedPassDataModel();


    private PassSampleDataModel passSampleDataModel = new PassSampleDataModel();


    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd yyyy 'at' hh:mm aa");

    // how much to skip when parsing pass numbers
    private final int PASS_NUMBER_OFFSET = "PASS-".length();


    private boolean onlyMine;

    // TODO mlc build Squid WS to return this stuff.  The methods to do this already exist, they just need WS wrappers
    // TODO actually not needed urgently since we can filter on the JEE "client" of the Squid WS
    private boolean onlyActive;


    public SummarizedPassDataModel getSummarizedPassModel() {

        if ( rebuild ) {

            log.info("Loading PASSes with onlyMine = " + onlyMine + ", onlyActive = " + onlyActive);

            List<SummarizedPass> passList;

            if ( onlyMine )
                // TODO mlc figure out how to get the logged in user
                passList = service.searchPassesByCreator("mcovarr").getSummarizedPassList();

            else
                passList = service.searchPasses().getSummarizedPassList();


            // cheating and doing ACTIVE filtering on the JEE "client" of the Squid WS since Squid does not offer a
            // WS that will calculate this for us
            if ( onlyActive ) {

                final Iterator<SummarizedPass> iterator = summarizedPassModel.iterator();

                while ( iterator.hasNext() ) {

                    final PassStatus status = iterator.next().getStatus();

                    if ( status == PassStatus.COMPLETE || status == PassStatus.ABANDONED )

                        iterator.remove();
                }
            }

            summarizedPassModel.setWrappedData(passList);

            rebuild = false;

        }

        return summarizedPassModel;

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


    public int sortFloatingPoint(Object o1, Object o2) {

        if (o1 == null && o2 == null)
            return 0;

        if (o1 == null)
            return 1;

        if (o2 == null)
            return -1;

        String str1 = (String) o1;
        String str2 = (String) o2;

        Double d1 = null;
        Double d2 = null;

        try {
            d1 = Double.valueOf(str1);
        }
        catch (NumberFormatException e) {
        }

        try {
            d2 = Double.valueOf(str2);
        }
        catch (NumberFormatException e) {
        }

        if (d1 == null && d2 == null)
            return 0;

        if (d1 == null)
            return 1;

        if (d2 == null)
            return -1;

        return d1.compareTo(d2);

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


    public void onOnlyMine(AjaxBehaviorEvent event) {
        onlyMine = !onlyMine;
        rebuild = true;
    }


    public void onOnlyActive(AjaxBehaviorEvent event) {
        onlyActive = !onlyActive;
        rebuild = true;
    }


    public void onRowSelect(SelectEvent event) {
        this.selectedSummarizedPass = (SummarizedPass) event.getObject();

        this.selectedPass = service.loadPassByNumber(selectedSummarizedPass.getPassNumber());

        List<PassSample> selectedPassSamples = new ArrayList<PassSample>();

        for (Sample sample : selectedPass.getSampleDetailsInformation().getSample()) {
            PassSample passSample = new PassSample();
            passSample.setSampleId(sample.getBspSampleID());
            selectedPassSamples.add(passSample);
        }

        bspSampleSearchService.lookupSampleDataInBSP(selectedPassSamples);

        passSampleDataModel.setWrappedData(selectedPassSamples);
    }


    public void onRowUnselect(UnselectEvent event) {
        this.selectedSummarizedPass = null;
    }



    public Object getSelectedSummarizedPass() {
        return selectedSummarizedPass;
    }

    public void setSelectedSummarizedPass(Object selectedSummarizedPass) {
        this.selectedSummarizedPass = (SummarizedPass) selectedSummarizedPass;
    }


    public AbstractPass getSelectedPass() {
        return selectedPass;
    }

    public void setSelectedPass(AbstractPass selectedPass) {
        this.selectedPass = selectedPass;
    }


    public ProjectInformation getProjectInfo() {
        if (selectedPass == null)
            return null;
        return selectedPass.getProjectInformation();
    }


    public String getCreatedDate() {
        if (selectedPass == null)
            return null;

        return format(selectedPass.getProjectInformation().getDateCreated());
    }


    public String getModifiedDate() {
        if (selectedPass == null)
            return null;

        return format(selectedPass.getProjectInformation().getLastModified());
    }


    public String getPassType() {

        if (selectedPass == null)
            return null;

        String text;
        if (selectedPass instanceof DirectedPass)
            text = "Directed";
        else if (selectedPass instanceof WholeGenomePass)
            text = "Whole Genome";
        else if (selectedPass instanceof RNASeqPass)
            text = "RNASeq";
        else
            throw new RuntimeException("Unrecognized PASS type: " + selectedPass.getClass().getCanonicalName());

        return text;
    }


    public PassSampleDataModel getPassSampleDataModel() {
        return passSampleDataModel;
    }
}
