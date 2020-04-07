package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.statehandler;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.FastQListBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.SampleSheetBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenFolderUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FastQList;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.ReadGroupUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
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
public class AggregationStateHandler extends StateHandler<AggregationState> {

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
    public boolean onEnter(AggregationState state) {
        logger.debug("Aggregation start()");

        try {
            AggregationState aggregationState = OrmUtil.proxySafeCast(state, AggregationState.class);
            MercurySample mercurySample = aggregationState.getMercurySamples().iterator().next();
            String sampleKey = mercurySample.getSampleKey();
            Set<FastQList> finalFastQList = new HashSet<>();
            Map<File, Map<Integer, Map<String, List<FastQList>>>> mapFileToFastqs = new HashMap<>();
            boolean failedToEnter = false;
            for (IlluminaSequencingRunChamber sequencingRunChamber : state.getSequencingRunChambers()) {
                IlluminaSequencingRun illuminaSequencingRun = sequencingRunChamber.getIlluminaSequencingRun();
                Optional<DemultiplexState> demultiplexStateOpt =
                        sequencingRunChamber.getMostRecentCompleteStateOfType(DemultiplexState.class);
                if (!demultiplexStateOpt.isPresent()) {
                    logger.info("Aggregation " + state + " is waiting on demultiplex of lane: "
                                + sequencingRunChamber.getLaneNumber() + " in " + illuminaSequencingRun.getRunName());
                    failedToEnter = true;
                }
            }

            if (failedToEnter) {
                return false;
            }

            for (IlluminaSequencingRunChamber sequencingRunChamber : state.getSequencingRunChambers()) {
                IlluminaSequencingRun illuminaSequencingRun = sequencingRunChamber.getIlluminaSequencingRun();
                System.out.println(illuminaSequencingRun.getRunName() + " " + sequencingRunChamber.getLaneNumber());
                Optional<DemultiplexState> demultiplexStateOpt =
                        sequencingRunChamber.getMostRecentCompleteStateOfType(DemultiplexState.class);
                DemultiplexState demultiplexState = demultiplexStateOpt.get();
                if (demultiplexState.isIgnored()) {
                    continue; //Skip all ignored positions (orphan rate fail etc).
                }
                IlluminaSequencingRun run =
                        demultiplexState.getRunForChamber(sequencingRunChamber);
                DragenFolderUtil dragenFolderUtil = new DragenFolderUtil(dragenConfig, run,
                        demultiplexState.getStateName());

                RunCartridge runCartridge = illuminaSequencingRun.getSampleCartridge();

                MercurySample sampleImport =
                        findSampleImport(runCartridge, sequencingRunChamber.getLanePosition(), mercurySample);
                if (sampleImport == null) {
                    sampleImport = mercurySample;
                }
                String sampleImportKey = sampleImport.getSampleKey();

                Pair<MercurySample, SampleInstanceV2> instancePair = SampleSheetBuilder.
                        findSampleInFlowcellLane(runCartridge, sequencingRunChamber.getLanePosition(),
                                mercurySample);
                SampleInstanceV2 sampleInstanceV2 = instancePair.getRight();
                String indexingSchemeString = sampleInstanceV2.getIndexingSchemeString();
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
                    throw new RuntimeException("Failed to find fastq map for " + fastQFile.getPath());
                }

                // Try normal sample name first, then converted if it contains special characters,
                // repeat with sample import if necessary
                List<FastQList> sampleFastQs = mapKeyToFastq.get(sampleKey);
                if (sampleFastQs == null) {
                    String ssKey = ReadGroupUtil.convertSampleKeyToSampleSheetId(sampleKey);
                    sampleFastQs = mapKeyToFastq.get(ssKey);
                    if (sampleFastQs == null) {
                        sampleFastQs = mapKeyToFastq.get(sampleImportKey);
                        if (sampleFastQs == null) {
                            ssKey = ReadGroupUtil.convertSampleKeyToSampleSheetId(sampleImportKey);
                            sampleFastQs = mapKeyToFastq.get(ssKey);
                        }
                    }
                }

                for (FastQList fq : sampleFastQs) {
                    fq.setRgSm(sampleKey);
                    fq.setRgLb(sampleInstanceV2.getSequencingLibraryName());
                    fq.setRgId(ReadGroupUtil.createRgId(runCartridge, sequencingRunChamber.getLaneNumber()));
                    fq.setRgPl("ILLUMINA");
                    fq.setRgPu(ReadGroupUtil.createRgPu(runCartridge.getLabel(), sequencingRunChamber.getLaneNumber(),
                            indexingSchemeString));
                    IlluminaFlowcell flowcell = OrmUtil.proxySafeCast(runCartridge, IlluminaFlowcell.class);
                    fq.setRgPm(flowcell.getFlowcellType().getSequencerModelShort());
                    fq.setRgCn("BI");
                }
                finalFastQList.addAll(sampleFastQs);
            }

            List<Task> activeTasks = aggregationState.getActiveTasks();
            for (Task task: activeTasks) {
                if (OrmUtil.proxySafeIsInstance(task, AggregationTask.class)) {
                    AggregationTask aggregationTask = OrmUtil.proxySafeCast(task, AggregationTask.class);
                    File fastQFile = aggregationTask.getFastQList();
                    if (!fastQFile.getParentFile().exists()) {
                        fastQFile.getParentFile().mkdir();
                    }
                    fastQListBuilder.buildAggregation(finalFastQList, mercurySample, fastQFile);
                    return true;
                }
            }

        } catch (Exception e) {
            logger.error("Failed to enter aggregation state: " + state.getStateId(), e);
            return false;
        }

        return false;
    }

    public static MercurySample findSampleImport(RunCartridge flowcell, VesselPosition lane, MercurySample sample ) {
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

    /**
     * Better if want to grab the list of expected flowcells dynamically. Most aggregations are created
     * by picking the expected run chambers.
     * @param state
     * @param mercurySample
     * @return
     */
    public boolean handleEnterDynamic(AggregationState state, MercurySample mercurySample) {
        try {
            Set<LabVessel> labVessels = mercurySample.getLabVessel();

            // PDO Sample Key but may require the export
            String sampleKey = mercurySample.getSampleKey();

            Set<FastQList> finalFastQList = new HashSet<>();
            Map<File, Map<Integer, Map<String, List<FastQList>>>> mapFileToFastqs = new HashMap<>();

            for (LabVessel labVessel: labVessels) {
                // TODO Why am I doing this? Just look at the run chambers?
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
                    // skip if this not in aggregation states run chamber list
                    boolean foundFlowcell = false;
                    for (IlluminaSequencingRunChamber runChamber: state.getSequencingRunChambers()) {
                        if (runChamber.getIlluminaSequencingRun().equals(illuminaSequencingRun)) {
                            foundFlowcell = true;
                        }
                    }

                    if (!foundFlowcell) {
                        continue;
                    }

                    // TODO This has nothing to do with the aggregation task
                    // Two tasks could exist on same, but with different fastqs
                    for (VesselPosition vesselPosition: labVesselAndPositions.getValue()) {
                        IlluminaSequencingRunChamber sequencingRunChamber =
                                illuminaSequencingRun.getSequencingRunChamber(vesselPosition);
                        MercurySample sampleImport = findSampleImport(flowcell, vesselPosition, mercurySample);
                        if (sampleImport == null) {
                            sampleImport = mercurySample;
                        }
                        String sampleImportKey = sampleImport.getSampleKey();
                        if (sequencingRunChamber != null) {
                            Optional<DemultiplexState> demultiplexStateOpt =
                                    sequencingRunChamber.getMostRecentCompleteStateOfType(DemultiplexState.class);
                            if (!demultiplexStateOpt.isPresent()) {
                                String msg = String.format("Waiting on lane: %s in %s to aggregate %s",
                                        vesselPosition.name(), flowcellBarcode, sampleKey);
                                logger.info(msg);
                                return false;
                            } else {
                                DemultiplexState demultiplexState = demultiplexStateOpt.get();
                                if (demultiplexState.isIgnored()) {
                                    continue; //Skip all ignored positions (orphan rate fail etc).
                                }
                                IlluminaSequencingRun run =
                                        demultiplexState.getRunForChamber(sequencingRunChamber);
                                DragenFolderUtil dragenFolderUtil = new DragenFolderUtil(dragenConfig, run,
                                        demultiplexState.getStateName());

                                Pair<MercurySample, SampleInstanceV2> instancePair = SampleSheetBuilder.
                                        findSampleInFlowcellLane(flowcell, sequencingRunChamber.getLanePosition(), mercurySample);
                                SampleInstanceV2 sampleInstanceV2 = instancePair.getRight();
                                String indexingSchemeString = sampleInstanceV2.getIndexingSchemeString();
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
                                    throw new RuntimeException("Failed to find fastq map for " + fastQFile.getPath());
                                }

                                // Try normal sample name first, then converted if it contains special characters,
                                // repeat with sample import if necessary
                                List<FastQList> sampleFastQs = mapKeyToFastq.get(sampleKey);
                                if (sampleFastQs == null) {
                                    String ssKey = ReadGroupUtil.convertSampleKeyToSampleSheetId(sampleKey);
                                    sampleFastQs = mapKeyToFastq.get(ssKey);
                                    if (sampleFastQs == null) {
                                        sampleFastQs = mapKeyToFastq.get(sampleImportKey);
                                        if (sampleFastQs == null) {
                                            ssKey = ReadGroupUtil.convertSampleKeyToSampleSheetId(sampleImportKey);
                                            sampleFastQs = mapKeyToFastq.get(ssKey);
                                        }
                                    }
                                }

                                for (FastQList fq: sampleFastQs) {
                                    fq.setRgSm(sampleKey);
                                    fq.setRgLb(sampleInstanceV2.getSequencingLibraryName());
                                    fq.setRgId(ReadGroupUtil.createRgId(flowcell, sequencingRunChamber.getLaneNumber()));
                                    fq.setRgPl("ILLUMINA");
                                    fq.setRgPu(ReadGroupUtil.createRgPu(flowcellBarcode, sequencingRunChamber.getLaneNumber(), indexingSchemeString));
                                    fq.setRgPm(flowcell.getFlowcellType().getSequencerModelShort());
                                    fq.setRgCn("BI");
                                }
                                finalFastQList.addAll(sampleFastQs);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to enter aggregation state: " + state.getStateId(), e);
        }

        return false;
    }
}
