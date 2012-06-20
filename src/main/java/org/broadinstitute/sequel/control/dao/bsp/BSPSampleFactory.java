package org.broadinstitute.sequel.control.dao.bsp;

import org.broadinstitute.sequel.boundary.GSSRSample;
import org.broadinstitute.sequel.boundary.GSSRSampleKitRequest;
import org.broadinstitute.sequel.boundary.RequestSampleSet;
import org.broadinstitute.sequel.boundary.squid.Sample;
import org.broadinstitute.sequel.control.dao.vessel.BSPPlatingReceiptDAO;
import org.broadinstitute.sequel.control.dao.vessel.BSPStockSampleDAO;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.sample.BSPStartingSampleDAO;
import org.broadinstitute.sequel.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.sequel.infrastructure.bsp.AliquotReceiver;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;

import javax.inject.Inject;
import java.util.*;

/**
 */
public class BSPSampleFactory {


    @Inject
    private BSPStockSampleDAO bspStockSampleDAO;

    @Inject
    private BSPStartingSampleDAO bspStartingSampleDAO;

    @Inject
    private BSPPlatingReceiptDAO bspPlatingReceiptDAO;

    public List<Starter> buildFromBSPExportMessage(GSSRSampleKitRequest bspRequest) throws Exception {
        List<Starter> bspSamples = new ArrayList<Starter>();
        String bspPlatingReceipt = bspRequest.getPlatingRequestID();
        BSPPlatingReceipt bspReceipt = bspPlatingReceiptDAO.findByReceipt(bspPlatingReceipt);
        Collection<BSPPlatingRequest> bspPlatingRequests = bspReceipt.getPlatingRequests();
        List<RequestSampleSet> requestSampleSetList = bspRequest.getRequestSampleSet();

        Map<String, Sample> aliquotPassSourceMap = new HashMap<String, Sample>(); //<bspAliquotLSID, PassSample>
        Map<String, BSPStartingSample> aliquotSourceMap = new HashMap<String, BSPStartingSample>(); //<bspAliquotLSID, StartingSample >
        Map<String, BSPSampleDTO> bspSampleDataMap = new HashMap<String, BSPSampleDTO>(); //<bspAliquotLSID, BSPSampleDTO>

        //Map<String, LabVessel> controlaliquotSourceMap = new HashMap<String, LabVessel>(); //<bspAliquotLSID, SourceSample (Parent)>
        Map<String, BSPSampleDTO> bspControlSampleDataMap = new HashMap<String, BSPSampleDTO>(); //<bspAliquotLSID, BSPSampleDTO>

        for (RequestSampleSet reqSampleSet : requestSampleSetList) {
            //Looks like control samples are passed in separate RequestSampleSet !!
            List<GSSRSample> sampleList = reqSampleSet.getGssrSample();
            List<String> sampleNames = new ArrayList<String>();
            for (GSSRSample sample : sampleList) {

                String sourceSample = sample.getSampleSource();
                //once BSP starts passing this no more lookup .. This should contain the stock we requested (StartingSample)

                //each sample is bsp sample
                String lsid = sample.getSampleOrganismAttributes().getLSId();
                aliquotPassSourceMap.put(lsid, null);
                String[] chunks = lsid.split(":");
                String bareId = chunks[chunks.length - 1];
                sampleNames.add(bareId);
                //volume, concentration ??

            }

            //now do sample search and figure out parents/source
            //once bsp passes source in xml we don't need this
            BSPSampleDataFetcher bspDataFetcher = new BSPSampleDataFetcher();
            Map<String, BSPSampleDTO> bspSampleDTOMap = bspDataFetcher.fetchSamplesFromBSP(sampleNames);

            Iterator<String> sampleItr = aliquotPassSourceMap.keySet().iterator();
            while (sampleItr.hasNext()) {
                String sampleName = sampleItr.next();
                BSPSampleDTO sampleData = bspSampleDataMap.get(sampleName);
                if (sampleData == null) {
                    throw new Exception("Failed to lookup bsp sample data for sample : " + sampleName);
                }

                String rootSample = sampleData.getRootSample();
                String stockAtExport = sampleData.getStockAtExport();
                //Boolean isPositiveControl = sampleData.isPositiveControl();
                //Boolean isNegativeControl = sampleData.isNegativeControl();

                if (sampleData.isPositiveControl() || sampleData.isNegativeControl()) {
                    bspControlSampleDataMap.put(sampleName, sampleData);
                } else {
                    //TODO .. all this lookup will go away  once we change the wsdl and bsp will pass actual stock requested
                    //TODO .. so that we can map directly rather than trying to lookup and match for requested stock during plating request
                    //lookup LabVessel for this rootSample

                    //you need the PassSample for this barcode
                    Sample passSample = null; //invoke Webservice to get the pass sample JAXB object
                    if (passSample == null) {
                        //lookup by stockAtExport
                        if (passSample == null) {
                            throw new Exception("Source Pass Stock Sample not identified for sample: " + sampleName);
                        }
                    }
                    aliquotPassSourceMap.put(sampleName, passSample);
                    //lookup BSPStartingSample
                    BSPStartingSample startingSample = bspStartingSampleDAO.findBySampleName(rootSample);
                    if (startingSample == null) {
                        startingSample = bspStartingSampleDAO.findBySampleName(stockAtExport);
                        if (startingSample == null) {
                            throw new Exception("Source Stock Sample not identified for sample: " + sampleName);
                        }
                    }
                    aliquotSourceMap.put(sampleName, startingSample);
                }
            }

            //looks like we don't need the BSPSampleData !!
            //now receive all the aliquots
            bspSamples = receiveBSPAliquots(bspReceipt, aliquotPassSourceMap, aliquotSourceMap, bspControlSampleDataMap);
        }

        return bspSamples;
    }

    public List<Starter> receiveBSPAliquots(BSPPlatingReceipt bspReceipt, Map<String, Sample> aliquotPassSourceMap,
                                            Map<String, BSPStartingSample> aliquotSourceMap,
                                            Map<String, BSPSampleDTO> bspControlSampleDataMap)
            throws Exception {

        if (bspReceipt == null) {
            throw new IllegalArgumentException("BSP Plating receipt cannot be null");
        }

        List<Starter> bspAliquots = new ArrayList<Starter>();

        //iterate through aliquotSourceMap (sampleStock )
        Iterator<String> sampleItr = aliquotPassSourceMap.keySet().iterator();
        while (sampleItr.hasNext()) {
            String sampleLSID = sampleItr.next();
            Sample passSampleSource = aliquotPassSourceMap.get(sampleLSID);
            if (passSampleSource == null) {
                throw new Exception("Source parent stock sample not found for aliquot LSID : " + sampleLSID);
            }

            //All below ugly parsing will go way once bsp passes the stock we asked for (source sample name / starting sample) ?
            String[] chunks = sampleLSID.split(":");
            String bareId = chunks[chunks.length - 1];
            String sampleName = "SM-" + bareId;

            //get ProjectPlan from startingSample
            BSPStartingSample startingSample = aliquotSourceMap.get(sampleLSID);
            if (startingSample == null) {
                throw new Exception("failed to lookup starting sample for sample : " + sampleLSID);
            }
            ProjectPlan projectPlan = startingSample.getRootProjectPlan();

            BSPStartingSample aliquot = new BSPStartingSample(sampleName, projectPlan);
            //TODO .. How to specify source...aliquot.. again
            //BSPSampleAuthorityTwoDTube bspAliquot = new BSPSampleAuthorityTwoDTube(passSampleSource, aliquot);
            BSPSampleAuthorityTwoDTube bspAliquot = new BSPSampleAuthorityTwoDTube(aliquot);
            projectPlan.setAliquot(startingSample, bspAliquot);

            bspAliquots.add(bspAliquot);
            //lookup or search for matching BSPPlatingRequest to get ProjectPlan ??
            AliquotReceiver aliquotReceiver = new AliquotReceiver();

            aliquotReceiver.receiveAliquot(startingSample, bspAliquot, bspReceipt);

            //TODO .. need any LabEvent Message ????
        }

        if (bspControlSampleDataMap != null) {
            Iterator<String> controlSampleItr = bspControlSampleDataMap.keySet().iterator();
            while (controlSampleItr.hasNext()) {
                String controlName = sampleItr.next();
                BSPStartingSample aliquot = new BSPStartingSample(controlName, null);//?? ProjectPlan
                BSPSampleAuthorityTwoDTube bspAliquot = new BSPSampleAuthorityTwoDTube(aliquot);
                bspAliquots.add(bspAliquot);
            }
        }

        return bspAliquots;
    }


}