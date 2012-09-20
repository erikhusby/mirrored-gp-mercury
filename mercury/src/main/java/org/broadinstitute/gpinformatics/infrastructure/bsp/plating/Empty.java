package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;


public class Empty implements Plateable {

    private Well well;

    public Empty(int index, Size size, Order order) {
        well = new Well(index, size, order);
    }


    @Override
    public String getSampleId() {
        return "EMPTY";
    }

    @Override
    public Well getSpecifiedWell() {
        return well;
    }

    @Override
    public String getPlatingQuote() {
        return null;
    }

    @Override
    public Float getVolume() {
        return null;
    }

    @Override
    public Float getConcentration() {
        return null;
    }

}
