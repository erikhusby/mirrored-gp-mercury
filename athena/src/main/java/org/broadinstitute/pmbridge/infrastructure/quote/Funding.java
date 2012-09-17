package org.broadinstitute.pmbridge.infrastructure.quote;

import org.broadinstitute.pmbridge.infrastructure.DateAdapter;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Funding funding = (Funding) o;

        if (broadName != null ? !broadName.equals(funding.broadName) : funding.broadName != null) return false;
        if (commonName != null ? !commonName.equals(funding.commonName) : funding.commonName != null) return false;
        if (costObject != null ? !costObject.equals(funding.costObject) : funding.costObject != null) return false;
        if (fundingType != null ? !fundingType.equals(funding.fundingType) : funding.fundingType != null) return false;
        if (grantDescription != null ? !grantDescription.equals(funding.grantDescription) : funding.grantDescription != null)
            return false;
        if (grantNumber != null ? !grantNumber.equals(funding.grantNumber) : funding.grantNumber != null) return false;
        if (grantStatus != null ? !grantStatus.equals(funding.grantStatus) : funding.grantStatus != null) return false;
        if (sponsorName != null ? !sponsorName.equals(funding.sponsorName) : funding.sponsorName != null) return false;
        if (institute != null ? !institute.equals(funding.institute) : funding.institute != null) return false;
        if (purchaseOrderNumber != null ? !purchaseOrderNumber.equals(funding.purchaseOrderNumber) : funding.purchaseOrderNumber != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = costObject != null ? costObject.hashCode() : 0;
        result = 31 * result + (broadName != null ? broadName.hashCode() : 0);
        result = 31 * result + (commonName != null ? commonName.hashCode() : 0);
        result = 31 * result + (grantDescription != null ? grantDescription.hashCode() : 0);
        result = 31 * result + (grantNumber != null ? grantNumber.hashCode() : 0);
        result = 31 * result + (grantStatus != null ? grantStatus.hashCode() : 0);
        result = 31 * result + (sponsorName != null ? sponsorName.hashCode() : 0);
        result = 31 * result + (fundingType != null ? fundingType.hashCode() : 0);
        result = 31 * result + (institute != null ? institute.hashCode() : 0);
        result = 31 * result + (purchaseOrderNumber != null ? purchaseOrderNumber.hashCode() : 0);
        return result;
    }
}
