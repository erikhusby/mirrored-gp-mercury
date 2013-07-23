package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.DateAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

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
        return new EqualsBuilder().append(costObject, castOther.getCostObject())
                                  .append(purchaseOrderNumber, castOther.getPurchaseOrderNumber())
                                  .append(fundingType, castOther.getFundingType()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(costObject).append(purchaseOrderNumber).append(fundingType).toHashCode();
    }

    public static Set<Funding> getFundingSet(Document response) {
        Set<Funding> fundingSet = new TreeSet<Funding>(byDisplayName);

        DateFormat quoteDateFormat = new SimpleDateFormat(QUOTE_SERVER_DATE_STRING);

        NodeList rowNodes = response.getElementsByTagName("rowData");
        for (int i = 0; i < rowNodes.getLength(); i++) {
            Node node = rowNodes.item(i);

            NodeList entries = node.getChildNodes();

            Map<String, String> nameValueAttributes = new HashMap<String, String> (rowNodes.getLength());
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

                fundingSet.add(funding);
            } catch (Exception ex) {
                // Ignoring this one piece of funding
            }
        }

        return fundingSet;
    }

    public static final Comparator<Funding> byDisplayName = new Comparator<Funding>() {
        @Override
        public int compare(Funding o1, Funding o2) {
            return o1.getDisplayName().compareTo(o2.getDisplayName());
        }
    };
}