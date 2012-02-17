package org.broadinstitute.sequel.entity.vessel;


import org.broadinstitute.sequel.entity.billing.Invoice;
import org.broadinstitute.sequel.entity.billing.Priceable;
import org.broadinstitute.sequel.entity.run.RunCartridge;
import org.broadinstitute.sequel.entity.run.RunChamber;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;

import java.util.Date;
import java.util.Set;

/**
 * A plate loaded into the pacbio instrument
 * is a plain old plate.  it's also a run
 * cartridge.  and it's probably priceable.
 */
public class PacbioPlate extends StaticPlate implements RunCartridge, Priceable {

    public PacbioPlate(String label) {
        super(label);
    }

    @Override
    public String getPriceListItemName() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getLabNameOfPricedItem() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public int getMaximumSplitFactor() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Invoice getInvoice() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Date getPriceableCreationDate() {
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * I'm not sure what a run chamber is for pacbio.
     * Is it a well on the plate?
     * @return
     */
    @Override
    public Iterable<RunChamber> getChambers() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getCartridgeName() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getCartridgeBarcode() {
        throw new RuntimeException("I haven't been written yet.");
    }


}
