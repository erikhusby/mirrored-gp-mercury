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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
@XmlRootElement(name="Project")
public class ResearchProject {

    private Name title;
    public static Long UNSPECIFIED_ID = 0L;
    private Long id = UNSPECIFIED_ID;
    private ChangeEvent creation;
    private String synopsis;
    private ChangeEvent modification;
    private final Collection<Person> sponsoringScientists = new HashSet<Person>();
    private final Collection<Funding> fundings = new HashSet<Funding>();
    private final Collection<Person> analysts = new HashSet<Person>();
    private final Collection<BSPCollection> sampleCohorts = new HashSet<BSPCollection>();
    private final Collection<String> irbNumbers = new HashSet<String>();
    private final Collection<ExperimentRequest> experimentRequests = new HashSet<ExperimentRequest>();
    private Collection<BSPSample> samples = new HashSet<BSPSample>();
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

    public Collection<Person> getSponsoringScientists() {
        return Collections.unmodifiableCollection(sponsoringScientists);
    }
    public Collection<Person> addSponsoringScientist(Person scientist) {
        sponsoringScientists.add(scientist);
        return Collections.unmodifiableCollection(sponsoringScientists);
    }
    public Collection<Person> removeSponsoringScientist(Person scientist) {
        sponsoringScientists.remove(scientist);
        return Collections.unmodifiableCollection(sponsoringScientists);
    }

    public Collection<Person> getAnalysts() {
        return Collections.unmodifiableCollection(analysts);
    }
    public Collection<Person> addAanalysts(Person analyst) {
        analysts.add(analyst);
        return Collections.unmodifiableCollection(analysts);
    }
    public Collection<Person> removeAnalyst(Person analyst) {
        analysts.remove(analyst);
        return Collections.unmodifiableCollection(analysts);
    }

    public Collection<Funding> getFundings() {
        return Collections.unmodifiableCollection(fundings);
    }
    public Collection<Funding> addFunding(Funding source) {
        fundings.add(source);
        return Collections.unmodifiableCollection(fundings);
    }
    public Collection<Funding> removeFunding(Funding source) {
        fundings.remove(source);
        return Collections.unmodifiableCollection(fundings);
    }

    public Collection<BSPCollection> getSampleCohorts() {
        return Collections.unmodifiableCollection(sampleCohorts);
    }
    public Collection<BSPCollection> addBSPCollection(BSPCollection bspCollection ){
        sampleCohorts.add(bspCollection);
        return Collections.unmodifiableCollection(sampleCohorts);
    }
    public Collection<BSPCollection> removeBSPCollection(BSPCollection bspCollection ){
        sampleCohorts.remove(bspCollection);
        return Collections.unmodifiableCollection(sampleCohorts);
    }

    public Collection<String> getIrbNumbers() {
        return Collections.unmodifiableCollection(irbNumbers);
    }
    public Collection<String> addIrbNumber(String irbNumber) {
        irbNumbers.add(irbNumber);
        return Collections.unmodifiableCollection(irbNumbers);
    }
    public Collection<String> removeIrbNumber(String irbNumber) {
        irbNumbers.remove(irbNumber);
        return Collections.unmodifiableCollection(irbNumbers);
    }


    public Collection<ExperimentRequest> getExperimentRequests() {
        return Collections.unmodifiableCollection(experimentRequests);
    }
    public Collection<ExperimentRequest> addExperimentRequest(ExperimentRequest experimentRequest) {
        experimentRequests.add(experimentRequest);
        return Collections.unmodifiableCollection(experimentRequests);
    }
    public Collection<ExperimentRequest> removeExperimentRequest(ExperimentRequest experimentRequest) {
        experimentRequests.remove(experimentRequest);
        return Collections.unmodifiableCollection(experimentRequests);
    }


    public Collection<BSPSample> addSample(BSPSample sample) {
        samples.add(sample);
        return Collections.unmodifiableCollection(samples);
    }
    public Collection<BSPSample> removeSample(BSPSample sample) {
        samples.remove(sample);
        return Collections.unmodifiableCollection(samples);
    }

    public Collection<BSPSample> getSamples() {
        return Collections.unmodifiableCollection(samples);
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
