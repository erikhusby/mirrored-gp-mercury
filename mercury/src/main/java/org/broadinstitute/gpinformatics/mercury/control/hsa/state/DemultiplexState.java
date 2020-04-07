package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexTask;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Audited
public class DemultiplexState extends State {

    public DemultiplexState() {

    }

    public DemultiplexState(String name, FiniteStateMachine finiteStateMachine,
                            Set<MercurySample> mercurySamples, Set<IlluminaSequencingRunChamber> sequencingRunChambers) {
        super(name, finiteStateMachine, mercurySamples, sequencingRunChambers);
    }

    @Override
    public Optional<Task> getExitTask() {
        return super.getExitTask();
    }

    public Set<DemultiplexTask> getDemultiplexTasks() {
        return getTasks().stream()
                .filter(t -> OrmUtil.proxySafeIsInstance(t, DemultiplexTask.class))
                .map(t -> OrmUtil.proxySafeCast(t, DemultiplexTask.class))
                .collect(Collectors.toSet());
    }

    public IlluminaSequencingRun getRunForChamber(IlluminaSequencingRunChamber sequencingRunChamber) {
        for (IlluminaSequencingRunChamber runChamber: getSequencingRunChambers()) {
            if (runChamber.equals(sequencingRunChamber)) {
                return runChamber.getIlluminaSequencingRun();
            }
        }
        return null;
    }

    public boolean isIgnored() {
        return getTasks().stream()
                .filter(t -> OrmUtil.proxySafeIsInstance(t, DemultiplexTask.class))
                .map(t -> OrmUtil.proxySafeCast(t, DemultiplexTask.class))
                .anyMatch(t -> t.getStatus() == Status.IGNORE);
    }
}
