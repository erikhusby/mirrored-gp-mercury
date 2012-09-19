package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.athena.entity.bsp.BSPCollection;
import org.broadinstitute.gpinformatics.athena.entity.bsp.BSPSample;
import org.broadinstitute.gpinformatics.athena.entity.common.ChangeEvent;
import org.broadinstitute.gpinformatics.athena.entity.common.Name;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequest;
import org.broadinstitute.gpinformatics.athena.entity.person.Person;
import org.broadinstitute.gpinformatics.athena.infrastructure.quote.Funding;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ResearchProject)) return false;

        final ResearchProject that = (ResearchProject) o;

        if (analysts != null ? !analysts.equals(that.analysts) : that.analysts != null) return false;
        if (creation != null ? !creation.equals(that.creation) : that.creation != null) return false;
        if (experimentRequests != null ? !experimentRequests.equals(that.experimentRequests) : that.experimentRequests != null)
            return false;
        if (fundings != null ? !fundings.equals(that.fundings) : that.fundings != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (irbNotes != null ? !irbNotes.equals(that.irbNotes) : that.irbNotes != null) return false;
        if (irbNumbers != null ? !irbNumbers.equals(that.irbNumbers) : that.irbNumbers != null) return false;
        if (modification != null ? !modification.equals(that.modification) : that.modification != null) return false;
        if (sampleCohorts != null ? !sampleCohorts.equals(that.sampleCohorts) : that.sampleCohorts != null)
            return false;
        if (samples != null ? !samples.equals(that.samples) : that.samples != null) return false;
        if (sponsoringScientists != null ? !sponsoringScientists.equals(that.sponsoringScientists) : that.sponsoringScientists != null)
            return false;
        if (synopsis != null ? !synopsis.equals(that.synopsis) : that.synopsis != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (creation != null ? creation.hashCode() : 0);
        result = 31 * result + (synopsis != null ? synopsis.hashCode() : 0);
        result = 31 * result + (modification != null ? modification.hashCode() : 0);
        result = 31 * result + (sponsoringScientists != null ? sponsoringScientists.hashCode() : 0);
        result = 31 * result + (fundings != null ? fundings.hashCode() : 0);
        result = 31 * result + (analysts != null ? analysts.hashCode() : 0);
        result = 31 * result + (sampleCohorts != null ? sampleCohorts.hashCode() : 0);
        result = 31 * result + (irbNumbers != null ? irbNumbers.hashCode() : 0);
        result = 31 * result + (experimentRequests != null ? experimentRequests.hashCode() : 0);
        result = 31 * result + (samples != null ? samples.hashCode() : 0);
        result = 31 * result + (irbNotes != null ? irbNotes.hashCode() : 0);
        return result;
    }


    //
//    @Override
//    public boolean equals(Object obj) {
//        return EqualsBuilder.reflectionEquals(this, obj);
//     }
//    @Override
//    public int hashCode() {
//        return HashCodeBuilder.reflectionHashCode(this);
//    }
//    @Override
//    public String toString() {
//        return ToStringBuilder.reflectionToString(this);
//    }
}
