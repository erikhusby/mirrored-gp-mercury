package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    @Column(name="PRODUCT_ORDER_KIT_ID")
    private Long productOrderKitId;

    @Column(name = "sample_collection_id")
    private Long sampleCollectionId;

    @Column(name = "site_id")
    private Long siteId;


    @OneToMany(mappedBy = "productOrderKit", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true)
    @BatchSize(size = 500)
    private final Set<ProductOrderKitPerson> notificationPeople = new HashSet<>();


    @OneToMany(mappedBy = "productOrderKit", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private Set<ProductOrderKitDetail> kitOrderDetails = new HashSet<> ();

    @Column(name = "work_request_id")
    private String workRequestId;

    @Column(name = "comments")
    private String comments;

    @Column(name = "exome_express")
    private Boolean exomeExpress;

    @Enumerated(EnumType.STRING)
    private SampleKitWorkRequest.TransferMethod transferMethod;

    @Transient
    private String siteName;

    @Transient
    private String sampleCollectionName;

    // Required by JPA and used when creating new pdo.
    public ProductOrderKit() {
    }

    /**
     * This is used by the web service call that sets up work requests (in ProductOrderResource).
     */
    public ProductOrderKit(SampleCollection sampleCollection, Site site,
                           List<ProductOrderKitDetail> kitDetails, boolean exomeExpress) {
        sampleCollectionId = sampleCollection.getCollectionId();
        sampleCollectionName = sampleCollection.getCollectionName();
        siteId = site.getId();
        siteName = site.getName();
        kitOrderDetails.addAll(kitDetails);
        this.exomeExpress = exomeExpress;
        for (ProductOrderKitDetail kitDetail : kitDetails) {
            kitDetail.setProductOrderKit(this);
        }
    }

    // Only used by tests.
    public ProductOrderKit(Long sampleCollectionId, Long siteId) {
        this.sampleCollectionId = sampleCollectionId;
        this.siteId = siteId;
    }

    public Long getProductOrderKitId() {
        return productOrderKitId;
    }

    public void setProductOrderKitId(Long productOrderKitId) {
        this.productOrderKitId = productOrderKitId;
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

    public Long[] getNotificationIds() {
        Long[] ids = new Long[notificationPeople.size()];

        int i = 0;
        for (ProductOrderKitPerson person : notificationPeople) {
            ids[i++] = person.getPersonId();
        }

        return ids;
    }

    public void setNotificationIds(List<String> ids) {
        notificationPeople.clear();
        for (String id : ids) {
            notificationPeople.add(new ProductOrderKitPerson(this, Long.valueOf(id)));
        }
    }

    public void setWorkRequestId(String workRequestId) {
        this.workRequestId = workRequestId;
    }

    public String getWorkRequestId() {
        return workRequestId;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public boolean isExomeExpress() {

        if(exomeExpress == null) {
            exomeExpress = false;
        }

        return exomeExpress;
    }

    public void setExomeExpress(boolean exomeExpress) {
        this.exomeExpress = exomeExpress;
    }

    public void setTransferMethod(SampleKitWorkRequest.TransferMethod transferMethod) {
        this.transferMethod = transferMethod;
    }

    public SampleKitWorkRequest.TransferMethod getTransferMethod() {
        return transferMethod;
    }

    public Set<ProductOrderKitDetail> getKitOrderDetails() {
        return kitOrderDetails;
    }

    public void setKitOrderDetails(Set<ProductOrderKitDetail> kitOrderDetails) {
        this.kitOrderDetails.clear();
        this.kitOrderDetails.addAll(kitOrderDetails);
    }

    public void addKitOrderDetail(ProductOrderKitDetail kitDetail) {
        kitDetail.setProductOrderKit(this);
        kitOrderDetails.add(kitDetail);
    }
}
