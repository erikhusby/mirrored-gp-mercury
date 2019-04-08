package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.DateUtils;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.ShortDateAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@XmlRootElement(name = "Funding")
public class Funding implements Displayable {

    private static final String QUOTE_SERVER_DATE_STRING = "yyyy-MM-dd hh:mm:ss.SSS";

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

    private String purchaseOrderContact;

    private String fundsReservationNumber;

    private String platform;

    public Funding() {}

    public Funding(String fundingType, String grantDescription, String costObject) {
        this.fundingType = fundingType;
        this.grantDescription = grantDescription;
        this.costObject = costObject;
    }

    @Override
    public String getDisplayName() {
        return FUNDS_RESERVATION.equals(getFundingType()) ? "CO-" + grantNumber : "PO-" + purchaseOrderNumber;
    }

    public String getCompareName() {
        StringBuilder initialName = new StringBuilder(getDisplayName());

        initialName.append("::").append(platform);
        if(FUNDS_RESERVATION.equals(getFundingType())) {
            initialName.append("Funds Reservation").append(fundsReservationNumber);
        }

        return initialName.toString();
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
    @XmlJavaTypeAdapter(ShortDateAdapter.class)
    public Date getGrantStartDate() {
        return grantStartDate;
    }
    public void setGrantStartDate(Date grantStartDate) {
        this.grantStartDate = grantStartDate;
    }

    @XmlAttribute(name = "grantEndDate")
    @XmlJavaTypeAdapter(ShortDateAdapter.class)
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

    @XmlAttribute(name = "purchaseOrderContact")
    public String getPurchaseOrderContact() { return purchaseOrderContact;}

    public void setPurchaseOrderContact(String purchaseOrderContact) {
        this.purchaseOrderContact = purchaseOrderContact;
    }

    @XmlAttribute(name = "fundsReservationNumber")
    public String getFundsReservationNumber() {return fundsReservationNumber;}

    public void setFundsReservationNumber(String fundsReservationNumber) {
        this.fundsReservationNumber = fundsReservationNumber;
    }

    @XmlAttribute(name = "platform")
    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
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
        return new EqualsBuilder().append(costObject, castOther.getCostObject())
            .append(purchaseOrderNumber, castOther.getPurchaseOrderNumber())
            .append(fundingType, castOther.getFundingType())
            .append(fundsReservationNumber, castOther.getFundsReservationNumber()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(costObject).append(purchaseOrderNumber).append(fundingType)
            .append(fundsReservationNumber).toHashCode();
    }

    public static Set<Funding> getFundingSet(Document response) {
        Set<Funding> fundingSet = new TreeSet<>(byDisplayName);

        DateFormat quoteDateFormat = new SimpleDateFormat(QUOTE_SERVER_DATE_STRING);

        NodeList rowNodes = response.getElementsByTagName("rowData");
        for (int i = 0; i < rowNodes.getLength(); i++) {
            Node node = rowNodes.item(i);

            NodeList entries = node.getChildNodes();

            Map<String, String> nameValueAttributes = new HashMap<>(rowNodes.getLength());
            for (int j = 0; j < entries.getLength(); j++) {
                Node entry = entries.item(j);

                // only get nodes that have children. There should be a name and value node
                if (entry.hasChildNodes()) {
                    NodeList entryNodes = entry.getChildNodes();
                    nameValueAttributes.put(entryNodes.item(1).getTextContent(), entryNodes.item(3).getTextContent());
                }
            }

            // Assign all the values to this new funding
            try {
                Funding funding = new Funding();
                funding.setBroadName(nameValueAttributes.get("BROAD_NAME"));
                funding.setCommonName(nameValueAttributes.get("COMMON_NAME"));
                funding.setCostObject(nameValueAttributes.get("COST_OBJECT_NAME"));
                funding.setGrantDescription(nameValueAttributes.get("GRANT_DESC"));

                String type = nameValueAttributes.get("FUNDING_TYPE").equals("FRS_NUMBER") ? FUNDS_RESERVATION : PURCHASE_ORDER;
                funding.setFundingType(type);

                String dateStr = nameValueAttributes.get("GRANT_END");
                Date date = StringUtils.isBlank(dateStr) ? null : quoteDateFormat.parse(dateStr);
                funding.setGrantEndDate(date);

                funding.setGrantNumber(nameValueAttributes.get("GRANT_NUM"));

                dateStr = nameValueAttributes.get("GRANT_START");
                date = StringUtils.isBlank(dateStr) ? null : quoteDateFormat.parse(dateStr);
                funding.setGrantStartDate(date);

                funding.setGrantStatus(nameValueAttributes.get("GRANT_STATUS"));
                funding.setInstitute(nameValueAttributes.get("NAME"));
                funding.setPurchaseOrderNumber(nameValueAttributes.get("PO_NUMBER"));
                funding.setSponsorName(nameValueAttributes.get("SPONSOR_NAME"));
                funding.setPlatform(nameValueAttributes.get("PLATFORM"));

                fundingSet.add(funding);
            } catch (Exception ex) {
                // Ignoring this one piece of funding
            }
        }

        return fundingSet;
    }

    public boolean isFundsReservation() {
        return StringUtils.isNotBlank(fundingType) && fundingType.equals(Funding.FUNDS_RESERVATION);
    }

    public boolean isPurchaseOrder() {
        return StringUtils.isNotBlank(fundingType) && fundingType.equals(Funding.PURCHASE_ORDER);
    }


    public boolean isGrantActiveForDate(Date effectiveDate) {
        Date relativeDate = DateUtils.truncate(new Date(), Calendar.DATE);

        if(effectiveDate != null && effectiveDate.compareTo(relativeDate) != 0) {
            relativeDate = DateUtils.truncate(effectiveDate, Calendar.DATE);
        }

        return FundingLevel.isGrantActiveForDate(relativeDate, this);
    }

    public static final Comparator<Funding> byDisplayName = new Comparator<Funding>() {
        @Override
        public int compare(Funding o1, Funding o2) {
            return o1.getCompareName().compareTo(o2.getCompareName());
        }
    };
}
