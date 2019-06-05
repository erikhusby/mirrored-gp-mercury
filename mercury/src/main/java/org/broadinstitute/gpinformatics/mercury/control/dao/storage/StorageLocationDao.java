package org.broadinstitute.gpinformatics.mercury.control.dao.storage;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.omg.Dynamic.Parameter;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.Query;
import javax.persistence.criteria.*;
import java.util.ArrayList;
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
     * A root location is basically a top level storage unit - no parent
     */
    public List<StorageLocation> findRootLocations() {
        return findAll(StorageLocation.class, new GenericDaoCallback<StorageLocation>() {
            @Override
            public void callback(CriteriaQuery<StorageLocation> criteriaQuery, Root<StorageLocation> root) {
                criteriaQuery.where(getCriteriaBuilder().isNull(root.get(StorageLocation_.parentStorageLocation)));
            }
        });
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
    public String getLocationTrail( Long storageLocationId ) {
        Query qry = getEntityManager().createNativeQuery("SELECT LISTAGG( label, ' > ' ) WITHIN GROUP ( ORDER BY level DESC ) \n"
                                                                 + "   FROM storage_location \n"
                                                                 + "START WITH storage_location_id = ? \n"
                                                                 + "CONNECT BY PRIOR parent_storage_location = storage_location_id");
        qry.setParameter( 1, storageLocationId );
        return qry.getSingleResult().toString();
    }

    public int getStoredVesselCount( StorageLocation location ) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<LabVessel> root = cq.from( LabVessel.class );
        Predicate p = cb.equal(root.get(LabVessel_.storageLocation), location.getStorageLocationId());
        cq.select( cb.count( root ) );
        cq.where(p);
        return getEntityManager().createQuery(cq).getSingleResult().intValue();
    }
}
