package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.StringUtils;
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

    public Long getSampleKitRequestId() {
        return sampleKitRequestId;
    }

    public void setSampleKitRequestId(Long sampleKitRequestId) {
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

    public String getIrbApprovalRequired() {
        return irbApprovalRequired;
    }

    public void setIrbApprovalRequired(String irbApprovalRequired) {
        this.irbApprovalRequired = irbApprovalRequired;
    }

    public String toBody() {
        return new StringBuilder(System.lineSeparator()).
                append("Name: ").append(getFirstName()).append(" ").append(getLastName()).
                append(System.lineSeparator()).
                append("Organization: ").append(getOrganization()).
                append(System.lineSeparator()).
                append("Address: ").append(StringUtils.join(new String[] {getAddress(), getCity(), getState(),
                        getPostalCode(), getCountry()}, ", ").replaceAll(" ,", "")).
                append(System.lineSeparator()).
                append("Phone: ").append(getPhone()).
                append(System.lineSeparator()).
                append("Email: ").append(getEmail()).
                append(System.lineSeparator()).
                append("Common Name: ").append(getCommonName()).
                append(System.lineSeparator()).
                append("Genus Species: ").append(getGenus()).append(" ").append(getSpecies()).
                append(System.lineSeparator()).
                append("Irb Approval Required: ").append(String.valueOf(getIrbApprovalRequired())).
                append(System.lineSeparator()).
                toString();
    }

    /** Lookup key of all relevant fields. */
    public interface SampleKitRequestKey {
        public String getFirstName();
        public String getLastName();
        public String getOrganization();
        public String getAddress();
        public String getCity();
        public String getState();
        public String getPostalCode();
        public String getCountry();
        public String getPhone();
        public String getEmail();
        public String getCommonName();
        public String getGenus();
        public String getSpecies();
        public String getIrbApprovalRequired();
    }
}
