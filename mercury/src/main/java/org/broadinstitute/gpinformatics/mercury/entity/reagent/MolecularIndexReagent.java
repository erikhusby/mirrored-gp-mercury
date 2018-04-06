package org.broadinstitute.gpinformatics.mercury.entity.reagent;


import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Collection;

@Entity
@Audited
public class MolecularIndexReagent extends Reagent {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MOLECULAR_INDEXING_SCHEME")
    @BatchSize(size = 500)
    private MolecularIndexingScheme molecularIndexingScheme;

    public MolecularIndexReagent(MolecularIndexingScheme molecularIndexingScheme) {
        // todo jmt what to pass to super?
        super(null, null, null);
        this.molecularIndexingScheme = molecularIndexingScheme;
    }

    public MolecularIndexReagent() {
    }

    public MolecularIndexingScheme getMolecularIndexingScheme() {
        return molecularIndexingScheme;
    }

    /**
     * Returns a string representation of the given indexes.
     */
    public static String getIndexesString(Collection<MolecularIndexReagent> indexes) {
        StringBuilder indexInfo = new StringBuilder();
        if (CollectionUtils.isNotEmpty(indexes)) {
            for (MolecularIndexReagent indexReagent : indexes) {
                indexInfo.append(indexReagent.getMolecularIndexingScheme().getName());
                indexInfo.append(" - ");
                for (MolecularIndexingScheme.IndexPosition hint : indexReagent.getMolecularIndexingScheme()
                        .getIndexes().keySet()) {
                    MolecularIndex index = indexReagent.getMolecularIndexingScheme().getIndexes().get(hint);
                    indexInfo.append(index.getSequence());
                    indexInfo.append("\n");
                }
            }
        }
        return indexInfo.toString();
    }


}
