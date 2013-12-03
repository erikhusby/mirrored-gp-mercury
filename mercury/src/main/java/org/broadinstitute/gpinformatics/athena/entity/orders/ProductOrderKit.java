package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.bsp.client.sample.MaterialInfo;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
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
    private String kitTypeName;

    @Column(name="sample_collection_id")
    private Long sampleCollectionId;

    @Column(name="organism_id")
    private Long organismId;

    @Column(name="site_id")
    private Long siteId;

    @Column(name="material_bsp_name")
    private String bspMaterialName;

    // notifications consists of Long values (bsp user ids) delimited by UserTokenInput.STRING_FORMAT_DELIMITER.
    @Column(name="notifications")
    private String notifications;

    @Transient
    private String organismName;

    @Transient
    private String siteName;

    @Transient
    private String sampleCollectionName;

    // Required by JPA and used when creating new pdo.
    public ProductOrderKit() {
    }

    public ProductOrderKit(Long numberOfSamples, KitType kitType, Long sampleCollectionId, Long organismId,
                           Long siteId, MaterialInfo materialInfo, String notifications) {
        this.numberOfSamples = numberOfSamples;
        this.kitTypeName = kitType != null ? kitType.getKitName() : null;
        this.sampleCollectionId = sampleCollectionId;
        this.organismId = organismId;
        this.siteId = siteId;
        this.bspMaterialName = materialInfo != null ? materialInfo.getBspName() : null;
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
        return KitType.valueOf(kitTypeName);
    }

    public void setKitType(KitType kitType) {
        kitTypeName = kitType != null ? kitType.getKitName() : null;
    }

    public String getKitTypeName() {
        return kitTypeName;
    }

    public void setKitTypeName(String kitTypeName) {
        this.kitTypeName = kitTypeName;
    }

    public Long getSampleCollectionId() {
        return sampleCollectionId;
    }

    public String getSampleCollectionName() {
        return sampleCollectionName;
    }

    public void setSampleCollectionId(Long sampleCollectionId) {
        this.sampleCollectionId = sampleCollectionId;
    }

    public void setSampleCollectionName(String s) {
        sampleCollectionName = s;
    }

    public Long getOrganismId() {
        return organismId;
    }

    public void setOrganismId(Long organismId) {
        this.organismId = organismId;
    }

    public void setOrganismName(String s) {
        organismName = s;
    }

    /** This is only populated after actionBean.populateTokenListsFromObjectData() is run. */
    public String getOrganismName() {
        return organismName;
    }

    public Long getSiteId() {
        return siteId;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public void setSiteName(String s) {
        siteName = s;
    }

    public MaterialInfo getMaterialInfo() {
        return new MaterialInfo(kitTypeName, bspMaterialName);
    }

    public void setMaterialInfo(MaterialInfo materialInfo) {
        bspMaterialName = materialInfo != null ? materialInfo.getBspName() : null;
    }

    public String getBspMaterialName() {
        return bspMaterialName;
    }

    public void setBspMaterialName(String materialInfo) {
        this.bspMaterialName = materialInfo;
    }

    public String getNotifications() {
        return notifications;
    }

    public void setNotifications(String notifications) {
        this.notifications = notifications;
    }

    /**
     * This turns the stored list of notification ids into the appropriate long array. Since the
     * notification member is storing the ids as a token separate list, use the token input object to
     * split this apart.
     *
     * @return The array of longs.
     */
    public Long[] getNotificationIds() {
        String[] keys = notifications.split(UserTokenInput.STRING_FORMAT_DELIMITER);
        Long[] notificationIds = new Long[keys.length];
        int i = 0;
        for (String key : keys) {
            notificationIds[i++] = Long.valueOf(key);
        }

        return notificationIds;
    }
}
