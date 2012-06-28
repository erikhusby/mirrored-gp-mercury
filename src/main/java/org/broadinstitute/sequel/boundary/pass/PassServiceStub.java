package org.broadinstitute.sequel.boundary.pass;


import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.collection.PredicatedCollection;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.boundary.*;
import org.broadinstitute.sequel.control.pass.PassService;
import org.broadinstitute.sequel.infrastructure.deployment.Stub;

import javax.inject.Inject;
import javax.jws.WebParam;
import java.lang.reflect.InvocationTargetException;
import java.util.*;



@Stub
/**
 * Stub implementation for Arquillian non-integration container tests or other non-integration tests.
 */
public class PassServiceStub implements PassService {

    @Inject
    private Log log;

    // starting number for PASSes we'll hand out
    private int counter = 6000;


    private Map<String, AbstractPass> passStore = new HashMap<String, AbstractPass>();


    private List<ReferenceSequence> referenceSequenceStore = new ArrayList<ReferenceSequence>();


    private List<BaitSet> baitSetStore = new ArrayList<BaitSet>();


    private List<SquidPerson> broadPIStore = new ArrayList<SquidPerson>();


    private List<Organism> organismStore = new ArrayList<Organism>();


    public PassServiceStub() {
        // initialize the fixture data above (refseqs, bait sets, pi's, maybe PASSes if we want to have some preloaded)
    }


    @Override
    public String getGreeting() {
        log.info("STUBBY getGreeting() invoked!");
        return "Hello PMBridge!";
    }


    @Override
    public boolean canEditPasses(@WebParam(name = "login", partName = "login") String login) {
        return true;
    }


    @Override
    public String storePass(@WebParam(name = "pass", partName = "pass") AbstractPass pass) {

        if ( pass.getProjectInformation().getPassNumber() == null )
            pass.getProjectInformation().setPassNumber("PASS-" + (counter++) );

        passStore.put(pass.getProjectInformation().getPassNumber(), pass);

        return pass.getProjectInformation().getPassNumber();
    }


    @Override
    public PassCritique validatePass(@WebParam(name = "pass", partName = "pass") AbstractPass pass) {
        // pretty sure this is not used, always returning error-free PASSes
        return new PassCritique();
    }


    @Override
    public void abandonPass(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {

        if ( ! passStore.containsKey(passNumber) )
            throw new RuntimeException(passNumber + " not found!");

        final AbstractPass abstractPass = passStore.get(passNumber);
        abstractPass.setStatus(PassStatus.ABANDONED);

    }


    private SummarizedPassListResult summarize(Collection<AbstractPass> passes) {

        SummarizedPassListResult result = new SummarizedPassListResult();

        for ( AbstractPass pass : passes ) {

            SummarizedPass summarizedPass = new SummarizedPass();
            summarizedPass.setUpdatedBy(pass.getUpdatedBy());
            summarizedPass.setVersion(pass.getProjectInformation().getVersion());
            summarizedPass.setCreatedDate(pass.getProjectInformation().getDateCreated());
            summarizedPass.setCreator(pass.getCreator());
            summarizedPass.setLastAcceptedVersion(pass.getProjectInformation().getLastAcceptedVersion());
            summarizedPass.setLastModified(pass.getProjectInformation().getLastModified());
            summarizedPass.setNickname(pass.getProjectInformation().getNickname());
            summarizedPass.setPassNumber(pass.getProjectInformation().getPassNumber());
            summarizedPass.setResearchProject(pass.getResearchProject());
            summarizedPass.setStatus(pass.getStatus());
            summarizedPass.setTitle(pass.getProjectInformation().getTitle());

            if ( pass instanceof DirectedPass )
                summarizedPass.setType(PassType.DIRECTED);

            else if ( pass instanceof WholeGenomePass )
                summarizedPass.setType(PassType.WG);

            else if ( pass instanceof RNASeqPass )
                summarizedPass.setType(PassType.RNASEQ);

            else
                throw new RuntimeException( "Unrecognized PASS type: " + pass.getClass() );


            result.getSummarizedPassList().add(summarizedPass);

        }

        return result;
    }


    /**
     * Pick out a bean property from an object and compare to a query value.  There's probably an implementation
     * of this in a Commons library someplace but I couldn't find it.
     *
     * @param <T> Type of the object containing the property to be searched
     *
     * @param <S> Type of the bean property being searched on
     */
    private class BeanPropertyPredicate<T, S> implements Predicate<T> {

        String propertyPath;

        S searchValue;

        public BeanPropertyPredicate(String propertyPath, S searchValue) {
            this.propertyPath = propertyPath;
            this.searchValue = searchValue;
        }

        @Override
        public boolean evaluate(T t) {

            try {

                final String value = BeanUtils.getProperty(t, propertyPath);

                if (searchValue == null)
                    return value == null;

                return searchValue.equals(value);

            }
            catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

        }
    }


    @Override
    public SummarizedPassListResult searchPassesByCreator(@WebParam(name = "creator", partName = "creator") String creator) {

        Predicate<AbstractPass> predicate =
                new BeanPropertyPredicate<AbstractPass, String>("creator", creator);

        final Collection<AbstractPass> filteredPasses =
                PredicatedCollection.<AbstractPass>decorate(passStore.values(), predicate);

        return summarize(filteredPasses);

    }


    @Override
    public SummarizedPassListResult searchPassesByResearchProject(@WebParam(name = "researchProject", partName = "researchProject") String researchProject) {

        Predicate<AbstractPass> predicate =
                new BeanPropertyPredicate<AbstractPass, String>("researchProject", researchProject);

        final Collection<AbstractPass> filteredPasses =
                PredicatedCollection.<AbstractPass>decorate(passStore.values(), predicate);

        return summarize(filteredPasses);

    }



    @Override
    public SummarizedPassListResult searchPasses() {
        return summarize(passStore.values());
    }


    @Override
    public ReferenceSequenceListResult getReferenceSequencesByOrganism(@WebParam(name = "organism", partName = "organism") Organism organism) {
        throw new NotImplementedException();
    }

    @Override
    public ReferenceSequenceListResult getReferenceSequencesByBaitSet(@WebParam(name = "baitSet", partName = "baitSet") BaitSet baitSet) {
        throw new NotImplementedException();
    }



    @Override
    public ReferenceSequenceListResult getReferenceSequences() {

        ReferenceSequenceListResult result = new ReferenceSequenceListResult();

        result.getReferenceSequenceList().addAll(referenceSequenceStore);

        return result;

    }


    @Override
    public BaitSetListResult getBaitSetsByReferenceSequence(@WebParam(name = "referenceSequence", partName = "referenceSequence") ReferenceSequence referenceSequence) {
        throw new NotImplementedException();
    }



    @Override
    public BaitSetListResult getBaitSets() {
        BaitSetListResult result = new BaitSetListResult();

        result.getBaitSetList().addAll(baitSetStore);

        return result;
    }



    @Override
    public OrganismListResult getOrganisms() {

        OrganismListResult result = new OrganismListResult();

        result.getOrganismList().addAll(organismStore);

        return result;
    }



    @Override
    public AbstractPass loadPassByNumber(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {

        if ( ! passStore.containsKey(passNumber) )
            throw new RuntimeException("No such PASS: " + passNumber);

        return passStore.get(passNumber);

    }



    @Override
    public SquidPersonList getBroadPIList() {

        SquidPersonList ret = new SquidPersonList();
        ret.getSquidPerson().addAll(broadPIStore);
        return ret;

    }


    @Override
    public boolean validatePassNumber(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {

        return passStore.containsKey(passNumber);

    }
}
