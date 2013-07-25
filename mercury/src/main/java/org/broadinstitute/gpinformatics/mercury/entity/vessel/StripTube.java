package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.Embedded;
import javax.persistence.Entity;

/**
 * Represents a strip tube, several tubes molded into a single piece of plasticware, e.g. 8 tubes in the same formation
 * as a rack column.  The Strip tube has a barcode, but each constituent tube does not.
 */
@Entity
@Audited
public class StripTube extends LabVessel implements VesselContainerEmbedder<StripTubeWell> {

    protected StripTube() {
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return VesselGeometry.STRIP_TUBE;
    }

    public enum Positions {
        ONE("1"),
        TWO("2"),
        THREE("3"),
        FOUR("4"),
        FIVE("5"),
        SIX("6"),
        SEVEN("7"),
        EIGHT("8");

        private String display;

        Positions(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return this.display;
        }
    }

    @Embedded
    VesselContainer<StripTubeWell> vesselContainer = new VesselContainer<>(this);

    public StripTube(String label) {
        super(label);
    }

    @Override
    public VesselContainer<StripTubeWell> getContainerRole() {
        return vesselContainer;
    }

    @Override
    public ContainerType getType() {
        return ContainerType.STRIP_TUBE;
    }

}
