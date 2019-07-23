package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.Query;
import java.math.BigDecimal;

/**
 * Data Access Object for creating new Genomics Sample ID's
 */
@Stateful
@RequestScoped
public class GenomicsSampleDao extends GenericDao {

    public static final String GS_PREFIX = "GS-";

    public String fetchNextGenomicsSampleId() {
        String sql = "select SEQ_GS_SAMPLE.nextval from dual";
        Query query = getEntityManager().createNativeQuery(sql);
        BigDecimal id = (BigDecimal) query.getSingleResult();
        return GS_PREFIX + Long.toString(id.longValue(), 36).toUpperCase();
    }
}
