package org.broadinstitute.gpinformatics.mercury.entity.storage;

/**
 * Created by jowalsh on 6/13/17.
 */
public class StorageContainerType {

    public enum Level {
        FREEZER,
        SHELF,
        RACK,
        SLOT
    }

    public enum LocationType {
        FreezerMinus80("Freezer (-80 Celsius)"),
        FreezerMinus20("Freezer (-20 Celsius)"),
        RefridgeratorPlus4("Refridgerator (4 Celsius)"),
        Freezer("Freezer"),
        ShelvingUnit("Shelving Unit"),
        Side("Side"),
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

}
