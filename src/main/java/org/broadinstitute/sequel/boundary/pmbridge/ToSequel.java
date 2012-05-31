package org.broadinstitute.sequel.boundary.pmbridge;


import org.broadinstitute.sequel.boundary.*;


public class ToSequel {

    public static ReferenceSequenceListResult map(org.broadinstitute.sequel.boundary.squid.ReferenceSequenceListResult squidList) {

        ReferenceSequenceListResult sequelList = new ReferenceSequenceListResult();

        for (org.broadinstitute.sequel.boundary.squid.ReferenceSequence squidSequence : squidList.getReferenceSequenceList()) {
            ReferenceSequence sequelSequence = new ReferenceSequence();
            sequelSequence.setActive(squidSequence.isActive());
            sequelSequence.setAlias(squidSequence.getAlias());
            sequelSequence.setId(squidSequence.getId());

            sequelList.getReferenceSequenceList().add(sequelSequence);

        }


        return sequelList;
    }



    public static BaitSetListResult map(org.broadinstitute.sequel.boundary.squid.BaitSetListResult squidList) {

        BaitSetListResult sequelList = new BaitSetListResult();

        for (org.broadinstitute.sequel.boundary.squid.BaitSet baitSet : squidList.getBaitSetList()) {
            BaitSet sequelBaitSet = new BaitSet();

            sequelBaitSet.setActive(baitSet.isActive());
            sequelBaitSet.setDesignName(baitSet.getDesignName());
            sequelBaitSet.setDesignType(BaitSetDesignType.fromValue(baitSet.getDesignType().value()));
            sequelBaitSet.setId(baitSet.getId());

            sequelList.getBaitSetList().add(sequelBaitSet);

        }

        return sequelList;
    }



    public static SummarizedPassListResult map(org.broadinstitute.sequel.boundary.squid.SummarizedPassListResult squidList) {

        SummarizedPassListResult ret = new SummarizedPassListResult();

        for (org.broadinstitute.sequel.boundary.squid.SummarizedPass summarizedPass : squidList.getSummarizedPassList()) {

            SummarizedPass sequelSummarizedPass = new SummarizedPass();
            sequelSummarizedPass.setCreatedDate(summarizedPass.getCreatedDate());
            sequelSummarizedPass.setCreator(summarizedPass.getCreator());
            sequelSummarizedPass.setLastAcceptedVersion(summarizedPass.getLastAcceptedVersion());
            sequelSummarizedPass.setLastModified(summarizedPass.getLastModified());
            sequelSummarizedPass.setNickname(summarizedPass.getNickname());
            sequelSummarizedPass.setPassNumber(summarizedPass.getPassNumber());
            sequelSummarizedPass.setResearchProject(summarizedPass.getResearchProject());
            sequelSummarizedPass.setStatus(PassStatus.fromValue(summarizedPass.getStatus().value()));
            sequelSummarizedPass.setTitle(summarizedPass.getTitle());
            sequelSummarizedPass.setType(PassType.fromValue(summarizedPass.getType().value()));
            sequelSummarizedPass.setUpdatedBy(summarizedPass.getUpdatedBy());
            sequelSummarizedPass.setVersion(summarizedPass.getVersion());

            ret.getSummarizedPassList().add(sequelSummarizedPass);

        }

        return ret;

    }
    
    
    
    public static OrganismListResult map(org.broadinstitute.sequel.boundary.squid.OrganismListResult squidList) {

        OrganismListResult ret = new OrganismListResult();

        for (org.broadinstitute.sequel.boundary.squid.Organism organism : squidList.getOrganismList()) {

            Organism sequelOrganism = new Organism();
            sequelOrganism.setCommonName(organism.getCommonName());
            sequelOrganism.setGenus(organism.getGenus());
            sequelOrganism.setId(organism.getId());
            sequelOrganism.setSpecies(organism.getSpecies());

            ret.getOrganismList().add(sequelOrganism);
        }

        return ret;

    }


    public static SquidPersonList map(org.broadinstitute.sequel.boundary.squid.SquidPersonList squidList) {

        SquidPersonList ret = new SquidPersonList();

        for (org.broadinstitute.sequel.boundary.squid.SquidPerson squidPerson : squidList.getSquidPerson()) {

            SquidPerson sequelPerson = new SquidPerson();
            sequelPerson.setFirstName(squidPerson.getFirstName());
            sequelPerson.setLastName(squidPerson.getLastName());
            sequelPerson.setLogin(squidPerson.getLogin());
            sequelPerson.setPersonID(squidPerson.getPersonID());

            ret.getSquidPerson().add(sequelPerson);
        }

        return ret;
    }





}
