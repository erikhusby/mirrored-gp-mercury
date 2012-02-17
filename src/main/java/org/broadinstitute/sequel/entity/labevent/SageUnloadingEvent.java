package org.broadinstitute.sequel.entity.labevent;


import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.billing.Invoice;
import org.broadinstitute.sequel.entity.billing.Priceable;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

public class SageUnloadingEvent extends AbstractLabEvent implements Priceable {

    private Invoice invoice;
    
    @Override
    public LabEventName getEventName() {
        return LabEventName.SAGE_UNLOADED;
    }

    @Override
    public boolean isBillable() {
        return true;
    }

    @Override
    public Invoice getInvoice() {
        return invoice;
    }

    @Override
    public Date getPriceableCreationDate() {
        return getEventDate();
    }

    @Override
    public Collection<SampleInstance> getSampleInstances() {
        final Collection<SampleInstance> sampleSheets = new HashSet<SampleInstance>();
        for (LabVessel target: getTargetLabVessels()) {
            sampleSheets.addAll(target.getSampleInstances());
        }
        // i wonder if we should start making lots of things
        // in here unmodifiable.
        return Collections.unmodifiableCollection(sampleSheets);

    }

    /**
     * Hey, however many samples I've got, that's
     * my max.  Maybe this should be factored out
     * somewhere.
     * @return
     */
    @Override
    public int getMaximumSplitFactor() {
        final Collection<StartingSample> aliquots = new HashSet<StartingSample>();
        for (LabVessel target: getTargetLabVessels()) {
            for (SampleInstance sampleInstance:target.getSampleInstances()) {
                aliquots.add(sampleInstance.getStartingSample());
            }
        }
        return aliquots.size();
    }

    @Override
    public String getLabNameOfPricedItem() {
        return "Sage Unloading for " + getTargetLabVessels();  //um, toString for all plates!
    }

    @Override
    public String getPriceListItemName() {
        return "Whatever The Quote Server Says, Hardcode it here.";
    }

    @Override
    public Collection<SampleSheet> getAllSampleSheets() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void applyMolecularStateChanges() throws InvalidMolecularStateException {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void validateSourceMolecularState() throws InvalidMolecularStateException {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void validateTargetMolecularState() throws InvalidMolecularStateException {
        throw new RuntimeException("I haven't been written yet.");
    }
}
