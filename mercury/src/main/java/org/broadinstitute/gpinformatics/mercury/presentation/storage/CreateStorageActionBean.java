package org.broadinstitute.gpinformatics.mercury.presentation.storage;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

/**
 * A Stripes action bean to allow the creation of Laboratory Storage locations such as Freezers
 */
@UrlBinding("/storage/createStorage.action")
public class CreateStorageActionBean extends CoreActionBean {
    private static final Log logger = LogFactory.getLog(CreateStorageActionBean.class);

    public static final String CHOOSE_STORAGE_UNIT = "chooseStorageUnit";

    private static final String VIEW_PAGE = "/storage/create_storage.jsp";

    private StorageUnit storageUnit;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        storageUnit = new StorageUnit();
    }

    //TODO validate storage unit name is not in database and not null
    @HandlesEvent(CHOOSE_STORAGE_UNIT)
    public Resolution chooseStorageUnit() {
        logger.debug("Storage Unit initialized: " + storageUnit);
        return new ForwardResolution(VIEW_PAGE);
    }

    public StorageUnit getStorageUnit() {
        return storageUnit;
    }

    public void setStorageUnit(
            StorageUnit storageUnit) {
        this.storageUnit = storageUnit;
    }

    public enum Level {
        FREEZER,
        SHELF,
        RACK,
        SLOT
    }

    public enum StorageUnitType {
        Refridgerator("Refridgerator", Level.FREEZER),
        Freezer("Freezer", Level.FREEZER),
        ShelvingUnit("Shelving Unit", Level.FREEZER),
        Cabinet("Cabinet", Level.FREEZER),
        Shelf("Shelf", Level.SHELF),
        Rack("Rack", Level.RACK);

        private final String displayName;
        private final Level level;

        StorageUnitType(String displayName, Level level) {
            this.displayName = displayName;
            this.level = level;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Level getLevel() {
            return level;
        }
    }

    public class StorageUnit {
        private String name;
        private StorageUnitType storageUnitType;
        private int sections;
        private int shelves;
        private int slots;

        public StorageUnit() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public StorageUnitType getStorageUnitType() {
            return storageUnitType;
        }

        public void setStorageUnitType(
                StorageUnitType storageUnitType) {
            this.storageUnitType = storageUnitType;
        }

        public int getSections() {
            return sections;
        }

        public void setSections(int sections) {
            this.sections = sections;
        }

        public int getShelves() {
            return shelves;
        }

        public void setShelves(int shelves) {
            this.shelves = shelves;
        }

        public int getSlots() {
            return slots;
        }

        public void setSlots(int slots) {
            this.slots = slots;
        }
    }
}
