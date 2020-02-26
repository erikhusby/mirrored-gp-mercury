package org.broadinstitute.gpinformatics.infrastructure.bsp.migration;

import org.apache.commons.lang3.tuple.Pair;
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

    // Map the container label to mercury vessel/bsp ID pair
    private Map<String, Pair<LabVessel, Long>> storedContainerLabelToVesselMap = new HashMap<>();

    void addBspToMercuryLocationPk( Long bspPk, StorageLocation mercuryStorageLocation) {
        bspToMercuryLocationMap.put( bspPk, mercuryStorageLocation );
    }

    StorageLocation getMercuryLocationPk( Long bspPk ) {
        return bspToMercuryLocationMap.get( bspPk );
    }

    void addVesselToBspLocation(LabVessel vessel, Long bspLocationId, Long bspReceptacleId ){
        storedContainerLabelToVesselMap.put( vessel.getLabel(), Pair.of( vessel, bspReceptacleId ) );
    }

    Map<String, Pair<LabVessel, Long>> getStoredContainerLabelToVesselMap(){
        return storedContainerLabelToVesselMap;
    }

    StorageLocation getLocationByBspId( Long bspLocationId ) {
        return bspToMercuryLocationMap.get(bspLocationId);
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
        // Allow drag and drop of racks in Mercury
        if( bspStorageType.equals("Rack") ) {
            return StorageLocation.LocationType.GAUGERACK;
        }
        return  StorageLocation.LocationType.getByDisplayName(bspStorageType);
    }

    /**
     * Try to find the mercury lab vessel type associated with BSP_RECEPTACLE_TYPE.RECEPTACLE_NAME
     * @return LabVessel subclass and type, null if no match found
     */
    static Pair<Class<? extends LabVessel>,Object> getMercuryVesselType( String bspReceptacleType ) {
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

        // Misspelled and don't want to add to Mercury, the BSP fix may or may not have been run by this time
        if (bspReceptacleType.equals("Eppendoff Flip-top [1.5mL] (Gates)")) {
            return Pair.of(BarcodedTube.class, BarcodedTube.BarcodedTubeType.EppendorfFliptop15_Gates);
        }

        // Caller has to deal with nothing found
        return null;
    }

}
