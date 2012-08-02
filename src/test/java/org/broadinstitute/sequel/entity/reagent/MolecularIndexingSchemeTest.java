package org.broadinstitute.sequel.entity.reagent;

import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A Test to import index definitions from Squid
 */
public class MolecularIndexingSchemeTest extends ContainerTest {

    @PersistenceContext(unitName = "squid_pu")
    private EntityManager entityManager;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testImport() {
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "     mis.NAME, " +
                "     mip.position_hint, " +
                "     mi.SEQUENCE " +
                "FROM " +
                "     molecular_indexing_scheme mis " +
                "     INNER JOIN molecular_index_position mip " +
                "          ON   mip.scheme_id = mis.ID " +
                "     INNER JOIN molecular_index mi " +
                "          ON   mi.ID = mip.index_id " +
                "ORDER BY " +
                "     mis.NAME ");
        List<?> resultList = nativeQuery.getResultList();

        String previousSchemeName = "";
        MolecularIndexingScheme molecularIndexingScheme = null;
        SortedMap<MolecularIndexingScheme.IndexPosition, MolecularIndex> indexesAndPositions = null;
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String schemeName = (String) columns[0];
            String positionHint = (String) columns[1];
            String sequence = (String) columns[2];

            if(!schemeName.equals(previousSchemeName)) {
                previousSchemeName = schemeName;
                if(molecularIndexingScheme != null) {
                    molecularIndexingScheme.setIndexPositions(indexesAndPositions);
                    molecularIndexingSchemeDao.persist(molecularIndexingScheme);
                    // Until fix the mapping problem that causes repeated deletes and inserts on molecular_index_position,
                    // clear the session to avoid dirty checks (each call to persist is in its own transaction, so it
                    // flushes immediately)
                    molecularIndexingSchemeDao.clear();
                }
                molecularIndexingScheme = new MolecularIndexingScheme();
                indexesAndPositions = new TreeMap<MolecularIndexingScheme.IndexPosition, MolecularIndex>();
            }
            indexesAndPositions.put(MolecularIndexingScheme.IndexPosition.valueOf(positionHint), new MolecularIndex(sequence));
        }
    }
}
