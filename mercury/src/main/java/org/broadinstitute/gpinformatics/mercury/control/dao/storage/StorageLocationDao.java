package org.broadinstitute.gpinformatics.mercury.control.dao.storage;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Stateful
@RequestScoped
public class StorageLocationDao extends GenericDao {

    public StorageLocation findByBarcode(final String barcode) {
        return findSingle(StorageLocation.class, new GenericDaoCallback<StorageLocation>() {
            @Override
            public void callback(CriteriaQuery<StorageLocation> criteriaQuery, Root<StorageLocation> root) {
                criteriaQuery.where(getCriteriaBuilder().equal(root.get(StorageLocation_.barcode), barcode));
            }
        });
    }

    public List<StorageLocation> findByListBarcodes(List<String> barcodes) {
        return findListByList(StorageLocation.class, StorageLocation_.barcode, barcodes);
    }

    public List<StorageLocation> findByLabel(final String label) {
        return findAll(StorageLocation.class, new GenericDaoCallback<StorageLocation>() {
            @Override
            public void callback(CriteriaQuery<StorageLocation> criteriaQuery, Root<StorageLocation> root) {
                criteriaQuery.where(getCriteriaBuilder().equal(root.get(StorageLocation_.label), label));
            }
        });
    }

    public List<StorageLocation> findByLocationType(final StorageLocation.LocationType locationType) {
        return findAll(StorageLocation.class, new GenericDaoCallback<StorageLocation>() {
            @Override
            public void callback(CriteriaQuery<StorageLocation> criteriaQuery, Root<StorageLocation> root) {
                criteriaQuery.where(getCriteriaBuilder().equal(root.get(StorageLocation_.locationType), locationType));
            }
        });
    }

    /**
     * A root location is basically a top level storage unit - no parent <br/>
     * A lot of display usage so sorting by label logic is applied
     */
    public List<StorageLocation> findRootLocations() {
        List<StorageLocation> freezerList =
        findAll(StorageLocation.class, new GenericDaoCallback<StorageLocation>() {
            @Override
            public void callback(CriteriaQuery<StorageLocation> criteriaQuery, Root<StorageLocation> root) {
                criteriaQuery.where(getCriteriaBuilder().isNull(root.get(StorageLocation_.parentStorageLocation)));
            }
        });
        Collections.sort(freezerList);
        return freezerList;
    }

    public List<StorageLocation> findByLocationTypes(List<StorageLocation.LocationType> locationTypes) {
        List<StorageLocation> resultList = new ArrayList<>();
        for (StorageLocation.LocationType locationType: locationTypes) {
            resultList.addAll(findByLocationType(locationType));
        }
        return resultList;
    }

    /**
     * Uses Oracle specific hierarchy query to get storage path to a location with one database round trip
     * @see org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation#buildLocationTrail() for JPA entity based logic
     */
    public String getLocationTrail( StorageLocation storageLocation ) {
        Long storageLocationId = storageLocation.getStorageLocationId();
        Query qry = getEntityManager().createNativeQuery("SELECT LISTAGG( label, ' > ' ) WITHIN GROUP ( ORDER BY level DESC ) \n"
                                                                 + "   FROM storage_location \n"
                                                                 + "START WITH storage_location_id = ? \n"
                                                                 + "CONNECT BY PRIOR parent_storage_location = storage_location_id");
        qry.setParameter( 1, storageLocationId );
        String locationTrail = qry.getSingleResult().toString();
        return locationTrail;
    }

    /**
     * Find count of racks and plates in a location (slot) <br/>
     * Use getRackStoredContainerCount for performance purposes if summing up everything in a GUAGERACK or BOX
     */
    public int getSlotStoredContainerCount(StorageLocation location ) {
        int count = 0;
        CriteriaBuilder cb = getCriteriaBuilder();

        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<RackOfTubes> rackRoot = cq.from( RackOfTubes.class );
        Predicate p = cb.equal(rackRoot.get(LabVessel_.storageLocation), location.getStorageLocationId());
        cq.select( cb.count( rackRoot ) );
        cq.where(p);
        count += getEntityManager().createQuery(cq).getSingleResult().intValue();

        cq = cb.createQuery(Long.class);
        Root<StaticPlate> plateRoot = cq.from( StaticPlate.class );
        p = cb.equal(plateRoot.get(LabVessel_.storageLocation), location.getStorageLocationId());
        cq.select( cb.count( plateRoot ) );
        cq.where(p);
        count += getEntityManager().createQuery(cq).getSingleResult().intValue();

        return count;
    }

    /**
     * Find count values in all SLOT types in a GUAGERACK or BOX type
     * @return Triple of 3 values : <br/>
     * Number of child SLOT types in the parent <br/>
     * Sum of the number of racks and static plates stored in all child SLOT types <br/>
     * Sum of the capacities of all child SLOT types
     *
     */
    public Triple<Integer,Integer,Integer> getRackStoredContainerCount(StorageLocation location ) {
        Integer slotCount, allocatedCount, capacityCount;
        Query q = getEntityManager().createNativeQuery("select count(storage_location_id) " +
                "     , sum(stored_count) " +
                "     , sum(capacity) " +
                "  from ( " +
                "    select storage_location_id " +
                "         , nvl( storage_capacity, 0 ) as capacity " +
                "         , ( select count(*) " +
                "               from lab_vessel " +
                "              where dtype in ( 'RackOfTubes', 'StaticPlate' ) " +
                "                and storage_location = storage_location_id ) as stored_count " +
                "      from storage_location " +
                "     where parent_storage_location = :parent_id " +
                "       and location_type = 'SLOT'  )");
        q.setParameter("parent_id", location.getStorageLocationId());
        try {
            Object[] counts = (Object[]) q.getSingleResult();
            slotCount = new Integer(counts[0].toString());
            allocatedCount = new Integer(counts[1].toString());
            capacityCount = new Integer(counts[2].toString());
        } catch ( NoResultException nre ){
            slotCount = allocatedCount = capacityCount = new Integer(0);
        }
        return Triple.of(slotCount, allocatedCount, capacityCount);
    }

    /**
     * Find count of tubes in a location (mostly relevant for LOOSE location type)
     */
    public int getStoredTubeCount(StorageLocation location ) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<BarcodedTube> root = cq.from( BarcodedTube.class );
        Predicate p = cb.equal(root.get(LabVessel_.storageLocation), location.getStorageLocationId());
        cq.select( cb.count( root ) );
        cq.where(p);
        return getEntityManager().createQuery(cq).getSingleResult().intValue();
    }
}
