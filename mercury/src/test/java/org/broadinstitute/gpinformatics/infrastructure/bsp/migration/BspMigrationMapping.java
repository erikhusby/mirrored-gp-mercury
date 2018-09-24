package org.broadinstitute.gpinformatics.infrastructure.bsp.migration;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;

import java.util.HashMap;
import java.util.Map;

public class BspMigrationMapping {

    // Map the BSP location ID to the Mercury StorageLocation
    private Map<Long, StorageLocation> bspToMercuryLocationMap = new HashMap<>();

    // Map the Mercury vessel label to the BSP location ID
    private Map<String, Long> labelToBspLocationMap = new HashMap<>();

    // Map the container label (Mercury) to mercury vessel/bsp ID pair
    private Map<String, Pair<LabVessel, Long>> storedContainerLabelToVesselMap = new HashMap<>();

    // Map the BSP tube/well ID to the Mercury vessel
    private Map<Long, LabVessel> bspToMercuryTubeMap = new HashMap<>();

    public void addBspToMercuryLocationPk( Long bspPk, StorageLocation mercuryStorageLocation) {
        bspToMercuryLocationMap.put( bspPk, mercuryStorageLocation );
    }

    public StorageLocation getMercuryLocationPk( Long bspPk ) {
        return bspToMercuryLocationMap.get( bspPk );
    }

    public void addVesselToBspLocation(LabVessel vessel, Long bspLocationId, Long bspReceptacleId ){
        labelToBspLocationMap.put( vessel.getLabel(), bspLocationId );
        storedContainerLabelToVesselMap.put( vessel.getLabel(), Pair.of( vessel, bspReceptacleId ) );
    }

    public Map<String, Pair<LabVessel, Long>> getStoredContainerLabelToVesselMap(){
        return storedContainerLabelToVesselMap;
    }

    public StorageLocation getLocationByBspId( Long bspLocationId ) {
        return bspToMercuryLocationMap.get(bspLocationId);
    }

    public void addBspSampleToVesselMap( Long bspId, LabVessel containedVessel) {
        bspToMercuryTubeMap.put(bspId, containedVessel);
    }

    /**
     * Get Mercury enum corresponding to BSP type, null if no match and let caller commit suicide on it
     */
    StorageLocation.LocationType getMercuryLocationTypeFromBspType(String bspStorageType ) {
        if( bspStorageType.startsWith("Freezer") ) {
            return StorageLocation.LocationType.FREEZER;
        }
        if( bspStorageType.startsWith("Refrigerator") ) {
            return StorageLocation.LocationType.REFRIGERATOR;
        }
        if( bspStorageType.startsWith("Side") ) {
            return StorageLocation.LocationType.SECTION;
        }
        return  StorageLocation.LocationType.getByDisplayName(bspStorageType);
    }

    /**
     * Try to find the mercury lab vessel type associated with BSP_RECEPTACLE_TYPE.RECEPTACLE_NAME
     * @return LabVessel subclass and type, null if no match found
     */
    Pair<Class<? extends LabVessel>,Object> getMercuryVesselType( String bspReceptacleType ) {
        RackOfTubes.RackType rackType = RackOfTubes.RackType.getByDisplayName(bspReceptacleType);
        if( rackType != null ) {
            return Pair.of( RackOfTubes.class, rackType );
        }
        StaticPlate.PlateType plateType = StaticPlate.PlateType.getByDisplayName(bspReceptacleType);
        if( plateType != null ) {
            return Pair.of( StaticPlate.class, plateType );
        }
        BarcodedTube.BarcodedTubeType tubeType = BarcodedTube.BarcodedTubeType.getByDisplayName(bspReceptacleType);
        if( tubeType != null ) {
            return Pair.of( BarcodedTube.class, tubeType );
        }
        PlateWell.WellType wellType = PlateWell.WellType.getByDisplayName(bspReceptacleType);
        if( wellType != null ) {
            return Pair.of( PlateWell.class, wellType );
        }
        // Caller has to deal with nothing found
        return null;
    }

}
