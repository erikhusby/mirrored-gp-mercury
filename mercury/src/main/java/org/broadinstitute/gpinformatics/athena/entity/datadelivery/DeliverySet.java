package org.broadinstitute.gpinformatics.athena.entity.datadelivery;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * A set of samples for delivery, e.g. to Google Bucket.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class DeliverySet {
    private String name;
    private Set<DeliveryItem> deliveryItems = new HashSet<>();
    // Caching to improve performance?
    // In transfer visualizer, visiting events and vessels is fast; why are sample instances slow?  Profile and diff
    // Is bucket a property of Set or Item?
}
