package org.broadinstitute.gpinformatics.mercury.infrastructure.bsp.plating;

/**
 * A PlatingArrayElement represents a Plateable that has realized its potential
 * to be plated into a PlatingArray
 */
public class PlatingArrayElement implements Plateable {

    private Plateable plateable;
    private Well destinationWell;

    public PlatingArrayElement(Plateable plateable, Well desinationWell) {
        this.plateable = plateable;
        this.destinationWell = desinationWell;
    }

    public Well getDestinationWell() {
        return destinationWell;
    }

    public boolean hasSpecifiedWell() {
        return plateable.getSpecifiedWell() != null;
    }


    public Plateable getPlateable() {
        return plateable;
    }

    @Override
    public String getSampleId() {
        return plateable.getSampleId();
    }

    @Override
    public Well getSpecifiedWell() {
        return plateable.getSpecifiedWell();
    }

    @Override
    public String getPlatingQuote() {
        return plateable.getPlatingQuote();
    }

    @Override
    public Float getVolume() {
        return plateable.getVolume();
    }

    @Override
    public Float getConcentration() {
        return plateable.getConcentration();
    }
}
