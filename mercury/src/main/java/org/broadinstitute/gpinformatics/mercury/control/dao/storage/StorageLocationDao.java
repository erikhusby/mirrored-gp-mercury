package org.broadinstitute.gpinformatics.mercury.control.dao.storage;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateful
@RequestScoped
public class StorageLocationDao extends GenericDao {

    public static Map<String, StorageLocation> testMap = new HashMap();
    public static StorageLocation testSlot = null;
    public static List<StorageLocation>  TestData = BuildTestData();

    private static List<StorageLocation> BuildTestData() {
        StorageLocation freezer80 = new StorageLocation("Freezer -80", StorageLocation.LocationType.Freezer, null);
        StorageLocation freezer20 = new StorageLocation("Freezer -20", StorageLocation.LocationType.Freezer, null);
        StorageLocation freezer4 = new StorageLocation("Freezer +4", StorageLocation.LocationType.Freezer, null);

        StorageLocation shelf = new StorageLocation("Shelf 1", StorageLocation.LocationType.Shelf, freezer80);
        freezer80.setChildrenStorageLocation(Collections.singletonList(shelf));
        StorageLocation rack = new StorageLocation("Rack A", StorageLocation.LocationType.GageRack, shelf);
        shelf.setChildrenStorageLocation(Collections.singletonList(rack));
        StorageLocation slot = new StorageLocation("Slot 23", StorageLocation.LocationType.Slot, rack);
        rack.setChildrenStorageLocation(Collections.singletonList(slot));
        testSlot = slot;

        testMap.put("Freezer -80", freezer80);
        testMap.put("Freezer -20", freezer20);
        testMap.put("Freezer +4", freezer4);

        return Arrays.asList(freezer4, freezer80, freezer20);
    }

    public StorageLocation findByIdentifier(String identifier) {
        if (testSlot == null)
            BuildTestData();
        if (testMap.containsKey(identifier)) {
            return testMap.get(identifier);
        }
        return testSlot;
//        return findSingle(Location.class, Location_.label, identifier);
    }

    public List<StorageLocation> findByLocationType(final StorageLocation.LocationType locationType) {
        return TestData;
        /*
        return findAll(Location.class, new GenericDaoCallback<Location>() {
            @Override
            public void callback(CriteriaQuery<Location> criteriaQuery, Root<Location> root) {
                criteriaQuery.where(getCriteriaBuilder().equal(root.get(Location_.locationType), locationType));
            }
        });*/
    }
}
