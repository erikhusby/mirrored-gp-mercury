package org.broadinstitute.gpinformatics.athena.entity.orders;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
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
    @Column(name="product_order_kit_id", unique=true, nullable=false)
    private Long productOrderKitId;

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Column(name = "number_samples")
    @Deprecated
    private Long numberOfSamples;

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Enumerated(EnumType.STRING)
    @Column(name = "kit_type")
    @Deprecated
    private KitType kitType;

    @Column(name = "sample_collection_id")
    private Long sampleCollectionId;

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Column(name = "organism_id")
    @Deprecated
    private Long organismId;

    @Column(name = "site_id")
    private Long siteId;

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Column(name = "material_bsp_name")
    @Deprecated
    private String bspMaterialName;

    @OneToMany(mappedBy = "productOrderKit", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true)
    @BatchSize(size = 500)
    private final Set<ProductOrderKitPerson> notificationPeople = new HashSet<>();

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(schema = "athena", name="PDO_KIT_POST_RECEIVE_OPT", joinColumns = {@JoinColumn(name="PRODUCT_ORDER_KIT_ID")})
    @Deprecated
    private final Set<PostReceiveOption> postReceiveOptions = new HashSet<>();

    @OneToMany(mappedBy = "productOrderKit", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private Set<ProductOrderKitDetail> kitOrderDetails = new HashSet();

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

    // Only used by tests.
    public ProductOrderKit(Long sampleCollectionId, Long siteId) {
        this.sampleCollectionId = sampleCollectionId;
        this.siteId = siteId;
    }

//    // Only used by tests.
//    @Deprecated //TODO SGM:  REMOVE:  moving to product order kit detail
//    public ProductOrderKit(Long numberOfSamples, KitType kitType, Long sampleCollectionId, Long organismId, Long siteId,
//                           MaterialInfoDto MaterialInfoDto) {
//        this.numberOfSamples = numberOfSamples;
//        this.kitType = kitType;
//        this.sampleCollectionId = sampleCollectionId;
//        this.organismId = organismId;
//        this.siteId = siteId;
//        setMaterialInfo(MaterialInfoDto);
//    }
//

    public Long getProductOrderKitId() {
        return productOrderKitId;
    }

    public void setProductOrderKitId(Long productOrderKitId) {
        this.productOrderKitId = productOrderKitId;
    }

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Deprecated
    public Long getNumberOfSamples() {
        return numberOfSamples;
    }

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Deprecated
    public void setNumberOfSamples(Long numberOfSamples) {
        this.numberOfSamples = numberOfSamples;
    }

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Deprecated
    public KitType getKitType() {
        return kitType;
    }

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Deprecated
    public void setKitType(KitType kitType) {
        this.kitType = kitType;
    }

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Deprecated
    public String getKitTypeDisplayName() {
        if (getKitType() == null) {
            return null;
        }

        return getKitType().getDisplayName();
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

    @Deprecated
    public Long getOrganismId() {
        return organismId;
    }

    @Deprecated
    public void setOrganismId(Long organismId) {
        this.organismId = organismId;
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

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Deprecated
    public MaterialInfoDto getMaterialInfo() {
        return new MaterialInfoDto(kitType.getKitName(), bspMaterialName);
    }

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Deprecated
    public void setMaterialInfo(MaterialInfoDto MaterialInfoDto) {
        bspMaterialName = MaterialInfoDto != null ? MaterialInfoDto.getBspName() : null;
    }

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Deprecated
    public String getBspMaterialName() {
        return bspMaterialName;
    }

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Deprecated
    public void setBspMaterialName(String bspMaterialName) {
        this.bspMaterialName = bspMaterialName;
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

    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Deprecated
    public Set<PostReceiveOption> getPostReceiveOptions() {
        return postReceiveOptions;
    }

    /**
     * Return a string representation of this kit's PostReceive options
     *
     * @param delimiter characters used to join list values
     */
    // TODO SGM:  REMOVE:  This field is moving to product order kit detail.  Will be removed after the next release
    // since a fixup test is required to convert existing product order kit information to use productOrderKitDetail
    // entities
    @Deprecated
    public String getPostReceivedOptionsAsString(@Nonnull String delimiter) {
        if (getPostReceiveOptions().isEmpty()){
            return "No Post-Received Options selected.";
        }

        List<String> options=new ArrayList<>(postReceiveOptions.size());
        for (PostReceiveOption postReceiveOption :postReceiveOptions) {
            options.add(postReceiveOption.getText());
        }

        return StringUtils.join(options, delimiter);
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
