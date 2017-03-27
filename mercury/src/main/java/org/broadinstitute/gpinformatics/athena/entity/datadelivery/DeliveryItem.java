package org.broadinstitute.gpinformatics.athena.entity.datadelivery;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
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

    @SequenceGenerator(name = "SEQ_DELIVERY_ITEM", schema = "mercury", sequenceName = "SEQ_DELIVERY_ITEM")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DELIVERY_ITEM")
    @Id
    private Long deliveryItemId;

    /** Pond or chip */
    @ManyToOne
    private LabVessel labVessel; // todo jmt what if the sample hasn't progressed that far yet?

    /** chip well */
    @Enumerated(EnumType.STRING)
    private VesselPosition vesselPosition;

    /** location of metrics */
    @Enumerated(EnumType.STRING)
    private MetricsType metricsType;

    /** ID in metrics table */
    private Long metricsId;

    @ManyToOne
    private DeliverySet deliverySet;
}
