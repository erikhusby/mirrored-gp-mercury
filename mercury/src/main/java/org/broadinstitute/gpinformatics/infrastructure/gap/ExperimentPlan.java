package org.broadinstitute.gpinformatics.infrastructure.gap;

import org.broadinstitute.gpinformatics.infrastructure.DateAdapter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

@XmlRootElement(name = "experimentPlan")
public class ExperimentPlan {

    private String id;
    private String experimentName;
    private Date dateCreated;
    private String createdBy;
    private Date effectiveDate;
    private String updatedBy;
    private String experimentDescription;
    private String planningStatus;
    private String broadPi;
    private String fundingPi;
    private String platformPm;
    private String researchProjectId;
    private Integer numberSamples;
    private String productName;
    private String notes;
    private String projectName;
    private String groupName;
    private Date projectStartDate;
    private String programPm;
    private String gapQuoteId;
    private String bspQuoteId;
    private Boolean irbEngaged;
    private String irbNumbers;
    private String irbInfo;
    private Date expectedKitReceiptDate;
    private Samples samples;


    @XmlElement(name = "gxpId")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement(name = "experimentName")
    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    @XmlElement(name = "experimentDescription")
    public String getExperimentDescription() {
        return experimentDescription;
    }

    public void setExperimentDescription(String experimentDescription) {
        this.experimentDescription = experimentDescription;
    }

    @XmlElement(name = "planningStatus")
    public String getPlanningStatus() {
        return planningStatus;
    }

    public void setPlanningStatus(String planningStatus) {
        this.planningStatus = planningStatus;
    }

    @XmlElement(name = "broadPi")
    public String getBroadPi() {
        return broadPi;
    }

    public void setBroadPi(String broadPi) {
        this.broadPi = broadPi;
    }

    @XmlElement(name = "fundingPi")
    public String getFundingPi() {
        return fundingPi;
    }

    public void setFundingPi(String fundingPi) {
        this.fundingPi = fundingPi;
    }

    @XmlElement(name = "platformPm")
    public String getPlatformPm() {
        return platformPm;
    }

    public void setPlatformPm(String platformPm) {
        this.platformPm = platformPm;
    }

    @XmlElement(name = "researchProjectId")
    public String getResearchProjectId() {
        return researchProjectId;
    }

    public void setResearchProjectId(String researchProjectId) {
        this.researchProjectId = researchProjectId;
    }

    @XmlElement(name = "numberSamples")
    public Integer getNumberSamples() {
        return numberSamples;
    }

    public void setNumberSamples(Integer numberSamples) {
        this.numberSamples = numberSamples;
    }

    @XmlElement(name = "productName")
    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    @XmlElement(name = "notes")
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @XmlElement(name = "projectName")
    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @XmlElement(name = "groupName")
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @XmlElement(name = "projectStartDate")
    @XmlJavaTypeAdapter(DateAdapter.class)
    public Date getProjectStartDate() {
        return projectStartDate;
    }

    public void setProjectStartDate(Date projectStartDate) {
        this.projectStartDate = projectStartDate;
    }

    @XmlElement(name = "programPm")
    public String getProgramPm() {
        return programPm;
    }

    public void setProgramPm(String programPm) {
        this.programPm = programPm;
    }

    @XmlElement(name = "quoteId")
    public String getGapQuoteId() {
        return gapQuoteId;
    }

    public void setGapQuoteId(String gapQuoteId) {
        this.gapQuoteId = gapQuoteId;
    }

    @XmlElement(name = "bspPlatingQuoteId")
    public String getBspQuoteId() {
        return bspQuoteId;
    }

    public void setBspQuoteId(String bspQuoteId) {
        this.bspQuoteId = bspQuoteId;
    }

    @XmlElement(name = "irbNumber")
    public String getIrbNumbers() {
        return irbNumbers;
    }

    public void setIrbNumbers(String irbNumbers) {
        this.irbNumbers = irbNumbers;
    }

    @XmlElement(name = "irbEngaged")
    public Boolean getIrbEngaged() {
        return irbEngaged;
    }

    public void setIrbEngaged(Boolean irbEngaged) {
        this.irbEngaged = irbEngaged;
    }

    @XmlElement(name = "irbInfo")
    public String getIrbInfo() {
        return irbInfo;
    }

    public void setIrbInfo(String irbInfo) {
        this.irbInfo = irbInfo;
    }

    @XmlElement(name = "expectedKitReceiptDate")
    @XmlJavaTypeAdapter(DateAdapter.class)
    public Date getExpectedKitReceiptDate() {
        return expectedKitReceiptDate;
    }

    public void setExpectedKitReceiptDate(Date expectedKitReceiptDate) {
        this.expectedKitReceiptDate = expectedKitReceiptDate;
    }


    @XmlElement(name = "dateCreated")
    @XmlJavaTypeAdapter(DateAdapter.class)
    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @XmlElement(name = "createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @XmlElement(name = "effectiveDate")
    @XmlJavaTypeAdapter(DateAdapter.class)
    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    @XmlElement(name = "updatedBy")
    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @XmlElement(name = "samples")
    public Samples getSamples() {
        return samples;
    }

    public void setSamples(Samples samples) {
        this.samples = samples;
    }


}
