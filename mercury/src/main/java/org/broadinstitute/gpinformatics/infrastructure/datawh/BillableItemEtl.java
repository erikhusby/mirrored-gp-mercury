package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.BillableItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillableItem;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;

import javax.inject.Inject;
import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BillableItemEtl {
    private static final Logger logger = Logger.getLogger(BillableItemEtl.class);
    private static final String BASE_FILENAME = "billable_item";

    @Inject
    BillableItemDao dao;

    /**
     * Iterates on the modified Mercury entities, converts them to sqlLoader records, and
     * write them to the data file.
     * Only the most recent version of a modified entity is recorded.
     *
     * @param lastDate     beginning of the time interval to look for entity changes.
     * @param etlDate      end of the time interval to look for entity changes.
     * @param etlDateStr   etlDate formatted as YYYYMMDDHHMMSS
     */
    void doEtl(long lastDate, long etlDate, String etlDateStr) {
        AuditReader auditReader = AuditReaderFactory.get(dao.getEntityManager());

        List<Object[]> dataChanges = Util.fetchDataChanges(lastDate, etlDate, auditReader, BillableItem.class);

        Set<Long> changedEntityIds = new HashSet<Long>();
        Set<Long> deletedEntityIds = new HashSet<Long>();
        String filename = Util.dataFilename(etlDateStr, BASE_FILENAME);
        Writer writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(filename));
            for (Object[] dataChange : dataChanges) {
                // Splits the result array.
                BillableItem entity = (BillableItem) dataChange[0];
                RevisionType revType = (RevisionType) dataChange[2];
                Long entityId = entity.getBillableItemId();

                // Writes a DW deletion record if entity was deleted.
                if (revType.equals(RevisionType.DEL)) {

                    String record = Util.makeRecord(etlDateStr, true, entityId);

                    writer.write(record);

                    deletedEntityIds.add(entityId);
                } else {
                    // Collects deduplicated entity ids in order to lookup the latest version once.
                    changedEntityIds.add(entityId);
                }

            }

            // Makes records for latest version of the changed entity.
            changedEntityIds.removeAll(deletedEntityIds);
            for (Long entityId : changedEntityIds) {
                BillableItem entity = dao.findById(BillableItem.class, entityId);

                String record =  Util.makeRecord(etlDateStr, false,
                        entity.getBillableItemId(),
                        entity.getProductOrderSample().getProductOrderSampleId(),
                        entity.getPriceItem().getPriceItemId(),
                        entity.getCount());

                writer.write(record);
            }
        } catch (IOException e) {
            logger.error("Problem writing " + etlDateStr + "_" + BASE_FILENAME);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                logger.error("Problem closing " + etlDateStr + "_" + BASE_FILENAME);
            }
        }
    }
}