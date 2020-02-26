package org.broadinstitute.gpinformatics.mercury.presentation.storage;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;

/**
 * A Stripes action bean to allow the creation of Laboratory Storage locations such as Freezers
 */
@UrlBinding("/storage/createStorage.action")
public class CreateStorageActionBean extends CoreActionBean {
    private static final Log logger = LogFactory.getLog(CreateStorageActionBean.class);

    public static final String CHOOSE_STORAGE_TYPE = "chooseStorageType";
    public static final String CHOOSE_STORAGE_UNIT = "chooseStorageUnit";
    public static final String VIEW_PAGE = "/storage/create_storage.jsp";

    private boolean readyForDetails;
    private String name;
    private String storageUnitTypeName;
    private long storageId;
    private Long createdStorageId;
    private StorageLocation parentStorageLocation;
    private StorageLocation.LocationType locationType;
    private int sections;
    private int shelves;
    private int slots;
    private int slotStorageCapacity = 1;

    private List<StorageLocation.LocationType> creatableLocationTypes =
            StorageLocation.LocationType.getCreateableLocationTypes();

    private MessageCollection messageCollection = new MessageCollection();

    @Inject
    private StorageLocationDao storageLocationDao;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(CHOOSE_STORAGE_TYPE)
    public Resolution chooseStorageType() {
        if (getName() == null || getName().isEmpty()) {
            messageCollection.addError("Storage name is required.");
        } else if (!storageLocationDao.findByLabel(getName()).isEmpty()) {
            messageCollection.addError("Storage name already in use: " + getName());
        } else if (storageUnitTypeName == null || storageUnitTypeName.isEmpty()) {
            messageCollection.addError("Location Type is required.");
        }
        locationType = StorageLocation.LocationType.valueOf(storageUnitTypeName);
        if (messageCollection.hasErrors()) {
            addMessages(messageCollection);
            readyForDetails = false;
            return new ForwardResolution(VIEW_PAGE);
        }
        readyForDetails = true;
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(CHOOSE_STORAGE_UNIT)
    public Resolution chooseStorageUnit() {
        if (getName() == null || getName().isEmpty()) {
            messageCollection.addError("Storage name is required.");
        } else if (!storageLocationDao.findByLabel(getName()).isEmpty()) {
            messageCollection.addError("Storage name already in use: " + getName());
            addMessages(messageCollection);
        } else if (storageUnitTypeName == null || storageUnitTypeName.isEmpty()) {
            messageCollection.addError("Location Type is required.");
            addMessages(messageCollection);
        }
        if (messageCollection.hasErrors()) {
            addMessages(messageCollection);
            return new ForwardResolution(VIEW_PAGE);
        }

        locationType = StorageLocation.LocationType.valueOf(storageUnitTypeName);
        boolean isRackType = locationType == StorageLocation.LocationType.GAUGERACK ||
                             locationType == StorageLocation.LocationType.BOX;
        if (isRackType) {
            if (getSlots() <= 0) {
                messageCollection.addError("Must have at least one slot ");
            }
            parentStorageLocation = storageLocationDao.findById(StorageLocation.class, storageId);
            if (parentStorageLocation == null) {
                messageCollection.addError("Failed to find storage location: " + storageId);
            } else if (parentStorageLocation.getLocationType().isMoveable()) {
                messageCollection.addError("Invalid Parent Storage Location Selected");
            }
        } else {
            if (getSections() <= 0) {
                messageCollection.addError("Must have at least one section ");
            }

            if (getShelves() <= 0) {
                messageCollection.addError("Must have at least one shelf ");
            }
        }
        if (messageCollection.hasErrors()) {
            addMessages(messageCollection);
            return new ForwardResolution(VIEW_PAGE);
        } else {
            StorageLocation storageLocation;
            if (isRackType) {
                storageLocation = createNewRack();
            } else {
                storageLocation = createNewFreezer();
            }
            storageLocationDao.persist(storageLocation);
            storageLocationDao.flush();
            createdStorageId = storageLocation.getStorageLocationId();
        }

        addMessage("Successfully created new storage.");
        name = "";
        sections = 0;
        shelves = 0;
        slots = 0;
        readyForDetails = false;

        return new RedirectResolution(CreateStorageActionBean.class, VIEW_ACTION)
                .flash(this);
    }

    private StorageLocation createNewRack() {
        StorageLocation rack = new StorageLocation(getName(), locationType, parentStorageLocation);
        for (int i = 1; i <= getSlots(); i++) {
            String slotName = "Slot " + i;
            StorageLocation slot = new StorageLocation(slotName, StorageLocation.LocationType.SLOT, rack);
            slot.setStorageCapacity(slotStorageCapacity);
            rack.getChildrenStorageLocation().add(slot);
        }
        return rack;
    }

    private StorageLocation createNewFreezer() {
        StorageLocation freezer = new StorageLocation(getName(), locationType, null);
        if (getSections() > 1) {
            for (int i = 0; i < getSections(); i++) {
                String sectionName = String.valueOf((char)('A' + i));
                StorageLocation section = new StorageLocation(sectionName, StorageLocation.LocationType.SECTION, freezer);
                for (int j = 0; j < getShelves(); j++) {
                    String shelfName = buildShelfName(j + 1, getShelves());
                    StorageLocation shelf = new StorageLocation(shelfName, StorageLocation.LocationType.SHELF, section);
                    section.getChildrenStorageLocation().add(shelf);
                }
                freezer.getChildrenStorageLocation().add(section);
            }
        } else {
            for (int j = 0; j < getShelves(); j++) {
                String shelfName = " " + buildShelfName(j + 1, getShelves());
                StorageLocation shelf = new StorageLocation(shelfName, StorageLocation.LocationType.SHELF, freezer);
                freezer.getChildrenStorageLocation().add(shelf);
            }
        }

        return freezer;
    }

    /**
     * Build a shelf name with leading zeroes (if necessary), to sort correctly.
     */
    private String buildShelfName(int currentShelf, int totalShelves) {
        int numShelfDigits = (int) Math.log10(currentShelf) + 1;
        int numTotalShelfDigits = (int) Math.log10(totalShelves) + 1;
        return "Shelf " + StringUtils.repeat('0', numTotalShelfDigits - numShelfDigits) + currentShelf;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStorageUnitTypeName() {
        return storageUnitTypeName;
    }

    public void setStorageUnitTypeName(String storageUnitTypeName) {
        this.storageUnitTypeName = storageUnitTypeName;
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

    public long getStorageId() {
        return storageId;
    }

    public void setStorageId(long storageId) {
        this.storageId = storageId;
    }

    public StorageLocation.LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(
            StorageLocation.LocationType locationType) {
        this.locationType = locationType;
    }

    public boolean isReadyForDetails() {
        return readyForDetails;
    }

    public void setReadyForDetails(boolean readyForDetails) {
        this.readyForDetails = readyForDetails;
    }

    public List<StorageLocation.LocationType> getCreatableLocationTypes() {
        return creatableLocationTypes;
    }

    public void setCreatableLocationTypes(
            List<StorageLocation.LocationType> creatableLocationTypes) {
        this.creatableLocationTypes = creatableLocationTypes;
    }

    public Long getCreatedStorageId() {
        return createdStorageId;
    }

    public void setCreatedStorageId(Long createdStorageId) {
        this.createdStorageId = createdStorageId;
    }

    public int getSlotStorageCapacity() {
        return slotStorageCapacity;
    }

    public void setSlotStorageCapacity(int slotStorageCapacity) {
        this.slotStorageCapacity = slotStorageCapacity;
    }
}
