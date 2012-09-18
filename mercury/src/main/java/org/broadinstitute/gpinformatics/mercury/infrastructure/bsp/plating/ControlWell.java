package org.broadinstitute.gpinformatics.mercury.infrastructure.bsp.plating;

public class ControlWell implements Plateable {

    private Well well;

    private Control control;

    private Float volume;

    private Float concentration;

    private String quoteID;


    public ControlWell(Well well, Control.Positive control, Float volume, Float concentration, String quoteID) {

        if (well == null)
            throw new RuntimeException("Well can not be null");

        if (control == null)
            throw new RuntimeException("Control can not be null");

        if (volume == null)
            throw new RuntimeException("Volume can not be null for a positive control well");
        if (volume < 1)
            throw new RuntimeException("Volume must be >= 1 uL");

        if (concentration == null)
            throw new RuntimeException("Concentration can not be null for a positive control well");
        if (concentration <= 0)
            throw new RuntimeException("Concentration must be positive for a positive control well");

        if (quoteID == null)
            throw new RuntimeException("Positive controls require a quote!");

        this.well = well;
        this.control = control;
        this.volume = volume;
        this.concentration = concentration;
        this.setQuoteID(quoteID);
    }


    public ControlWell(Well well, Control.Negative control) {

        if (well == null)
            throw new RuntimeException("Well can not be null");

        if (control == null)
            throw new RuntimeException("Control can not be null");

        this.well = well;
        this.control = control;
    }


    /**
     * For "extra" positive or negative controls that BSP will plate to
     * unspecified wells.
     *
     * @param control
     */
    public ControlWell(Control control) {

        if (control == null)
            throw new RuntimeException("Control can not be null");

        this.control = control;
    }


    public Well getWell() {
        return well;
    }


    public void setWell(Well well) {
        this.well = well;
    }


    public Control getControl() {
        return control;
    }


    public void setControl(Control control) {
        this.control = control;
    }


    public Float getVolume() {
        return volume;
    }


    public void setVolume(Float volume) {
        this.volume = volume;
    }


    public Float getConcentration() {
        return concentration;
    }


    public void setConcentration(Float concentration) {
        this.concentration = concentration;
    }


    public String getQuoteID() {
        return quoteID;
    }


    public void setQuoteID(String quoteID) {
        this.quoteID = quoteID;
    }


    @Override
    public String toString() {
        return "ControlWell [well=" + well + ", control=" + control
                + ", volume=" + volume + ", concentration=" + concentration
                + "]";
    }


    @Override
    public String getSampleId() {
        return getControl().getSampleId();
    }

    @Override
    public Well getSpecifiedWell() {
        return getWell();
    }

    @Override
    public String getPlatingQuote() {
        return getQuoteID();
    }
}
