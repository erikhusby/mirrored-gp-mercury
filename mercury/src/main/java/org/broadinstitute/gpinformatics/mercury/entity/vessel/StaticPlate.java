package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.hibernate.envers.Audited;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A traditional plate.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class StaticPlate extends LabVessel implements VesselContainerEmbedder<PlateWell>, Serializable {

    public enum PlateType {
        Eppendorf96("Eppendorf96", VesselGeometry.G12x8),
        CovarisRack("CovarisRack", VesselGeometry.G12x8),
        IndexedAdapterPlate96("IndexedAdapterPlate96", VesselGeometry.G12x8),
        SageCassette("SageCassette", VesselGeometry.SAGE_CASSETTE),
        Fluidigm48_48AccessArrayIFC("Fluidigm48.48AccessArrayIFC", VesselGeometry.FLUIDIGM_48_48),
        FilterPlate96("FilterPlate96", VesselGeometry.G12x8),
        Eppendorf384("Eppendorf384", VesselGeometry.G24x16);

        private String displayName;
        private VesselGeometry vesselGeometry;

        PlateType(String displayName, VesselGeometry vesselGeometry) {
            this.displayName = displayName;
            this.vesselGeometry = vesselGeometry;
        }

        public String getDisplayName() {
            return displayName;
        }

        private static Map<String, PlateType> mapDisplayNameToType = new HashMap<String, PlateType>();
        static {
            for (PlateType plateType : PlateType.values()) {
                mapDisplayNameToType.put(plateType.getDisplayName(), plateType);
            }
        }

        public static PlateType getByDisplayName(String displayName) {
            return mapDisplayNameToType.get(displayName);
        }

        public VesselGeometry getVesselGeometry() {
            return vesselGeometry;
        }
    }


    @Embedded
    private VesselContainer<PlateWell> vesselContainer = new VesselContainer<PlateWell>(this);

    @Enumerated(EnumType.STRING)
    private PlateType plateType;

    public StaticPlate(String label, PlateType plateType) {
        super(label);
        this.plateType = plateType;
    }

    /** For Hibernate */
    protected StaticPlate() {
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return plateType.getVesselGeometry();
    }

    public PlateType getPlateType() {
        return plateType;
    }

    @Override
    public CONTAINER_TYPE getType() {
        return CONTAINER_TYPE.STATIC_PLATE;
    }

    public VesselContainer<PlateWell> getContainerRole() {
        return this.vesselContainer;
    }

    public void setVesselContainer(VesselContainer<PlateWell> vesselContainer) {
        this.vesselContainer = vesselContainer;
    }
}
