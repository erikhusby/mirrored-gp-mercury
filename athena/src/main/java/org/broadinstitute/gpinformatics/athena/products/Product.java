package org.broadinstitute.gpinformatics.athena.products;


import org.broadinstitute.gpinformatics.athena.Namespaces;
import org.broadinstitute.gpinformatics.athena.orders.RiskContingency;

import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/28/12
 * Time: 10:26 AM
 */
@XmlType(namespace = Namespaces.PRODUCT_NS)
public class Product implements Serializable {

    private String name;
    private ProductFamily productFamily;
    private String description;
    private String partNumber;
    private Date availabilityDate;
    private Date discontinuedDate;
    private Integer expectedCycleTimeHours;
    private Integer guaranteedCycleTimeHours;
    private Integer samplesPerWeek;
    private List<String> inputRequirements;
    private List<String> deliverables;
    private PriceItem productPrice;
    private String workflowName;
    private List<CoverageModelType> availableCoverageModelTypes;
    private List<AlignerType> availableAlignerTypes;
    private CoverageAndAnalysisInformation selectedCoverageAndAnalysisInformation;
    private GenomicsTechnology genomicsTechnology;
    private List<RiskContingency> riskContingencies;


    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public ProductFamily getProductFamily() {
        return productFamily;
    }

    public void setProductFamily(final ProductFamily productFamily) {
        this.productFamily = productFamily;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(final String partNumber) {
        this.partNumber = partNumber;
    }

    public Date getAvailabilityDate() {
        return availabilityDate;
    }

    public void setAvailabilityDate(final Date availabilityDate) {
        this.availabilityDate = availabilityDate;
    }

    public Date getDiscontinuedDate() {
        return discontinuedDate;
    }

    public void setDiscontinuedDate(final Date discontinuedDate) {
        this.discontinuedDate = discontinuedDate;
    }

    public Integer getExpectedCycleTimeHours() {
        return expectedCycleTimeHours;
    }

    public void setExpectedCycleTimeHours(final Integer expectedCycleTimeHours) {
        this.expectedCycleTimeHours = expectedCycleTimeHours;
    }

    public Integer getGuaranteedCycleTimeHours() {
        return guaranteedCycleTimeHours;
    }

    public void setGuaranteedCycleTimeHours(final Integer guaranteedCycleTimeHours) {
        this.guaranteedCycleTimeHours = guaranteedCycleTimeHours;
    }

    public Integer getSamplesPerWeek() {
        return samplesPerWeek;
    }

    public void setSamplesPerWeek(final Integer samplesPerWeek) {
        this.samplesPerWeek = samplesPerWeek;
    }

    public List<String> getInputRequirements() {
        return inputRequirements;
    }

    public void setInputRequirements(final List<String> inputRequirements) {
        this.inputRequirements = inputRequirements;
    }

    public List<String> getDeliverables() {
        return deliverables;
    }

    public void setDeliverables(final List<String> deliverables) {
        this.deliverables = deliverables;
    }

    public PriceItem getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(final PriceItem productPrice) {
        this.productPrice = productPrice;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(final String workflowName) {
        this.workflowName = workflowName;
    }

    public List<CoverageModelType> getAvailableCoverageModelTypes() {
        return availableCoverageModelTypes;
    }

    public void setAvailableCoverageModelTypes(final List<CoverageModelType> availableCoverageModelTypes) {
        this.availableCoverageModelTypes = availableCoverageModelTypes;
    }

    public List<AlignerType> getAvailableAlignerTypes() {
        return availableAlignerTypes;
    }

    public void setAvailableAlignerTypes(final List<AlignerType> availableAlignerTypes) {
        this.availableAlignerTypes = availableAlignerTypes;
    }

    public CoverageAndAnalysisInformation getSelectedCoverageAndAnalysisInformation() {
        return selectedCoverageAndAnalysisInformation;
    }

    public void setSelectedCoverageAndAnalysisInformation(final CoverageAndAnalysisInformation selectedCoverageAndAnalysisInformation) {
        this.selectedCoverageAndAnalysisInformation = selectedCoverageAndAnalysisInformation;
    }

    public GenomicsTechnology getGenomicsTechnology() {
        return genomicsTechnology;
    }

    public void setGenomicsTechnology(final GenomicsTechnology genomicsTechnology) {
        this.genomicsTechnology = genomicsTechnology;
    }

    public List<RiskContingency> getRiskContingencies() {
        return riskContingencies;
    }

    public void setRiskContingencies(final List<RiskContingency> riskContingencies) {
        this.riskContingencies = riskContingencies;
    }
}
