package org.broadinstitute.gpinformatics.mercury.entity.vessel;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
@Entity
@Audited
@Table(schema = "mercury")
/**
 * Represents a tube with a two dimensional barcode on its bottom.  These tubes are usually stored in racks.
 */
public class TwoDBarcodedTube extends LabVessel {

    private static Log gLog = LogFactory.getLog(TwoDBarcodedTube.class);
    
    public TwoDBarcodedTube(String twoDBarcode) {
        super(twoDBarcode);
        if (twoDBarcode == null) {
            throw new IllegalArgumentException("twoDBarcode must be non-null in TwoDBarcodedTube.TwoDBarcodedTube");
        }
    }

    protected TwoDBarcodedTube() {
    }

    @Override
    public void addMetric(LabMetric m) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabMetric> getMetrics() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public LabMetric getMetric(LabMetric.MetricName metricName, MetricSearchMode searchMode, SampleInstance sampleInstance) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean isAncestor(LabVessel progeny) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<LabEvent> getTransfersFrom() {
        Set<LabEvent> transfersFrom = new HashSet<LabEvent>();
        for (VesselContainer<?> vesselContainer : getContainers()) {
            transfersFrom.addAll(vesselContainer.getTransfersFrom());
        }
        return transfersFrom;
    }

    @Override
    public Set<LabEvent> getTransfersTo() {
        Set<LabEvent> transfersTo = new HashSet<LabEvent>();
        for (VesselContainer<?> vesselContainer : getContainers()) {
            transfersTo.addAll(vesselContainer.getTransfersTo());
        }
        return transfersTo;
    }

    @Override
    public Set<LabEvent> getInPlaceEvents() {
        Set<LabEvent> inPlaceEvents = new HashSet<LabEvent>();
        for (VesselContainer<?> vesselContainer : getContainers()) {
            // todo jmt fix this
//            inPlaceEvents.addAll(vesselContainer.getTransfersTo());
        }
        return inPlaceEvents;
    }

    @Override
    public CONTAINER_TYPE getType() {
        return CONTAINER_TYPE.TUBE;
    }

    @Override
    public boolean isProgeny(LabVessel ancestor) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public LabVessel getContainingVessel() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Float getVolume() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Float getConcentration() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
