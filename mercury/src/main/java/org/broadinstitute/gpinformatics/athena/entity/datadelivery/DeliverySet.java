package org.broadinstitute.gpinformatics.athena.entity.datadelivery;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * A set of samples for delivery, e.g. VCFs to Google Bucket.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class DeliverySet {

    @SequenceGenerator(name = "SEQ_DELIVERY_SET", schema = "mercury", sequenceName = "SEQ_DELIVERY_SET")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DELIVERY_SET")
    @Id
    private Long deliverySetId;

    private String name;

    @OneToMany(mappedBy = "deliverySet")
    private Set<DeliveryItem> deliveryItems = new HashSet<>();
    // Caching to improve performance?
    // In transfer visualizer, visiting events and vessels is fast; why are sample instances slow?  Profile and diff

    // Is bucket a property of Set or Item?
    private String bucketName;
}
