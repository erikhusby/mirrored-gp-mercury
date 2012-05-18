package org.broadinstitute.pmbridge.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.broadinstitute.pmbridge.entity.bsp.BSPCollection;
import org.broadinstitute.pmbridge.entity.bsp.BSPSample;
import org.broadinstitute.pmbridge.entity.common.ChangeEvent;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequest;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.infrastructure.quote.Funding;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

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
@XmlRootElement(name="Project")
public class ResearchProject {

    private Name title;
    public static Long UNSPECIFIED_ID = 0L;
    private Long id = UNSPECIFIED_ID;
    private ChangeEvent creation;
    private String synopsis;
    private ChangeEvent modification;
    private final Set<Person> sponsoringScientists = new HashSet<Person>();
    private final Set<Funding> fundings = new HashSet<Funding>();
    private final Set<Person> analysts = new HashSet<Person>();
    private final Set<BSPCollection> sampleCohorts = new HashSet<BSPCollection>();
    private final Set<String> irbNumbers = new HashSet<String>();
    private final Set<ExperimentRequest> experimentRequests = new HashSet<ExperimentRequest>();
    private Set<BSPSample> samples = new HashSet<BSPSample>();
    private String irbNotes;


    public ResearchProject(Person creator, Name title, String synopsis) {
        this.title = title;
        this.id = id;
        this.synopsis = synopsis;
        this.creation = new ChangeEvent(creator);
        this.modification = new ChangeEvent(new Date(this.creation.date.getTime()), creator);
    }


    // Getters
    @XmlAttribute(name="researchProjectName")
    public Name getTitle() {
        return title;
    }

    @XmlAttribute(name="RPID")
    public Long getId() {
        return id;
    }

    public ChangeEvent getCreation() {
        return creation;
    }

    public String getSynopsis() {
        return synopsis;
    }
    public ChangeEvent getModification() {
        return modification;
    }

    //Setters
    public void setTitle(Name title) {
        this.title = title;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreation(ChangeEvent creation) {
        this.creation = creation;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }
    public void setModification(ChangeEvent modification) {
        this.modification = modification;
    }

    public String getIrbNotes() {
        return irbNotes;
    }

    public Set<Person> getSponsoringScientists() {
        return Collections.unmodifiableSet(sponsoringScientists);
    }
    public Set<Person> addSponsoringScientist(Person scientist) {
        sponsoringScientists.add(scientist);
        return Collections.unmodifiableSet(sponsoringScientists);
    }
    public Set<Person> removeSponsoringScientist(Person scientist) {
        sponsoringScientists.remove(scientist);
        return Collections.unmodifiableSet(sponsoringScientists);
    }

    public Set<Person> getAnalysts() {
        return Collections.unmodifiableSet(analysts);
    }
    public Set<Person> addAanalysts(Person analyst) {
        analysts.add(analyst);
        return Collections.unmodifiableSet(analysts);
    }
    public Set<Person> removeAnalyst(Person analyst) {
        analysts.remove(analyst);
        return Collections.unmodifiableSet(analysts);
    }

    public Set<Funding> getFundings() {
        return Collections.unmodifiableSet(fundings);
    }
    public Set<Funding> addFunding(Funding source) {
        fundings.add(source);
        return Collections.unmodifiableSet(fundings);
    }
    public Set<Funding> removeFunding(Funding source) {
        fundings.remove(source);
        return Collections.unmodifiableSet(fundings);
    }

    public Set<BSPCollection> getSampleCohorts() {
        return Collections.unmodifiableSet(sampleCohorts);
    }
    public Set<BSPCollection> addBSPCollection(BSPCollection bspCollection ){
        sampleCohorts.add(bspCollection);
        return Collections.unmodifiableSet(sampleCohorts);
    }
    public Set<BSPCollection> removeBSPCollection(BSPCollection bspCollection ){
        sampleCohorts.remove(bspCollection);
        return Collections.unmodifiableSet(sampleCohorts);
    }

    public Set<String> getIrbNumbers() {
        return Collections.unmodifiableSet(irbNumbers);
    }
    public Set<String> addIrbNumber(String irbNumber) {
        irbNumbers.add(irbNumber);
        return Collections.unmodifiableSet(irbNumbers);
    }
    public Set<String> removeIrbNumber(String irbNumber) {
        irbNumbers.remove(irbNumber);
        return Collections.unmodifiableSet(irbNumbers);
    }


    public Set<ExperimentRequest> getExperimentRequests() {
        return Collections.unmodifiableSet(experimentRequests);
    }
    public Set<ExperimentRequest> addExperimentRequest(ExperimentRequest experimentRequest) {
        experimentRequests.add(experimentRequest);
        return Collections.unmodifiableSet(experimentRequests);
    }
    public Set<ExperimentRequest> removeExperimentRequest(ExperimentRequest experimentRequest) {
        experimentRequests.remove(experimentRequest);
        return Collections.unmodifiableSet(experimentRequests);
    }


    public Set<BSPSample> addSample(BSPSample sample) {
        samples.add(sample);
        return Collections.unmodifiableSet(samples);
    }
    public Set<BSPSample> removeSample(BSPSample sample) {
        samples.remove(sample);
        return Collections.unmodifiableSet(samples);
    }

    public Set<BSPSample> getSamples() {
        return Collections.unmodifiableSet(samples);
    }
    public void setSamples(Set<BSPSample> samples) {
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
