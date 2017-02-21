package org.broadinstitute.gpinformatics.athena.entity.datadelivery;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Represents an item in a Delivery Set
 */
@Entity
@Audited
@Table(schema = "mercury")
public class DeliveryItem {

    enum MetricsType {
        /** METRICS.AGGREGATION.ID */
        ON_PREM_SEQ,
        /** ANALYTICS.ARRAYS_QC.ID */
        CLOUD_ARRAYS
    }

    /** Pond or chip */
    private LabVessel labVessel; // todo jmt what if the sample hasn't progressed that far yet?
    /** chip well */
    private VesselPosition vesselPosition;
    /** location of metrics */
    private MetricsType metricsType;
    /** ID in metrics table */
    private Long metricsId;
}
