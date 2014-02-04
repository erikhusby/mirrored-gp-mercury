package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlRootElement;
import java.text.MessageFormat;
import java.util.ArrayList;
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

    public PDOSamples() {}

    public void addPdoSamplePair(@Nonnull String pdoKey,@Nonnull String sampleName,Boolean hasPrimaryPriceItemBeenBilled) {
        pdoSamples.add(new PDOSample(pdoKey,sampleName,hasPrimaryPriceItemBeenBilled));
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
     * @return
     */
    @DaoFree
    public Map<String,Set<String>> convertPdoSamplePairsListToMap() {
        Map<String,Set<String>> pdoToSamples = new HashMap<>();
        for (PDOSample pdoSample : getPdoSamples()) {
            String pdoKey = pdoSample.getPdoKey();
            if (!pdoToSamples.containsKey(pdoKey)) {
                pdoToSamples.put(pdoSample.getPdoKey(),new HashSet<String>());
            }
            pdoToSamples.get(pdoKey).add(pdoSample.getSampleName());
        }
        return pdoToSamples;
    }

    /**
     * Lines up the PDOSamples passed in
     * from the client with the query results
     * that came from the database.
     * @param pdoSamples the pdo samples that were returned
     *                   from the database.
     *                   @see ProductOrderSampleDao#findByOrderKeyAndSampleNames(String, java.util.Set)
     * @return a new PDOSamples object that has the right billing
     * status and has error fields set for pdo/sample pairs
     * that were not found.
     */
    @DaoFree
    public PDOSamples buildOutputPDOSamplePairsFromInputAndQueryResults(List<ProductOrderSample> pdoSamples) {
        PDOSamples pdoSamplesResults = new PDOSamples();
        for (PDOSample requestedPdoSample : getPdoSamples()) {
            boolean foundIt = false;
            String requestedPdoKey = requestedPdoSample.getPdoKey();
            String requestedSampleName = requestedPdoSample.getSampleName();
            for (ProductOrderSample pdoSample : pdoSamples) {
                if (requestedPdoKey.equals(pdoSample.getProductOrder().getBusinessKey()) && requestedSampleName.equals(pdoSample.getName())) {
                    pdoSamplesResults.addPdoSamplePair(requestedPdoKey,requestedSampleName,pdoSample.isCompletelyBilled());
                    foundIt = true;
                }
            }
            if (!foundIt) {
                pdoSamplesResults.addPdoSamplePair(requestedPdoKey,requestedSampleName,null);
                String errorMessage = MessageFormat
                        .format("Could not find sample {0} in PDO {1}.", requestedSampleName, requestedPdoKey);
                pdoSamplesResults.addError(errorMessage);
            }
        }
        return pdoSamplesResults;
    }

    /**
     * Given a list of productOrderSamples calculate the sample risk and return all at-risk samples.
     */
    @DaoFree
    public static PDOSamples findAtRiskPDOSamples(List<ProductOrderSample> productOrderSamples){
        PDOSamples samplesAtRisk = new PDOSamples();
        for (ProductOrderSample productOrderSample : productOrderSamples){
            if (productOrderSample.calculateRisk()){
                PDOSample pdoSample = new PDOSample(productOrderSample.getBusinessKey(), productOrderSample.getName(),
                                                        productOrderSample.isCompletelyBilled());

                samplesAtRisk.addError(productOrderSample.getRiskString());
                samplesAtRisk.getPdoSamples().add(pdoSample);
            }
        }
        return samplesAtRisk;
    }
}
