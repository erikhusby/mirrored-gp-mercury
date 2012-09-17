package org.broadinstitute.sequel.infrastructure.bsp.plating;


/**
 * Interface for mapping between Squid Sequencing Work Requests and BSP Plating
 * Work Requests. This describes the way GSP PMs would like aliquots made from
 * BSP stock samples.
 *
 * @author mcovarr
 */
public class SeqWorkRequestAliquot implements Plateable {

    private String bspStockSampleId;

    private Float desiredDNAConcentration;

    private Float desiredVolume;

    private String quoteId;


    public SeqWorkRequestAliquot(String bspStockSampleId, Float desiredVolume, Float desiredDNAConcentration, String quoteId) {
        setBspStockSampleId(bspStockSampleId);
        setDesiredVolume(desiredVolume);
        setDesiredDNAConcentration(desiredDNAConcentration);
        setQuoteId(quoteId);
    }

    /**
     * @return The Sample ID (e.g. SM-12CO4) of the BSP stock sample from
     *         which the aliquot should be made.
     */
    public String getBspStockSampleId() {
        return bspStockSampleId;
    }

    /**
     * @return Stringified quote to use for this sample/aliquot
     */
    String getQuoteId() {
        return quoteId;
    }

    /**
     * @return desired aliquot concentration (ng/uL)
     */
    public Float getDesiredDNAConcentration() {
        return desiredDNAConcentration;
    }

    /**
     * @return desired aliquot volume (uL)
     */
    public Float getDesiredVolume() {
        return desiredVolume;
    }


    public void setBspStockSampleId(String bspStockSampleId) {

        if (bspStockSampleId == null)
            throw new RuntimeException("BSP stock sample ID can not be null");

        this.bspStockSampleId = bspStockSampleId;
    }

    public void setDesiredDNAConcentration(Float desiredDNAConcentration) {

        if (desiredDNAConcentration == null)
            throw new RuntimeException("Desired DNA concentration can not be null");
        if (desiredDNAConcentration <= 0)
            throw new RuntimeException("Desired DNA concentration must be positive");

        this.desiredDNAConcentration = desiredDNAConcentration;
    }

    public void setDesiredVolume(Float desiredVolume) {
        if (desiredVolume == null)
            throw new RuntimeException("Desired volume can not be null");
        if (desiredVolume < 1)
            throw new RuntimeException("Desired volume must be >= 1.0 uL");

        this.desiredVolume = desiredVolume;
    }


    public void setQuoteId(String quoteId) {
        if (quoteId == null)
            throw new RuntimeException("Plating quote ID can not be null");
        this.quoteId = quoteId;
    }


    @Override
    public String getSampleId() {
        return getBspStockSampleId();
    }

    @Override
    public Well getSpecifiedWell() {
        // non-controls don't specify a well
        return null;
    }

    @Override
    public String getPlatingQuote() {
        return quoteId;
    }

    @Override
    public Float getVolume() {
        return getDesiredVolume();
    }

    @Override
    public Float getConcentration() {
        return getDesiredDNAConcentration();
    }
}
