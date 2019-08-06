package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingProductOrderMapping;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class ProductOrderEtl extends GenericEntityAndStatusEtl<ProductOrder, ProductOrder> {
    private BSPUserList userList;
    private ProductEjb productEjb;
    private AttributeArchetypeDao archetypeDao;

    public ProductOrderEtl() {
    }

    @Inject
    public ProductOrderEtl(ProductOrderDao dao, BSPUserList userList, ProductEjb productEjb, AttributeArchetypeDao archetypeDao) {
        super(ProductOrder.class, "product_order", "product_order_status", "athena.product_order_aud", "product_order_id", dao);
        this.userList = userList;
        this.productEjb = productEjb;
        this.archetypeDao = archetypeDao;
    }

    @Override
    Long entityId(ProductOrder entity) {
        return entity.getProductOrderId();
    }

    @Override
    Path rootId(Root<ProductOrder> root) {
        return root.get(ProductOrder_.productOrderId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(ProductOrder.class, entityId));
    }

    @Override
    String statusRecord(String etlDateStr, boolean isDelete, ProductOrder entity, Date statusDate) {
        if (entity != null && entity.getOrderStatus() != null) {
            return genericRecord(etlDateStr, isDelete,
                    entity.getProductOrderId(),
                    format(statusDate),
                    format(entity.getOrderStatus().getDisplayName())
            );
        }
        return null;
    }


    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ProductOrder entity) {
        Long personId = entity.getCreatedBy();
        BspUser bspUser = personId != null ? userList.getById(personId) : null;

        String regInfoData = "";
        if( !isDelete ) {
            regInfoData = getRegInfoData(entity);
        }

        String arrayChipType = null;
        String callThreshold = null;
        Pair<String,String> chipData = productEjb.getGenotypingChip(entity, entity.getCreatedDate());
        // "Infinium" -> "InfiniumOmniExpressExome-8v1-3_A"
        if( chipData != null ) {
            arrayChipType = chipData.getRight();
            GenotypingChip chip = archetypeDao.findGenotypingChip( chipData.getLeft(), chipData.getRight() );
            if( chip != null ) {
                ArchetypeAttribute attrib = chip.getAttribute(GenotypingProductOrderMapping.CALL_RATE_THRESHOLD);
                if( attrib != null ) {
                    callThreshold = attrib.getAttributeValue();
                }
                // Check if a mapping override exists for this pdo, if so override default values if not null
                GenotypingProductOrderMapping genotypingProductOrderMapping =
                        archetypeDao.findGenotypingProductOrderMapping(entity.getProductOrderId());
                if (genotypingProductOrderMapping != null) {
                    for (ArchetypeAttribute attribute : genotypingProductOrderMapping.getAttributes()) {
                        if( attribute.getAttributeName().equals(GenotypingProductOrderMapping.CALL_RATE_THRESHOLD)) {
                            if (attribute.getAttributeValue() != null) {
                                callThreshold = attribute.getAttributeValue();
                            }
                        }
                    }
                }
            }
        }
        ProductOrder.OrderAccessType orderType = entity.getOrderType();
        String orderTypeString = "";
        if (orderType != null) {
            orderTypeString = format(orderType.getCompanyCode());
        }

        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderId(),
                format(entity.getResearchProject() != null ? entity.getResearchProject().getResearchProjectId() : null),
                format(entity.getProduct() != null ? entity.getProduct().getProductId() : null),
                format(entity.getOrderStatus().getDisplayName()),
                format(entity.getCreatedDate()),
                format(entity.getModifiedDate()),
                format(entity.getTitle()),
                format(entity.getQuoteId()),
                format(entity.getJiraTicketKey()),
                format(bspUser != null ? bspUser.getUsername() : null),
                format(entity.getPlacedDate()),
                format(entity.getSkipRegulatoryReason()),
                format(entity.getSapOrderNumber()),
                format(arrayChipType),
                format(callThreshold),
                format(regInfoData),
                orderTypeString
        );
    }

    /**
     * Gets list of regulatory info data associated with a PDO
     * @param entity
     * @return  Arrays.toString() output (comma delimited values enclosed in square brackets, e.g.:  [3,5,7] }
     */
    private String getRegInfoData(ProductOrder entity){
        String regInfoData = "";
        Collection<RegulatoryInfo> regInfos = entity.getRegulatoryInfos();
        if( !regInfos.isEmpty() ) {
            Long[] reginfoIds = new Long[regInfos.size()];
            int i = 0;
            for( RegulatoryInfo regInfo : regInfos ) {
                reginfoIds[i] = regInfo.getRegulatoryInfoId();
                i++;
            }
            regInfoData = Arrays.toString(reginfoIds);
        }
        return regInfoData;
    }
}
