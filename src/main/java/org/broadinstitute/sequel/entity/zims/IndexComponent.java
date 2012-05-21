package org.broadinstitute.sequel.entity.zims;


import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "IndexComponent")
public class IndexComponent {

    @JsonProperty("hint")
    private String hint;

    @JsonProperty("sequence")
    private String sequence;

    public IndexComponent() {}

    public IndexComponent(IndexPosition thriftPosition,String sequence) {
        
        if (thriftPosition == IndexPosition.A) {
            this.hint = "A";                    
        }
        else if (thriftPosition == IndexPosition.B) {
            this.hint = "B";
        }
        else if (thriftPosition == IndexPosition.DEV) {
            this.hint = "DEV";
        }
        else if (thriftPosition == IndexPosition.INTRA) {
            this.hint = "INTRA";
        }
        else if (thriftPosition == IndexPosition.ONLY) {
            this.hint = "ONLY";
        }
        else if (thriftPosition == IndexPosition.P5) {
            this.hint = "P5";
        }
        else if (thriftPosition == IndexPosition.P7) {
            this.hint = "P7";
        }
        else {
            throw new RuntimeException("SequeL cannot map index position " + thriftPosition);
        }
        this.sequence = sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexComponent that = (IndexComponent) o;

        if (hint != null ? !hint.equals(that.hint) : that.hint != null) return false;
        if (sequence != null ? !sequence.equals(that.sequence) : that.sequence != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = hint != null ? hint.hashCode() : 0;
        result = 31 * result + (sequence != null ? sequence.hashCode() : 0);
        return result;
    }

    public String getSequence() {
        return sequence;
    }

    public String getHint() {
        return hint;
    }
}
