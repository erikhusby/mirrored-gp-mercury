package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.BillableItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillableItem;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Stateless
public class BillableItemEtl {
    private static final Logger logger = Logger.getLogger(BillableItemEtl.class);
    private static final String BASE_FILENAME = "billable_item";

    @Inject
    BillableItemDao dao;

    @Inject
    Util util;

    /**
     * Iterates on the modified Mercury entities, converts them to sqlLoader records, and
     * write them to the data file.
     * Only the most recent version of a modified entity is recorded.
     *
     * @param lastRev      beginning of the interval to look for entity changes.
     * @param etlRev       end of the interval to look for entity changes.
     * @param etlDateStr   etlDate formatted as YYYYMMDDHHMMSS
     */
    public void doEtl(long lastRev, long etlRev, String etlDateStr) {
        List<Object[]> dataChanges = util.fetchDataChanges(lastRev, etlRev, BillableItem.class);

        Set<Long> changedEntityIds = new HashSet<Long>();
        Set<Long> deletedEntityIds = new HashSet<Long>();
        String filename = util.dataFilename(etlDateStr, BASE_FILENAME);
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(filename));
            for (Object[] dataChange : dataChanges) {
                // Splits the result array.
                BillableItem entity = (BillableItem) dataChange[0];
                RevisionType revType = (RevisionType) dataChange[2];
                Long entityId = entity.getBillableItemId();

                // Writes a DW deletion record if entity was deleted.
                if (revType.equals(RevisionType.DEL)) {

                    String record = util.makeRecord(etlDateStr, true, entityId);

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
                if (entity == null) {
                    logger.info("Cannot export BillableItem having id " + entityId + " since it no longer exists.");
                } else {
                    String record =  util.makeRecord(etlDateStr, false,
                            entity.getBillableItemId(),
                            entity.getProductOrderSample() == null ? "" : entity.getProductOrderSample().getProductOrderSampleId(),
                            entity.getPriceItem() == null ? "" : entity.getPriceItem().getPriceItemId(),
                            entity.getCount());

                    writer.write(record);
                    writer.newLine();
                }
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