package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.Query;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Stateful
public class LedgerEntryEtl extends GenericEntityEtl {

    private LedgerEntryDao dao;

    @Inject
    public void setLedgerEntryDao(LedgerEntryDao dao) {
        this.dao = dao;
    }

    /** {@inheritDoc} */
    @Override
    Class getEntityClass() {
        return LedgerEntry.class;
    }

    /** {@inheritDoc} */
    @Override
    String getBaseFilename() {
        return "product_order_sample_bill";
    }

    /** {@inheritDoc} */
    @Override
    Long entityId(Object entity) {
        return ((LedgerEntry)entity).getLedgerId();
    }

    /** {@inheritDoc} */
    @Override
    protected void processChanges(List<Object[]> dataChanges, DataFile dataFile, String etlDateStr) {

        // The warehouse only cares about the overall pdoSample's boolean billed status, so this code
        // exports only that pdoSample boolean, and not the LedgerEntry items.

        try {
            // Collects and deduplicates ids of changed ledger entries.
            Set<Long> ledgerEntryIds = new HashSet<Long>();
            for (Object[] dataChange : dataChanges) {
                ledgerEntryIds.add(entityId(dataChange[0]));
            }
            // Collects and deduplicates the relevant pdo sample ids, and writes update records for them.
            for (BigDecimal pdoSampleId : lookupSampleIds(ledgerEntryIds)) {
                if (null == pdoSampleId) continue;
                // ATHENA.BILLING_LEDGER_AUD.product_sample_id is a generated numeric field (a BigDecimal)
                // but we know it will always fit in a Long.
                Long id = Long.parseLong(String.valueOf(pdoSampleId));
                for (String record : entityRecords(etlDateStr, false, id)) {
                    dataFile.write(record);
                }
            }

        } catch (IOException e) {
            logger.error("Error while writing file " + dataFile.getFilename(), e);
        } finally {
            dataFile.close();
        }
    }
    private final int SQL_IN_CLAUSE_LIMIT = 1000;

    private Collection<BigDecimal> lookupSampleIds(Collection<Long> ledgerEntryIds) {
        Set<BigDecimal> pdoSampleIds = new HashSet<BigDecimal>();
        // Chunks as necessary to limit sql "in" clause to 1000 elements.
        Long[] ledgerEntryIdArray = ledgerEntryIds.toArray(new Long[ledgerEntryIds.size()]);

        int endIdx = ledgerEntryIdArray.length - 1;
        while (endIdx >= 0) {
            int startIdx = Math.max(0, endIdx - SQL_IN_CLAUSE_LIMIT + 1);
            String inClause = StringUtils.join(ledgerEntryIdArray, ",", startIdx, endIdx);
            endIdx = startIdx - 1;

            String queryString = "select distinct product_order_sample_id from ATHENA.BILLING_LEDGER_AUD " +
                    " where ledger_id in (" + inClause + ")";
            Query query = dao.getEntityManager().createNativeQuery(queryString);
            // Overwriting earlier pdoSample ids is intended.
            pdoSampleIds.addAll((Collection<BigDecimal>)query.getResultList());

        }
        return pdoSampleIds;
    }

    public int doBackfillEtl(Class entityClass, long startId, long endId, String etlDateStr) {
        // No-op unless the implementing class is the requested entity class.
        if (!getEntityClass().equals(entityClass) || !isEntityEtl()) {
            return 0;
        }

        // Creates the wrapped Writer to the sqlLoader data file.
        String filename = dataFilename(etlDateStr, getBaseFilename());
        DataFile dataFile = new DataFile(filename);

        try {
            String queryString = "select distinct product_order_sample_id from ATHENA.BILLING_LEDGER_AUD " +
                    " where ledger_id between :startId and :endId";

            Query query = dao.getEntityManager().createNativeQuery(queryString);
            query.setParameter("startId", startId);
            query.setParameter("endId", endId);
            // Deduplicates the ids.
            Set<BigDecimal> pdoSampleIds = new HashSet<BigDecimal>(query.getResultList());

            // Writes the records.
            for (BigDecimal pdoSampleId : pdoSampleIds) {
                if (pdoSampleId != null) {
                    for (String record : entityRecords(etlDateStr, false, pdoSampleId.longValue())) {
                        dataFile.write(record);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error while writing file " + dataFile.getFilename(), e);
        } finally {
            dataFile.close();
        }

        return dataFile.getRecordCount();
    }


    Collection<String> entityRecords(String etlDateStr, boolean isDelete, Long pdoSampleId) {
        Collection<String> recordList = new ArrayList<String>();
        ProductOrderSample entity = dao.findById(ProductOrderSample.class, pdoSampleId);
        if (entity != null) {
            recordList.add(entityRecord(etlDateStr, isDelete, entity));
        }
        return recordList;
    }

    /**
     * Makes a data record from an entity, in a format that matches the corresponding SqlLoader control file.
     *
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    String entityRecord(String etlDateStr, boolean isDelete, ProductOrderSample entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderSampleId(),
                format(entity.getBillableLedgerItems().size() == 0)
        );
    }


    @Override
    Collection<String> entityRecordsInRange(long startId, long endId, String etlDateStr, boolean isDelete) {
        throw new RuntimeException("This method cannot apply to this etl class.");
    }

    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object revObject, boolean isDelete) {
        return null;
    }

    @Override
    boolean isEntityEtl() {
        return true;
    }
}
