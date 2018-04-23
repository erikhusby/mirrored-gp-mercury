package org.broadinstitute.gpinformatics.athena.entity.orders;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.hibernate.envers.Audited;

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
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Defines the specific details for a sample kit contained in a sample initiation order.  This information is isolated
 * to its own entity since a sample initiation order can have multiple kits defined for it.
 */
@Entity
@Audited
@Table(name = "PRODUCT_ORDER_KIT_DETAIL", schema = "athena")
public class ProductOrderKitDetail implements Serializable {

    private static final long serialVersionUID = 154655454790873331L;
    public static final String NO_POST_RECEIVED_OPTIONS_SELECTED = "No Post-Received Options selected.";

    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT_ORDER_KIT_DETAIL", schema = "athena",
            sequenceName = "SEQ_PRODUCT_ORDER_KIT_DETAIL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT_ORDER_KIT_DETAIL")
    @Column(name = "PRODUCT_ORDER_KIT_DETAIL_ID")
    private Long productOrderKitDetailId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="PRODUCT_ORDER_KIT")
    private ProductOrderKit productOrderKit;

    @Column(name = "number_samples")
    private Long numberOfSamples;

    @Enumerated(EnumType.STRING)
    @Column(name = "kit_type")
    private KitType kitType;

    @Column(name = "organism_id")
    private Long organismId;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(schema = "athena", name = "PDO_KIT_DTL_POST_REC_OPT",
            joinColumns = {@JoinColumn(name = "product_order_kit_detail_id")})
    private Set<PostReceiveOption> postReceiveOptions = EnumSet.noneOf(PostReceiveOption.class);

    @Column(name = "material_bsp_name")
    private String bspMaterialName;

    @Transient
    private String organismName;

    public ProductOrderKitDetail() {
    }

    public ProductOrderKitDetail(Long numberOfSamples, KitType kitType, Long organismId, MaterialInfoDto materialInfo) {
        this(numberOfSamples, kitType, organismId, materialInfo, EnumSet.noneOf(PostReceiveOption.class));
    }

    public ProductOrderKitDetail(Long numberOfSamples, KitType kitType, Long organismId,
                                 MaterialInfoDto materialInfo, Set<PostReceiveOption> postReceiveOptions) {
        this.numberOfSamples = numberOfSamples;
        this.kitType = kitType;
        this.organismId = organismId;
        setMaterialInfo(materialInfo);
        this.postReceiveOptions = postReceiveOptions;
    }

    public Long getProductOrderKitDetailId() {
        return productOrderKitDetailId;
    }

    public void setProductOrderKitDetailId(Long productOrderKitDetailId) {
        this.productOrderKitDetailId = productOrderKitDetailId;
    }

    public ProductOrderKit getProductOrderKit() {
        return productOrderKit;
    }

    public Long getNumberOfSamples() {
        return numberOfSamples;
    }

    public KitType getKitType() {
        return kitType;
    }

    public Long getOrganismId() {
        return organismId;
    }

    public Set<PostReceiveOption> getPostReceiveOptions() {
        return postReceiveOptions;
    }

    public void setPostReceiveOptions(Set<PostReceiveOption> postReceiveOptions) {
        this.postReceiveOptions = postReceiveOptions;
    }

    public String getBspMaterialName() {
        return bspMaterialName;
    }

    public void setProductOrderKit(ProductOrderKit productOrderKit) {
        this.productOrderKit = productOrderKit;
    }


    public MaterialInfoDto getMaterialInfo() {
        return new MaterialInfoDto(kitType.getKitName(), bspMaterialName);
    }

    public void setMaterialInfo(MaterialInfoDto MaterialInfoDto) {
        bspMaterialName = MaterialInfoDto != null ? MaterialInfoDto.getBspName() : null;
    }

    public void setOrganismName(String s) {
        organismName = s;
    }

    /**
     * This is only populated after actionBean.populateTokenListsFromObjectData() is run.
     */
    public String getOrganismName() {
        return organismName;
    }

    public void setKitType(KitType kitType) {
        this.kitType = kitType;
    }

    public void setNumberOfSamples(Long numberOfSamples) {
        this.numberOfSamples = numberOfSamples;
    }

    public void setOrganismId(Long organismId) {
        this.organismId = organismId;
    }

    public void setBspMaterialName(String bspMaterialName) {
        this.bspMaterialName = bspMaterialName;
    }

    /**
     * Return a string representation of this kit's PostReceive options
     *
     * @param delimiter characters used to join list values
     */
    public String getPostReceivedOptionsAsString(String delimiter) {
        if (StringUtils.isBlank(delimiter)) {
            delimiter = ", ";
        }
        if (getPostReceiveOptions().isEmpty()) {
            return NO_POST_RECEIVED_OPTIONS_SELECTED;
        }

        List<String> options = new ArrayList<>(postReceiveOptions.size());
        for (PostReceiveOption postReceiveOption : postReceiveOptions) {
            options.add(postReceiveOption.getText());
        }
        return StringUtils.join(options, delimiter);
    }

    public String getKitTypeDisplayName() {
        if (getKitType() == null) {
            return null;
        }

        return getKitType().getDisplayName();
    }

    /**
     * Helper method to update the values of a product order kit detail based on another kit detail definition
     *
     * @param kitDetailUpdate a ProductOrderKitDetail entity from which values are to be updated.
     */
    public void updateDetailValues(ProductOrderKitDetail kitDetailUpdate) {
        setKitType(kitDetailUpdate.getKitType());
        setBspMaterialName(kitDetailUpdate.getBspMaterialName());
        setOrganismId(kitDetailUpdate.getOrganismId());
        setNumberOfSamples(kitDetailUpdate.getNumberOfSamples());
        getPostReceiveOptions().clear();
        postReceiveOptions.addAll(kitDetailUpdate.getPostReceiveOptions());
    }

}
