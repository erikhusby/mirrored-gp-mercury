package org.broadinstitute.gpinformatics.mercury.entity.storage;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.codehaus.jackson.annotate.JsonIgnore;
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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collection;

/**
 * Stores position of materials in the laboratory. Self references to Parent Location allow for deeply
 * nested locations. A RackOfTubes can be found in Slot A1 of Gage Rack G5 on Shelf 3 of the Deli Fridge
 *
 * e.g. to create a Freezer with two cabinets one with 4 shelves and one with 3 shelves
 */
@Entity
@Audited
@Table(schema = "mercury")
public class StorageLocation {

    public enum LocationType {
        Refridgerator("Refridgerator"),
        Freezer("Freezer"),
        ShelvingUnit("Shelving Unit"),
        Shelf("Shelf"),
        GageRack("Gage Rack"),
        Slot("Slot"),
        Cabinet("Cabinet");

        private final String displayName;

        LocationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Id
    @SequenceGenerator(name = "SEQ_LOCATION", schema = "mercury", sequenceName = "SEQ_LOCATION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LOCATION")
    @Column(name = "LOCATION_ID", nullable = true)
    private Long storageLocationId;

    private String label;

    @Enumerated(EnumType.STRING)
    private LocationType locationType;

    @ManyToOne
    private StorageLocation parentStorageLocation;

    @OneToMany(mappedBy = "parentStorageLocation")
    private Collection<StorageLocation> childrenStorageLocation;

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, mappedBy = "storageLocation")
    private Collection<LabVessel> labVessels;

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

    @JsonIgnore
    public StorageLocation getParentStorageLocation() {
        return parentStorageLocation;
    }

    public void setParentStorageLocation(StorageLocation parentStorageLocation) {
        this.parentStorageLocation = parentStorageLocation;
    }

    public Collection<StorageLocation> getChildrenStorageLocation() {
        return childrenStorageLocation;
    }

    public void setChildrenStorageLocation(
            Collection<StorageLocation> childrenStorageLocation) {
        this.childrenStorageLocation = childrenStorageLocation;
    }

    public Collection<LabVessel> getLabVessels() {
        return labVessels;
    }

    public void setLabVessels(Collection<LabVessel> labVessels) {
        this.labVessels = labVessels;
    }
}
