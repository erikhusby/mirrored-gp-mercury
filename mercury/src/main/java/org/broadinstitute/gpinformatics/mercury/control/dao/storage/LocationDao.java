package org.broadinstitute.gpinformatics.mercury.control.dao.storage;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.storage.Location;
import org.broadinstitute.gpinformatics.mercury.entity.storage.Location_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.Collections;

@Stateful
@RequestScoped
public class LocationDao extends GenericDao {

    public Location findByIdentifier(String identifier) {
        Location freezer = new Location("Freezer -80", Location.LocationType.FreezerMinus80, null);
        Location shelf = new Location("Shelf 1", Location.LocationType.Shelf, freezer);
        freezer.setChildrenLocation(Collections.singletonList(shelf));
        Location rack = new Location("Rack A", Location.LocationType.GageRack, shelf);
        shelf.setChildrenLocation(Collections.singletonList(rack));
        Location slot = new Location("Slot 23", Location.LocationType.Slot, rack);
        rack.setChildrenLocation(Collections.singletonList(slot));
        return slot;
//        return findSingle(Location.class, Location_.label, identifier);
    }
}
