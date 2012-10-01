package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.common.ChangeEvent;
import org.broadinstitute.gpinformatics.athena.entity.orders.Order;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

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
public class ResearchProject {

    public enum Status {
        Open, Archived
    }

    private Long id;

    private Status status;

    private ChangeEvent creation;
    private ChangeEvent modification;
    private String title;
    private String synopsis;

    // People related to the project
    private final Map<RoleType, Set<ProjectPerson>> associatedPeople = new HashMap<RoleType, Set<ProjectPerson>> ();

    // Information about externally managed items
    private final Set<Cohort> sampleCohorts = new HashSet<Cohort>();
    private final Set<FundingId> fundingIDs = new HashSet<FundingId>();

    private final Set<String> irbNumbers = new HashSet<String>();
    private String irbNotes;

    private final Set<Order> orders = new HashSet<Order>();

    public ResearchProject(Person creator, String title, String synopsis) {
        this.title = title;
        this.synopsis = synopsis;
        this.creation = new ChangeEvent(creator);
        this.modification = new ChangeEvent(new Date(this.creation.date.getTime()), creator);
    }

    // Getters
    public String getTitle() {
        return title;
    }

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
    public void setTitle(String title) {
        this.title = title;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreation(ChangeEvent creation) {
        this.creation = creation;
    }

    public Set<Order> getOrders() {
        return orders;
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

    public void setIrbNotes(String irbNotes) {
        this.irbNotes = irbNotes;
    }

    public Set<Cohort> getSampleCohorts() {
        return Collections.unmodifiableSet(sampleCohorts);
    }
    public Set<Cohort> addCohort(Cohort sampleCohort ){
        sampleCohorts.add(sampleCohort);
        return Collections.unmodifiableSet(sampleCohorts);
    }

    public Set<Cohort> removeCohort(Cohort sampleCohort ){
        sampleCohorts.remove(sampleCohort);
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

    public void addPerson(RoleType role, Person person) {
        Set<ProjectPerson> peopleForRole = associatedPeople.get(role);
        if (peopleForRole == null) {
            peopleForRole = new HashSet<ProjectPerson>();
            associatedPeople.put(role, peopleForRole);
        }

        peopleForRole.add(new ProjectPerson(role, person));
    }

    public Set<Person> getPeople(RoleType role) {
        Set<Person> people = new HashSet<Person> ();

        if (associatedPeople.get(role) != null) {
            for (ProjectPerson projectPerson : associatedPeople.get(role)) {
                people.add(projectPerson.getPerson());
            }
        }

        return people;
    }

    public Set<FundingId> getFundingIds() {
        return Collections.unmodifiableSet(fundingIDs);
    }

    public Set<FundingId> addFunding(FundingId fundingId) {
        fundingIDs.add(fundingId);
        return Collections.unmodifiableSet(fundingIDs);
    }

    public Set<FundingId> removeFunding(FundingId fundingID) {
        fundingIDs.remove(fundingID);
        return Collections.unmodifiableSet(fundingIDs);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     *
     * @param other The other object
     * @return boolean
     */
    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( !(other instanceof ResearchProject) ) return false;
        ResearchProject castOther = (ResearchProject) other;
        return new EqualsBuilder().append(title, castOther.title).isEquals();
    }

    /**
     *
     * @return int
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(title).toHashCode();
    }
}
