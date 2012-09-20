package org.broadinstitute.gpinformatics.mercury.control.dao.bsp;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.mercury.boundary.GSSRSample;
import org.broadinstitute.gpinformatics.mercury.boundary.GSSRSampleKitRequest;
import org.broadinstitute.gpinformatics.mercury.boundary.RequestSampleSet;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BSPPlatingReceiptDAO;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPStartingSample;
import org.broadinstitute.gpinformatics.mercury.entity.project.ProjectPlan;
import org.broadinstitute.gpinformatics.mercury.entity.queue.AliquotParameters;
import org.broadinstitute.gpinformatics.mercury.entity.sample.BSPStartingSampleDAO;
import org.broadinstitute.gpinformatics.mercury.entity.sample.StartingSample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.infrastructure.bsp.AliquotReceiver;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.*;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;

import javax.inject.Inject;
import java.util.*;

/**
 */
public class BSPSampleFactory {


    @Inject
    private BSPStartingSampleDAO bspStartingSampleDAO;

    @Inject
    private BSPPlatingReceiptDAO bspPlatingReceiptDAO;

    @Inject
    private BSPPlatingRequestService bspPlatingRequestService;

    @Inject
    private QuoteService quoteService;

    @Inject
    private Log log;

    private static final Float WATER_CONTROL_CONCENTARTION = 0F;

    public List<LabVessel> buildFromBSPExportMessage(GSSRSampleKitRequest bspRequest) throws Exception {
        List<LabVessel> bspSamples = new ArrayList<LabVessel>();
        String bspPlatingReceipt = bspRequest.getPlatingRequestID();
        BSPPlatingReceipt bspReceipt = bspPlatingReceiptDAO.findByReceipt(bspPlatingReceipt);
        Collection<BSPPlatingRequest> bspPlatingRequests = bspReceipt.getPlatingRequests();
        List<RequestSampleSet> requestSampleSetList = bspRequest.getRequestSampleSet();

        Map<String, StartingSample> aliquotSourceMap = new HashMap<String, StartingSample>(); //<bspAliquotLSID, StartingSample >
        Map<String, BSPSampleDTO> bspControlSampleDataMap = new HashMap<String, BSPSampleDTO>(); //<bspAliquotLSID, BSPSampleDTO>
        Map<String, String> lsidMap = new HashMap<String, String>(); //<bareID, LSID>
        for (RequestSampleSet reqSampleSet : requestSampleSetList) {
            //Looks like control samples are passed in separate RequestSampleSet !!
            List<GSSRSample> sampleList = reqSampleSet.getGssrSample();
            List<String> sampleNames = new ArrayList<String>();
            for (GSSRSample sample : sampleList) {

                String sourceSample = sample.getSampleSource();
                //once BSP starts passing this no more lookup .. This should contain the stock we requested (StartingSample)

                //each sample is bsp sample
                String lsid = sample.getSampleOrganismAttributes().getLSId();
                //aliquotPassSourceMap.put(lsid, null);
                String[] chunks = lsid.split(":");
                String bareId = chunks[chunks.length - 1];
                sampleNames.add(bareId);
                lsidMap.put(bareId, lsid);
                //volume, concentration ??
            }

            //now do sample search and figure out parents/source
            //once bsp passes source in xml we don't need this
            BSPSampleDataFetcher bspDataFetcher = new BSPSampleDataFetcher();
            Map<String, BSPSampleDTO> bspSampleDTOMap = bspDataFetcher.fetchSamplesFromBSP(sampleNames);

            for (String sampleName : sampleNames) {
                BSPSampleDTO sampleData = bspSampleDTOMap.get(sampleName);
                if (sampleData == null) {
                    throw new Exception("Failed to lookup bsp sample data for sample : " + sampleName);
                }

                String rootSample = sampleData.getRootSample();
                String stockAtExport = sampleData.getStockAtExport();

                if (sampleData.isPositiveControl() || sampleData.isNegativeControl()) {
                    bspControlSampleDataMap.put(sampleName, sampleData);
                } else {
                    //TODO .. all this lookup will go away  once we change the wsdl and bsp will pass actual stock requested
                    //TODO .. so that we can map directly rather than trying to lookup and match for requested stock during plating request
                    //lookup BSPStartingSample
                    StartingSample startingSample = bspStartingSampleDAO.findBySampleName(rootSample);
                    if (startingSample == null) {
                        startingSample = bspStartingSampleDAO.findBySampleName(stockAtExport);
                        if (startingSample == null) {
                            throw new Exception("Source Stock Sample not identified for sample: " + sampleName);
                        }
                    }
                    aliquotSourceMap.put(lsidMap.get(sampleName), startingSample);
                }
            }

            //looks like we don't need the BSPSampleData !!
            //now receive all the aliquots
            bspSamples = receiveBSPAliquots(bspReceipt, aliquotSourceMap, bspControlSampleDataMap);
        }

        return bspSamples;
    }

    public List<LabVessel> receiveBSPAliquots(BSPPlatingReceipt bspReceipt,
                                              Map<String, StartingSample> aliquotSourceMap,
                                              Map<String, BSPSampleDTO> bspControlSampleDataMap)
            throws Exception {

        if (bspReceipt == null) {
            throw new IllegalArgumentException("BSP Plating receipt cannot be null");
        }

        List<LabVessel> bspAliquots = new ArrayList<LabVessel>();

        //iterate through aliquotSourceMap (sampleStock )
        Iterator<String> sampleItr = aliquotSourceMap.keySet().iterator();
        while (sampleItr.hasNext()) {
            String sampleLSID = sampleItr.next();

            //All below ugly parsing will go way once bsp passes the stock we asked for (source sample name / starting sample) ?
            String[] chunks = sampleLSID.split(":");
            String bareId = chunks[chunks.length - 1];
            String sampleName = "SM-" + bareId;

            //get ProjectPlan from startingSample
            StartingSample startingSample = aliquotSourceMap.get(sampleLSID);
            if (startingSample == null) {
                throw new Exception("failed to lookup starting sample for sample : " + sampleLSID);
            }
            ProjectPlan projectPlan = startingSample.getRootProjectPlan();

            StartingSample aliquot = new BSPStartingSample(sampleName, projectPlan);
            LabVessel bspAliquot = new BSPSampleAuthorityTwoDTube(aliquot);
            projectPlan.addAliquotForStarter(startingSample, bspAliquot);

            bspAliquots.add(bspAliquot);
            AliquotReceiver aliquotReceiver = new AliquotReceiver();

            aliquotReceiver.receiveAliquot(startingSample, bspAliquot, bspReceipt);

            //TODO .. need any LabEvent Message ????
        }

        if (bspControlSampleDataMap != null) {
            Iterator<String> controlSampleItr = bspControlSampleDataMap.keySet().iterator();
            while (controlSampleItr.hasNext()) {
                String controlName = sampleItr.next();
                StartingSample aliquot = new BSPStartingSample(controlName, null);//?? ProjectPlan
                LabVessel bspAliquot = new BSPSampleAuthorityTwoDTube(aliquot);
                bspAliquots.add(bspAliquot);
            }
        }

        return bspAliquots;
    }

    public List<BSPPlatingRequest> buildBSPPlatingRequests(Map<StartingSample, AliquotParameters> starterMap)
            throws Exception {

        List<BSPPlatingRequest> bspPlatingRequests = new ArrayList<BSPPlatingRequest>();
        BSPPlatingRequest platingRequest;
        if (starterMap == null || starterMap.isEmpty()) {
            throw new IllegalArgumentException("Null or empty Starter list ");
        }

        Iterator<StartingSample> starterIterator = starterMap.keySet().iterator();
        while (starterIterator.hasNext()) {
            StartingSample starter = starterIterator.next();
            AliquotParameters parameters = starterMap.get(starter);
            platingRequest = new BSPPlatingRequest(starter.getSampleName(), parameters);
            bspPlatingRequests.add(platingRequest);

            //TODO .. check back ..addToProjectPlan ??
            starter.getRootProjectPlan().addPlatingRequest(platingRequest);
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
     * @param projectPlan
     * @param negControlCount
     * @param negControlVolume
     * @param posControlQuote
     * @param negControlQuote
     * @return
     * @throws Exception
     */
    //TODO .. Why is this here rather than in BSPPlatingRequestServiceImpl ??
    //for testability so that this code cna be tested from stub/impl/xxxxxx without duplicating ???
    public List<ControlWell> buildControlWells(Map<String, AliquotParameters> positiveControlMap,
                                               ProjectPlan projectPlan, int negControlCount, Float negControlVolume,
                                               String posControlQuote, String negControlQuote)
            throws Exception {

        List<ControlWell> controls = new ArrayList<ControlWell>();
        Character posControlRow = 'H';
        int posControlCol = 02;
        if (positiveControlMap != null) {
            Iterator<String> posControlIterator = positiveControlMap.keySet().iterator();
            while (posControlIterator.hasNext()) {
                String externalID = posControlIterator.next();
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

    public BSPPlatingReceipt buildPlatingReceipt(List<BSPPlatingRequest> requests, BSPPlatingRequestResult platingResult) {
        BSPPlatingReceipt receipt = null;

        if (platingResult != null && platingResult.getPlatingRequestReceipt() != null && (platingResult.getErrors() == null || platingResult.getErrors().isEmpty())) {
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