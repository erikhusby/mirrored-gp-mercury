package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.mercury.control.reagent.MolecularIndexingSchemeFactory;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO used by IndexedPlateFactory
 */
public class PlateWellIndexAssociation {
    private final String plateBarcode;
    private final String wellName;
    private final String technology;
    private final List<MolecularIndexingSchemeFactory.IndexPositionPair> positionPairs =
            new ArrayList<>();

    PlateWellIndexAssociation(String barcode, String well, String tech) {
        this.plateBarcode = barcode;
        this.wellName = well;
        this.technology = tech;
    }

    MolecularIndexingSchemeFactory.IndexPositionPair getFirstSequence() {
        if (this.positionPairs.isEmpty()) {
            throw new IllegalStateException("No indexes were ever set on PlateWellIndexAssociation.");
        }

        if (this.positionPairs.size() > 1) {
            throw new IllegalStateException("Multiple indexes were found.");
        }

        return this.positionPairs.get(0);
    }


    int getIndexCount() {
        return this.positionPairs.size();
    }

    void addIndex(MolecularIndexingScheme.IndexPosition hint, String sequence) {
        this.positionPairs.add(new MolecularIndexingSchemeFactory.IndexPositionPair(hint, sequence));
    }

    MolecularIndexingSchemeFactory.IndexPositionPair[] getPositionPairs() {
        MolecularIndexingSchemeFactory.IndexPositionPair[] array =
                new MolecularIndexingSchemeFactory.IndexPositionPair[this.positionPairs.size()];
        return this.positionPairs.toArray(array);
    }

    public String getPlateBarcode() {
        return plateBarcode;
    }

    public String getWellName() {
        return wellName;
    }

    public String getTechnology() {
        return technology;
    }
}
