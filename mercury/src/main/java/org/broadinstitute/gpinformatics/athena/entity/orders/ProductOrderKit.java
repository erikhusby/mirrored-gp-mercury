package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.bsp.client.sample.MaterialInfo;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Represents the Kit info which is associated with a Sample Initiation PDO.
 */
@Entity
@Audited
@Table(name = "PRODUCT_ORDER_KIT", schema = "athena")
public class ProductOrderKit implements Serializable {
    private static final long serialVersionUID = 8645451167948826402L;

    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT_ORDER_KIT", schema = "athena", sequenceName = "SEQ_PRODUCT_ORDER_KIT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT_ORDER_KIT")
    @Column(name="product_order_kit_id", unique=true, nullable=false)
    private Long productOrderKitId;

    @Column(name="number_samples")
    private Long numberOfSamples;

    @Column(name="kit_type")
    private KitType kitType;

    @Column(name="sample_collection_id")
    private Long sampleCollectionId;

    @Column(name="organism_id")
    private Long organismId;

    @Column(name="shipping_location")
    private String shippingLocation;

    @Column(name="material")
    private String materialInfo;

    @Column(name="notifications")
    private String notifications;

    // Required by JPA.
    @SuppressWarnings("UnusedDeclaration")
    protected ProductOrderKit() {
    }

    public ProductOrderKit(Long numberOfSamples, KitType kitType, long sampleCollectionId, Long organismId,
                           long siteId, MaterialInfo materialInfo, String notifications) {
        this.numberOfSamples = numberOfSamples;
        this.kitType = kitType;
        this.sampleCollectionId = sampleCollectionId;
        this.organismId = organismId;
        this.shippingLocation = shippingLocation;
//        this.materialInfo = materialInfo;
        this.notifications = notifications;
    }

    public Long getProductOrderKitId() {
        return productOrderKitId;
    }

    public void setProductOrderKitId(Long productOrderKitId) {
        this.productOrderKitId = productOrderKitId;
    }

    public Long getNumberOfSamples() {
        return numberOfSamples;
    }

    public void setNumberOfSamples(Long numberOfSamples) {
        this.numberOfSamples = numberOfSamples;
    }

    public KitType getKitType() {
        return kitType;
    }

    public void setKitType(KitType kitType) {
        this.kitType = kitType;
    }

    public Long getSampleCollectionId() {
        return sampleCollectionId;
    }

    public void setSampleCollectionId(Long sampleCollectionId) {
        this.sampleCollectionId = sampleCollectionId;
    }

    public Long getOrganismId() {
        return organismId;
    }

    public void setOrganismId(Long organismId) {
        this.organismId = organismId;
    }

    public String getShippingLocation() {
        return shippingLocation;
    }

    public void setShippingLocation(String shippingLocation) {
        this.shippingLocation = shippingLocation;
    }

    public String getMaterialInfo() {
        return materialInfo;
    }

    public void setMaterialInfo(String materialInfo) {
        this.materialInfo = materialInfo;
    }

    public String getNotifications() {
        return notifications;
    }

    public void setNotifications(String notifications) {
        this.notifications = notifications;
    }
}
