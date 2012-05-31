package org.broadinstitute.sequel.boundary.pmbridge;


import org.broadinstitute.sequel.boundary.squid.BaitSet;
import org.broadinstitute.sequel.boundary.squid.BaitSetDesignType;
import org.broadinstitute.sequel.boundary.squid.Organism;
import org.broadinstitute.sequel.boundary.squid.ReferenceSequence;

public class ToSquid {

    public static Organism map(org.broadinstitute.sequel.boundary.Organism organism) {

        Organism ret = new Organism();

        ret.setCommonName(organism.getCommonName());
        ret.setGenus(organism.getGenus());
        ret.setId(organism.getId());
        ret.setSpecies(organism.getSpecies());

        return ret;
    }



    public static BaitSet map(org.broadinstitute.sequel.boundary.BaitSet baitSet) {

        BaitSet ret = new BaitSet();

        ret.setActive(baitSet.isActive());
        ret.setDesignName(baitSet.getDesignName());
        ret.setDesignType(BaitSetDesignType.fromValue(baitSet.getDesignType().value()));
        ret.setId(baitSet.getId());

        return ret;

    }


    public static ReferenceSequence map(org.broadinstitute.sequel.boundary.ReferenceSequence referenceSequence) {

        ReferenceSequence ret = new ReferenceSequence();

        ret.setActive(referenceSequence.isActive());
        ret.setAlias(referenceSequence.getAlias());
        ret.setId(referenceSequence.getId());

        return ret;
    }
}
