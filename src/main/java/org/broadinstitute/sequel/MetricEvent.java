package org.broadinstitute.sequel;

import java.util.Collection;

/**
 * When someone applies a metric like a quant
 * measurement, a volume measurement, etc.
 * we use this class.
 */
public class MetricEvent extends AbstractLabEvent {

    public MetricEvent(LabMetric metric,LabVessel container) {
        container.addMetric(metric);
    }

    @Override
    public LabEventName getEventName() {
        return LabEventName.METRIC_APPLIED;
    }

    @Override
    public boolean isBillable() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<SampleSheet> getAllSampleSheets() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void applyMolecularStateChanges() throws InvalidMolecularStateException {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void validateSourceMolecularState() throws InvalidMolecularStateException {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void validateTargetMolecularState() throws InvalidMolecularStateException {
        throw new RuntimeException("I haven't been written yet.");
    }
}
