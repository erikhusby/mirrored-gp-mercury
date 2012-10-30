package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.log4j.Logger;
import org.apache.poi.ss.formula.functions.T;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.hibernate.Session;
import org.hibernate.envers.*;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is a JEE scheduled bean that does the initial parts of ETL for the data warehouse.
 *
 * Extract processing is first, and it uses Envers AuditReader to get relevant data from the
 * _AUD tables.  These tables contain changes to entities marked with a revision.  For most
 * entities ETL only wants the latest version.  However there are several historical status
 * tables that will need to iterate over the relevant range of revisions, typically all changes
 * since the last ETL run, and extract the status and status dates.
 *
 * Transform processing is next.  It maps the entities to DW import table records, writes
 * them to a data file that is named with a etl_date.  The same etl_date is used for all DW
 * data processed by this run of ExtractTransform.
 *
 * Created with IntelliJ IDEA.
 * User: epolk
 * Date: 10/29/12
 */
public class ExtractTransform {
    private static String DATAFILE_DIR = "/seq/lims/datawh/dev/new";
    private static String LAST_TIMESTAMP_FILE = "last_etl_timestamp";
    private static String READY_FILE_SUFFIX = "_is_ready";
    private static final Logger logger = Logger.getLogger(ExtractTransform.class);

    private void incrementalETL() {
        final long etlDate = System.currentTimeMillis();
        long lastDate = 0L;

        // Obtains the timestamp of the last ETL run
        try {
            File file = new File (DATAFILE_DIR, LAST_TIMESTAMP_FILE);
            BufferedReader rdr = new BufferedReader(new FileReader(file));
            String s = rdr.readLine();
            Long.parseLong(s);
            rdr.close();
        } catch (FileNotFoundException e) {
            logger.error("Missing file: " + LAST_TIMESTAMP_FILE);
            return;
        } catch (IOException e) {
            logger.error("Error processing file " + LAST_TIMESTAMP_FILE, e);
            return;
        } catch (NumberFormatException e) {
            logger.error("Cannot parse mSec timestamp in" + LAST_TIMESTAMP_FILE, e);
            return;
        }
        AuditReader auditReader = AuditReaderFactory.get(new GenericDao().getEntityManager());
        doExtract(lastDate, etlDate, auditReader, ResearchProject.class);
    }

    /**
     * Finds and records all data changes that happened in the given interval.
     *
     * @param lastDate start of interval
     * @param etlDate end of interval, inclusive
     * @param reader auditReader to use
     * @param entityClass the class of entity to process
     */
    private void doExtract(long lastDate, long etlDate, AuditReader reader, Class entityClass) {

        AuditQuery query = reader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true)
                .add(AuditEntity.revisionProperty("timestamp").gt(lastDate))
                .add(AuditEntity.revisionProperty("timestamp").le(etlDate));
        List<Object[]> dataChanges = query.getResultList();

        List<String> dwRecords = new ArrayList<String>();
        Set<String> changedEntityIds = new HashSet<String>();
        Set<String> deletedEntityIds = new HashSet<String>();

        for (Object[] dataChange : dataChanges) {
            // Splits the result array.
            Object entity = dataChange[0];
            DefaultRevisionEntity dre = (DefaultRevisionEntity)dataChange[1];
            RevisionType revType = (RevisionType)dataChange[2];
            String entityId = getId(entity, entityClass);

            // Writes a DW deletion record if entity was deleted.
            if (revType.equals(RevisionType.DEL)) {
                dwRecords.add(deletionTransform(entity, entityClass, etlDate));
                deletedEntityIds.add(entityId);
            }

            // Collects deduplicated entity ids in order to lookup the latest version once.
            if (!deletedEntityIds.contains(entityId)) {
                changedEntityIds.add(entityId);
            }

            // For entities having status changes, DW wants the sequence of status
            // changes and their dates.  These DW records go into a different load file.
            if (entity instanceof ResearchProject
                || entity instanceof ProductOrder
                || entity instanceof ProductOrderSample) {
                writeStatusChangeRecord(entity, entityClass, etlDate);
            }
        }

        // Makes records for latest version of changed entity
        for (String entityId : changedEntityIds) {
            dwRecords.add(transform(entityClass, entityId));
        }
        writeLastTimestampFile(etlDate);
        writeIsReadyFile(etlDate);
    }

    private String getId(ResearchProject entity) {
        return String.valueOf(entity.getResearchProjectId());
    }

    private String deletionTransform(Object entity, ResearchProject entity, long etlDate) {
        //xxx
    }

    private void writeStatusChangeRecord(ResearchProject entity, long etlDate) {
        //xxx
    }

    private void writeLastTimestampFile(long etlDate) {
        try {
            File file = new File (DATAFILE_DIR, LAST_TIMESTAMP_FILE);
            FileWriter fw = new FileWriter(file);
            fw.write(String.valueOf(etlDate));
            fw.close();
        } catch (IOException e) {
            logger.error("Error creating file " + LAST_TIMESTAMP_FILE);
        }
    }

    private void writeIsReadyFile(long etlDate) {
        try {
            File file = new File (DATAFILE_DIR, String.valueOf(etlDate) + READY_FILE_SUFFIX);
            FileWriter fw = new FileWriter(file);
            fw.write("is_ready");
            fw.close();
        } catch (IOException e) {
            logger.error("Error creating file " + etlDate + READY_FILE_SUFFIX, e);
        }
    }

}

