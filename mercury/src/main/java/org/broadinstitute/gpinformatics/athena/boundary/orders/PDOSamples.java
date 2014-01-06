package org.broadinstitute.gpinformatics.athena.boundary.orders;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;


/**
 * Simple bean class used for looking up pdo/sample
 * billing information via WS.
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
}
