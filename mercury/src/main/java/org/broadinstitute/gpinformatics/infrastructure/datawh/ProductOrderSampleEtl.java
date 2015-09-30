package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Stateful
public class ProductOrderSampleEtl extends GenericEntityAndStatusEtl<ProductOrderSample, ProductOrderSample> {
    private String samplePositionJoinTable;

    // Keys to metadata values
    private static String PARTICIPANT_ID_KEY = "PARTICIPANT_ID";
    private static String SAMPLE_TYPE_KEY = "SAMPLE_TYPE";
    private static String RECEIPT_DATE_KEY = "RECEIPT_DATE";
    private static String ORIGINAL_SAMPLE_TYPE_KEY = "ORIGINAL_SAMPLE_TYPE";

    private static Map<String,Metadata.Key> etlMetadataKeyMap = new HashMap<>();
    static {
        etlMetadataKeyMap.put(PARTICIPANT_ID_KEY, Metadata.Key.PATIENT_ID );
        etlMetadataKeyMap.put(SAMPLE_TYPE_KEY, Metadata.Key.MATERIAL_TYPE );
        etlMetadataKeyMap.put(ORIGINAL_SAMPLE_TYPE_KEY, Metadata.Key.ORIGINAL_MATERIAL_TYPE );
    }

    public ProductOrderSampleEtl() {
    }

    @Inject
    public ProductOrderSampleEtl(ProductOrderSampleDao dao) {
        super(ProductOrderSample.class, "product_order_sample", "product_order_sample_status",
                "athena.product_order_sample_aud", "product_order_sample_id", dao);
        samplePositionJoinTable = "athena.product_order_sample_join_aud";
    }

    @Override
    Long entityId(ProductOrderSample entity) {
        return entity.getProductOrderSampleId();
    }

    @Override
    Path rootId(Root<ProductOrderSample> root) {
        return root.get(ProductOrderSample_.productOrderSampleId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(ProductOrderSample.class, entityId));
    }

    @Override
    String statusRecord(String etlDateStr, boolean isDelete, ProductOrderSample entity, Date revDate) {
        if (entity != null && entity.getDeliveryStatus() != null) {
            return genericRecord(etlDateStr, isDelete,
                    entity.getProductOrderSampleId(),
                    format(revDate),
                    format(entity.getDeliveryStatus().name())
            );
        } else {
            return null;
        }
    }

    // Queries PRODUCT_ORDER_SAMPLE_JOIN_AUD table directly to detect changes in sample position.
    //
    // Does not pass the revtype (add, modify, delete) to the entity etl class since what happens to
    // the sample position join entity must not propagate back to the pdo sample entity itself; it just
    // should tell the entity etl class to fetch the most recent sample position value for the entity.
    @Override
    protected Collection<Long> fetchAdditionalModifies(Collection<Long>revIds) {
        return lookupAssociatedIds(revIds,
                "SELECT " + auditTableEntityIdColumnName + " entity_id FROM " + samplePositionJoinTable +
                " WHERE rev IN ( " + IN_CLAUSE_PLACEHOLDER + " ) ");
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ProductOrderSample entity) {

        Map<String, String> etlMetadata = getMetadata(entity );

        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderSampleId(),
                format(entity.getProductOrder() != null ? entity.getProductOrder().getProductOrderId() : null),
                format(entity.getName()),
                format(entity.getDeliveryStatus().name()),
                format(entity.getSamplePosition()),
                format(etlMetadata.get(PARTICIPANT_ID_KEY)),
                format(etlMetadata.get(SAMPLE_TYPE_KEY)),
                format(etlMetadata.get(RECEIPT_DATE_KEY)),
                format(etlMetadata.get(ORIGINAL_SAMPLE_TYPE_KEY))
        );
    }

    Map<String,String> getMetadata( ProductOrderSample entity ) {
        Map<String,String> etlMetadata = new HashMap<>();
        if( entity.getMercurySample() == null ) {
            return etlMetadata;
        } else {
            MercurySample mercurySample = entity.getMercurySample();
            Set<Metadata> metadataSet = mercurySample.getMetadata();
            for( Map.Entry<String,Metadata.Key> mapEntry : etlMetadataKeyMap.entrySet() ) {
                for( Metadata metadata : metadataSet ) {
                    if( mapEntry.getValue() == metadata.getKey() ) {
                        etlMetadata.put(mapEntry.getKey(), metadata.getValue());
                        break;
                    }
                }
            }
            // Tack on mercury sample reciept date (PDO sample calls BSP!)
            Date receiptDate = mercurySample.getReceivedDate();
            if( receiptDate != null ) {
                etlMetadata.put(RECEIPT_DATE_KEY, format(receiptDate));
            }

            return etlMetadata;
        }

    }

}
