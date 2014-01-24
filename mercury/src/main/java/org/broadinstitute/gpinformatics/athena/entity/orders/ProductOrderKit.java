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

    @Column(name = "number_samples")
    private Long numberOfSamples;

    @Enumerated(EnumType.STRING)
    @Column(name = "kit_type")
    private KitType kitType;

    @Column(name = "sample_collection_id")
    private Long sampleCollectionId;

    @Column(name = "organism_id")
    private Long organismId;

    @Column(name = "site_id")
    private Long siteId;

    @Column(name = "material_bsp_name")
    private String bspMaterialName;

    @OneToMany(mappedBy = "productOrderKit", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true)
    @BatchSize(size = 500)
    private final Set<ProductOrderKitPerson> notificationPeople = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(schema = "athena", name="PDO_KIT_POST_RECEIVE_OPT", joinColumns = {@JoinColumn(name="PRODUCT_ORDER_KIT_ID")})
    private final Set<PostReceiveOption> postReceiveOptions = new HashSet<>();

    @Column(name = "work_request_id")
    private String workRequestId;

    @Column(name = "comments")
    private String comments;

    @Column(name = "exome_express")
    private Boolean exomeExpress;

    @Enumerated(EnumType.STRING)
    private SampleKitWorkRequest.TransferMethod transferMethod;

    @Transient
    private String organismName;

    @Transient
    private String siteName;

    @Transient
    private String sampleCollectionName;

    // Required by JPA and used when creating new pdo.
    public ProductOrderKit() {
    }

    // Only used by tests.
    public ProductOrderKit(Long numberOfSamples, KitType kitType, Long sampleCollectionId, Long organismId, Long siteId,
                           MaterialInfoDto MaterialInfoDto) {
        this.numberOfSamples = numberOfSamples;
        this.kitType = kitType;
        this.sampleCollectionId = sampleCollectionId;
        this.organismId = organismId;
        this.siteId = siteId;
        setMaterialInfo(MaterialInfoDto);
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

    public MaterialInfoDto getMaterialInfo() {
        return new MaterialInfoDto(kitType.getKitName(), bspMaterialName);
    }

    public void setMaterialInfo(MaterialInfoDto MaterialInfoDto) {
        bspMaterialName = MaterialInfoDto != null ? MaterialInfoDto.getBspName() : null;
    }

    public String getBspMaterialName() {
        return bspMaterialName;
    }

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

    public Set<PostReceiveOption> getPostReceiveOptions() {
        return postReceiveOptions;
    }

    /**
     * Return a string representation of this kit's PostReceive options
     *
     * @param delimiter characters used to join list values
     */
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
}
