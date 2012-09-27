package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.common.ChangeEvent;
import org.broadinstitute.gpinformatics.athena.entity.common.Name;
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

    private ResearchProjectID id;

    private ChangeEvent creation;
    private ChangeEvent modification;
    private Name title;
    private String synopsis;

    // People related to the project
    private final Map<RoleType, Set<ProjectPerson>> associatedPeople = new HashMap<RoleType, Set<ProjectPerson>> ();

    // Information about externally managed items
    private final Set<Cohort> sampleCohorts = new HashSet<Cohort>();
    private final Set<FundingID> fundingIDs = new HashSet<FundingID>();

    private final Set<String> irbNumbers = new HashSet<String>();
    private String irbNotes;

    private final Set<Order> orders = new HashSet<Order>();

    public ResearchProject(Person creator, Name title, String synopsis) {
        this.title = title;
        this.synopsis = synopsis;
        this.creation = new ChangeEvent(creator);
        this.modification = new ChangeEvent(new Date(this.creation.date.getTime()), creator);
    }

    // Getters
    public Name getTitle() {
        return title;
    }

    public ResearchProjectID getId() {
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

    public void setId(ResearchProjectID id) {
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
    public Set<Cohort> addBSPCollection(Cohort bspCollection ){
        sampleCohorts.add(bspCollection);
        return Collections.unmodifiableSet(sampleCohorts);
    }
    public Set<Cohort> removeBSPCollection(Cohort bspCollection ){
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

    public void addPerson(RoleType role, Person person) {
        Set<ProjectPerson> peopleForRole = associatedPeople.get(role);
        if (peopleForRole == null) {
            peopleForRole = new HashSet<ProjectPerson>();
            associatedPeople.put(role, peopleForRole);
        }

        peopleForRole.add(new ProjectPerson(role, person));
    }

    public Set<FundingID> getFundingIds() {
        return Collections.unmodifiableSet(fundingIDs);
    }

    public Set<FundingID> addFunding(FundingID fundingId) {
        fundingIDs.add(fundingId);
        return Collections.unmodifiableSet(fundingIDs);
    }

    public Set<FundingID> removeFunding(FundingID fundingID) {
        fundingIDs.remove(fundingID);
        return Collections.unmodifiableSet(fundingIDs);
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
        return new EqualsBuilder().append(id, castOther.id).isEquals();
    }

    /**
     *
     * @return int
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).toHashCode();
    }
}
