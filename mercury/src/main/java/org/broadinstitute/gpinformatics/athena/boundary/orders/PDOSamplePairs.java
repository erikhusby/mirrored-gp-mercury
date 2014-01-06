package org.broadinstitute.gpinformatics.athena.boundary.orders;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement

/**
 * Simple bean class used for looking up pdo/sample
 * billing information via WS.
 */
public class PDOSamplePairs {

    private List<PDOSamplePair> pdoSamplePairs = new ArrayList<>();

    private List<String> errors = new ArrayList<>();

    public PDOSamplePairs() {}

    public void addPdoSamplePair(@Nonnull String pdoKey,@Nonnull String sampleName,Boolean hasPrimaryPriceItemBeenBilled) {
        pdoSamplePairs.add(new PDOSamplePair(pdoKey,sampleName,hasPrimaryPriceItemBeenBilled));
    }

    public void addError(String errorMessage) {
        errors.add(errorMessage);
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<PDOSamplePair> getPdoSamplePairs() {
        return pdoSamplePairs;
    }

    public void setPdoSamplePairs(List<PDOSamplePair> pdoSamplePairs) {
        this.pdoSamplePairs = pdoSamplePairs;
    }
}
