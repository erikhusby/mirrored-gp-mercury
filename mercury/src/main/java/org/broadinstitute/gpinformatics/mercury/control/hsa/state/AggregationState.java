package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentTask;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Alignments for a given a Mercury Sample. Tasks are required to be actually completed AlignmentTasks
 * so when the aggregation cram is built the paths to the fastq's can be found.
 */
@Entity
@Audited
public class AggregationState extends State {

    public AggregationState() {
    }

    public AggregationState(String stateName, FiniteStateMachine finiteStateMachine, Set<MercurySample> mercurySamples,
                            Set<IlluminaSequencingRunChamber> sequencingRunChambers) {
        super(stateName, finiteStateMachine, mercurySamples, sequencingRunChambers);
    }

    public Optional<Task> getAggregationTask() {
        return getTasks().stream()
                .filter(t -> OrmUtil.proxySafeIsInstance(t, AggregationTask.class))
                .findFirst();
    }

    public Set<AlignmentTask> getAlignmentTasks() {
        return getTasks().stream()
                .filter(t -> OrmUtil.proxySafeIsInstance(t, AlignmentTask.class))
                .map(t -> OrmUtil.proxySafeCast(t, AlignmentTask.class))
                .collect(Collectors.toSet());
    }

    @Override
    public List<Task> getActiveTasks() {
        Optional<Task> aggregationTask = getAggregationTask();
        if (aggregationTask.isPresent()) {
            return Collections.singletonList(aggregationTask.get());
        }

        return Collections.emptyList();
    }
}
