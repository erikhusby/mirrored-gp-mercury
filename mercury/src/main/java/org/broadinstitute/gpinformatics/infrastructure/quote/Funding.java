package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.DateAdapter;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

@XmlRootElement(name = "Funding")
public class Funding {

    public static final String FUNDS_RESERVATION = "Funds Reservation";

    public static final String PURCHASE_ORDER = "Purchase Order";

    private String costObject;

    private String broadName;

    private String commonName;

    private String grantDescription;

    private String grantNumber;

    private String grantStatus;

    private String sponsorName;

    private String fundingType;

    private Date grantStartDate;

    private Date grantEndDate;

    private String institute;

    private String purchaseOrderNumber;

    public Funding() {}

    public Funding(String fundingType,
                   String grantDescription) {
        this.fundingType = fundingType;
        this.grantDescription = grantDescription;
    }

    public String getFundingTypeAndName() {
        return FUNDS_RESERVATION.equals(getFundingType()) ? "CO-" + grantNumber : "PO-" + purchaseOrderNumber;
    }

    public String getMatchDescription() {
        return FUNDS_RESERVATION.equals(getFundingType()) ? grantDescription : institute;
    }

    @XmlAttribute(name = "costObject")
    public String getCostObject() {
        return costObject;
    }

    public void setCostObject(String costObject) {
        this.costObject = costObject;
    }

    @XmlAttribute(name = "broadName")
    public String getBroadName() {
        return broadName;
    }

    public void setBroadName(String broadName) {
        this.broadName = broadName;
    }

    @XmlAttribute(name = "commonName")
    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    @XmlAttribute(name = "grantDescription")
    public String getGrantDescription() {
        return grantDescription;
    }

    public void setGrantDescription(String grantDescription) {
        this.grantDescription = grantDescription;
    }

    @XmlAttribute(name = "grantNumber")
    public String getGrantNumber() {
        return grantNumber;
    }

    public void setGrantNumber(String grantNumber) {
        this.grantNumber = grantNumber;
    }

    @XmlAttribute(name = "grantStatus")
    public String getGrantStatus() {
        return grantStatus;
    }

    public void setGrantStatus(String grantStatus) {
        this.grantStatus = grantStatus;
    }

    @XmlAttribute(name = "sponsorName")
    public String getSponsorName() {
        return sponsorName;
    }
    public void setSponsorName(String sponsorName) {
        this.sponsorName = sponsorName;
    }

    @XmlAttribute(name = "fundingType")
    public String getFundingType() {
        return fundingType;
    }

    public void setFundingType(String fundingType) {
        this.fundingType = fundingType;
    }

    @XmlAttribute(name = "grantStartDate")
    @XmlJavaTypeAdapter(DateAdapter.class)
    public Date getGrantStartDate() {
        return grantStartDate;
    }
    public void setGrantStartDate(Date grantStartDate) {
        this.grantStartDate = grantStartDate;
    }

    @XmlAttribute(name = "grantEndDate")
    @XmlJavaTypeAdapter(DateAdapter.class)
    public Date getGrantEndDate() {
        return grantEndDate;
    }
    public void setGrantEndDate(Date grantEndDate) {
        this.grantEndDate = grantEndDate;
    }

    @XmlAttribute(name = "institute")
    public String getInstitute() {
        return institute;
    }

    public void setInstitute(String institute) {
        this.institute = institute;
    }

    @XmlAttribute(name = "purchaseOrderNumber")
    public String getPurchaseOrderNumber() {
        return purchaseOrderNumber;
    }

    public void setPurchaseOrderNumber(String purchaseOrderNumber) {
        this.purchaseOrderNumber = purchaseOrderNumber;
    }

    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) {
            return true;
        }

        if ( !(other instanceof Funding) ) {
            return false;
        }

        Funding castOther = (Funding) other;
        return new EqualsBuilder().append(getCostObject(), castOther.getCostObject())
                                  .append(getPurchaseOrderNumber(), castOther.getPurchaseOrderNumber())
                                  .append(getFundingType(), castOther.getFundingType()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getCommonName()).append(getFundingType()).toHashCode();
    }
}