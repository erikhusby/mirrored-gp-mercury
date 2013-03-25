package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.RiskItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.Query;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Stateful
public class RiskItemEtl extends GenericEntityEtl {

    private RiskItemDao dao;

    @Inject
    public void setRiskItemDao(RiskItemDao dao) {
        this.dao = dao;
    }

    /** {@inheritDoc} */
    @Override
    Class getEntityClass() {
        return RiskItem.class;
    }

    /** {@inheritDoc} */
    @Override
    String getBaseFilename() {
        return "product_order_sample_risk";
    }

    /** {@inheritDoc} */
    @Override
    Long entityId(Object entity) {
        return ((RiskItem)entity).getRiskItemId();
    }

    /** {@inheritDoc} */
    @Override
    protected void processChanges(List<Object[]> dataChanges, DataFile dataFile, String etlDateStr) {

        // The warehouse only cares about the overall pdoSample's on-risk boolean, so this code
        // exports only that pdoSample boolean, and not the RiskItem as such.
        // Only the latest version of each risk item needs to be kept regardless of revType.
        // Determines the pdoSample on-risk boolean based on current pdoSample entity, not the risk item.
        // Further weirdness: When risk items are deleted the only record left in the operational db that
        // shows the the mapping from deleted risk id to pdo sample id is in the join table's audit table.

        try {
            // Collects and deduplicates ids of changed riskItems.
            Set<Long> riskIds = new HashSet<Long>();
            for (Object[] dataChange : dataChanges) {
                riskIds.add(entityId(dataChange[0]));
            }
            // Collects and deduplicates the relevant pdo sample ids, and writes update records for them.
            for (BigDecimal pdoSampleId : lookupSampleIds(riskIds)) {
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
    }
    private final int SQL_IN_CLAUSE_LIMIT = 1000;

    private Collection<BigDecimal> lookupSampleIds(Collection<Long> riskIds) {
        Set<BigDecimal> pdoSampleIds = new HashSet<BigDecimal>();
        // Chunks as necessary to limit sql "in" clause to 1000 elements.
        // TODO Splitterize
        Long[] riskIdArray = riskIds.toArray(new Long[riskIds.size()]);

        int endIdx = riskIdArray.length - 1;
        while (endIdx >= 0) {
            int startIdx = Math.max(0, endIdx - SQL_IN_CLAUSE_LIMIT + 1);
            String inClause = StringUtils.join(riskIdArray, ",", startIdx, endIdx);
            endIdx = startIdx - 1;

            String queryString = "select distinct product_order_sample from ATHENA.PO_SAMPLE_RISK_JOIN_AUD " +
                    " where risk_item_id in (" + inClause + ")";
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
            String queryString = "select distinct product_order_sample from ATHENA.PO_SAMPLE_RISK_JOIN_AUD " +
                    " where risk_item_id between :startId and :endId";
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

    /** {@inheritDoc} */
    @Override
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
                format(entity.isOnRisk())
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
