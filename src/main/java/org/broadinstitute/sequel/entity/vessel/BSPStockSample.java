package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.sample.SampleInstance;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a BSP stock sample
 * LabVessel restricted to NO transfers
 */

@Entity
@NamedQueries({
        @NamedQuery(
                name = "BSPStockSample.fetchByBarcodes",
                query = "select s from BSPStockSample s where barcodes in (:barcodes)"
        ),
        @NamedQuery(
                name = "BSPStockSample.fetchByBarcode",
                query = "select s from BSPStockSample s where barcode = :barcode"
        )
})

public class BSPStockSample extends TwoDBarcodedTube {

    public BSPStockSample(String sampleName) {
        super(sampleName);
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        //no transfers .. no instances ?????
        return sampleInstances;
    }

//    @Override
//    public Set<LabEvent> getTransfersFrom() {
//        return null; //throw Exception
//    }
//
//    @Override
//    public Set<LabEvent> getTransfersTo() {
//        return null; //throw Exception
//    }


}
