package org.broadinstitute.gpinformatics.mercury.presentation.storage;

import net.sourceforge.stripes.action.*;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.*;
import java.util.logging.Logger;

/**
 * This action bean handles allocation availability in storage system.
 */
@UrlBinding(value = StorageAllocationActionBean.ACTION_BEAN_URL)
public class StorageAllocationActionBean extends CoreActionBean {

    private static final Logger logger = Logger.getLogger(StorageAllocationActionBean.class.getName());

    public static final String ACTION_BEAN_URL = "/storage/allocation.action";
    private static final String VIEW_PAGE = "/storage/space_allocation.jsp";
    private static final String USAGE_CONTENT = "/storage/freezer_space_root.jsp";

    // Events and outcomes
    private static final String EVT_INIT = "evtInit" ;
    private static final String EVT_DRILLDOWN = "evtDrillDown";

    // Web params
    private Long storageLocationId;

    // Outputs
    Map<Long, Map> parentChildMap;
    // Slot count, used count, total count
    Map<Long, Triple<Integer,Integer,Integer>> childCapacityMap;
    Map<Long,String> nameMap;

    public StorageAllocationActionBean(){
        super();
    }

    @Inject
    private StorageLocationDao storageLocationDao;

    @DefaultHandler
    @HandlesEvent(EVT_INIT)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * AJAX call to expand a refrigerator or freezer
     */
    @HandlesEvent(EVT_DRILLDOWN)
    public Resolution search() {
        if( storageLocationId == null ) {
            return new StreamingResolution("text/html","<div class='alert alert-danger'>Storage location ID unavailable</div>");
        }

        StorageLocation freezer = storageLocationDao.findById(StorageLocation.class, storageLocationId);
        parentChildMap = new LinkedHashMap<>();
        childCapacityMap = new HashMap<>();
        nameMap = new HashMap<>();

        // Recursion
        for( StorageLocation child : freezer.getSortedChildLocations() ) {
            buildHierarchy(child, parentChildMap);
        }

        return new ForwardResolution(USAGE_CONTENT);
    }

    private void buildHierarchy(StorageLocation parent, Map<Long, Map> parentNode) {
        nameMap.put(parent.getStorageLocationId(), parent.getLabel());
        Map<Long, Map>  childNode = new LinkedHashMap<>();
        parentNode.put(parent.getStorageLocationId(), childNode );

        StorageLocation.LocationType type = parent.getLocationType();
        if( type.expectSlots() ) {
            // Get summary and stop recursion  Triple is: [slots, used, total]
            Triple<Integer,Integer,Integer> usage = storageLocationDao.getRackStoredContainerCount(parent);
            childCapacityMap.put(parent.getStorageLocationId(), usage);
        } else {
            for( StorageLocation child : parent.getSortedChildLocations() ) {
                buildHierarchy( child, childNode );
            }
        }
    }

    /**
     * Freezer list for initial accordion layout filtered by applicability to storage allocation capacity logic
     */
    public List<StorageLocation> getRootLocations(){
        List<StorageLocation> rootLocations = storageLocationDao.findRootLocations();
        for(Iterator<StorageLocation> iter = rootLocations.iterator(); iter.hasNext(); ) {
            StorageLocation storageLocation = iter.next();
            if( storageLocation.getLocationType() != StorageLocation.LocationType.FREEZER
                    && storageLocation.getLocationType() != StorageLocation.LocationType.REFRIGERATOR ) {
                // Allocation valid only for freezers and fridges
                iter.remove();
            } else if (storageLocation.getLabel().equals("BSP LN2 2")){
                // Allocation useless for Cryostraw LN2 tank
                iter.remove();
            }
        }
        return rootLocations;
    }

    public void setStorageLocationId(Long storageLocationId) {
        this.storageLocationId = storageLocationId;
    }

    /**
     * Descending tree starting at first level
     */
    public Map<Long,Map> getFreezerChildMap() {
        return parentChildMap;
    }

    /**
     * Storage name
     */
    public String getLocName( Long id) {
        return nameMap.get(id);
    }

    /**
     * Storage usage percentage, check usage for null first
     */
    public short getCapacityPercentage(Long id) {
        Triple<Integer,Integer,Integer> usage = childCapacityMap.get(id);
        if( usage != null ) {
            if( usage.getRight() > 0 ) {
                double fraction = usage.getMiddle().doubleValue() / usage.getRight().doubleValue();
                return (short) Math.round( fraction * 100.00 );
            } else {
                // No capacity?  0% used
                return 0;
            }
        } else {
            // Should have checked usage for null first
            return 0;
        }
    }

    /**
     * Storage usage used on left, total on right
     */
    public Triple<Integer,Integer,Integer> getCapacityUsage(Long id) {
        return childCapacityMap.get(id);
    }
}
