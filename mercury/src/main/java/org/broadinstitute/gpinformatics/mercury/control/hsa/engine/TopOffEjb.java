package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationWithRollbackException;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.GenericState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.PoolGroupState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.TopOffStateMachineDecorator;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Stateful
@RequestScoped
public class TopOffEjb {

    private static final Log log = LogFactory.getLog(TopOffEjb.class);

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private StateMachineDao stateMachineDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    public TopOffStateMachineDecorator findOrCreateTopoffStateMachine() {
        FiniteStateMachine fsm = stateMachineDao.findByIdentifier(TopOffStateMachineDecorator.NAME);
        if (fsm == null) {
            fsm = new FiniteStateMachine();
            fsm.setStateMachineName(TopOffStateMachineDecorator.NAME);
            fsm.setStatus(Status.TRIAGE);

            List<State> states = new ArrayList<>();
            for (TopOffStateMachineDecorator.StateNames stateName: TopOffStateMachineDecorator.StateNames.values()) {
                if (stateName.isCreateState()) {
                    State state = new GenericState(stateName.name(), fsm, Collections.emptySet());
                    states.add(state);
                }
            }

            fsm.setStates(states);
        }
        return new TopOffStateMachineDecorator(fsm);
    }

    public void updateTopOffStateMachine(String seqType, List<String> pdoSamples) {
        TopOffStateMachineDecorator topOffStateMachine = findOrCreateTopoffStateMachine();
        TopOffStateMachineDecorator.StateNames stateName =
                TopOffStateMachineDecorator.StateNames.getStateByName(seqType);
        State stateToUpdate = topOffStateMachine.getStateByName(stateName);
        List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKeys(pdoSamples);
        mercurySamples.forEach(stateToUpdate::addSample);
        stateMachineDao.persist(topOffStateMachine.getFiniteStateMachine());
    }

    public State createPoolGroupAndDrainState(List<String> selectedSamples, String username,
                                              TopOffStateMachineDecorator.StateNames stateNames) {
        TopOffStateMachineDecorator topOffStateMachine = findOrCreateTopoffStateMachine();
        String dateNow = DateUtils.getDateTime(new Date());
        List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKeys(selectedSamples);

        PoolGroupState poolGroup = new PoolGroupState(username + "_" + dateNow, topOffStateMachine.getFiniteStateMachine(),
                new HashSet<>(mercurySamples));
        topOffStateMachine.getFiniteStateMachine().getStates().add(poolGroup);

        State drainState = topOffStateMachine.getStateByName(stateNames);
        drainState.getMercurySamples().removeAll(mercurySamples);

        stateMachineDao.persist(topOffStateMachine.getFiniteStateMachine());
        return poolGroup;
    }

    public void drainPools(Map<String, List<String>> mapSeqTypeToSample) {
        TopOffStateMachineDecorator topOffStateMachine = findOrCreateTopoffStateMachine();
        for (Map.Entry<String, List<String>> entry: mapSeqTypeToSample.entrySet()) {
            List<String> samples = entry.getValue();
            Pair<Set<MercurySample>, Set<State>> pair = drainPoolGroups(samples);
            Set<MercurySample> mercurySamples = pair.getKey();

            TopOffStateMachineDecorator.StateNames toStateName = TopOffStateMachineDecorator.StateNames.getStateByName(
                    entry.getKey());
            State toState = topOffStateMachine.getStateByName(toStateName);
            toState.getMercurySamples().addAll(mercurySamples);
        }
    }

    public Pair<Set<MercurySample>, Set<State>> drainPoolGroups(List<String> samples) {
        TopOffStateMachineDecorator topOffStateMachine = findOrCreateTopoffStateMachine();
        Set<State> statesToDelete = new HashSet<>();
        Set<MercurySample> mercurySamples = new HashSet<>();

        for (State state : topOffStateMachine.getPoolGroups()) {
            for (MercurySample mercurySample : state.getMercurySamples()) {
                if (samples.contains(mercurySample.getSampleKey())) {
                    mercurySamples.add(mercurySample);
                }
            }
            state.getMercurySamples().removeAll(mercurySamples);
            if (state.getMercurySamples().isEmpty()) {
                statesToDelete.add(state);
            }
        }

        for (State state : statesToDelete) {
            topOffStateMachine.getFiniteStateMachine().removeState(state);
        }
        return Pair.of(mercurySamples, statesToDelete);
    }

    public IlluminaFlowcell.FlowcellType getLatestFlowcellForSample(MercurySample mercurySample) {
        Optional<AggregationState> aggregationOpt =
                mercurySample.getMostRecentStateOfType(AggregationState.class);
        if (aggregationOpt.isPresent()) {
            AggregationState state = aggregationOpt.get();
            IlluminaSequencingRunChamber runChamber = state.getSequencingRunChambers().iterator().next();
            String fcBarcode = runChamber.getIlluminaSequencingRun().getSampleCartridge().getLabel();
            return IlluminaFlowcell.FlowcellType.getTypeForBarcode(fcBarcode);
        }
        return null;
    }

    public LabBatch createBatchToState(List<Long> bucketEntryIds, String bucketName, MessageReporter messageReporter,
                                       String username, List<String> selectedSamples, String seqType,
                                       String selectedWorkflow, String summary) throws ValidationException {
        LabBatch batch = labBatchEjb.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW, selectedWorkflow,
                bucketEntryIds, Collections.emptyList(), summary.trim(), null, null, "",
                username, bucketName, messageReporter,
                Collections.emptyList());

        updateTopOffStateMachine(seqType, selectedSamples);
        return null;
    }

    public void updateBatchToState(String batchName, List<Long> bucketEntryIds, String bucketName,
                                   MessageReporter messageReporter, String seqType, List<String> selectedSamples)
            throws IOException, ValidationException {
        labBatchEjb.updateLabBatch(batchName, Collections.emptyList(), bucketEntryIds,
                Collections.emptyList(),
                bucketName, messageReporter, Collections.emptyList());
        updateTopOffStateMachine(seqType, selectedSamples);
    }
}
