package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Class is used to create objects generated from bettalims.xsd.
 * The maven jaxb2 xjc generator does not create constructors, so here we do it manually.
 */
public class BettaLimsObjectFactory {
    public static CherryPickSourceType createCherryPickSourceType(String barcode, String well,
                                                                  String destinationBarcode,
                                                                  String destinationWell) {
        CherryPickSourceType cherryPickSourceType = new CherryPickSourceType();
        cherryPickSourceType.setBarcode(barcode);
        cherryPickSourceType.setWell(well);
        cherryPickSourceType.setDestinationBarcode(destinationBarcode);
        cherryPickSourceType.setDestinationWell(destinationWell);
        return cherryPickSourceType;
    }

    public static PositionMapType createPositionMapType(String barcode, List<ReceptacleType> receptacle) {
        PositionMapType positionMapType = new PositionMapType();
        positionMapType.setBarcode(barcode);
        positionMapType.getReceptacle().addAll(receptacle);
        return positionMapType;
    }

    public static ReceptacleType createReceptacleType(String barcode, String receptacleType, String position) {
        return createReceptacleType(barcode, receptacleType, position, null, null, null, null, null, null);
    }

    public static ReceptacleType createReceptacleType(String barcode, String receptacleType, String position,
                                                      String materialType, BigDecimal volume, BigDecimal concentration,
                                                      BigDecimal fragmentSize, List<ReagentType> reagent,
                                                      List<MetadataType> metadata) {
        ReceptacleType receptacle = new ReceptacleType();
        receptacle.setReceptacleType(receptacleType);
        receptacle.setBarcode(barcode);
        receptacle.setPosition(position);
        receptacle.setMaterialType(materialType);
        receptacle.setVolume(volume);
        receptacle.setConcentration(concentration);
        receptacle.setFragmentSize(fragmentSize);
        if (reagent != null) {
            receptacle.getReagent().addAll(reagent);
        }
        if (metadata != null) {
            receptacle.getMetadata().addAll(metadata);
        }
        return receptacle;
    }

    public static PlateType createPlateType(String barcode, String physType, String section, String tubeType) {
        PlateType plateType = new PlateType();
        plateType.setBarcode(barcode);
        plateType.setPhysType(physType);
        plateType.setSection(section);
        plateType.setTubeType(tubeType);
        return plateType;
    }
}
