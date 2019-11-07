package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.statehandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.FastQListBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenFolderUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FastQList;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.ReadGroupUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Dependent
public class AggregationStateHandler extends StateHandler {

    private static final Log logger = LogFactory.getLog(AggregationStateHandler.class);

    @Inject
    private DragenConfig dragenConfig;

    @Inject
    private FastQListBuilder fastQListBuilder;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    /**
     * Only want to enter if its in a state ready to aggregate
     */
    @Override
    public boolean onEnter(State state) {
        logger.info("Aggregation start()");
        if (!OrmUtil.proxySafeIsInstance(state, AggregationState.class)) {
            throw new RuntimeException("Expect only aggregation states");
        }

        try {
            AggregationState aggregationState = OrmUtil.proxySafeCast(state, AggregationState.class);
            MercurySample mercurySample = aggregationState.getMercurySamples().iterator().next();
            Set<LabVessel> labVessels = mercurySample.getLabVessel();

            // PDO Sample Key but may require the export
            String sampleKey = mercurySample.getSampleKey();

            Set<FastQList> finalFastQList = new HashSet<>();
            Map<File, Map<Integer, Map<String, List<FastQList>>>> mapFileToFastqs = new HashMap<>();

            for (LabVessel labVessel: labVessels) {
                LabVesselSearchDefinition.VesselsForEventTraverserCriteria eval
                        = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(LabVesselSearchDefinition.FLOWCELL_LAB_EVENT_TYPES);
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                Map<LabVessel, Collection<VesselPosition>> labVesselCollectionMap = eval.getPositions().asMap();

                // No Flowcells, shouldn't be in this state ever.
                if (labVesselCollectionMap.isEmpty()) {
                    logger.info("No flowcells found for sample: " + sampleKey + " not aggregating.");
                    return false;
                }

                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions: labVesselCollectionMap.entrySet()) {
                    IlluminaFlowcell flowcell =
                            OrmUtil.proxySafeCast(labVesselAndPositions.getKey(), IlluminaFlowcell.class);
                    String flowcellBarcode = flowcell.getLabel();

                    List<IlluminaSequencingRun> runs = illuminaSequencingRunDao.findByFlowcellBarcode(flowcellBarcode);
                    Optional<IlluminaSequencingRun> optionalRun =
                            runs.stream().max(Comparator.comparing(IlluminaSequencingRun::getRunDate));

                    if (!optionalRun.isPresent()) {
                        logger.info("No run registered found for fc: " + flowcellBarcode + " not aggregating: " + sampleKey);
                        continue;
                    }

                    IlluminaSequencingRun illuminaSequencingRun = optionalRun.get();
                    for (VesselPosition vesselPosition: labVesselAndPositions.getValue()) {
                        IlluminaSequencingRunChamber sequencingRunChamber =
                                illuminaSequencingRun.getSequencingRunChamber(vesselPosition); // TODO Seqrun chamber may be empty, if so not true
                        // TODO Jw Think about this more
                        MercurySample sampleImport = findSampleImport(flowcell, vesselPosition, mercurySample);
                        String sampleImportKey = sampleImport.getSampleKey();
                        if (sequencingRunChamber != null) {
                            Optional<DemultiplexState> demultiplexStateOpt =
                                    sequencingRunChamber.getMostRecentCompleteStateOfType(DemultiplexState.class);
                            if (demultiplexStateOpt == null) {
                                String msg = String.format("Waiting on lane: %s in %s to aggregate %s",
                                        vesselPosition.name(), flowcellBarcode, sampleKey);
                                logger.info(msg);
                                return false;
                            } else {
                                DemultiplexState demultiplexState = demultiplexStateOpt.get();
                                IlluminaSequencingRun run =
                                        demultiplexState.getRunForChamber(sequencingRunChamber);
                                DragenFolderUtil dragenFolderUtil = new DragenFolderUtil(dragenConfig, run,
                                        demultiplexState.getStateName());

                                Map<Integer, Map<String, List<FastQList>>> fastQList = null;
                                File fastQFile = dragenFolderUtil.getFastQListFile();
                                if (!fastQFile.exists()) {
                                    throw new RuntimeException("Failed to find file " + fastQFile.getPath());
                                }
                                if (!mapFileToFastqs.containsKey(fastQFile)) {
                                    fastQList = fastQListBuilder.parseFastQFile(fastQFile);
                                    mapFileToFastqs.put(fastQFile, fastQList);
                                }
                                fastQList = mapFileToFastqs.get(fastQFile);
                                Map<String, List<FastQList>> mapKeyToFastq =
                                        fastQList.get(sequencingRunChamber.getLaneNumber());
                                if (mapKeyToFastq == null) {
                                    throw new RuntimeException("Failed to find fast map for " + fastQFile.getPath());
                                }
                                List<FastQList> sampleFastQs = mapKeyToFastq.get(sampleKey);
                                if (sampleFastQs == null) {
                                    sampleFastQs = mapKeyToFastq.get(sampleImportKey);
                                }
                                for (FastQList fq: sampleFastQs) {
                                    fq.setRgId(ReadGroupUtil.createRgId(flowcell, sequencingRunChamber.getLaneNumber(), mercurySample));
                                }
                                finalFastQList.addAll(sampleFastQs);
                            }
                        }
                    }
                }
            }

            Optional<AggregationTask> aggregationTaskOpt = aggregationState.getAggregationTask();
            if (!aggregationTaskOpt.isPresent()) {
                logger.error("Expect Aggregation State to have an AggregationTask: " + aggregationState.getStateId());
                return false;
            }

            AggregationTask aggregationTask = aggregationTaskOpt.get();

            File fastQFile = aggregationTask.getFastQList();
            if (!fastQFile.getParentFile().exists()) {
                fastQFile.getParentFile().mkdir();
            }
            fastQListBuilder.buildAggregation(finalFastQList, mercurySample, fastQFile);
            return true;
        } catch (Exception e) {
            logger.error("Failed to enter aggregation state: " + state.getStateId(), e);
        }

        return false;
    }

    public MercurySample findSampleImport(RunCartridge flowcell, VesselPosition lane, MercurySample sample ) {
        for (SampleInstanceV2 sampleInstance : flowcell.getContainerRole().getSampleInstancesAtPositionV2(lane)) {
            ProductOrderSample productOrderSample = sampleInstance.getSingleProductOrderSample();
            MercurySample mercurySample;
            if (productOrderSample != null) {
                mercurySample = productOrderSample.getMercurySample();
            } else {
                // Controls won't have a ProductOrderSample, so use root sample ID.
                mercurySample = sampleInstance.getRootOrEarliestMercurySample();
            }

            if (mercurySample.equals(sample)) {
                LabBatchStartingVessel importLbsv =
                        sampleInstance.getSingleBatchVessel(LabBatch.LabBatchType.SAMPLES_IMPORT);
                if (importLbsv != null) {
                    Collection<MercurySample> mercurySamples = importLbsv.getLabVessel().getMercurySamples();
                    if (!mercurySamples.isEmpty()) {
                        return mercurySamples.iterator().next();
                    }
                }
            }

        }

        return null;
    }


}
