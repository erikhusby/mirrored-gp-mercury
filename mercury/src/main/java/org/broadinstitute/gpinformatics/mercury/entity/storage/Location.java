package org.broadinstitute.gpinformatics.mercury.entity.storage;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores position of materials in the laboratory. Self references to Parent Location allow for deeply
 * nested locations. A RackOfTubes can be found in Slot A1 of Gage Rack G5 on Shelf 3 of the Deli Fridge
 *
 * e.g. to create a Freezer with two cabinets one with 4 shelves and one with 3 shelves
 */
@Entity
@Audited
@Table(schema = "mercury")
public class Location {

    public enum Level {

    }

    public enum LocationType {
        FreezerMinus80("Freezer (-80 Celsius)"),
        FreezerMinus20("Freezer (-20 Celsius)"),
        RefridgeratorPlus4("Refridgerator (4 Celsius)"),
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
    private Long locationId;

    private String label;

    @Enumerated(EnumType.STRING)
    private LocationType locationType;

    @ManyToOne
    private Location parentLocation;

    @OneToMany(mappedBy = "parentLocation")
    private Collection<Location> childrenLocation;

    public Location() {
    }

    public Location(String label,
                    LocationType locationType,
                    Location parentLocation) {
        this.label = label;
        this.locationType = locationType;
        this.parentLocation = parentLocation;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
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
    public Location getParentLocation() {
        return parentLocation;
    }

    public void setParentLocation(Location parentLocation) {
        this.parentLocation = parentLocation;
    }

    public Collection<Location> getChildrenLocation() {
        return childrenLocation;
    }

    public void setChildrenLocation(
            Collection<Location> childrenLocation) {
        this.childrenLocation = childrenLocation;
    }
}
