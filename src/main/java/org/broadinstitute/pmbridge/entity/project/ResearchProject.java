package org.broadinstitute.pmbridge.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.broadinstitute.pmbridge.entity.bsp.BSPCollection;
import org.broadinstitute.pmbridge.entity.bsp.BSPSample;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequest;
import org.broadinstitute.pmbridge.entity.person.Person;

import java.util.Collection;
import java.util.HashSet;

/**
 * Concrete class for a Research project.
 * Note the collection members are sets as we we shouldn't have dupe values in these collections
 * There will only be small numbers of scientists, funding-sources, sample-cohorts, IrbNumbers and
 * experiment  requests per research project but there could be many many samples per research project.
 *
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/12/12
 * Time: 12:21 PM
 */
public class ResearchProject extends AbstractResearchProject {

    private final Collection<Person> sponsoringScientists = new HashSet<Person>();
    private final Collection<FundingSource> fundingSources = new HashSet<FundingSource>();
    private final Collection<BSPCollection> sampleCohorts = new HashSet<BSPCollection>();
    private final Collection<String> irbNumbers = new HashSet<String>();
    private final Collection<ExperimentRequest> experimentRequests = new HashSet<ExperimentRequest>();
    private Collection<BSPSample> samples = new HashSet<BSPSample>();
    private String irbNotes;

    public ResearchProject(Person creator, Name title, ResearchProjectId id, String synopsis ) {
        super(creator, title, id, synopsis);
    }

    public String getIrbNotes() {
        return irbNotes;
    }

    public Collection<Person> getSponsoringScientists() {
        return sponsoringScientists;
    }
    public Collection<Person> addSponsoringScientist(Person scientist) {
        sponsoringScientists.add(scientist);
        return sponsoringScientists;
    }
    public Collection<Person> removeSponsoringScientist(Person scientist) {
        sponsoringScientists.remove(scientist);
        return sponsoringScientists;
    }

    public Collection<FundingSource> getFundingSources() {
        return fundingSources;
    }
    public Collection<FundingSource> addFundingSource(FundingSource source) {
        fundingSources.add(source);
        return fundingSources;
    }
    public Collection<FundingSource> removeFundingSource(FundingSource source) {
        fundingSources.remove(source);
        return fundingSources;
    }

    public Collection<BSPCollection> getSampleCohorts() {
        return sampleCohorts;
    }
    public Collection<BSPCollection> addBSPCollection(BSPCollection bspCollection ){
        sampleCohorts.add(bspCollection);
        return sampleCohorts;
    }
    public Collection<BSPCollection> removeBSPCollection(BSPCollection bspCollection ){
        sampleCohorts.remove(bspCollection);
        return sampleCohorts;
    }

    public Collection<String> getIrbNumbers() {
        return irbNumbers;
    }
    public Collection<String> addIrbNumber(String irbNumber) {
        irbNumbers.add(irbNumber);
        return irbNumbers;
    }
    public Collection<String> removeIrbNumber(String irbNumber) {
        irbNumbers.remove(irbNumber);
        return irbNumbers;
    }


    public Collection<ExperimentRequest> getExperimentRequests() {
        return experimentRequests;
    }
    public Collection<ExperimentRequest> addExperimentRequest(ExperimentRequest experimentRequest) {
        experimentRequests.add(experimentRequest);
        return experimentRequests;
    }
    public Collection<ExperimentRequest> removeExperimentRequest(ExperimentRequest experimentRequest) {
        experimentRequests.add(experimentRequest);
        return experimentRequests;
    }


    public Collection<BSPSample> addSample(BSPSample sample) {
        samples.add(sample);
        return samples;
    }
    public Collection<BSPSample> removeSample(BSPSample sample) {
        samples.add(sample);
        return samples;
    }

    public Collection<BSPSample> getSamples() {
        return samples;
    }
    public void setSamples(Collection<BSPSample> samples) {
        this.samples = samples;
    }
    public void setIrbNotes(String irbNotes) {
        this.irbNotes = irbNotes;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
     }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
