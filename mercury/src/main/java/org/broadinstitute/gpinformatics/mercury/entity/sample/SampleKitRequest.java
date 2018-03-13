package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Represents the upload manifest of an external library (EZPass, NewTech) spreadsheet upload.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "sample_kit_request")
@BatchSize(size = 50)
public class SampleKitRequest {

    @SequenceGenerator(name = "seq_sample_kit_request", schema = "mercury",  sequenceName = "seq_sample_kit_request")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_sample_kit_request")
    @Id
    private Long sampleKitRequestId;

    private String firstName;

    private String lastName;

    private String organization;

    private String address;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private String phone;

    private String email;

    private String commonName;

    private String genus;

    private String species;

    private String irbApprovalRequired;

    public Long getSampleKitRequest() {
        return sampleKitRequestId;
    }

    public void setSampleKitRequest(Long sampleKitRequestId) {
        this.sampleKitRequestId = sampleKitRequestId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getCollaboratorName() {
        return firstName + " " + lastName;
    }

    public String getIrbApprovalRequired() {
        return irbApprovalRequired;
    }

    public void setIrbApprovalRequired(String irbApprovalRequired) {
        this.irbApprovalRequired = irbApprovalRequired;
    }
}
