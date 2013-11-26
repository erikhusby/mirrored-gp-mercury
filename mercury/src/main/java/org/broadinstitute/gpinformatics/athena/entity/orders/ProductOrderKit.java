package org.broadinstitute.gpinformatics.athena.entity.orders;

import net.sourceforge.stripes.action.StreamingResolution;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.sample.MaterialInfo;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.BspGroupCollectionTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGroupCollectionList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Collection;

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
    private String materialBspName;

    @Column(name="notifications")
    private String notifications;

    // Required by JPA and used when creating new pdo.
    public ProductOrderKit() {
    }

    public ProductOrderKit(Long numberOfSamples, KitType kitType, long sampleCollectionId, Long organismId,
                           long siteId, MaterialInfo materialInfo, String notifications) {
        this.numberOfSamples = numberOfSamples;
        this.kitTypeName = kitType != null ? kitType.getKitName() : null;
        this.sampleCollectionId = sampleCollectionId;
        this.organismId = organismId;
        this.siteId = siteId;
        this.materialBspName = materialInfo != null ? materialInfo.getBspName() : null;
        this.notifications = notifications;
    }

    public ProductOrderKit(Long numberOfSamples, KitType kitType, long sampleCollectionId, Long organismId,
                           long siteId, String materialInfoString, String notifications) {
        this.numberOfSamples = numberOfSamples;
        this.kitTypeName = kitType != null ? kitType.getKitName() : null;
        this.sampleCollectionId = sampleCollectionId;
        this.organismId = organismId;
        this.siteId = siteId;
        this.materialBspName = materialInfoString;
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

    public void setSampleCollectionId(Long sampleCollectionId) {
        this.sampleCollectionId = sampleCollectionId;
    }

    public Long getOrganismId() {
        return organismId;
    }

    public void setOrganismId(Long organismId) {
        this.organismId = organismId;
    }

    public String getOrganismName() {
        //need method from bspclient to get organism by id
/*        BspGroupCollectionTokenInput bspGroupCollectionTokenInput = new BspGroupCollectionTokenInput();
        SampleCollection sampleCollection = bspGroupCollectionTokenInput.getTokenObject();

        JSONObject collectionAndOrganismsList = new JSONObject();
        if (sampleCollection != null) {
            Collection<Pair<Long, String>> organisms = sampleCollection.getOrganisms();

            collectionAndOrganismsList.put("collectionName", sampleCollection.getCollectionName());

            // Create the json array of items for the chunk
            JSONArray itemList = new JSONArray();
            collectionAndOrganismsList.put("organisms", itemList);

            for (Pair<Long, String> organism : organisms) {
                JSONObject item = new JSONObject();
                item.put("id", organism.getLeft());
                item.put("name", organism.getRight());

                itemList.put(item);
            }
        }

        return new StreamingResolution("text", new StringReader(collectionAndOrganismsList.toString()));*/
        return "";
    }

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(long siteId) {
        this.siteId = siteId;
    }

    public String getSiteName() {
        //tried getting siteName but the following collection is empty.
        BSPGroupCollectionList bspGroupCollectionList = new BSPGroupCollectionList();
        return bspGroupCollectionList.getById(siteId).getCollectionName();
    }

    public MaterialInfo getMaterialInfo() {
        return new MaterialInfo(kitTypeName, materialBspName);
    }

    public void setMaterialInfo(MaterialInfo materialInfo) {
        materialBspName = materialInfo != null ? materialInfo.getBspName() : null;
    }

    public String getMaterialBspName() {
        return materialBspName;
    }

    public void setMaterialBspName(String materialInfo) {
        this.materialBspName = materialInfo;
    }

    public String getNotifications() {
        return notifications;
    }

    public void setNotifications(String notifications) {
        this.notifications = notifications;
    }
}
