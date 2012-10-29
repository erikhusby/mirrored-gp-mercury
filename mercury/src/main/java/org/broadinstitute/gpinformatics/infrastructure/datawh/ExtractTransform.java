package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.hibernate.Session;
import org.hibernate.envers.*;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import java.io.*;
import java.util.List;

/**
 * This is a JEE scheduled bean that does the initial parts of ETL for data warehouse ("DW").
 *
 * Extract processing is first, and it uses Envers AuditReader to get relevant data from the
 * _AUD tables.  These tables contain changes to entities marked with a revision.  For most
 * entities ETL only wants the latest version.  However there are three historical status
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
    private static String LAST_TIMESTAMP_FILE = "/seq/lims/datawh/dev/last_etl_timestamp";
    private static final Logger logger = Logger.getLogger(ExtractTransform.class);

    private void incrementalETL() {
        long currentTimestamp = System.currentTimeMillis();
        long lastTimestamp = 0L;

        // Obtains the timestamp of the last ETL run
        try {
            BufferedReader rdr = new BufferedReader(new FileReader(LAST_TIMESTAMP_FILE));
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
        AuditReader reader = AuditReaderFactory.get(new GenericDao().getEntityManager());
        doExtract(lastTimestamp, currentTimestamp, reader, ResearchProject.class);
    }

    /** Finds all data changes that happened between last run and now. */
    private void doExtract(long lastTimestamp, long currentTimestamp, AuditReader reader,
                           Class entityClass) {

        AuditQuery query = reader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true)
                .add(AuditEntity.revisionProperty("timestamp").gt(lastTimestamp))
                .add(AuditEntity.revisionProperty("timestamp").le(currentTimestamp));
        List<Object[]> results = query.getResultList();

        for (Object[] result : results) {
            ResearchProject rp = (ResearchProject)result[0];
            DefaultRevisionEntity dre = (DefaultRevisionEntity)result[0];

            RevisionType revType = (RevisionType)result[2];

            if (revType.equals(RevisionType.DEL)) {
                // write a deletion record
            }
        }

        // Collects only the audited id or natural key
        // For ResearchProject, ProductOrder, and ProductOrderSample ids
        //   Gets the status/billing_status for each envers rev
        //   Gets the corresponding status date
        //   Maps status data to DW record
        //   Creates a data file as needed, one data file per warehouse entity type
        //   Writes a DW record to the file
        // Dedups all ids
        // For each id,
        //   Gets the latest object
        //   Maps data to DW record
        //   Creates a data file as needed, one data file per warehouse entity type
        //   Writes a DW record to the file
        // Writes the is_ready indicator file


    }
}

