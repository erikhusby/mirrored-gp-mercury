package org.broadinstitute.gpinformatics.mercury.entity.vessel;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.Failure;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    public void addFailure(Failure failureMode) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<Failure> getFailures() {
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
        return Collections.emptySet();
    }

    @Override
    public Set<LabEvent> getTransfersTo() {
        return Collections.emptySet();
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return VesselGeometry.TUBE;
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
