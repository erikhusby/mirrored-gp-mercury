package org.broadinstitute.gpinformatics.mercury.entity.storage;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores location of vessels in the lab. Self references to Parent Location allow for deeply
 * nested locations. A RackOfTubes can be found in Slot A1 of Gage Rack G5 on Shelf 3 of the Deli Fridge
 */
@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(name = "U_STORAGE_BARCODE",columnNames = {"BARCODE"}))
public class StorageLocation {

    public enum LocationType {
        REFRIGERATOR("Refrigerator", ExpectParentLocation.FALSE),
        FREEZER("Freezer", ExpectParentLocation.FALSE),
        SHELVINGUNIT("Shelving Unit", ExpectParentLocation.FALSE),
        CABINET("Cabinet", ExpectParentLocation.FALSE),
        SECTION("Section", ExpectParentLocation.TRUE, ExpectSlots.FALSE, Moveable.FALSE, CanCreate.FALSE),
        SHELF("Shelf", ExpectParentLocation.TRUE, CanRename.TRUE),
        GAUGERACK("Gauge Rack", ExpectParentLocation.TRUE, ExpectSlots.TRUE, Moveable.TRUE, CanRename.TRUE),
        BOX("Box", ExpectParentLocation.TRUE, ExpectSlots.TRUE, Moveable.TRUE, CanRename.TRUE),
        SLOT("Slot", ExpectParentLocation.TRUE, ExpectSlots.FALSE, Moveable.FALSE, CanCreate.FALSE);

        private enum CanCreate {
            TRUE(true),
            FALSE(false);
            private boolean value;

            private CanCreate(boolean value) {
                this.value = value;
            }
        }

        private enum ExpectSlots {
            TRUE(true),
            FALSE(false);
            private boolean value;

            private ExpectSlots(boolean value) {
                this.value = value;
            }
        }

        /**
         * Whether a location would expect to have a parent. E.g. a shelf only makes sense in a freezer,
         * but a freezer wouldn't have a parent.
         */
        private enum ExpectParentLocation {
            TRUE(true),
            FALSE(false);
            private boolean value;

            private ExpectParentLocation(boolean value) {
                this.value = value;
            }
        }

        /**
         * Whether it makes sense for a location to move from a parent to another. E.g. A shelf should never move
         * between freezers, whereas Gage Racks and boxes can readily move.
         */
        private enum Moveable {
            TRUE(true),
            FALSE(false);
            private boolean value;

            private Moveable(boolean value) {
                this.value = value;
            }
        }

        private enum CanRename {
            TRUE(true),
            FALSE(false);
            private boolean value;

            private CanRename(boolean value) {
                this.value = value;
            }
        }

        private final String displayName;
        private final ExpectParentLocation expectParentLocation;
        private final ExpectSlots expectSlots;
        private final Moveable moveable;
        private final CanCreate canCreate;
        private final CanRename canRename;

        private static final List<LocationType> LIST_TOP_LEVEL_LOCATION_TYPES = new
                ArrayList<>(LocationType.values().length);
        private static final List<LocationType> LIST_CREATEABLE_LOCATION_TYPES = new
                ArrayList<>(LocationType.values().length);

        private static final Map<String, LocationType> MAP_NAME_TO_LOCATION = new
                HashMap<>(LocationType.values().length);

        LocationType(String displayName, ExpectParentLocation expectParentLocation) {
            this(displayName, expectParentLocation, ExpectSlots.FALSE, Moveable.FALSE);
        }

        LocationType(String displayName, ExpectParentLocation expectParentLocation, CanRename canRename) {
            this(displayName, expectParentLocation, ExpectSlots.FALSE, Moveable.FALSE, CanCreate.FALSE, canRename);
        }

        LocationType(String displayName, ExpectParentLocation expectParentLocation, ExpectSlots expectSlots,
                     Moveable moveable) {
            this(displayName, expectParentLocation, expectSlots, moveable, CanCreate.TRUE);
        }

        LocationType(String displayName, ExpectParentLocation expectParentLocation, ExpectSlots expectSlots,
                     Moveable moveable, CanCreate canCreate) {
            this (displayName, expectParentLocation, expectSlots, moveable, canCreate, CanRename.FALSE);
        }

        LocationType(String displayName, ExpectParentLocation expectParentLocation, ExpectSlots expectSlots,
                     Moveable moveable, CanRename canRename) {
            this(displayName, expectParentLocation, expectSlots, moveable, CanCreate.TRUE, canRename);

        }

        LocationType(String displayName, ExpectParentLocation expectParentLocation, ExpectSlots expectSlots,
                     Moveable moveable, CanCreate canCreate, CanRename canRename) {
            this.displayName = displayName;
            this.expectParentLocation = expectParentLocation;
            this.expectSlots = expectSlots;
            this.moveable = moveable;
            this.canCreate = canCreate;
            this.canRename = canRename;
        }

        static {
            for (LocationType location: LocationType.values()) {
                if (location.isTopLevelLocation()) {
                    LIST_TOP_LEVEL_LOCATION_TYPES.add(location);
                }
                if (location.isCreateable()) {
                    LIST_CREATEABLE_LOCATION_TYPES.add(location);
                }
                MAP_NAME_TO_LOCATION.put(location.getDisplayName(), location);
            }
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isTopLevelLocation() {
            return expectParentLocation == ExpectParentLocation.FALSE;
        }

        public boolean isMoveable() {
            return moveable == Moveable.TRUE;
        }

        public boolean canRename() {
            return canRename == CanRename.TRUE;
        }

        public boolean isCreateable() {
            return canCreate == CanCreate.TRUE;
        }

        public boolean expectSlots() {
            return expectSlots == ExpectSlots.TRUE;
        }

        public static List<LocationType> getTopLevelLocationTypes() {
            return LIST_TOP_LEVEL_LOCATION_TYPES;
        }

        public static List<LocationType> getCreateableLocationTypes() {
            return LIST_CREATEABLE_LOCATION_TYPES;
        }

        public static LocationType getByDisplayName(String displayName) {
            return MAP_NAME_TO_LOCATION.get(displayName);
        }
    }

    @Id
    @SequenceGenerator(name = "SEQ_STORAGE_LOCATION", schema = "mercury", sequenceName = "SEQ_STORAGE_LOCATION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_STORAGE_LOCATION")
    @Column(name = "STORAGE_LOCATION_ID")
    private Long storageLocationId;

    private String label;

    @Enumerated(EnumType.STRING)
    private LocationType locationType;

    @ManyToOne
    @JoinColumn(name = "PARENT_STORAGE_LOCATION")
    private StorageLocation parentStorageLocation;

    @OneToMany(mappedBy = "parentStorageLocation", cascade = CascadeType.PERSIST)
    private Set<StorageLocation> childrenStorageLocation = new HashSet<>();

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, mappedBy = "storageLocation")
    private Set<LabVessel> labVessels = new HashSet<>();

    private String barcode;

    public StorageLocation() {
    }

    public StorageLocation(String label,
                           LocationType locationType,
                           StorageLocation parentStorageLocation) {
        this.label = label;
        this.locationType = locationType;
        this.parentStorageLocation = parentStorageLocation;
    }

    public Long getStorageLocationId() {
        return storageLocationId;
    }

    public void setStorageLocationId(Long storageLocationId) {
        this.storageLocationId = storageLocationId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

    public StorageLocation getParentStorageLocation() {
        return parentStorageLocation;
    }

    public void setParentStorageLocation(
            StorageLocation parentStorageLocation) {
        this.parentStorageLocation = parentStorageLocation;
    }

    public Set<StorageLocation> getChildrenStorageLocation() {
        return childrenStorageLocation;
    }

    public void setChildrenStorageLocation(
            Set<StorageLocation> childrenStorageLocation) {
        this.childrenStorageLocation = childrenStorageLocation;
    }

    public Set<LabVessel> getLabVessels() {
        return labVessels;
    }

    public void setLabVessels(Set<LabVessel> labVessels) {
        this.labVessels = labVessels;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    /**
     * Uses JPA entities to get hierarchy storage path to a location - requires multiple database round trips
     * @see org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao#getLocationTrail(java.lang.Long) for single Oracle hierarchy query logic
     */
    @Transient
    public String buildLocationTrail() {
        String locationTrailString = "";
        LinkedList<StorageLocation> locationTrail = new LinkedList<>();
        locationTrail.add(this);
        StorageLocation parentLocation = getParentStorageLocation();
        while (parentLocation != null) {
            locationTrail.addFirst(parentLocation);
            parentLocation = parentLocation.getParentStorageLocation();
        }

        for (int i = 0; i < locationTrail.size(); i++) {
            StorageLocation location = locationTrail.get(i);
            locationTrailString += location.getLabel();
            if (i < locationTrail.size() - 1) {
                locationTrailString += " > ";
            }
        }
        return locationTrailString;
    }

    public static class StorageLocationLabelComparator
            implements Comparator<StorageLocation> {
        @Override
        public int compare(StorageLocation storageLocation1, StorageLocation storageLocation2) {
            boolean isNumbered = storageLocation1.getLabel().matches("[A-Za-z]+ \\d+");
            if (storageLocation1.getLocationType() == LocationType.SLOT &&
                storageLocation2.getLocationType() == LocationType.SLOT &&
                isNumbered) {
                Integer slotNumber = Integer.parseInt(storageLocation1.getLabel().replaceAll("\\D+",""));
                Integer slot2Number = Integer.parseInt(storageLocation2.getLabel().replaceAll("\\D+",""));
                return slotNumber.compareTo(slot2Number);
            }
            return storageLocation1.getLabel()
                    .compareTo(storageLocation2.getLabel());
        }
    }
}
