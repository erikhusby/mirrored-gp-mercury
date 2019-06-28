package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class ProductOrderSampleEtl extends GenericEntityAndStatusEtl<ProductOrderSample, ProductOrderSample> {
    private String samplePositionJoinTable;

    private int counter = 0;

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
        if( ++counter > JPA_CLEAR_THRESHOLD ) {
            counter = 0;
            dao.clear();
        }
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

        SampleEtlData etlMetadata = getMetadata(entity );

        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderSampleId(),
                format(entity.getProductOrder() != null ? entity.getProductOrder().getProductOrderId() : null),
                format(entity.getName()),
                format(entity.getDeliveryStatus().name()),
                format(entity.getSamplePosition()),
                format(etlMetadata.getParticipantId()),
                format(etlMetadata.getSampleType()),
                format(etlMetadata.getReceiptDate()),
                format(etlMetadata.getOriginalSampleType())
        );
    }

    private SampleEtlData getMetadata( ProductOrderSample entity ) {
        SampleEtlData etlMetadata = new SampleEtlData();
        if( entity.getMercurySample() == null ) {
            return etlMetadata;
        } else {
            MercurySample mercurySample = entity.getMercurySample();
            Set<Metadata> metadataSet = mercurySample.getMetadata();
            etlMetadata.setParticipantId(getMetadataValue(metadataSet, Metadata.Key.PATIENT_ID));
            etlMetadata.setSampleType(getMetadataValue(metadataSet, Metadata.Key.TUMOR_NORMAL));
            etlMetadata.setOriginalSampleType(getMetadataValue(metadataSet, Metadata.Key.ORIGINAL_MATERIAL_TYPE));
            // Use mercury sample reciept date (PDO sample calls BSP!)
            etlMetadata.setReceiptDate(mercurySample.getReceivedDate());
            return etlMetadata;
        }

    }

    private String getMetadataValue( Set<Metadata> metadataSet, Metadata.Key key ) {
        String value = null;
        for( Metadata metadata : metadataSet ) {
            if( metadata.getKey() == key ) {
                value = metadata.getValue();
                break;
            }
        }
        return value;
    }

    private class SampleEtlData {

        private String participantId;
        private String sampleType;
        private Date receiptDate;
        private String originalSampleType;

        public SampleEtlData(){ }

        public void setParticipantId(String participantId){
            this.participantId = participantId;
        }

        public String getParticipantId(){
            return participantId;
        }

        public void setSampleType(String sampleType) {
            this.sampleType = sampleType;
        }

        public String getSampleType() {
            return sampleType;
        }

        public void setReceiptDate(Date receiptDate){
            this.receiptDate = receiptDate;
        }

        public Date getReceiptDate(){
            return receiptDate;
        }

        public void setOriginalSampleType(String originalSampleType){
            this.originalSampleType = originalSampleType;
        }

        public String getOriginalSampleType(){
            return originalSampleType;
        }

    }

}
