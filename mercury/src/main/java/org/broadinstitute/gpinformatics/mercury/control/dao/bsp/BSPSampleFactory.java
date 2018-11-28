package org.broadinstitute.gpinformatics.mercury.control.dao.bsp;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestResult;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.Control;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.ControlWell;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.Plateable;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.Well;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BSPPlatingReceiptDao;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.gpinformatics.mercury.entity.queue.AliquotParameters;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
public class BSPSampleFactory {

    private static final Float WATER_CONTROL_CONCENTARTION = 0F;

    public List<BSPPlatingRequest> buildBSPPlatingRequests(Map<MercurySample, AliquotParameters> starterMap)
            throws Exception {

        List<BSPPlatingRequest> bspPlatingRequests = new ArrayList<>();
        BSPPlatingRequest platingRequest;
        if (starterMap == null || starterMap.isEmpty()) {
            throw new IllegalArgumentException("Null or empty Starter list ");
        }

        for (MercurySample starter : starterMap.keySet()) {
            AliquotParameters parameters = starterMap.get(starter);
            platingRequest = new BSPPlatingRequest(starter.getSampleKey(), parameters);
            bspPlatingRequests.add(platingRequest);

            //TODO .. check back ..addToProjectPlan ??
//            starter.getRootProjectPlan().addPlatingRequest(platingRequest);
        }

        //TODO .. Do we controls added to BSPPlatingRequest ??
/*
        if (posControlMap != null) {
            Iterator<String> posControlIterator = posControlMap.keySet().iterator();
            while (posControlIterator.hasNext()) {
                String externalID = posControlIterator.next();
                AliquotParameters parameters = posControlMap.get(externalID);
                platingRequest = new BSPPlatingRequest(null, parameters);
                bspPlatingRequests.add(platingRequest);
            }
        }

        //negative controls /water controls
        for (int i = 0; i < negcontrolCount; i++) {

            ProjectPlan projPlan = bspPlatingRequests.get(0).getAliquotParameters().getProjectPlan();
            AliquotParameters parameters = new AliquotParameters(projPlan, negControlVolume, WATER_CONTROL_CONCENTARTION); //TODO .. check back vol,conc
            platingRequest = new BSPPlatingRequest(null, parameters);
            bspPlatingRequests.add(platingRequest);
        }
*/

        return bspPlatingRequests;
    }


    /**
     * @param positiveControlMap
     * @param negControlCount
     * @param negControlVolume
     * @param posControlQuote
     * @param negControlQuote
     *
     * @return
     *
     * @throws Exception
     */
    //TODO .. Why is this here rather than in BSPPlatingRequestServiceImpl ??
    //for testability so that this code cna be tested from stub/impl/xxxxxx without duplicating ???
    public List<ControlWell> buildControlWells(Map<String, AliquotParameters> positiveControlMap,
                                               /*ProjectPlan projectPlan, */int negControlCount, Float negControlVolume,
                                               String posControlQuote, String negControlQuote)
            throws Exception {

        List<ControlWell> controls = new ArrayList<>();
        Character posControlRow = 'H';
        int posControlCol = 02;
        if (positiveControlMap != null) {
            for (String externalID : positiveControlMap.keySet()) {
                AliquotParameters parameters = positiveControlMap.get(externalID);

                Control.Positive posControl = new Control.Positive(externalID);
                //TODO..This can be moved into service
                Well well = new Well(posControlRow, posControlCol, Plateable.Size.WELLS_96);
                ControlWell posControlWell = new ControlWell(well, posControl, parameters.getTargetVolume(),
                        parameters.getTargetConcentration(), posControlQuote);
                controls.add(posControlWell);
                posControlCol++;
                //TODO .. check ?? need to add to BSPPlatingRequest
            }
        }

        //negative controls /water controls
        Character negControlRow = 'D';
        int negControlCol = 10;
        for (int i = 0; i < negControlCount; i++) {

            Well well = new Well(negControlRow, negControlCol, Plateable.Size.WELLS_96);
            ControlWell negcontrol = new ControlWell(well, Control.Negative.WATER_CONTROL);
            negcontrol.setQuoteID(negControlQuote); //TODO . .pass QUOTE
            negcontrol.setVolume(negControlVolume);
            negcontrol.setConcentration(WATER_CONTROL_CONCENTARTION);
            controls.add(negcontrol);
            negControlCol++;

            //AliquotParameters parameters = new AliquotParameters(projectPlan, negControlVolume, WATER_CONTROL_CONCENTARTION); //TODO .. check back vol,conc

            //TODO .. add to BSPPR's ??
            //platingRequest = new BSPPlatingRequest(null, parameters);
            //bspPlatingRequests.add(platingRequest);
        }

        return controls;

    }

    public BSPPlatingReceipt buildPlatingReceipt(List<BSPPlatingRequest> requests,
                                                 BSPPlatingRequestResult platingResult) {
        BSPPlatingReceipt receipt = null;

        if (platingResult != null && platingResult.getPlatingRequestReceipt() != null && (
                platingResult.getErrors() == null || platingResult.getErrors().isEmpty())) {
            //BSP work request was created
            receipt = new BSPPlatingReceipt(platingResult.getPlatingRequestReceipt());
            receipt.getPlatingRequests().addAll(requests);
        }

        return receipt;
    }

/*

    public BSPPlatingRequestResult issueBSPPlatingRequest(Map<StartingSample, AliquotParameters> starterMap,
                                                          Map<String, AliquotParameters> posControlMap, int negcontrolCount, Float negControlVolume,
                                                          String posControlQuote, String negControlQuote,
                                                          String platingRequestName, String technology, String login, String label, String comments )
            throws Exception {

        List<SeqWorkRequestAliquot> bspStocks = new ArrayList<SeqWorkRequestAliquot>();

        List<BSPPlatingRequest> bspPlatingRequests = new ArrayList<BSPPlatingRequest>();
        BSPPlatingRequest platingRequest;
        if (starterMap == null || starterMap.isEmpty()) {
            throw new IllegalArgumentException("Null or empty Starter list ");
        }

        Iterator<StartingSample> starterIterator = starterMap.keySet().iterator();
        while (starterIterator.hasNext()) {
            StartingSample starter = starterIterator.next();
            AliquotParameters parameters = starterMap.get(starter);
            SeqWorkRequestAliquot aliquot = new SeqWorkRequestAliquot(starter.getSampleName(), parameters.getTargetVolume(),
                    parameters.getTargetConcentration(), parameters.getProjectPlan().getQuoteDTO(quoteService).getAlphanumericId());
            bspStocks.add(aliquot);
            platingRequest = new BSPPlatingRequest(starter.getSampleName(), parameters);
            bspPlatingRequests.add(platingRequest);
        }

        //pass control AliquotParameters .. quote
        List<ControlWell> controls = new ArrayList<ControlWell>();
        Character posControlRow = 'H';
        int posControlCol = 02;
        if (posControlMap != null) {
            Iterator<String> posControlIterator = posControlMap.keySet().iterator();
            while (posControlIterator.hasNext()) {
                String externalID = posControlIterator.next();
                AliquotParameters parameters = posControlMap.get(externalID);
                Control.Positive posControl = new Control.Positive(externalID);
                //TODO..This can be moved into service
                Well well = new Well(posControlRow, posControlCol, Plateable.Size.WELLS_96);
                ControlWell posControlWell = new ControlWell(well, posControl, parameters.getTargetVolume(),
                        parameters.getTargetConcentration(), posControlQuote);
                controls.add(posControlWell);
                posControlCol++;

                //platingRequest = new BSPPlatingRequest(null, parameters);
                //bspPlatingRequests.add(platingRequest);
            }
        }

        //negative controls /water controls
        Character negControlRow = 'D';
        int negControlCol = 10;
        for (int i = 0; i < negcontrolCount; i++) {
            Well well = new Well(negControlRow, negControlCol, Plateable.Size.WELLS_96);
            ControlWell negcontrol = new ControlWell(well, Control.Negative.WATER_CONTROL);
            negcontrol.setQuoteID(negControlQuote);
            negcontrol.setVolume(negControlVolume);
            negcontrol.setConcentration(WATER_CONTROL_CONCENTARTION);
            controls.add(negcontrol);
            negControlCol++;
            //platingRequest = new BSPPlatingRequest(null, parameters);
            //bspPlatingRequests.add(platingRequest);
        }

        BSPPlatingRequestOptions defaultOptions = bspPlatingRequestService.getBSPPlatingRequestDefaultOptions();
        //TODO ..
        //set PlatformAndProcess, notificationList
        defaultOptions.setNotificationList("sampath@broadinstitute.org");
        defaultOptions.setPlatformAndProcess(BSPPlatingRequestOptions.PlatformAndProcess.ILLUMINA_6_14Kb_JUMP); //TODO.. pass this
        if (platingRequestName == null || platingRequestName.isEmpty()) {
            platingRequestName = label;
        }

        BSPPlatingRequestResult platingResult = bspPlatingRequestService.createPlatingRequest(defaultOptions, login, platingRequestName, bspStocks, controls,
                comments, technology, label);

        return platingResult;
    }

*/


}