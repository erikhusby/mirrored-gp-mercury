package org.broadinstitute.gpinformatics.infrastructure.bsp.migration;

import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;

import java.util.HashMap;
import java.util.Map;

public class BspMigrationMapping {

    private Map<Long, StorageLocation> bspToMercuryLocationMap = new HashMap<>();

    public void addBspToMercuryLocationPk( Long bspPk, StorageLocation mercuryStorageLocation) {
        bspToMercuryLocationMap.put( bspPk, mercuryStorageLocation );
    }

    public StorageLocation getMercuryLocationPk( Long bspPk ) {
        return bspToMercuryLocationMap.get( bspPk );
    }

}
