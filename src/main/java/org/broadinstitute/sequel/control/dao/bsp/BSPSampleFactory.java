package org.broadinstitute.sequel.control.dao.bsp;

import org.broadinstitute.sequel.boundary.GSSRSample;
import org.broadinstitute.sequel.boundary.GSSRSampleKitRequest;
import org.broadinstitute.sequel.boundary.RequestSampleSet;
import org.broadinstitute.sequel.control.dao.vessel.BSPPlatingReceiptDAO;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.sample.BSPStartingSampleDAO;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.infrastructure.bsp.AliquotReceiver;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;

import javax.inject.Inject;
import java.util.*;

/**
 */
public class BSPSampleFactory {


    @Inject
    private BSPStartingSampleDAO bspStartingSampleDAO;

    @Inject
    private BSPPlatingReceiptDAO bspPlatingReceiptDAO;

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
            projectPlan.setAliquot(startingSample, bspAliquot);

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


}