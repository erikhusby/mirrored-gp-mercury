package org.broadinstitute.gpinformatics.athena.boundary.orders;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.MaterialInfo;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;

public class ProductOrderKitDetailData {
    private int numberOfSamples;
    private SampleKitWorkRequest.MoleculeType moleculeType;
    private MaterialInfo materialInfo;

    @SuppressWarnings("UnusedDeclaration")
    /** Required by JAXB. */
    ProductOrderKitDetailData() {
    }

    public int getNumberOfSamples() {
        return numberOfSamples;
    }

    public void setNumberOfSamples(int numberOfSamples) {
        this.numberOfSamples = numberOfSamples;
    }

    public SampleKitWorkRequest.MoleculeType getMoleculeType() {
        return moleculeType;
    }

    public void setMoleculeType(SampleKitWorkRequest.MoleculeType moleculeType) {
        this.moleculeType = moleculeType;
    }

    public MaterialInfo getMaterialInfo() {
        return materialInfo;
    }

    public void setMaterialInfo(MaterialInfo materialInfo) {
        this.materialInfo = materialInfo;
    }
}
