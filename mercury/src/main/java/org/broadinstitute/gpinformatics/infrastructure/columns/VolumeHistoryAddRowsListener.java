package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.ejb.HibernateEntityManager;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition.MultiRefTerm.INITIAL_VOLUME;
import static org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition.MultiRefTerm.VOLUME_HISTORY;

/**
 * A listener for the ConfigurableList addRows method.  Fetches vessel volume changes from audit trail.
 */
public class VolumeHistoryAddRowsListener implements ConfigurableList.AddRowsListener {

    /**
     * Ugh, this is gross.  Can't use Oracle arrays: lab_vessel_id in ( select COLUMN_VALUE from table( ? ) ) and pass in an array of longs <br/>
     * No permission to create a type in DB, and JPA/Hibernate far too isolated from ability to use Oracle functionality
     */
    private final String auditQueryTemplate =
            "select label, min( rev_date ) as rev_date, volume, username "
          + "  from ( select a.label, r.rev_date, trunc(a.volume, 2) as volume, r.username "
          + "           from lab_vessel_aud a "
          + "              , rev_info r "
          + "          where lab_vessel_id in ( :ids ) "
          + "            and a.rev = r.rev_info_id "
          + "            and a.volume is not null ) "
          + "group by label, volume, username "
          + "order by label, rev_date ";

    private Map<String, Triple<Date, BigDecimal, String>> mapIdToInitialVolumeData = new HashMap<>();
    private MultiValuedMap<String, Triple<Date, BigDecimal, String>> mapIdToVolumeHistoryData = new ArrayListValuedHashMap();

    public VolumeHistoryAddRowsListener() {
    }

    @Override
    public void addRows(List<?> entityList, SearchContext context, Map<Integer,ColumnTabulation> nonPluginTabulations) {

        // Don't do anything if data retrieval is not required
        boolean isDataRetrievalRequired = false;
        for( ColumnTabulation column : nonPluginTabulations.values() ) {
            if( column.getName().equals(VOLUME_HISTORY.getTermRefName())
                    || column.getName().equals(INITIAL_VOLUME.getTermRefName()) ) {
                isDataRetrievalRequired = true;
                break;
            }
        }
        if( !isDataRetrievalRequired ) {
            return;
        }

        List<Long> ids = new ArrayList<>();
        for (Object entity : entityList) {
            LabVessel labVessel = OrmUtil.proxySafeCast( entity, LabVessel.class );
            ids.add( labVessel.getLabVesselId() );
        }

        EntityManager em = getEntityManager(context);
        HibernateEntityManager hibernateEntityManager = em.unwrap(HibernateEntityManager.class);
        Session hibernateSession = hibernateEntityManager.getSession();

        SQLQuery qry = hibernateSession.createSQLQuery(auditQueryTemplate);
        // Puts parameter placeholders in query - one for each element: where lab_vessel_id in ( ?, ?, ?, ? )
        qry.setParameterList("ids", ids );

        List<Object[]> results = qry.list();
        String prevLabel = "";
        for( Object[] cols : results ) {
            String label = (String) cols[0];
            Date date = (Date) cols[1];
            BigDecimal volume = (BigDecimal) cols[2];
            String username = (String) cols[3];
            // First row with new label is the initial volume
            if( !prevLabel.equals(label) ) {
                mapIdToInitialVolumeData.put(label, Triple.of(date, volume, username));
                prevLabel = label;
            }
            mapIdToVolumeHistoryData.put(label, Triple.of(date, volume, username));
        }
    }

    @Override
    public void reset() {
        mapIdToInitialVolumeData.clear();
    }

    /**
     * Earliest recorded row vessel volume
     * @return Three values, left value is date volume was recorded, middle value is the recorded volume
     *      , and right value is the person who recorded the measurement
     */
    public Triple<Date, BigDecimal, String> getInitialVolumeData(String barcode) {
        return mapIdToInitialVolumeData.get(barcode);
    }

    /**
     * A collection (list) of recorded row vessel volume changes
     * @return Ascending list by date of three values, left value is date volume was recorded, middle value is the recorded volume
     *      , and right value is the person who recorded the measurement
     */
    public Collection<Triple<Date, BigDecimal, String>> getVolumeHistoryData(String barcode) {
        return mapIdToVolumeHistoryData.get(barcode);
    }

    private EntityManager getEntityManager(SearchContext context){
        // Try to avoid JNDI lookup
        GenericDao dao = context.getOptionValueDao();
        return dao.getEntityManager();
    }

}
