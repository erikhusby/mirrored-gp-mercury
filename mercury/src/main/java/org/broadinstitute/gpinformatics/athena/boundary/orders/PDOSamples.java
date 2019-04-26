package org.broadinstitute.gpinformatics.athena.boundary.orders;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlRootElement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Simple bean class used for looking up pdo/sample information via WS.
 */
@XmlRootElement
public class PDOSamples {

    private List<PDOSample> pdoSamples = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public PDOSamples() {
    }

    public void addPdoSample(@Nonnull String pdoKey, @Nonnull String sampleName,
                             Boolean hasPrimaryPriceItemBeenBilled, Boolean atRisk, Date receiptDate) {
        addPdoSample(pdoKey, sampleName, hasPrimaryPriceItemBeenBilled, atRisk, true, receiptDate);
    }

    public void addPdoSample(@Nonnull String pdoKey, @Nonnull String sampleName,
                             Boolean hasPrimaryPriceItemBeenBilled, Boolean atRisk, boolean isCalculated,
                             Date receiptDate) {
        pdoSamples.add(new PDOSample(pdoKey, sampleName, hasPrimaryPriceItemBeenBilled, atRisk, isCalculated,
                receiptDate));
    }

    public void addError(String errorMessage) {
        errors.add(errorMessage);
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<PDOSample> getPdoSamples() {
        return pdoSamples;
    }

    public void setPdoSamples(List<PDOSample> pdoSamples) {
        this.pdoSamples = pdoSamples;
    }

    /**
     * Converts the list of pdo/sample tuples to a map
     * where the key is the PDO key and the value is
     * a list of sample names for that PDO.
     */
    @DaoFree
    public Map<String, Set<String>> convertPdoSamplePairsListToMap() {
        Map<String, Set<String>> pdoToSamples = new HashMap<>();
        for (PDOSample pdoSample : getPdoSamples()) {
            String pdoKey = pdoSample.getPdoKey();
            if (!pdoToSamples.containsKey(pdoKey)) {
                pdoToSamples.put(pdoSample.getPdoKey(), new HashSet<String>());
            }
            pdoToSamples.get(pdoKey).add(pdoSample.getSampleName());
        }
        return pdoToSamples;
    }

    /**
     * Lines up the PDOSamples passed in
     * from the client with the query results
     * that came from the database.
     *
     * @param pdoSamples the pdo samples that were returned
     *                   from the database.
     *
     * @return a new PDOSamples object that has the right billing
     * status and has error fields set for pdo/sample pairs
     * that were not found.
     *
     * @see ProductOrderSampleDao#findByOrderKeyAndSampleNames(String, java.util.Set)
     */
    @DaoFree
    public PDOSamples buildOutputPDOSamplePairsFromInputAndQueryResults(List<ProductOrderSample> pdoSamples) {
        PDOSamples pdoSamplesResults = new PDOSamples();
        for (PDOSample requestedPdoSample : getPdoSamples()) {
            boolean foundIt = false;
            String requestedPdoKey = requestedPdoSample.getPdoKey();
            String requestedSampleName = requestedPdoSample.getSampleName();
            for (ProductOrderSample pdoSample : pdoSamples) {
                if (requestedPdoKey.equals(pdoSample.getProductOrder().getBusinessKey()) &&
                    requestedSampleName.equals(pdoSample.getName())) {

                    PDOSample pdoSampleBean =
                            new PDOSample(requestedPdoKey, requestedSampleName, pdoSample.isCompletelyBilled(),
                                    pdoSample.isOnRisk(), !pdoSample.getRiskItems().isEmpty(), pdoSample.getReceiptDate());

                    Collection<String> riskCategories = new HashSet<>();
                    Collection<String> riskInformation = new ArrayList<>(pdoSample.getRiskItems().size());
                    if (pdoSample.isOnRisk()) {
                        for (RiskItem riskItem : pdoSample.getRiskItems()) {
                            riskCategories.add(riskItem.getRiskCriterion().getCalculationString());
                            riskInformation.add(riskItem.getInformation());
                        }
                    }

                    pdoSampleBean.setRiskCategories(new ArrayList<>(riskCategories));
                    pdoSampleBean.setRiskInformation(new ArrayList<>(riskInformation));
                    pdoSamplesResults.getPdoSamples().add(pdoSampleBean);
                    foundIt = true;
                }
            }

            if (!foundIt) {
                pdoSamplesResults.addPdoSample(requestedPdoKey, requestedSampleName, null, null, false, null);
                String errorMessage = MessageFormat
                        .format("Could not find sample {0} in PDO {1}.", requestedSampleName, requestedPdoKey);
                pdoSamplesResults.addError(errorMessage);
            }
        }
        return pdoSamplesResults;
    }

    @JsonIgnore
    public Collection<PDOSample> getAtRiskPdoSamples() {
        List<PDOSample> onRiskSamples = new ArrayList<>();
        for (PDOSample pdoSample : getPdoSamples()) {
            if (pdoSample != null && pdoSample.isOnRisk()) {
                onRiskSamples.add(pdoSample);
            }
        }
        return onRiskSamples;
    }
}
